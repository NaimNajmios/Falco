package com.najmi.falco.provider

import android.util.Log
import com.najmi.falco.data.remote.LlmClient
import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.data.remote.LlmResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRouter @Inject constructor(
    private val clients: Map<LlmProvider, @JvmSuppressWildcards LlmClient>,
    private val healthTracker: LlmProviderHealthTracker
) {
    companion object { private const val TAG = "ProviderRouter" }

    suspend fun routeFor(prompt: String, preferred: LlmProvider = LlmProvider.GROQ): LlmResponse {
        val primary = clients[preferred]
        if (primary != null && healthTracker.isAvailable(preferred)) {
            try {
                return primary.chat(prompt)
            } catch (e: Exception) {
                Log.e(TAG, "Primary provider ($preferred) failed: ${e.message}")
                healthTracker.reportError(preferred.name)
            }
        }

        if (preferred != LlmProvider.GEMINI) {
            val gemini = clients[LlmProvider.GEMINI]
            if (gemini != null && healthTracker.isAvailable(LlmProvider.GEMINI)) {
                try {
                    Log.i(TAG, "Falling back to GEMINI")
                    return gemini.chat(prompt)
                } catch (e: Exception) {
                    Log.e(TAG, "Fallback GEMINI failed: ${e.message}")
                    healthTracker.reportError(LlmProvider.GEMINI.name)
                }
            }
        }

        val fallbacks = LlmProvider.values().filter { it != preferred && it != LlmProvider.GEMINI }
        for (provider in fallbacks) {
            val client = clients[provider]
            if (client != null && healthTracker.isAvailable(provider)) {
                try {
                    Log.i(TAG, "Trying fallback: ${provider.name}")
                    return client.chat(prompt)
                } catch (e: Exception) {
                    Log.e(TAG, "Fallback ${provider.name} failed: ${e.message}")
                    healthTracker.reportError(provider.name)
                }
            }
        }

        throw Exception("All LLM providers failed or are unhealthy. Pipeline stalled.")
    }
}
