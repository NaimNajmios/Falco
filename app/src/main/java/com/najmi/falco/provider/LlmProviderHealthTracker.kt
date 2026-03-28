package com.najmi.falco.provider

import android.util.Log
import com.najmi.falco.data.remote.LlmProvider
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmProviderHealthTracker @Inject constructor() {
    private val providerErrors = ConcurrentHashMap<String, MutableList<Long>>()

    companion object {
        private const val ERROR_THRESHOLD = 3
        private const val HEALTH_WINDOW_MS = 300_000L
        private const val TAG = "LlmHealthTracker"
    }

    fun reportError(providerId: String) {
        val now = System.currentTimeMillis()
        val errors = providerErrors.getOrPut(providerId) { mutableListOf() }
        synchronized(errors) {
            errors.add(now)
            errors.removeIf { it < now - HEALTH_WINDOW_MS }
        }
        Log.w(TAG, "Reported error for $providerId. Total in window: ${errors.size}")
    }

    fun isHealthy(providerId: String): Boolean {
        val now = System.currentTimeMillis()
        val errors = providerErrors[providerId] ?: return true
        return synchronized(errors) {
            errors.removeIf { it < now - HEALTH_WINDOW_MS }
            errors.size < ERROR_THRESHOLD
        }
    }

    fun isAvailable(provider: LlmProvider): Boolean = isHealthy(provider.name)
}
