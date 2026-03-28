package com.najmi.falco.domain.repository

import com.najmi.falco.domain.model.Verdict
import kotlinx.coroutines.flow.Flow

data class RecentClaim(
    val id: String,
    val text: String,
    val type: String,
    val submittedAt: Long,
    val lean: String?,
    val confidence: Float?
)

interface IVerdictRepository {
    suspend fun save(verdict: Verdict)
    suspend fun getByClaimId(claimId: String): Verdict?
    fun getAllVerdicts(): Flow<List<Verdict>>
    fun getRecentClaims(): Flow<List<RecentClaim>>
}
