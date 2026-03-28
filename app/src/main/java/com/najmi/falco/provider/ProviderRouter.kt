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

    private suspend fun tryProvider(provider: LlmProvider, prompt: String): LlmResponse? {
        val client = clients[provider] ?: return null
        if (!healthTracker.isAvailable(provider)) return null
        if (!apiKeyProvider.hasUserKey(provider)) {
            Log.w(TAG, "Provider ${provider.name} has no API key configured")
            return null
        }

        return try {
            client.chat(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Provider ${provider.name} failed: ${e.message}")
            healthTracker.reportError(provider.name)
            null
        }
    }

    suspend fun routeFor(prompt: String, preferred: LlmProvider = LlmProvider.GROQ): LlmResponse {
        val result = tryProvider(preferred, prompt)
        if (result != null) return result

        if (preferred != LlmProvider.GEMINI) {
            val geminiResult = tryProvider(LlmProvider.GEMINI, prompt)
            if (geminiResult != null) {
                Log.i(TAG, "Falling back to GEMINI")
                return geminiResult
            }
        }

        val fallbacks = LlmProvider.entries.filter { it != preferred && it != LlmProvider.GEMINI }
        for (provider in fallbacks) {
            val result = tryProvider(provider, prompt)
            if (result != null) {
                Log.i(TAG, "Using fallback: ${provider.name}")
                return result
            }
        }

        throw Exception("All LLM providers failed, unavailable, or have no API key configured. Please add your keys in Settings.")
    }
}
