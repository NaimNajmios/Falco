package com.najmi.falco.data.repository

import com.najmi.falco.data.local.dao.ClaimDao
import com.najmi.falco.data.local.dao.VerdictDao
import com.najmi.falco.data.local.entity.ClaimEntity
import com.najmi.falco.data.local.entity.VerdictEntity
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.domain.repository.IVerdictRepository
import com.najmi.falco.domain.repository.RecentClaim
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerdictRepositoryImpl @Inject constructor(
    private val claimDao: ClaimDao,
    private val verdictDao: VerdictDao
) : IVerdictRepository {

    override suspend fun save(verdict: Verdict) {
        claimDao.insert(
            ClaimEntity(
                id = verdict.claimId,
                text = verdict.claim,
                type = "EMPIRICAL",
                submittedAt = System.currentTimeMillis()
            )
        )
        verdictDao.insert(
            VerdictEntity(
                id = verdict.claimId,
                claimId = verdict.claimId,
                lean = verdict.lean.name,
                confidence = verdict.confidence,
                summary = verdict.summary,
                totalPapersRetrieved = verdict.totalPapersRetrieved,
                totalPapersPassedGate = verdict.totalPapersPassedGate,
                temporalWarning = verdict.temporalWarning,
                completedAt = verdict.completedAt
            )
        )
    }

    override suspend fun getByClaimId(claimId: String): Verdict? {
        val entity = verdictDao.getVerdictForClaim(claimId) ?: return null
        return Verdict(
            claimId = entity.claimId,
            claim = "",
            lean = Stance.valueOf(entity.lean),
            confidence = entity.confidence,
            summary = entity.summary,
            stances = emptyList(),
            totalPapersRetrieved = entity.totalPapersRetrieved,
            totalPapersPassedGate = entity.totalPapersPassedGate,
            supportingCount = 0,
            opposingCount = 0,
            neutralCount = 0,
            dominantField = "",
            temporalWarning = entity.temporalWarning,
            completedAt = entity.completedAt
        )
    }

    override fun getAllVerdicts(): Flow<List<Verdict>> {
        return verdictDao.getAllVerdicts().map { list ->
            list.map { vw ->
                Verdict(
                    claimId = vw.verdict.claimId,
                    claim = "",
                    lean = Stance.valueOf(vw.verdict.lean),
                    confidence = vw.verdict.confidence,
                    summary = vw.verdict.summary,
                    stances = emptyList(),
                    totalPapersRetrieved = vw.verdict.totalPapersRetrieved,
                    totalPapersPassedGate = vw.verdict.totalPapersPassedGate,
                    supportingCount = 0,
                    opposingCount = 0,
                    neutralCount = 0,
                    dominantField = "",
                    temporalWarning = vw.verdict.temporalWarning,
                    completedAt = vw.verdict.completedAt
                )
            }
        }
    }

    override fun getRecentClaims(): Flow<List<RecentClaim>> {
        return claimDao.getRecentClaimsWithVerdicts().map { list ->
            list.map { cwv ->
                RecentClaim(
                    id = cwv.id,
                    text = cwv.text,
                    type = cwv.type,
                    submittedAt = cwv.submittedAt,
                    lean = cwv.lean,
                    confidence = cwv.confidence
                )
            }
        }
    }

    override suspend fun deleteClaim(id: String) {
        claimDao.deleteById(id)
    }
}
