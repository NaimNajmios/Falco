package com.najmi.falco.pipeline

import com.najmi.falco.agent.AggregatorAgent
import com.najmi.falco.agent.AggregatorInput
import com.najmi.falco.agent.ClaimClassifierAgent
import com.najmi.falco.agent.QueryExpansionAgent
import com.najmi.falco.agent.StanceActorAgent
import com.najmi.falco.agent.StanceActorInput
import com.najmi.falco.agent.StanceCriticAgent
import com.najmi.falco.agent.StanceCriticInput
import com.najmi.falco.domain.model.Claim
import com.najmi.falco.domain.model.PaperQuality
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.VerificationStage
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.domain.repository.IPaperRepository
import com.najmi.falco.domain.repository.IVerdictRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FalcoOrchestrator @Inject constructor(
    private val claimClassifier: ClaimClassifierAgent,
    private val queryExpander: QueryExpansionAgent,
    private val paperRepo: IPaperRepository,
    private val paperQualityGate: PaperQualityGate,
    private val temporalAnalyzer: TemporalFreshnessAnalyzer,
    private val stanceActor: StanceActorAgent,
    private val stanceCritic: StanceCriticAgent,
    private val algorithmicGrounding: AlgorithmicGrounding,
    private val aggregator: AggregatorAgent,
    private val verdictRepo: IVerdictRepository
) {
    fun verify(claimText: String): Flow<VerificationState> = channelFlow {
        try {
            send(VerificationState.InProgress(VerificationStage.CLASSIFYING, "Identifying claim type..."))
            val claim = claimClassifier.execute(claimText)

            send(VerificationState.InProgress(VerificationStage.EXPANDING_QUERIES, "Generating academic search queries..."))
            val queries = queryExpander.execute(claim)

            send(VerificationState.InProgress(VerificationStage.RETRIEVING_PAPERS, "Searching academic databases..."))
            val papers = paperRepo.searchAll(queries)

            send(VerificationState.InProgress(VerificationStage.QUALITY_GATING, "Filtering papers by quality..."))
            val qualityPapers = paperQualityGate.filter(papers, claim.type)

            send(VerificationState.InProgress(VerificationStage.TEMPORAL_CHECK, "Checking evidence freshness..."))
            val analyzedPapers = temporalAnalyzer.analyze(qualityPapers, claim.type)
            val temporalWarning = temporalAnalyzer.generateTemporalWarning(analyzedPapers)

            if (analyzedPapers.isEmpty()) {
                val emptyVerdict = com.najmi.falco.domain.model.Verdict(
                    claimId = claim.id,
                    claim = claim.text,
                    lean = Stance.NEUTRAL,
                    confidence = 0f,
                    summary = "No papers passed the quality gate. Unable to verify this claim.",
                    stances = emptyList(),
                    totalPapersRetrieved = papers.size,
                    totalPapersPassedGate = 0,
                    supportingCount = 0,
                    opposingCount = 0,
                    neutralCount = 0,
                    dominantField = "Unknown",
                    temporalWarning = null
                )
                verdictRepo.save(emptyVerdict)
                send(VerificationState.Success(emptyVerdict))
                return@channelFlow
            }

            send(VerificationState.InProgress(VerificationStage.ACTOR_CLASSIFICATION, "Classifying stances across ${analyzedPapers.size} papers..."))
            val actorStances: List<PaperStance> = analyzedPapers.map { qualityPaper ->
                async { stanceActor.execute(StanceActorInput(claim.text, qualityPaper.paper)) }
            }.awaitAll().filter { it.confidence > 0.3f }

            send(VerificationState.InProgress(VerificationStage.CRITIC_REVIEW, "Critic reviewing stances..."))
            val criticStances = actorStances.map { actorStance ->
                stanceCritic.execute(StanceCriticInput(claim.text, actorStance))
            }

            send(VerificationState.InProgress(VerificationStage.GROUNDING, "Verifying reasoning against abstracts..."))
            val groundedStances = algorithmicGrounding.verify(criticStances)

            send(VerificationState.InProgress(VerificationStage.AGGREGATING, "Building verdict..."))
            val supportingCount = groundedStances.count { it.finalStance == Stance.SUPPORTS }
            val opposingCount = groundedStances.count { it.finalStance == Stance.OPPOSES }
            val neutralCount = groundedStances.count { it.finalStance == Stance.NEUTRAL }

            val verdict = aggregator.execute(
                AggregatorInput(
                    claimId = claim.id,
                    claimText = claim.text,
                    claimType = claim.type,
                    stances = groundedStances,
                    totalRetrieved = papers.size,
                    totalPapersPassedGate = analyzedPapers.size,
                    temporalWarning = temporalWarning,
                    supportingCount = supportingCount,
                    opposingCount = opposingCount,
                    neutralCount = neutralCount
                )
            )

            verdictRepo.save(verdict)
            send(VerificationState.Success(verdict))

        } catch (e: Exception) {
            send(VerificationState.Error(null, e.message ?: "Verification failed"))
        }
    }
}
