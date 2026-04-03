package com.najmi.falco.chunking

import com.najmi.falco.domain.model.Paper
import com.najmi.falco.domain.model.Stance
import javax.inject.Inject
import javax.inject.Singleton

data class IncrementalAnalysisResult(
    val overallStance: Stance,
    val overallConfidence: Float,
    val chunkResults: List<SmartStanceResult.ExcerptAnalysis>,
    val reasoning: String,
    val chunksAnalyzed: Int,
    val didStopEarly: Boolean,
    val stopReason: String?,
    val tokensConsumed: Int = 0
)

data class EarlyStopDecision(
    val shouldStop: Boolean,
    val reason: String,
    val currentConfidence: Float,
    val stanceAgreement: Float
)

@Singleton
class IncrementalChunkAnalyzer @Inject constructor(
    private val assembler: BatchAssembler,
    private val tieredProviderRouter: TieredProviderRouter
) {
    companion object {
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.75f
        const val DEFAULT_MIN_CHUNKS = 1
        const val DEFAULT_STANCE_AGREEMENT_THRESHOLD = 0.8f
    }

    fun shouldStopEarly(
        currentConfidence: Float,
        stanceAgreement: Float,
        chunksAnalyzed: Int,
        minChunks: Int = DEFAULT_MIN_CHUNKS,
        confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
    ): EarlyStopDecision {
        if (chunksAnalyzed < minChunks) {
            return EarlyStopDecision(
                shouldStop = false,
                reason = "Minimum chunks ($minChunks) not yet analyzed",
                currentConfidence = currentConfidence,
                stanceAgreement = stanceAgreement
            )
        }

        if (currentConfidence >= confidenceThreshold && stanceAgreement >= DEFAULT_STANCE_AGREEMENT_THRESHOLD) {
            return EarlyStopDecision(
                shouldStop = true,
                reason = "High confidence ($currentConfidence) with strong agreement ($stanceAgreement)",
                currentConfidence = currentConfidence,
                stanceAgreement = stanceAgreement
            )
        }

        if (currentConfidence >= confidenceThreshold && stanceAgreement >= 0.6f && chunksAnalyzed >= 2) {
            return EarlyStopDecision(
                shouldStop = true,
                reason = "Moderate confidence ($currentConfidence) after analyzing $chunksAnalyzed chunks",
                currentConfidence = currentConfidence,
                stanceAgreement = stanceAgreement
            )
        }

        if (currentConfidence >= 0.9f) {
            return EarlyStopDecision(
                shouldStop = true,
                reason = "Very high confidence ($currentConfidence) - likely clear-cut case",
                currentConfidence = currentConfidence,
                stanceAgreement = stanceAgreement
            )
        }

        return EarlyStopDecision(
            shouldStop = false,
            reason = "Confidence ($currentConfidence) below threshold ($confidenceThreshold)",
            currentConfidence = currentConfidence,
            stanceAgreement = stanceAgreement
        )
    }

    fun mergeResults(
        previousResult: SmartStanceResult?,
        newChunkResult: SmartStanceResult
    ): IncrementalAnalysisResult {
        val allChunkAnalyses = previousResult?.excerptAnalyses?.let { previous ->
            previous + newChunkResult.excerptAnalyses
        } ?: newChunkResult.excerptAnalyses

        if (allChunkAnalyses.isEmpty()) {
            return IncrementalAnalysisResult(
                overallStance = newChunkResult.overallStance,
                overallConfidence = newChunkResult.overallConfidence,
                chunkResults = emptyList(),
                reasoning = newChunkResult.reasoning,
                chunksAnalyzed = 1,
                didStopEarly = false,
                stopReason = null
            )
        }

        val dominantStance = findDominantStance(allChunkAnalyses)
        val stanceAgreement = calculateStanceAgreement(allChunkAnalyses, dominantStance)
        
        val weightedConfidence = calculateWeightedConfidence(allChunkAnalyses)
        
        val combinedReasoning = buildCombinedReasoning(previousResult, newChunkResult, allChunkAnalyses)

        return IncrementalAnalysisResult(
            overallStance = dominantStance,
            overallConfidence = weightedConfidence,
            chunkResults = allChunkAnalyses,
            reasoning = combinedReasoning,
            chunksAnalyzed = allChunkAnalyses.size,
            didStopEarly = false,
            stopReason = null
        )
    }

    fun mergeToSmartStance(
        previousResult: SmartStanceResult?,
        newChunkResult: SmartStanceResult
    ): SmartStanceResult {
        val allChunkAnalyses = previousResult?.excerptAnalyses?.let { previous ->
            previous + newChunkResult.excerptAnalyses
        } ?: newChunkResult.excerptAnalyses

        if (allChunkAnalyses.isEmpty()) {
            return SmartStanceResult(
                overallStance = newChunkResult.overallStance,
                overallConfidence = newChunkResult.overallConfidence,
                excerptAnalyses = emptyList(),
                reasoning = newChunkResult.reasoning,
                chunksUsed = 1,
                tokensConsumed = newChunkResult.tokensConsumed
            )
        }

        val dominantStance = findDominantStance(allChunkAnalyses)
        val weightedConfidence = calculateWeightedConfidence(allChunkAnalyses)
        val combinedReasoning = buildCombinedReasoning(previousResult, newChunkResult, allChunkAnalyses)

        return SmartStanceResult(
            overallStance = dominantStance,
            overallConfidence = weightedConfidence,
            excerptAnalyses = allChunkAnalyses,
            reasoning = combinedReasoning,
            chunksUsed = allChunkAnalyses.size,
            tokensConsumed = newChunkResult.tokensConsumed
        )
    }

    private fun findDominantStance(analyses: List<SmartStanceResult.ExcerptAnalysis>): Stance {
        return analyses
            .groupingBy { it.stance }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: Stance.NEUTRAL
    }

    private fun calculateStanceAgreement(analyses: List<SmartStanceResult.ExcerptAnalysis>, dominantStance: Stance): Float {
        if (analyses.isEmpty()) return 0f
        val agreementCount = analyses.count { it.stance == dominantStance }
        return agreementCount.toFloat() / analyses.size
    }

    private fun calculateWeightedConfidence(analyses: List<SmartStanceResult.ExcerptAnalysis>): Float {
        if (analyses.isEmpty()) return 0f
        
        val totalWeight = analyses.sumOf { it.confidence.toDouble() }
        return (totalWeight / analyses.size).toFloat().coerceIn(0f, 1f)
    }

    private fun buildCombinedReasoning(
        previousResult: SmartStanceResult?,
        newChunkResult: SmartStanceResult,
        allAnalyses: List<SmartStanceResult.ExcerptAnalysis>
    ): String {
        val sb = StringBuilder()
        
        if (previousResult != null && previousResult.excerptAnalyses.isNotEmpty()) {
            sb.append("Previous analysis: ${previousResult.reasoning}\n\n")
        }
        
        val latestAnalysis = newChunkResult.excerptAnalyses.lastOrNull()
        if (latestAnalysis != null) {
            sb.append("Latest chunk (${latestAnalysis.chunkId}): ${latestAnalysis.reasoning}")
        }

        val dominant = findDominantStance(allAnalyses)
        val agreement = calculateStanceAgreement(allAnalyses, dominant)
        
        sb.append("\n\nCombined: ${allAnalyses.size} chunks analyzed. " +
                 "Dominant stance: $dominant with ${String.format("%.0f", agreement * 100)}% agreement.")

        return sb.toString()
    }

    suspend fun analyzeSingleChunk(
        chunk: EvidenceChunk,
        claim: String,
        paper: Paper,
        preferredProvider: com.najmi.falco.data.remote.LlmProvider? = null
    ): Result<SmartStanceResult> {
        val singleChunkList = listOf(chunk)
        
        val batchPrompt = assembler.assemble(
            claim = claim,
            paperTitle = paper.title,
            paperYear = paper.year,
            chunks = singleChunkList
        )

        val routeResult = if (preferredProvider != null) {
            tieredProviderRouter.routeWithProvider(preferredProvider, batchPrompt.prompt)
        } else {
            tieredProviderRouter.routeWithFastFallback(
                prompt = batchPrompt.prompt,
                tokenCount = batchPrompt.estimatedInputTokens
            )
        }

        return routeResult.map { result ->
            val parsed = SmartStanceParser.parseWithFallback(result.response.text)
                .copy(
                    providerUsed = result.provider.name,
                    tokensConsumed = result.response.usage.totalTokens,
                    chunksUsed = 1
                )
            parsed
        }
    }
}
