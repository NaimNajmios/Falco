package com.najmi.falco.data.repository

import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.domain.repository.IVerdictRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerdictRepositoryImpl @Inject constructor() : IVerdictRepository {

    private val verdicts = MutableStateFlow<List<Verdict>>(emptyList())

    override suspend fun save(verdict: Verdict) {
        verdicts.value = listOf(verdict) + verdicts.value
    }

    override suspend fun getByClaimId(claimId: String): Verdict? {
        return verdicts.value.find { it.claimId == claimId }
    }

    override fun getAllVerdicts(): Flow<List<Verdict>> = verdicts.asStateFlow()
}
