package com.najmi.falco.data.local

import android.util.Log

object DebugLogger {
    private const val TAG = "FALCO"
    private var isEnabled = false

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isEnabled(): Boolean = isEnabled

    fun d(message: String) {
        if (isEnabled) Log.d(TAG, message)
    }

    fun v(message: String) {
        if (isEnabled) Log.v(TAG, message)
    }

    fun w(message: String) {
        if (isEnabled) Log.w(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (isEnabled) Log.e(TAG, message, throwable)
    }

    fun network(method: String, url: String, status: Int, latencyMs: Long) {
        if (isEnabled) Log.d(TAG, "[NET] $method $url -> $status (${latencyMs}ms)")
    }

    fun llm(provider: String, model: String, tokens: Int, latencyMs: Long) {
        if (isEnabled) Log.d(TAG, "[LLM] $provider/$model: ${tokens}tokens (${latencyMs}ms)")
    }

    fun stage(stage: String, durationMs: Long) {
        if (isEnabled) Log.d(TAG, "[STAGE] $stage completed in ${durationMs}ms")
    }
}
