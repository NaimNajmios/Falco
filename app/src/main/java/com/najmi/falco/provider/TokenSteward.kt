package com.najmi.falco.provider

import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.domain.repository.IQuotaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenSteward @Inject constructor(
    private val quotaRepository: IQuotaRepository
) {
    private val dailyLimits = mapOf(
        LlmProvider.GROQ to 400_000,
        LlmProvider.GEMINI to 1_500_000,
        LlmProvider.CEREBRAS to 1_000_000,
        LlmProvider.OPENROUTER to 50_000,
        LlmProvider.MISTRAL to 100_000,
        LlmProvider.COHERE to 100_000
    )

    suspend fun recordUsage(provider: LlmProvider, tokensUsed: Int) {
        quotaRepository.incrementTokens(provider, tokensUsed)
        quotaRepository.incrementRequests(provider)
    }

    suspend fun recordRequest(provider: LlmProvider) {
        quotaRepository.incrementRequests(provider)
    }

    suspend fun hasQuota(provider: LlmProvider): Boolean {
        val used = quotaRepository.getTokensUsedToday(provider)
        val limit = dailyLimits[provider] ?: 0
        return used < limit
    }

    suspend fun getQuotaSummary(): Map<LlmProvider, Float> {
        return LlmProvider.values().associateWith { provider ->
            val used = quotaRepository.getTokensUsedToday(provider).toFloat()
            val limit = dailyLimits[provider]?.toFloat() ?: 1f
            (used / limit).coerceIn(0f, 1f)
        }
    }

    suspend fun getRemainingTokens(provider: LlmProvider): Int {
        val used = quotaRepository.getTokensUsedToday(provider)
        val limit = dailyLimits[provider] ?: 0
        return (limit - used).coerceAtLeast(0)
    }

    suspend fun getRequestLimit(provider: LlmProvider): Int {
        return when (provider) {
            LlmProvider.GEMINI -> 1500
            LlmProvider.GROQ -> 5000
            LlmProvider.CEREBRAS -> 10000
            LlmProvider.OPENROUTER -> 5000
            LlmProvider.MISTRAL -> 5000
            LlmProvider.COHERE -> 5000
        }
    }

    suspend fun hasRequestQuota(provider: LlmProvider): Boolean {
        val used = quotaRepository.getRequestsUsedToday(provider)
        val limit = getRequestLimit(provider)
        return used < limit
    }
}
