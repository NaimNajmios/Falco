package com.najmi.falco.chunking

import com.najmi.falco.domain.model.Stance
import javax.inject.Inject
import javax.inject.Singleton

data class StanceAnalysisState(
    val analyzedCount: Int = 0,
    val stances: List<Stance> = emptyList(),
    val confidences: List<Float> = emptyList(),
    val analysisResults: List<SmartStanceResult> = emptyList()
) {
    fun withResult(result: SmartStanceResult): StanceAnalysisState {
        return copy(
            analyzedCount = analyzedCount + 1,
            stances = stances + result.overallStance,
            confidences = confidences + result.overallConfidence,
            analysisResults = analysisResults + result
        )
    }

    fun consensusReached(): Boolean {
        if (stances.size < MIN_PAPERS_FOR_CONSENSUS) return false
        
        val stanceCounts = stances.groupingBy { it }.eachCount()
        val dominantCount = stanceCounts.values.maxOrNull() ?: 0
        val consensusRatio = dominantCount.toFloat() / stances.size
        
        return consensusRatio >= CONSENSUS_THRESHOLD
    }

    fun averageConfidence(): Float {
        if (confidences.isEmpty()) return 0f
        return confidences.average().toFloat()
    }

    fun dominantStance(): Stance? {
        if (stances.isEmpty()) return null
        
        return stances.groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    fun stanceDistribution(): Map<Stance, Int> {
        return stances.groupingBy { it }.eachCount()
    }

    fun isStrongConsensus(): Boolean {
        return consensusReached() && averageConfidence() >= MIN_CONFIDENCE
    }

    fun canStopEarly(): Boolean {
        return consensusReached() && averageConfidence() >= MIN_CONFIDENCE
    }

    fun shouldAnalyzeMore(): Boolean {
        if (analyzedCount >= MAX_PAPERS) return false
        if (canStopEarly()) return false
        if (analyzedCount < MIN_PAPERS_FOR_CONSENSUS) return true
        
        val distribution = stanceDistribution()
        val hasMixedResults = distribution.keys.size > 1
        
        return hasMixedResults && analyzedCount < MAX_PAPERS
    }

    fun progress(): Float {
        return (analyzedCount.toFloat() / MAX_PAPERS).coerceIn(0f, 1f)
    }

    fun summary(): StateSummary {
        return StateSummary(
            analyzedCount = analyzedCount,
            maxPapers = MAX_PAPERS,
            consensusReached = consensusReached(),
            dominantStance = dominantStance(),
            averageConfidence = averageConfidence(),
            stanceDistribution = stanceDistribution(),
            canStop = canStopEarly(),
            progress = progress()
        )
    }

    data class StateSummary(
        val analyzedCount: Int,
        val maxPapers: Int,
        val consensusReached: Boolean,
        val dominantStance: Stance?,
        val averageConfidence: Float,
        val stanceDistribution: Map<Stance, Int>,
        val canStop: Boolean,
        val progress: Float
    )

    companion object {
        const val MIN_PAPERS_FOR_CONSENSUS = 3
        const val CONSENSUS_THRESHOLD = 0.7f
        const val MIN_CONFIDENCE = 0.85f
        const val MAX_PAPERS = 10
    }
}

@Singleton
class EarlyStopEvaluator @Inject constructor() {

    data class StopCriteria(
        val minPapersAnalyzed: Int = StanceAnalysisState.MIN_PAPERS_FOR_CONSENSUS,
        val consensusThreshold: Float = StanceAnalysisState.CONSENSUS_THRESHOLD,
        val minConfidence: Float = StanceAnalysisState.MIN_CONFIDENCE,
        val maxPapers: Int = StanceAnalysisState.MAX_PAPERS
    )

    data class StopDecision(
        val shouldContinue: Boolean,
        val reason: String,
        val confidence: Float,
        val dominantStance: Stance?
    )

    fun shouldContinue(state: StanceAnalysisState, criteria: StopCriteria = StopCriteria()): Boolean {
        val decision = evaluate(state, criteria)
        return decision.shouldContinue
    }

    fun evaluate(state: StanceAnalysisState, criteria: StopCriteria = StopCriteria()): StopDecision {
        if (state.analyzedCount >= criteria.maxPapers) {
            return StopDecision(
                shouldContinue = false,
                reason = "Max papers reached (${criteria.maxPapers})",
                confidence = state.averageConfidence(),
                dominantStance = state.dominantStance()
            )
        }

        if (state.analyzedCount < criteria.minPapersAnalyzed) {
            return StopDecision(
                shouldContinue = true,
                reason = "Need at least ${criteria.minPapersAnalyzed} papers, have ${state.analyzedCount}",
                confidence = state.averageConfidence(),
                dominantStance = null
            )
        }

        if (!state.consensusReached()) {
            val distribution = state.stanceDistribution()
            val stanceCounts = distribution.values.sum()
            val maxStanceCount = distribution.values.maxOrNull() ?: 0
            val currentRatio = maxStanceCount.toFloat() / stanceCounts
            
            return StopDecision(
                shouldContinue = state.analyzedCount < criteria.maxPapers,
                reason = "No consensus yet (current ratio: ${String.format("%.1f", currentRatio * 100)}%)",
                confidence = state.averageConfidence(),
                dominantStance = state.dominantStance()
            )
        }

        if (state.averageConfidence() < criteria.minConfidence) {
            return StopDecision(
                shouldContinue = state.analyzedCount < criteria.maxPapers,
                reason = "Confidence too low (${String.format("%.2f", state.averageConfidence())} < ${String.format("%.2f", criteria.minConfidence)})",
                confidence = state.averageConfidence(),
                dominantStance = state.dominantStance()
            )
        }

        return StopDecision(
            shouldContinue = false,
            reason = "Strong consensus: ${state.dominantStance()} with ${String.format("%.0f", state.averageConfidence() * 100)}% confidence",
            confidence = state.averageConfidence(),
            dominantStance = state.dominantStance()
        )
    }

    fun estimateStoppingPoint(state: StanceAnalysisState): Int? {
        if (state.stances.isEmpty()) return null

        val currentStances = state.stances
        val stanceCounts = currentStances.groupingBy { it }.eachCount()
        val dominantStance = stanceCounts.maxByOrNull { it.value }?.key ?: return null
        val dominantCount = stanceCounts[dominantStance] ?: 0

        if (dominantCount.toFloat() / currentStances.size >= StanceAnalysisState.CONSENSUS_THRESHOLD) {
            return state.analyzedCount
        }

        val otherCount = currentStances.size - dominantCount
        val neededForConsensus = (currentStances.size * StanceAnalysisState.CONSENSUS_THRESHOLD).toInt() - dominantCount

        return if (neededForConsensus > 0 && state.analyzedCount + neededForConsensus <= StanceAnalysisState.MAX_PAPERS) {
            state.analyzedCount + neededForConsensus
        } else {
            null
        }
    }

    fun isClearCutCase(state: StanceAnalysisState): Boolean {
        if (state.analyzedCount < 2) return false
        
        val distribution = state.stanceDistribution()
        val total = distribution.values.sum()
        
        if (distribution.size == 1) {
            return true
        }
        
        if (distribution.size == 2) {
            val counts = distribution.values.sortedDescending()
            val dominantRatio = counts[0].toFloat() / total
            return dominantRatio >= 0.8f && state.averageConfidence() >= 0.9f
        }
        
        return false
    }

    fun confidenceThreshold(state: StanceAnalysisState, targetRatio: Float): Float? {
        if (state.stanceDistribution().size == 1) {
            return state.averageConfidence()
        }
        
        val sortedConfidences = state.confidences.sortedDescending()
        val requiredCount = (sortedConfidences.size * targetRatio).toInt()
        
        return sortedConfidences.take(requiredCount).average().toFloat()
    }
}
