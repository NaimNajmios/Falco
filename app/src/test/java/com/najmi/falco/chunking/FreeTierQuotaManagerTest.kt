package com.najmi.falco.chunking

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.najmi.falco.data.remote.LlmProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.quotaDataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_chunking_quota_test")

@Singleton
class TestableQuotaManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class ProviderLimits(
        val dailyRequests: Int,
        val rpm: Int,
        val dailyTokens: Int
    )

    data class ProviderQuota(
        val provider: LlmProvider,
        val requestsUsedToday: Int,
        val requestsLimit: Int,
        val tokensUsedToday: Int,
        val tokensLimit: Int,
        val rpmUsed: Int,
        val rpmLimit: Int
    )

    data class QuotaStatus(
        val available: Boolean,
        val provider: LlmProvider,
        val reason: String?
    )

    companion object {
        private val providerLimits = mapOf(
            LlmProvider.GROQ to ProviderLimits(1000, 20, 4_000_000),
            LlmProvider.CEREBRAS to ProviderLimits(14_400, 30, 10_000_000),
            LlmProvider.GEMINI to ProviderLimits(1500, 60, 1_500_000),
            LlmProvider.OPENROUTER to ProviderLimits(200, 20, 500_000),
            LlmProvider.MISTRAL to ProviderLimits(500, 10, 100_000),
            LlmProvider.COHERE to ProviderLimits(500, 10, 100_000)
        )

        private const val RPM_WINDOW_MS = 60_000L

        private fun requestsKey(provider: LlmProvider) = intPreferencesKey("${provider.name}_requests")
        private fun tokensKey(provider: LlmProvider) = intPreferencesKey("${provider.name}_tokens")
        private fun rpmKey(provider: LlmProvider) = intPreferencesKey("${provider.name}_rpm")
    }

    suspend fun hasQuota(provider: LlmProvider): QuotaStatus {
        val limits = providerLimits[provider] ?: return QuotaStatus(false, provider, "Unknown provider")
        
        val prefs = context.quotaDataStore.data.first()
        val requestsUsed = prefs[requestsKey(provider)] ?: 0
        val tokensUsed = prefs[tokensKey(provider)] ?: 0
        val rpmUsed = prefs[rpmKey(provider)] ?: 0

        if (requestsUsed >= limits.dailyRequests) {
            return QuotaStatus(false, provider, "Daily request limit reached")
        }
        if (tokensUsed >= limits.dailyTokens) {
            return QuotaStatus(false, provider, "Daily token limit reached")
        }
        if (rpmUsed >= limits.rpm) {
            return QuotaStatus(false, provider, "Rate limit exceeded (${limits.rpm}/min)")
        }
        return QuotaStatus(true, provider, null)
    }

    suspend fun consumeQuota(provider: LlmProvider, tokensUsed: Int) {
        context.quotaDataStore.edit { prefs ->
            prefs[requestsKey(provider)] = (prefs[requestsKey(provider)] ?: 0) + 1
            prefs[tokensKey(provider)] = (prefs[tokensKey(provider)] ?: 0) + tokensUsed
            prefs[rpmKey(provider)] = (prefs[rpmKey(provider)] ?: 0) + 1
        }
    }

    suspend fun getQuota(provider: LlmProvider): ProviderQuota {
        val limits = providerLimits[provider] ?: return ProviderQuota(provider, 0, 0, 0, 0, 0, 0)
        
        val prefs = context.quotaDataStore.data.first()
        return ProviderQuota(
            provider = provider,
            requestsUsedToday = prefs[requestsKey(provider)] ?: 0,
            requestsLimit = limits.dailyRequests,
            tokensUsedToday = prefs[tokensKey(provider)] ?: 0,
            tokensLimit = limits.dailyTokens,
            rpmUsed = prefs[rpmKey(provider)] ?: 0,
            rpmLimit = limits.rpm
        )
    }

    suspend fun resetAll() {
        context.quotaDataStore.edit { it.clear() }
    }
}

class FreeTierQuotaManagerTest {

    private lateinit var manager: TestableQuotaManager

    suspend fun hasQuota_returnsAvailableForGroqWithinLimits() {
        val result = manager.hasQuota(LlmProvider.GROQ)
        assertTrue(result.available)
        assertNull(result.reason)
    }

    suspend fun hasQuota_returnsExhaustedWhenDailyRequestsExceeded() {
        manager.consumeQuota(LlmProvider.GROQ, 0)
        for (i in 0 until 999) {
            manager.consumeQuota(LlmProvider.GROQ, 0)
        }
        val result = manager.hasQuota(LlmProvider.GROQ)
        assertFalse(result.available)
        assertTrue(result.reason?.contains("Daily request limit") == true)
    }

    suspend fun hasQuota_returnsExhaustedWhenRpmExceeded() {
        for (i in 0 until 19) {
            manager.consumeQuota(LlmProvider.GROQ, 0)
        }
        val result = manager.hasQuota(LlmProvider.GROQ)
        assertFalse(result.available)
        assertTrue(result.reason?.contains("Rate limit exceeded") == true)
    }

    suspend fun consumeQuota_incrementsCounters() {
        val before = manager.getQuota(LlmProvider.GROQ)
        manager.consumeQuota(LlmProvider.GROQ, 500)
        val after = manager.getQuota(LlmProvider.GROQ)
        assertEquals(before.requestsUsedToday + 1, after.requestsUsedToday)
        assertEquals(before.tokensUsedToday + 500, after.tokensUsedToday)
    }

    suspend fun getQuota_returnsCorrectLimits() {
        val quota = manager.getQuota(LlmProvider.GROQ)
        assertEquals(1000, quota.requestsLimit)
        assertEquals(20, quota.rpmLimit)
    }

    private fun assertTrue(condition: Boolean) {
        if (!condition) throw AssertionError("Expected true but was false")
    }

    private fun assertFalse(condition: Boolean) {
        if (condition) throw AssertionError("Expected false but was true")
    }

    private fun assertNull(value: Any?) {
        if (value != null) throw AssertionError("Expected null but was $value")
    }

    private fun assertEquals(expected: Any?, actual: Any?) {
        if (expected != actual) throw AssertionError("Expected $expected but was $actual")
    }
}
