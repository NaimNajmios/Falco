package com.najmi.falco.data.repository

import com.najmi.falco.data.local.dao.ClaimDao
import com.najmi.falco.data.local.dao.VerdictDao
import com.najmi.falco.data.local.entity.ClaimEntity
import com.najmi.falco.data.local.entity.VerdictEntity
import com.najmi.falco.domain.model.AnalysisMetadata
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.UncertaintyInfo
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.domain.repository.IVerdictRepository
import com.najmi.falco.domain.repository.RecentClaim
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerdictRepositoryImpl @Inject constructor(
    private val claimDao: ClaimDao,
    private val verdictDao: VerdictDao
) : IVerdictRepository {
    
    private val json = Json { ignoreUnknownKeys = true }

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
                completedAt = verdict.completedAt,
                analysisMetadataJson = json.encodeToString(verdict.analysisMetadata),
                uncertaintyInfoJson = json.encodeToString(verdict.uncertaintyInfo)
            )
        )
    }

    override suspend fun getByClaimId(claimId: String): Verdict? {
        val entity = verdictDao.getVerdictForClaim(claimId) ?: return null
        return mapEntityToVerdict(entity)
    }

    override fun getAllVerdicts(): Flow<List<Verdict>> {
        return verdictDao.getAllVerdicts().map { list ->
            list.map { vw ->
                mapEntityToVerdict(vw.verdict)
            }
        }
    }

    private fun mapEntityToVerdict(entity: VerdictEntity): Verdict {
        val analysisMetadata = entity.analysisMetadataJson?.let {
            try { json.decodeFromString<AnalysisMetadata>(it) } catch (e: Exception) { AnalysisMetadata() }
        } ?: AnalysisMetadata()
        
        val uncertaintyInfo = entity.uncertaintyInfoJson?.let {
            try { json.decodeFromString<UncertaintyInfo>(it) } catch (e: Exception) { UncertaintyInfo() }
        } ?: UncertaintyInfo()
        
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
            completedAt = entity.completedAt,
            analysisMetadata = analysisMetadata,
            uncertaintyInfo = uncertaintyInfo
        )
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
