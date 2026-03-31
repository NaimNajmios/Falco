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
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val Context.quotaDataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_chunking_quota")

@Singleton
class FreeTierQuotaManager @Inject constructor(
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
        val rpmLimit: Int,
        val isExhausted: Boolean,
        val exhaustedReason: String?
    )

    data class QuotaStatus(
        val available: Boolean,
        val provider: LlmProvider,
        val reason: String?
    )

    companion object {
        private val providerLimits = mapOf(
            LlmProvider.GROQ to ProviderLimits(
                dailyRequests = 1000,
                rpm = 20,
                dailyTokens = 4_000_000
            ),
            LlmProvider.CEREBRAS to ProviderLimits(
                dailyRequests = 14_400,
                rpm = 30,
                dailyTokens = 10_000_000
            ),
            LlmProvider.GEMINI to ProviderLimits(
                dailyRequests = 1500,
                rpm = 60,
                dailyTokens = 1_500_000
            ),
            LlmProvider.OPENROUTER to ProviderLimits(
                dailyRequests = 200,
                rpm = 20,
                dailyTokens = 500_000
            ),
            LlmProvider.MISTRAL to ProviderLimits(
                dailyRequests = 500,
                rpm = 10,
                dailyTokens = 100_000
            ),
            LlmProvider.COHERE to ProviderLimits(
                dailyRequests = 500,
                rpm = 10,
                dailyTokens = 100_000
            ),
            LlmProvider.ROUTEWAY to ProviderLimits(
                dailyRequests = 180,
                rpm = 15,
                dailyTokens = 0
            )
        )

        private const val RPM_WINDOW_MS = 60_000L

        private fun requestsKey(provider: LlmProvider) = 
            intPreferencesKey("${provider.name}_requests_today")
        private fun tokensKey(provider: LlmProvider) = 
            intPreferencesKey("${provider.name}_tokens_today")
        private fun rpmKey(provider: LlmProvider) = 
            intPreferencesKey("${provider.name}_rpm")
        private fun rpmResetKey(provider: LlmProvider) = 
            longPreferencesKey("${provider.name}_rpm_reset")
        private fun exhaustedKey(provider: LlmProvider) = 
            intPreferencesKey("${provider.name}_exhausted")
        private fun exhaustedReasonKey(provider: LlmProvider) = 
            intPreferencesKey("${provider.name}_exhausted_reason")
        private fun lastDateKey(provider: LlmProvider) = 
            intPreferencesKey("${provider.name}_last_date")
    }

    private val exhaustedReasons = arrayOf(
        "Daily request limit reached",
        "Rate limit exceeded (RPM)",
        "Daily token limit reached",
        "Provider marked unhealthy"
    )

    suspend fun hasQuota(provider: LlmProvider): QuotaStatus {
        val limits = providerLimits[provider] ?: return QuotaStatus(
            available = false,
            provider = provider,
            reason = "Unknown provider"
        )

        checkAndResetRpm(provider)
        awaitNewDayReset(provider)

        val prefs = context.quotaDataStore.data.first()
        val requestsUsed = prefs[requestsKey(provider)] ?: 0
        val tokensUsed = prefs[tokensKey(provider)] ?: 0
        val rpmUsed = prefs[rpmKey(provider)] ?: 0
        val isExhausted = (prefs[exhaustedKey(provider)] ?: 0) == 1
        val exhaustedReasonIdx = prefs[exhaustedReasonKey(provider)] ?: 0

        if (isExhausted) {
            return QuotaStatus(
                available = false,
                provider = provider,
                reason = exhaustedReasons.getOrElse(exhaustedReasonIdx) { "Provider exhausted" }
            )
        }

        if (requestsUsed >= limits.dailyRequests) {
            return QuotaStatus(
                available = false,
                provider = provider,
                reason = "Daily request limit reached (${limits.dailyRequests})"
            )
        }

        if (tokensUsed >= limits.dailyTokens) {
            return QuotaStatus(
                available = false,
                provider = provider,
                reason = "Daily token limit reached"
            )
        }

        if (rpmUsed >= limits.rpm) {
            return QuotaStatus(
                available = false,
                provider = provider,
                reason = "Rate limit exceeded (${limits.rpm}/min)"
            )
        }

        return QuotaStatus(available = true, provider = provider, reason = null)
    }

    suspend fun consumeQuota(provider: LlmProvider, tokensUsed: Int) {
        val limits = providerLimits[provider] ?: return

        context.quotaDataStore.edit { prefs ->
            val currentDate = LocalDate.now().dayOfYear
            val lastDate = prefs[lastDateKey(provider)] ?: 0
            
            if (lastDate != currentDate) {
                prefs[requestsKey(provider)] = 0
                prefs[tokensKey(provider)] = 0
                prefs[lastDateKey(provider)] = currentDate
            }

            prefs[requestsKey(provider)] = (prefs[requestsKey(provider)] ?: 0) + 1
            prefs[tokensKey(provider)] = (prefs[tokensKey(provider)] ?: 0) + tokensUsed
            
            val currentRpm = prefs[rpmKey(provider)] ?: 0
            prefs[rpmKey(provider)] = currentRpm + 1
            
            prefs[exhaustedKey(provider)] = 0
        }
    }

    suspend fun markExhausted(provider: LlmProvider, reason: String) {
        val reasonIdx = exhaustedReasons.indexOfFirst { reason.contains(it.split(" ").first()) }
            .coerceAtLeast(0)

        context.quotaDataStore.edit { prefs ->
            prefs[exhaustedKey(provider)] = 1
            prefs[exhaustedReasonKey(provider)] = reasonIdx
        }
    }

    suspend fun clearExhausted(provider: LlmProvider) {
        context.quotaDataStore.edit { prefs ->
            prefs[exhaustedKey(provider)] = 0
            prefs[exhaustedReasonKey(provider)] = 0
        }
    }

    suspend fun getQuota(provider: LlmProvider): ProviderQuota {
        val limits = providerLimits[provider] ?: return ProviderQuota(
            provider = provider,
            requestsUsedToday = 0,
            requestsLimit = 0,
            tokensUsedToday = 0,
            tokensLimit = 0,
            rpmUsed = 0,
            rpmLimit = 0,
            isExhausted = false,
            exhaustedReason = null
        )

        checkAndResetRpm(provider)
        awaitNewDayReset(provider)

        val prefs = context.quotaDataStore.data.first()
        
        return ProviderQuota(
            provider = provider,
            requestsUsedToday = prefs[requestsKey(provider)] ?: 0,
            requestsLimit = limits.dailyRequests,
            tokensUsedToday = prefs[tokensKey(provider)] ?: 0,
            tokensLimit = limits.dailyTokens,
            rpmUsed = prefs[rpmKey(provider)] ?: 0,
            rpmLimit = limits.rpm,
            isExhausted = (prefs[exhaustedKey(provider)] ?: 0) == 1,
            exhaustedReason = if ((prefs[exhaustedKey(provider)] ?: 0) == 1) {
                exhaustedReasons.getOrElse(prefs[exhaustedReasonKey(provider)] ?: 0) { "Unknown" }
            } else null
        )
    }

    fun getQuotaFlow(provider: LlmProvider): Flow<ProviderQuota> {
        return context.quotaDataStore.data.map {
            kotlinx.coroutines.runBlocking {
                getQuota(provider)
            }
        }
    }

    suspend fun getAllQuotas(): Map<LlmProvider, ProviderQuota> {
        return LlmProvider.entries.associateWith { provider ->
            getQuota(provider)
        }
    }

    suspend fun resetAll() {
        context.quotaDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private suspend fun checkAndResetRpm(provider: LlmProvider) {
        val prefs = context.quotaDataStore.data.first()
        val lastReset = prefs[rpmResetKey(provider)] ?: 0
        val now = System.currentTimeMillis()

        if (now - lastReset >= RPM_WINDOW_MS) {
            context.quotaDataStore.edit { p ->
                p[rpmKey(provider)] = 0
                p[rpmResetKey(provider)] = now
            }
        }
    }

    private suspend fun awaitNewDayReset(provider: LlmProvider) {
        val currentDate = LocalDate.now().dayOfYear
        val prefs = context.quotaDataStore.data.first()
        val lastDate = prefs[lastDateKey(provider)] ?: 0

        if (lastDate != currentDate && lastDate != 0) {
            context.quotaDataStore.edit { p ->
                p[requestsKey(provider)] = 0
                p[tokensKey(provider)] = 0
                p[lastDateKey(provider)] = currentDate
                p[exhaustedKey(provider)] = 0
            }
        } else if (lastDate == 0) {
            context.quotaDataStore.edit { p ->
                p[lastDateKey(provider)] = currentDate
            }
        }
    }

    fun getLimitsForProvider(provider: LlmProvider): ProviderLimits? = providerLimits[provider]

    fun getBestAvailableProvider(tokenCount: Int): LlmProvider? {
        val orderedProviders = listOf(
            LlmProvider.GROQ,
            LlmProvider.CEREBRAS,
            LlmProvider.GEMINI,
            LlmProvider.OPENROUTER,
            LlmProvider.MISTRAL,
            LlmProvider.COHERE,
            LlmProvider.ROUTEWAY
        )

        return orderedProviders.firstOrNull { provider ->
            kotlinx.coroutines.runBlocking {
                hasQuota(provider).available
            }
        }
    }
}
