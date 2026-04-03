package com.najmi.falco.pipeline

import com.najmi.falco.agent.AggregatorAgent
import com.najmi.falco.agent.AggregatorInput
import com.najmi.falco.agent.AggregatorOutput
import com.najmi.falco.agent.ClaimClassifierAgent
import com.najmi.falco.agent.CrossReferenceAgent
import com.najmi.falco.agent.CrossReferenceInput
import com.najmi.falco.agent.QueryExpansionAgent
import com.najmi.falco.agent.SmartStanceActor
import com.najmi.falco.agent.SmartStanceActorInput
import com.najmi.falco.agent.StanceCriticAgent
import com.najmi.falco.agent.StanceCriticInput
import com.najmi.falco.chunking.EvidenceChunk
import com.najmi.falco.data.local.DebugLogger
import com.najmi.falco.domain.model.AnalysisMetadata
import com.najmi.falco.domain.model.Claim
import com.najmi.falco.domain.model.PaperQuality
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.UncertaintyInfo
import com.najmi.falco.domain.model.VerificationStage
import com.najmi.falco.domain.model.VerificationState
import com.najmi.falco.domain.repository.IPaperRepository
import com.najmi.falco.domain.repository.IVerdictRepository
import com.najmi.falco.provider.ActorCriticProviderSelector
import com.najmi.falco.provider.AllProvidersFailedException
import com.najmi.falco.provider.ProviderAssignment
import com.najmi.falco.provider.RateLimitException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FalcoOrchestrator @Inject constructor(
    private val claimClassifier: ClaimClassifierAgent,
    private val queryExpander: QueryExpansionAgent,
    private val paperRepo: IPaperRepository,
    private val paperQualityGate: PaperQualityGate,
    private val temporalAnalyzer: TemporalFreshnessAnalyzer,
    private val stanceActor: SmartStanceActor,
    private val crossReferenceAgent: CrossReferenceAgent,
    private val stanceCritic: StanceCriticAgent,
    private val algorithmicGrounding: AlgorithmicGrounding,
    private val aggregator: AggregatorAgent,
    private val verdictRepo: IVerdictRepository,
    private val providerSelector: ActorCriticProviderSelector
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

            providerSelector.logHealthStatus()
            val providers = providerSelector.selectProviders(claim.type, claim.text)
            DebugLogger.d("[PIPELINE] Provider selection: ${providers.rationale}")
            DebugLogger.d("[PIPELINE] Actor: ${providers.actor.name} (fallbacks: ${providers.actorFallbacks.joinToString { it.name }})")
            DebugLogger.d("[PIPELINE] Critic: ${providers.critic.name} (fallbacks: ${providers.criticFallbacks.joinToString { it.name }})")

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.EXPANDING, "Generating academic search queries..."))
            
            val queriesResult = queryExpander.execute(claim)
            val queries = queriesResult.getOrElse { e ->
                DebugLogger.e("[PIPELINE] Query expansion failed: ${e.message}")
                send(VerificationState.Error(VerificationStage.EXPANDING, "Failed to generate queries: ${getUserFriendlyError(e)}"))
                return@channelFlow
            }
            DebugLogger.stage("EXPANDING", System.currentTimeMillis() - stageStart)

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.RETRIEVING, "Searching academic databases..."))
            val searchResult = paperRepo.searchAll(queries)
            val papers = searchResult.papers
            val databasesQueried = searchResult.databasesQueried
            DebugLogger.stage("RETRIEVING (${papers.size} papers)", System.currentTimeMillis() - stageStart)

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
                    lean = Stance.INSUFFICIENT_EVIDENCE,
                    confidence = 0f,
                    summary = "No papers passed the quality gate. Unable to verify this claim.",
                    stances = emptyList(),
                    totalPapersRetrieved = papers.size,
                    totalPapersPassedGate = 0,
                    supportingCount = 0,
                    opposingCount = 0,
                    neutralCount = 0,
                    dominantField = "Unknown",
                    temporalWarning = null,
                    caveat = "No papers passed the quality gate. No evidence available to verify this claim."
                )
                verdictRepo.save(emptyVerdict)
                send(VerificationState.Success(emptyVerdict))
                DebugLogger.d("[PIPELINE] No papers passed gate, completed in ${System.currentTimeMillis() - totalStart}ms")
                return@channelFlow
            }

            stageStart = System.currentTimeMillis()
            val totalPapers = analyzedPapers.size
            send(VerificationState.InProgress(VerificationStage.ACTOR_CLASSIFICATION, "Classifying stances across $totalPapers papers...", 0, totalPapers))
            
            val backpressureQueue = PaperBackpressureQueue()
            val actorResults = mutableListOf<Result<PaperStance>>()
            var totalTokensAnalyzed = 0
            var processedCount = 0
            
            launch {
                analyzedPapers.forEach { qualityPaper ->
                    backpressureQueue.send(qualityPaper.paper)
                }
                backpressureQueue.close()
            }
            
            val producer = launch {
                backpressureQueue.papersFlow.collect { paper ->
                    val result = stanceActor.execute(
                        SmartStanceActorInput(
                            claimText = claim.text,
                            paper = paper,
                            preferredProvider = providers.actor
                        ),
                        providers.actor
                    )
                    var shouldUpdateProgress = false
                    synchronized(actorResults) {
                        actorResults.add(result)
                        processedCount++
                        shouldUpdateProgress = true
                    }
                    if (shouldUpdateProgress) {
                        send(VerificationState.InProgress(VerificationStage.ACTOR_CLASSIFICATION, "Processing papers...", processedCount, totalPapers))
                    }
                }
            }
            
            producer.join()
            
            val actorFailures = actorResults.count { it.isFailure }
            if (actorFailures > 0) {
                DebugLogger.w("[PIPELINE] ${actorFailures}/${actorResults.size} stance classifications failed")
            }
            
            val actorStancesWithTokens: List<PaperStance> = actorResults.mapNotNull { result ->
                result.getOrNull()?.let { stance ->
                    totalTokensAnalyzed += estimateTokensFromChunks(stance)
                    stance
                }
            }
            
            val actorStances = actorStancesWithTokens.filter { it.confidence > 0.3f }
            
            if (actorStances.isEmpty()) {
                send(VerificationState.Error(VerificationStage.ACTOR_CLASSIFICATION, "All paper stance classifications failed. Check your API keys or try again later."))
                return@channelFlow
            }
            
            DebugLogger.stage("ACTOR_CLASSIFICATION (${actorStances.size} stances, ~$totalTokensAnalyzed tokens)", System.currentTimeMillis() - stageStart)

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.ACTOR_CLASSIFICATION, "Classifying stances and analyzing consensus..."))
            
            val crossRefResult = crossReferenceAgent.execute(
                CrossReferenceInput(claim.text, actorStances)
            )
            val enrichedStances = crossRefResult.getOrNull()?.enrichedStances ?: actorStances
            DebugLogger.stage("ACTOR_CLASSIFICATION + CROSS_REFERENCE (${enrichedStances.count { it.isConsensus }} consensus, ${enrichedStances.count { it.isOutlier }} outliers)", System.currentTimeMillis() - stageStart)

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.CRITIC_REVIEW, "Critic reviewing stances..."))
            
            val criticResults = enrichedStances.map { stance ->
                val result = stanceCritic.execute(StanceCriticInput(claim.text, stance), providers.critic)
                result.onFailure { e ->
                    DebugLogger.w("[PIPELINE] Primary critic (${providers.critic}) failed for ${stance.paper.title.take(30)}: ${e.message}")
                    providers.criticFallbacks.forEach { fallback ->
                        if (result.isFailure) {
                            DebugLogger.d("[PIPELINE] Trying critic fallback: $fallback")
                            val fallbackResult = stanceCritic.execute(StanceCriticInput(claim.text, stance), fallback)
                            if (fallbackResult.isSuccess) {
                                DebugLogger.d("[PIPELINE] Fallback critic ($fallback) succeeded for ${stance.paper.title.take(30)}")
                                return@forEach
                            }
                        }
                    }
                }
                result
            }
            
            val criticFailures = criticResults.count { it.isFailure }
            if (criticFailures > 0) {
                DebugLogger.w("[PIPELINE] ${criticFailures}/${criticResults.size} critic reviews failed")
            }
            
            val criticStances = criticResults.mapNotNull { it.getOrNull() }
            DebugLogger.stage("CRITIC_REVIEW (${criticStances.size} succeeded)", System.currentTimeMillis() - stageStart)

            stageStart = System.currentTimeMillis()
            send(VerificationState.InProgress(VerificationStage.GROUNDING, "Verifying reasoning against abstracts..."))
            var allGroundedStances = algorithmicGrounding.verify(criticStances)
            var totalPapersRetrieved = papers.size
            var totalPapersPassed = analyzedPapers.size
            var suggestedQueries: List<String>? = null
            val maxAdaptiveRetries = 2
            var adaptiveRetryCount = 0

            var aggregatorOutput: AggregatorOutput? = null
            
            while (adaptiveRetryCount < maxAdaptiveRetries) {
                stageStart = System.currentTimeMillis()
                send(VerificationState.InProgress(VerificationStage.AGGREGATING, "Building verdict..."))
                val supportingCount = allGroundedStances.count { it.finalStance == Stance.SUPPORTS }
                val opposingCount = allGroundedStances.count { it.finalStance == Stance.OPPOSES }
                val neutralCount = allGroundedStances.count { it.finalStance == Stance.NEUTRAL }

                val aggregatorInput = AggregatorInput(
                    claimId = claim.id,
                    claimText = claim.text,
                    claimType = claim.type,
                    stances = allGroundedStances,
                    totalRetrieved = totalPapersRetrieved,
                    totalPapersPassedGate = totalPapersPassed,
                    temporalWarning = temporalWarning,
                    supportingCount = supportingCount,
                    opposingCount = opposingCount,
                    neutralCount = neutralCount,
                    confidenceThreshold = 0.5f
                )
                
                val result = aggregator.execute(aggregatorInput)
                aggregatorOutput = result.getOrElse { e ->
                    DebugLogger.e("[PIPELINE] Aggregation failed: ${e.message}")
                    send(VerificationState.Error(VerificationStage.AGGREGATING, "Failed to build verdict: ${getUserFriendlyError(e)}"))
                    return@channelFlow
                }

                if (!aggregatorOutput!!.needsMorePapers || adaptiveRetryCount >= maxAdaptiveRetries) {
                    break
                }
                
                suggestedQueries = aggregatorOutput!!.suggestedQueries
                adaptiveRetryCount++
                DebugLogger.d("[PIPELINE] Adaptive retrieval: ${suggestedQueries?.size ?: 0} new queries, retry $adaptiveRetryCount/$maxAdaptiveRetries")
                
                if (!suggestedQueries.isNullOrEmpty()) {
                    DebugLogger.d("[PIPELINE] Adaptive retrieval: fetching additional evidence (${adaptiveRetryCount}/${maxAdaptiveRetries})")
                    
                    val additionalSearchResult = paperRepo.searchAll(suggestedQueries)
                    val additionalPapers = additionalSearchResult.papers
                    val additionalQualityPapers = paperQualityGate.filter(additionalPapers, claim.type)
                    val additionalAnalyzedPapers = temporalAnalyzer.analyze(additionalQualityPapers, claim.type)
                    
                    if (additionalAnalyzedPapers.isNotEmpty()) {
                        val additionalBackpressureQueue = PaperBackpressureQueue()
                        val additionalActorResults = mutableListOf<Result<PaperStance>>()
                        
                        launch {
                            additionalAnalyzedPapers.forEach { qualityPaper ->
                                additionalBackpressureQueue.send(qualityPaper.paper)
                            }
                            additionalBackpressureQueue.close()
                        }
                        
                        val additionalProducer = launch {
                            additionalBackpressureQueue.papersFlow.collect { paper ->
                                val actorResult = stanceActor.execute(
                                    SmartStanceActorInput(
                                        claimText = claim.text,
                                        paper = paper,
                                        preferredProvider = providers.actor
                                    ),
                                    providers.actor
                                )
                                synchronized(additionalActorResults) {
                                    additionalActorResults.add(actorResult)
                                }
                            }
                        }
                        
                        additionalProducer.join()
                        
                        val additionalActorStancesWithTokens = additionalActorResults.mapNotNull { result ->
                            result.getOrNull()?.let { stance ->
                                totalTokensAnalyzed += estimateTokensFromChunks(stance)
                                stance
                            }
                        }
                        
                        val additionalActorStances = additionalActorStancesWithTokens.filter { it.confidence > 0.3f }
                        
                        if (additionalActorStances.isNotEmpty()) {
                            val additionalCrossRefResult = crossReferenceAgent.execute(
                                CrossReferenceInput(claim.text, additionalActorStances)
                            )
                            val additionalEnrichedStances = additionalCrossRefResult.getOrNull()?.enrichedStances ?: additionalActorStances
                            
                            val additionalCriticResults = additionalEnrichedStances.map { stance ->
                                stanceCritic.execute(StanceCriticInput(claim.text, stance), providers.critic)
                            }
                            val additionalCriticStances = additionalCriticResults.mapNotNull { it.getOrNull() }
                            val additionalGroundedStances = algorithmicGrounding.verify(additionalCriticStances)
                            
                            allGroundedStances = allGroundedStances + additionalGroundedStances
                            totalPapersRetrieved += additionalPapers.size
                            totalPapersPassed += additionalAnalyzedPapers.size
                        }
                    }
                }
            }

            val verdict = aggregatorOutput!!.verdict

            val estimatedFullTextTokens = analyzedPapers.sumOf { qualityPaper ->
                EvidenceChunk.estimateFullTextTokens(qualityPaper.paper.abstract, qualityPaper.paper.year)
            }
            
            val analysisMetadata = AnalysisMetadata(
                totalTokensAnalyzed = totalTokensAnalyzed,
                estimatedFullTextTokens = estimatedFullTextTokens,
                efficiencyComparison = calculateEfficiencyComparison(totalTokensAnalyzed, estimatedFullTextTokens),
                analysisDurationMs = System.currentTimeMillis() - totalStart,
                databasesQueried = databasesQueried,
                algorithmVersion = "v1.0",
                completedAt = System.currentTimeMillis()
            )
            
            val uncertaintyInfo = buildUncertaintyInfo(analyzedPapers, temporalWarning)
            
            val enhancedVerdict = verdict.copy(
                analysisMetadata = analysisMetadata,
                uncertaintyInfo = uncertaintyInfo
            )

            verdictRepo.save(enhancedVerdict)
            DebugLogger.stage("AGGREGATING (${adaptiveRetryCount} adaptive retries)", System.currentTimeMillis() - stageStart)
            DebugLogger.d("[PIPELINE] Completed in ${System.currentTimeMillis() - totalStart}ms")
            send(VerificationState.Success(enhancedVerdict))

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
    
    private fun estimateTokensFromChunks(stance: PaperStance): Int {
        return stance.chunksAnalyzed.sumOf { it.estimatedTokens }
    }
    
    private fun calculateEfficiencyComparison(tokensAnalyzed: Int, fullTextTokens: Int): String? {
        if (fullTextTokens <= 0 || tokensAnalyzed <= 0) return null
        val savings = ((fullTextTokens - tokensAnalyzed).toFloat() / fullTextTokens * 100).toInt()
        return if (savings > 0) {
            "$savings% more efficient than full-text analysis"
        } else {
            null
        }
    }
    
    private fun buildUncertaintyInfo(
        analyzedPapers: List<com.najmi.falco.domain.model.PaperQuality>,
        temporalWarning: String?
    ): UncertaintyInfo {
        val qualityWarnings = mutableListOf<String>()
        val gaps = mutableListOf<String>()
        var recencyAlert: String? = null
        
        val years = analyzedPapers.mapNotNull { it.paper.year }
        if (years.isNotEmpty()) {
            val mostRecentYear = years.maxOrNull() ?: 0
            val yearsAgo = 2026 - mostRecentYear
            if (yearsAgo > 5) {
                recencyAlert = "Most recent study: $mostRecentYear"
            }
        }
        
        analyzedPapers.forEach { qualityPaper ->
            if (qualityPaper.paper.citationCount < 10) {
                qualityWarnings.add("Low citation count: ${qualityPaper.paper.title.take(40)}...")
            }
        }
        
        return UncertaintyInfo(
            gaps = gaps,
            qualityWarnings = qualityWarnings,
            recencyAlert = recencyAlert,
            fundingDisclosure = null
        )
    }
}
