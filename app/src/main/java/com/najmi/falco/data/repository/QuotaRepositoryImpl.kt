package com.najmi.falco.data.repository

import com.najmi.falco.data.local.dao.QuotaDao
import com.najmi.falco.data.local.entity.QuotaEntity
import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.domain.repository.IQuotaRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuotaRepositoryImpl @Inject constructor(
    private val quotaDao: QuotaDao
) : IQuotaRepository {

    override suspend fun getTokensUsedToday(provider: LlmProvider): Int {
        val today = LocalDate.now().toString()
        val quota = quotaDao.getQuota(provider.name, today)
        return quota?.tokensUsedToday ?: 0
    }

    override suspend fun getRequestsUsedToday(provider: LlmProvider): Int {
        val today = LocalDate.now().toString()
        val quota = quotaDao.getQuota(provider.name, today)
        return quota?.requestsUsedToday ?: 0
    }

    override suspend fun incrementTokens(provider: LlmProvider, tokens: Int) {
        resetIfNewDay(provider)
        quotaDao.incrementTokens(provider.name, tokens)
    }

    override suspend fun incrementRequests(provider: LlmProvider) {
        resetIfNewDay(provider)
        quotaDao.incrementRequests(provider.name)
    }

    override suspend fun getOrCreateQuota(provider: LlmProvider) {
        val today = LocalDate.now().toString()
        if (quotaDao.getQuota(provider.name, today) == null) {
            quotaDao.upsertQuota(
                QuotaEntity(
                    provider = provider.name,
                    tokensUsedToday = 0,
                    requestsUsedToday = 0,
                    lastResetDate = today
                )
            )
        }
    }

    override suspend fun resetIfNewDay(provider: LlmProvider) {
        val today = LocalDate.now().toString()
        val quota = quotaDao.getQuota(provider.name, today)
        
        if (quota == null) {
            quotaDao.upsertQuota(
                QuotaEntity(
                    provider = provider.name,
                    tokensUsedToday = 0,
                    requestsUsedToday = 0,
                    lastResetDate = today
                )
            )
        }
    }

    override suspend fun getAllQuotas(): Map<LlmProvider, Pair<Int, Int>> {
        val today = LocalDate.now().toString()
        val quotas = quotaDao.getAllQuotas()
        
        return LlmProvider.values().associateWith { provider ->
            val quota = quotas.find { it.provider == provider.name && it.lastResetDate == today }
            Pair(quota?.tokensUsedToday ?: 0, quota?.requestsUsedToday ?: 0)
        }
    }
}
