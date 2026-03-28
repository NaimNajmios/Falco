package com.najmi.falco.provider

import android.util.Log
import com.najmi.falco.data.remote.LlmClient
import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.data.remote.LlmResponse
import com.najmi.falco.di.ApiKeyProvider
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRouter @Inject constructor(
    private val clients: Map<LlmProvider, @JvmSuppressWildcards LlmClient>,
    private val healthTracker: LlmProviderHealthTracker,
    private val apiKeyProvider: ApiKeyProvider,
    private val tokenSteward: TokenSteward
) {
    companion object { 
        private const val TAG = "ProviderRouter"
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 2000L
    }

    private val rateLimitedProviders = mutableMapOf<LlmProvider, Long>()
    private val rateLimitCooldownMs = 60_000L

    private fun isRateLimited(provider: LlmProvider): Boolean {
        val lastRateLimited = rateLimitedProviders[provider] ?: return false
        return System.currentTimeMillis() - lastRateLimited < rateLimitCooldownMs
    }

    private suspend fun tryProvider(provider: LlmProvider, prompt: String): Result<LlmResponse> {
        val client = clients[provider] ?: return Result.failure(NoSuchElementException("No client for ${provider.name}"))
        
        if (!healthTracker.isAvailable(provider)) {
            return Result.failure(IllegalStateException("${provider.name} is marked unhealthy"))
        }

        if (isRateLimited(provider)) {
            return Result.failure(IllegalStateException("${provider.name} is rate limited, skipping temporarily"))
        }
        
        if (!apiKeyProvider.hasUserKey(provider)) {
            Log.w(TAG, "Provider ${provider.name} has no API key configured")
            return Result.failure(NoSuchElementException("${provider.name} has no API key configured"))
        }

        if (!client.canMakeRequest()) {
            val remaining = tokenSteward.getRemainingTokens(provider)
            Log.w(TAG, "${provider.name} quota exceeded, remaining tokens: $remaining")
            return Result.failure(RateLimitException(provider.name, "Quota exceeded for ${provider.name}"))
        }

        return try {
            val response = client.chat(prompt)
            tokenSteward.recordUsage(provider, response.usage.totalTokens)
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Provider ${provider.name} failed: ${e.message}")
            val isHttp429 = e.message?.contains("429", ignoreCase = true) == true ||
                e.message?.contains("rate limit", ignoreCase = true) == true
            val isHttp400 = e.message?.contains("400", ignoreCase = true) == true ||
                e.message?.contains("Bad Request", ignoreCase = true) == true
            val isModelError = (e.message?.contains("model", ignoreCase = true) == true && 
                e.message?.contains("not found", ignoreCase = true) == true) ||
                e.message?.contains("does not exist", ignoreCase = true) == true ||
                e.message?.contains("Invalid model", ignoreCase = true) == true
            val isPaymentError = e.message?.contains("credits", ignoreCase = true) == true ||
                e.message?.contains("402", ignoreCase = true) == true
            
            if (isHttp429) {
                rateLimitedProviders[provider] = System.currentTimeMillis()
                Log.w(TAG, "${provider.name} got 429, marking as temporarily unavailable for ${rateLimitCooldownMs/1000}s")
            } else if (isModelError || isPaymentError || isHttp400) {
                healthTracker.reportError(provider.name)
                Log.w(TAG, "${provider.name} got permanent error (model/payment/400), marking unhealthy")
            }
            Result.failure(e)
        }
    }

    private suspend fun tryProviderWithRetry(provider: LlmProvider, prompt: String): Result<LlmResponse> {
        for (attempt in 0..MAX_RETRIES) {
            val result = tryProvider(provider, prompt)
            if (result.isSuccess) return result
            
            val error = result.exceptionOrNull()
            val isRateLimit = error is RateLimitException || 
                error?.message?.contains("rate limit", ignoreCase = true) == true ||
                error?.message?.contains("429", ignoreCase = true) == true
            
            if (isRateLimit && attempt < MAX_RETRIES) {
                val delayMs = RETRY_DELAY_MS * (attempt + 1)
                Log.w(TAG, "Rate limited on ${provider.name}, retrying in ${delayMs}ms (attempt ${attempt + 1}/${MAX_RETRIES})")
                delay(delayMs)
            } else {
                return result
            }
        }
        return tryProvider(provider, prompt)
    }

    suspend fun routeFor(prompt: String, preferred: LlmProvider = LlmProvider.GROQ): Result<LlmResponse> {
        val result = tryProviderWithRetry(preferred, prompt)
        if (result.isSuccess) return result

        Log.w(TAG, "${preferred.name} failed: ${result.exceptionOrNull()?.message}, trying fallback...")

        if (preferred != LlmProvider.GEMINI) {
            val geminiResult = tryProviderWithRetry(LlmProvider.GEMINI, prompt)
            if (geminiResult.isSuccess) {
                Log.i(TAG, "Falling back to GEMINI succeeded")
                return geminiResult
            }
            Log.w(TAG, "GEMINI failed: ${geminiResult.exceptionOrNull()?.message}")
        }

        val fallbacks = LlmProvider.entries.filter { 
            it != preferred && it != LlmProvider.GEMINI && !isRateLimited(it) 
        }
        for (provider in fallbacks) {
            val fallbackResult = tryProviderWithRetry(provider, prompt)
            if (fallbackResult.isSuccess) {
                Log.i(TAG, "Fallback ${provider.name} succeeded")
                return fallbackResult
            }
            Log.w(TAG, "Fallback ${provider.name} failed: ${fallbackResult.exceptionOrNull()?.message}")
        }

        val failedProviders = LlmProvider.entries.filter { apiKeyProvider.hasUserKey(it) }
        Log.e(TAG, "All ${failedProviders.size} configured providers failed")
        return Result.failure(AllProvidersFailedException())
    }
}
