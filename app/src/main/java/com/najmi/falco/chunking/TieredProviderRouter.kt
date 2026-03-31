package com.najmi.falco.chunking

import android.util.Log
import com.najmi.falco.data.remote.LlmClient
import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.data.remote.LlmResponse
import com.najmi.falco.di.ApiKeyProvider
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TieredProviderRouter @Inject constructor(
    private val clients: Map<LlmProvider, @JvmSuppressWildcards LlmClient>,
    private val quotaManager: FreeTierQuotaManager,
    private val apiKeyProvider: ApiKeyProvider
) {
    companion object {
        private const val TAG = "TieredProviderRouter"
        
        private const val GROQ_MAX_TOKENS = 4000
        private const val CEREBRAS_MAX_TOKENS = 64000
        private const val GEMINI_MAX_TOKENS = 128000
        
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 2000L
        private const val FALLBACK_TIMEOUT_MS = 3000L
    }

    data class RouteResult(
        val provider: LlmProvider,
        val response: LlmResponse,
        val usedFallback: Boolean = false
    )

    data class RouteConfig(
        val preferFastProvider: Boolean = true,
        val allowFallback: Boolean = true,
        val maxFallbackTimeMs: Long = FALLBACK_TIMEOUT_MS
    )

    fun selectProvider(tokenCount: Int): LlmProvider {
        return when {
            tokenCount <= GROQ_MAX_TOKENS -> LlmProvider.ROUTEWAY
            tokenCount <= CEREBRAS_MAX_TOKENS -> LlmProvider.ROUTEWAY
            tokenCount <= GEMINI_MAX_TOKENS -> LlmProvider.CEREBRAS
            else -> LlmProvider.CEREBRAS
        }
    }

    fun getProviderOrder(tokenCount: Int): List<LlmProvider> {
        val primary = selectProvider(tokenCount)
        val allProviders = listOf(
            LlmProvider.ROUTEWAY,
            LlmProvider.CEREBRAS,
            LlmProvider.GROQ,
            LlmProvider.GEMINI,
            LlmProvider.OPENROUTER
        )
        
        val ordered = allProviders.filter { it == primary }.toMutableList()
        ordered.addAll(allProviders.filter { it != primary })
        
        return ordered
    }

    suspend fun routeFor(
        prompt: String,
        tokenCount: Int,
        config: RouteConfig = RouteConfig()
    ): Result<RouteResult> {
        val providers = getProviderOrder(tokenCount)
        
        for (provider in providers) {
            if (!apiKeyProvider.hasUserKey(provider)) {
                Log.w(TAG, "Provider ${provider.name} has no API key configured")
                continue
            }

            val quotaStatus = quotaManager.hasQuota(provider)
            if (!quotaStatus.available) {
                Log.w(TAG, "Provider ${provider.name} quota unavailable: ${quotaStatus.reason}")
                continue
            }

            val result = tryProvider(provider, prompt)
            if (result.isSuccess) {
                return Result.success(RouteResult(
                    provider = provider,
                    response = result.getOrThrow(),
                    usedFallback = provider != selectProvider(tokenCount)
                ))
            }

            val error = result.exceptionOrNull()
            Log.w(TAG, "Provider ${provider.name} failed: ${error?.message}")

            if (isProviderExhaustedError(error)) {
                quotaManager.markExhausted(provider, error?.message ?: "Provider exhausted")
                continue
            }

            if (!config.allowFallback) {
                return result.map { RouteResult(provider, it, false) }
            }
        }

        return Result.failure(
            AllChunkingProvidersFailedException(
                "All providers failed for token count: $tokenCount"
            )
        )
    }

    suspend fun routeWithFastFallback(
        prompt: String,
        tokenCount: Int
    ): Result<RouteResult> {
        val primary = selectProvider(tokenCount)
        
        if (!apiKeyProvider.hasUserKey(primary)) {
            Log.w(TAG, "Primary ${primary.name} has no API key, trying fallbacks")
            return routeFor(prompt, tokenCount)
        }

        val primaryQuota = quotaManager.hasQuota(primary)
        if (!primaryQuota.available) {
            Log.w(TAG, "Primary ${primary.name} quota unavailable, trying fallbacks")
            return routeFor(prompt, tokenCount)
        }

        val primaryResult = tryProviderWithRetry(primary, prompt, MAX_RETRIES)
        if (primaryResult.isSuccess) {
            return Result.success(RouteResult(
                provider = primary,
                response = primaryResult.getOrThrow(),
                usedFallback = false
            ))
        }

        Log.w(TAG, "Primary ${primary.name} failed, trying Cerebras directly")
        val cerebrasQuota = quotaManager.hasQuota(LlmProvider.CEREBRAS)
        if (!cerebrasQuota.available) {
            Log.w(TAG, "Cerebras quota exhausted, skipping fast fallback")
            return routeFor(prompt, tokenCount)
        }

        val cerebrasResult = tryProviderWithRetry(LlmProvider.CEREBRAS, prompt, 1)
        if (cerebrasResult.isSuccess) {
            return Result.success(RouteResult(
                provider = LlmProvider.CEREBRAS,
                response = cerebrasResult.getOrThrow(),
                usedFallback = true
            ))
        }

        Log.w(TAG, "Cerebras also failed, trying all providers")
        return routeFor(prompt, tokenCount)
    }

    private suspend fun tryProvider(provider: LlmProvider, prompt: String): Result<LlmResponse> {
        val client = clients[provider] ?: return Result.failure(
            NoSuchElementException("No client for ${provider.name}")
        )

        return try {
            val response = client.chat(prompt)
            quotaManager.consumeQuota(provider, response.usage.totalTokens)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun tryProviderWithRetry(
        provider: LlmProvider,
        prompt: String,
        maxRetries: Int
    ): Result<LlmResponse> {
        for (attempt in 0..maxRetries) {
            val result = tryProvider(provider, prompt)
            if (result.isSuccess) return result

            val error = result.exceptionOrNull()
            if (isProviderExhaustedError(error)) {
                quotaManager.markExhausted(provider, error?.message ?: "Provider exhausted")
                return result
            }

            if (attempt < maxRetries) {
                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        return tryProvider(provider, prompt)
    }

    private fun isProviderExhaustedError(error: Throwable?): Boolean {
        if (error == null) return false
        val message = error.message ?: ""
        return message.contains("429", ignoreCase = true) ||
                message.contains("rate limit", ignoreCase = true) ||
                message.contains("Too Many Requests", ignoreCase = true) ||
                message.contains("402", ignoreCase = true) ||
                message.contains("404", ignoreCase = true) ||
                message.contains("does not exist", ignoreCase = true) ||
                message.contains("Insufficient credits", ignoreCase = true) ||
                message.contains("Insufficient balance", ignoreCase = true)
    }

    fun getProviderTier(provider: LlmProvider): String {
        return when (provider) {
            LlmProvider.GROQ -> "Fast (Llama 3.3 70B)"
            LlmProvider.CEREBRAS -> "Accurate (GPT-OSS 120B)"
            LlmProvider.GEMINI -> "Long Context (Gemini 2.5 Flash)"
            LlmProvider.OPENROUTER -> "Fallback (NVIDIA Nemotron)"
            else -> "Unknown"
        }
    }

    fun getRecommendedProvider(tokenCount: Int): LlmProvider {
        val provider = selectProvider(tokenCount)
        val limits = quotaManager.getLimitsForProvider(provider)
        
        return if (limits != null) {
            provider
        } else {
            LlmProvider.GROQ
        }
    }
}

class AllChunkingProvidersFailedException(message: String) : Exception(message)
