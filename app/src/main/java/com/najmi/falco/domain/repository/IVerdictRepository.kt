package com.najmi.falco.domain.repository

import com.najmi.falco.domain.model.Verdict
import kotlinx.coroutines.flow.Flow

interface IVerdictRepository {
    suspend fun save(verdict: Verdict)
    suspend fun getByClaimId(claimId: String): Verdict?
    fun getAllVerdicts(): Flow<List<Verdict>>
}
