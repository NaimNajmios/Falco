package com.najmi.falco.pipeline

import com.najmi.falco.agent.AggregatorAgent
import com.najmi.falco.agent.AggregatorInput
import com.najmi.falco.agent.ClaimClassifierAgent
import com.najmi.falco.agent.QueryExpansionAgent
import com.najmi.falco.agent.StanceActorAgent
import com.najmi.falco.agent.StanceActorInput
import com.najmi.falco.agent.StanceCriticAgent
import com.najmi.falco.agent.StanceCriticInput
import com.najmi.falco.data.local.DebugLogger
import com.najmi.falco.domain.model.Claim
import com.najmi.falco.domain.model.PaperQuality
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.VerificationStage
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.domain.repository.IPaperRepository
import com.najmi.falco.domain.repository.IVerdictRepository
import com.najmi.falco.provider.AllProvidersFailedException
import com.najmi.falco.provider.RateLimitException
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
        val totalStart = System.currentTimeMillis()
        
        try {
            var stageStart: Long
            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.CLASSIFYING, "Identifying claim type..."))
            
            val claimResult = claimClassifier.execute(claimText)
            val claim = claimResult.getOrElse { e ->
                DebugLogger.e("[PIPELINE] Claim classification failed: ${e.message}")
                send(VerificationState.Error(VerificationStage.CLASSIFYING, "Failed to classify claim: ${getUserFriendlyError(e)}"))
                return@channelFlow
            }
            DebugLogger.stage("CLASSIFYING", System.currentTimeMillis() - stageStart)

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.EXPANDING_QUERIES, "Generating academic search queries..."))
            
            val queriesResult = queryExpander.execute(claim)
            val queries = queriesResult.getOrElse { e ->
                DebugLogger.e("[PIPELINE] Query expansion failed: ${e.message}")
                send(VerificationState.Error(VerificationStage.EXPANDING_QUERIES, "Failed to generate queries: ${getUserFriendlyError(e)}"))
                return@channelFlow
            }
            DebugLogger.stage("EXPANDING_QUERIES", System.currentTimeMillis() - stageStart)

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.RETRIEVING_PAPERS, "Searching academic databases..."))
            val papers = paperRepo.searchAll(queries)
            DebugLogger.stage("RETRIEVING_PAPERS (${papers.size} papers)", System.currentTimeMillis() - stageStart)

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.QUALITY_GATING, "Filtering papers by quality..."))
            val qualityPapers = paperQualityGate.filter(papers, claim.type)
            DebugLogger.stage("QUALITY_GATING (${qualityPapers.size} passed)", System.currentTimeMillis() - stageStart)

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.TEMPORAL_CHECK, "Checking evidence freshness..."))
            val analyzedPapers = temporalAnalyzer.analyze(qualityPapers, claim.type)
            val temporalWarning = temporalAnalyzer.generateTemporalWarning(analyzedPapers)
            DebugLogger.stage("TEMPORAL_CHECK", System.currentTimeMillis() - stageStart)

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
                DebugLogger.d("[PIPELINE] No papers passed gate, completed in ${System.currentTimeMillis() - totalStart}ms")
                return@channelFlow
            }

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.ACTOR_CLASSIFICATION, "Classifying stances across ${analyzedPapers.size} papers..."))
            
            val actorResults = analyzedPapers.map { qualityPaper ->
                async { stanceActor.execute(StanceActorInput(claim.text, qualityPaper.paper)) }
            }.awaitAll()
            
            val actorFailures = actorResults.count { it.isFailure }
            if (actorFailures > 0) {
                DebugLogger.w("[PIPELINE] ${actorFailures}/${actorResults.size} stance classifications failed")
            }
            
            val actorStances: List<PaperStance> = actorResults
                .mapNotNull { it.getOrNull() }
                .filter { it.confidence > 0.3f }
            
            if (actorStances.isEmpty()) {
                send(VerificationState.Error(VerificationStage.ACTOR_CLASSIFICATION, "All paper stance classifications failed. Check your API keys or try again later."))
                return@channelFlow
            }
            
            DebugLogger.stage("ACTOR_CLASSIFICATION (${actorStances.size} stances)", System.currentTimeMillis() - stageStart)

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.CRITIC_REVIEW, "Critic reviewing stances..."))
            
            val criticResults = actorStances.map { stanceCritic.execute(StanceCriticInput(claim.text, it)) }
            
            val criticFailures = criticResults.count { it.isFailure }
            if (criticFailures > 0) {
                DebugLogger.w("[PIPELINE] ${criticFailures}/${criticResults.size} critic reviews failed")
            }
            
            val criticStances = criticResults.mapNotNull { it.getOrNull() }
            DebugLogger.stage("CRITIC_REVIEW", System.currentTimeMillis() - stageStart)

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.GROUNDING, "Verifying reasoning against abstracts..."))
            val groundedStances = algorithmicGrounding.verify(criticStances)
            DebugLogger.stage("GROUNDING", System.currentTimeMillis() - stageStart)

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.AGGREGATING, "Building verdict..."))
            val supportingCount = groundedStances.count { it.finalStance == Stance.SUPPORTS }
            val opposingCount = groundedStances.count { it.finalStance == Stance.OPPOSES }
            val neutralCount = groundedStances.count { it.finalStance == Stance.NEUTRAL }

            val aggregatorInput = AggregatorInput(
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
            
            val verdictResult = aggregator.execute(aggregatorInput)
            val verdict = verdictResult.getOrElse { e ->
                DebugLogger.e("[PIPELINE] Aggregation failed: ${e.message}")
                send(VerificationState.Error(VerificationStage.AGGREGATING, "Failed to build verdict: ${getUserFriendlyError(e)}"))
                return@channelFlow
            }

            verdictRepo.save(verdict)
            DebugLogger.stage("AGGREGATING", System.currentTimeMillis() - stageStart)
            DebugLogger.d("[PIPELINE] Completed in ${System.currentTimeMillis() - totalStart}ms")
            send(VerificationState.Success(verdict))

        } catch (e: Exception) {
            DebugLogger.e("[PIPELINE] Failed: ${e.message}")
            send(VerificationState.Error(null, e.message ?: "Verification failed"))
        }
    }
    
    private fun getUserFriendlyError(error: Throwable): String {
        return when (error) {
            is AllProvidersFailedException -> "All AI providers failed. Please check your API keys in Settings."
            is RateLimitException -> "Rate limit exceeded. Please wait a moment and try again."
            else -> error.message ?: "An unexpected error occurred"
        }
    }
}
