package com.najmi.falco.domain.repository

import com.najmi.falco.data.remote.LlmProvider

interface IQuotaRepository {
    suspend fun getTokensUsedToday(provider: LlmProvider): Int
    suspend fun getRequestsUsedToday(provider: LlmProvider): Int
    suspend fun incrementTokens(provider: LlmProvider, tokens: Int)
    suspend fun incrementRequests(provider: LlmProvider)
    suspend fun getOrCreateQuota(provider: LlmProvider)
    suspend fun resetIfNewDay(provider: LlmProvider)
    suspend fun getAllQuotas(): Map<LlmProvider, Pair<Int, Int>>
}
