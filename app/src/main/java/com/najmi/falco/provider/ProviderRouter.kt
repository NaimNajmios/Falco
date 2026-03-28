package com.najmi.falco.provider

import android.util.Log
import com.najmi.falco.data.remote.LlmClient
import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.data.remote.LlmResponse
import com.najmi.falco.di.ApiKeyProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRouter @Inject constructor(
    private val clients: Map<LlmProvider, @JvmSuppressWildcards LlmClient>,
    private val healthTracker: LlmProviderHealthTracker,
    private val apiKeyProvider: ApiKeyProvider
) {
    companion object { private const val TAG = "ProviderRouter" }

    private suspend fun tryProvider(provider: LlmProvider, prompt: String): Result<LlmResponse> {
        val client = clients[provider] ?: return Result.failure(NoSuchElementException("No client for ${provider.name}"))
        if (!healthTracker.isAvailable(provider)) {
            return Result.failure(IllegalStateException("${provider.name} is not available"))
        }
        if (!apiKeyProvider.hasUserKey(provider)) {
            Log.w(TAG, "Provider ${provider.name} has no API key configured")
            return Result.failure(NoSuchElementException("${provider.name} has no API key configured"))
        }

        return try {
            val response = client.chat(prompt)
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Provider ${provider.name} failed: ${e.message}")
            healthTracker.reportError(provider.name)
            Result.failure(e)
        }
    }

    suspend fun routeFor(prompt: String, preferred: LlmProvider = LlmProvider.GROQ): Result<LlmResponse> {
        val result = tryProvider(preferred, prompt)
        if (result.isSuccess) return result

        Log.w(TAG, "${preferred.name} failed: ${result.exceptionOrNull()?.message}, trying fallback...")

        if (preferred != LlmProvider.GEMINI) {
            val geminiResult = tryProvider(LlmProvider.GEMINI, prompt)
            if (geminiResult.isSuccess) {
                Log.i(TAG, "Falling back to GEMINI succeeded")
                return geminiResult
            }
            Log.w(TAG, "GEMINI failed: ${geminiResult.exceptionOrNull()?.message}")
        }

        val fallbacks = LlmProvider.entries.filter { it != preferred && it != LlmProvider.GEMINI }
        for (provider in fallbacks) {
            val fallbackResult = tryProvider(provider, prompt)
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
