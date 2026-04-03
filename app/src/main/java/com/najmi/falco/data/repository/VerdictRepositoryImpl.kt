package com.najmi.falco.data.repository

import com.najmi.falco.data.local.dao.ClaimDao
import com.najmi.falco.data.local.dao.PaperStanceDao
import com.najmi.falco.data.local.dao.VerdictDao
import com.najmi.falco.data.local.dao.VerdictWithStances
import com.najmi.falco.data.local.entity.ClaimEntity
import com.najmi.falco.data.local.entity.PaperStanceEntity
import com.najmi.falco.data.local.entity.VerdictEntity
import com.najmi.falco.domain.model.AnalysisMetadata
import com.najmi.falco.domain.model.Paper
import com.najmi.falco.domain.model.PaperSource
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.UncertaintyInfo
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.domain.repository.IVerdictRepository
import com.najmi.falco.domain.repository.RecentClaim
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerdictRepositoryImpl @Inject constructor(
    private val claimDao: ClaimDao,
    private val verdictDao: VerdictDao,
    private val paperStanceDao: PaperStanceDao
) : IVerdictRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun save(verdict: Verdict) {
        claimDao.insert(
            ClaimEntity(
                id = verdict.claimId,
                text = verdict.claim,
                type = "EMPIRICAL",
                submittedAt = verdict.completedAt
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
                supportingCount = verdict.supportingCount,
                opposingCount = verdict.opposingCount,
                neutralCount = verdict.neutralCount,
                dominantField = verdict.dominantField,
                temporalWarning = verdict.temporalWarning,
                caveat = verdict.caveat,
                completedAt = verdict.completedAt,
                analysisMetadataJson = json.encodeToString(verdict.analysisMetadata),
                uncertaintyInfoJson = json.encodeToString(verdict.uncertaintyInfo)
            )
        )
        paperStanceDao.insertAll(verdict.stances.map { stanceToEntity(it, verdict.claimId) })
    }

    override suspend fun getByClaimId(claimId: String): Verdict? {
        val vws = verdictDao.getVerdictWithStancesForClaim(claimId) ?: return null
        val claim = claimDao.getClaimById(claimId) ?: return null
        return mapVwsToVerdict(vws, claim.text)
    }

    override fun getAllVerdicts(): Flow<List<Verdict>> {
        return verdictDao.getAllVerdicts().map { list ->
            list.mapNotNull { vws ->
                val claim = claimDao.getClaimById(vws.verdict.claimId)
                if (claim != null) mapVwsToVerdict(vws, claim.text) else null
            }
        }
    }

    private fun mapVwsToVerdict(vws: VerdictWithStances, claimText: String): Verdict {
        val analysisMetadata = vws.verdict.analysisMetadataJson?.let {
            try { json.decodeFromString<AnalysisMetadata>(it) } catch (e: Exception) { AnalysisMetadata() }
        } ?: AnalysisMetadata()

        val uncertaintyInfo = vws.verdict.uncertaintyInfoJson?.let {
            try { json.decodeFromString<UncertaintyInfo>(it) } catch (e: Exception) { UncertaintyInfo() }
        } ?: UncertaintyInfo()

        val stances = vws.stances.map { entityToStance(it) }

        return Verdict(
            claimId = vws.verdict.claimId,
            claim = claimText,
            lean = try { Stance.valueOf(vws.verdict.lean) } catch (e: Exception) { Stance.NEUTRAL },
            confidence = vws.verdict.confidence,
            summary = vws.verdict.summary,
            stances = stances,
            totalPapersRetrieved = vws.verdict.totalPapersRetrieved,
            totalPapersPassedGate = vws.verdict.totalPapersPassedGate,
            supportingCount = vws.verdict.supportingCount,
            opposingCount = vws.verdict.opposingCount,
            neutralCount = vws.verdict.neutralCount,
            dominantField = vws.verdict.dominantField,
            temporalWarning = vws.verdict.temporalWarning,
            completedAt = vws.verdict.completedAt,
            analysisMetadata = analysisMetadata,
            uncertaintyInfo = uncertaintyInfo,
            caveat = vws.verdict.caveat
        )
    }

    private fun entityToStance(entity: PaperStanceEntity): PaperStance {
        val finalStance = try { Stance.valueOf(entity.finalStance) } catch (e: Exception) { Stance.NEUTRAL }
        return PaperStance(
            paper = Paper(
                id = entity.paperTitle.hashCode().toString(),
                title = entity.paperTitle,
                abstract = entity.paperAbstract.takeIf { it.isNotBlank() } ?: "",
                authors = emptyList(),
                year = entity.paperYear,
                citationCount = entity.paperCitationCount,
                isOpenAccess = false,
                doi = null,
                url = entity.paperUrl,
                source = PaperSource.SEMANTIC_SCHOLAR,
                fieldsOfStudy = emptyList()
            ),
            actorStance = finalStance,
            actorReasoning = entity.actorReasoning,
            confidence = entity.confidence,
            keyEvidence = "",
            relevanceScore = 0f,
            finalStance = finalStance,
            criticChallenge = entity.criticChallenge,
            groundingScore = entity.groundingScore
        )
    }

    private fun stanceToEntity(stance: PaperStance, verdictId: String): PaperStanceEntity {
        return PaperStanceEntity(
            id = UUID.randomUUID().toString(),
            verdictId = verdictId,
            paperTitle = stance.paper.title,
            paperAbstract = stance.paper.abstract,
            paperYear = stance.paper.year,
            paperCitationCount = stance.paper.citationCount,
            paperUrl = stance.paper.url,
            finalStance = (stance.finalStance ?: stance.actorStance).name,
            actorReasoning = stance.actorReasoning,
            criticChallenge = stance.criticChallenge,
            groundingScore = stance.groundingScore,
            confidence = stance.confidence
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
                    confidence = cwv.confidence,
                    supportingCount = cwv.supportingCount,
                    opposingCount = cwv.opposingCount,
                    neutralCount = cwv.neutralCount,
                    isFavorite = cwv.isFavorite
                )
            }
        }
    }

    override suspend fun toggleFavorite(id: String) {
        val claim = claimDao.getClaimById(id)
        claim?.let {
            claimDao.updateFavorite(id, !it.isFavorite)
        }
    }

    override suspend fun deleteClaim(id: String) {
        claimDao.deleteById(id)
    }

    override suspend fun getById(id: String): Verdict? {
        return getByClaimId(id)
    }

    override fun exportAllVerdicts(): Flow<List<Verdict>> {
        return getAllVerdicts()
    }
}
