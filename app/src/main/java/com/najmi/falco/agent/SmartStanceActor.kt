package com.najmi.falco.agent

import android.util.Log
import com.najmi.falco.chunking.*
import com.najmi.falco.data.local.DebugLogger
import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.domain.model.Paper
import com.najmi.falco.domain.model.PaperStance
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class SmartStanceActorInput(
    val claimText: String,
    val paper: Paper,
    val enableSmartChunking: Boolean = true,
    val enableIncrementalAnalysis: Boolean = true,
    val confidenceThreshold: Float = SmartStanceActor.DEFAULT_CONFIDENCE_THRESHOLD,
    val minChunks: Int = SmartStanceActor.DEFAULT_MIN_CHUNKS
)

@Singleton
class SmartStanceActor @Inject constructor(
    private val chunker: ContentChunker,
    private val assembler: BatchAssembler,
    private val providerRouter: TieredProviderRouter,
    private val earlyStopEvaluator: EarlyStopEvaluator,
    private val quotaManager: FreeTierQuotaManager,
    private val incrementalAnalyzer: IncrementalChunkAnalyzer
) : IFalcoAgent<SmartStanceActorInput, PaperStance> {

    companion object {
        private const val TAG = "SmartStanceActor"
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.75f
        const val DEFAULT_MIN_CHUNKS = 1
    }

    override val agentName = "SmartStanceActor"
    override val defaultProvider = LlmProvider.GROQ

    private var analysisState = StanceAnalysisState()

    override suspend fun execute(
        input: SmartStanceActorInput,
        preferredProvider: LlmProvider?
    ): Result<PaperStance> {
        val startTime = System.currentTimeMillis()

        return try {
            val chunks = if (input.enableSmartChunking) {
                chunker.chunk(input.paper, input.claimText)
            } else {
                listOf(EvidenceChunk(
                    id = 0,
                    priority = 0,
                    label = "[ABSTRACT]",
                    content = input.paper.abstract,
                    estimatedTokens = estimateTokens(input.paper.abstract),
                    sourceSection = ChunkSource.ABSTRACT
                ))
            }

            if (chunks.isEmpty()) {
                return Result.failure(IllegalStateException("No content chunks available for analysis"))
            }

            val validation = assembler.validate(chunks, estimateTokens(chunks.joinToString { it.content }))
            if (!validation.isValid) {
                Log.w(TAG, "Chunk validation warnings: ${validation.warnings}")
            }

            val sortedChunks = chunks.sortedBy { it.priority }
            
            val incrementalResult = if (input.enableIncrementalAnalysis) {
                analyzeIncrementally(
                    sortedChunks = sortedChunks,
                    claim = input.claimText,
                    paper = input.paper,
                    confidenceThreshold = input.confidenceThreshold,
                    minChunks = input.minChunks,
                    preferredProvider = preferredProvider,
                    startTime = startTime
                )
            } else {
                analyzeBatch(
                    sortedChunks = sortedChunks,
                    claim = input.claimText,
                    paper = input.paper,
                    preferredProvider = preferredProvider,
                    startTime = startTime
                )
            }

            updateAnalysisState(incrementalResult)

            Log.d(TAG, "Stance analysis completed: ${incrementalResult.overallStance} " +
                    "(confidence: ${incrementalResult.overallConfidence}, " +
                    "chunks: ${incrementalResult.chunksUsed}, " +
                    "tokens: ${incrementalResult.tokensConsumed}, " +
                    "earlyStop: ${incrementalResult.didStopEarly}, " +
                    "time: ${System.currentTimeMillis() - startTime}ms)")

            Result.success(incrementalResult.toPaperStance(input.paper))
        } catch (e: Exception) {
            Log.e(TAG, "Smart stance actor error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun analyzeIncrementally(
        sortedChunks: List<EvidenceChunk>,
        claim: String,
        paper: Paper,
        confidenceThreshold: Float,
        minChunks: Int,
        preferredProvider: LlmProvider?,
        startTime: Long
    ): SmartStanceResult {
        var cumulativeResult: SmartStanceResult? = null
        var totalTokens = 0
        var index = 0
        var processedChunks = 0
        
        while (index < sortedChunks.size) {
            val chunk = sortedChunks[index]
            
            val chunkAnalysisResult = incrementalAnalyzer.analyzeSingleChunk(
                chunk = chunk,
                claim = claim,
                paper = paper,
                preferredProvider = preferredProvider
            )
            
            if (chunkAnalysisResult.isSuccess) {
                val chunkResult = chunkAnalysisResult.getOrThrow()
                processedChunks++
                totalTokens += chunkResult.tokensConsumed
                
                cumulativeResult = incrementalAnalyzer.mergeToSmartStance(cumulativeResult, chunkResult)
                
                val currentResult = cumulativeResult!!
                val stanceAgreement = currentResult.stanceAgreement()
                
                val stopDecision = incrementalAnalyzer.shouldStopEarly(
                    currentConfidence = currentResult.overallConfidence,
                    stanceAgreement = stanceAgreement,
                    chunksAnalyzed = processedChunks,
                    minChunks = minChunks,
                    confidenceThreshold = confidenceThreshold
                )
                
                if (stopDecision.shouldStop) {
                    val elapsed = System.currentTimeMillis() - startTime
                    DebugLogger.stage("INCREMENTAL_ANALYSIS", elapsed)
                    Log.d(TAG, "EARLY STOP at chunk ${index + 1}: ${stopDecision.reason}")
                    
                    return currentResult.copy(
                        didStopEarly = true,
                        tokensConsumed = totalTokens
                    )
                }
            } else {
                Log.w(TAG, "Failed to analyze chunk ${chunk.id}, continuing...")
            }
            
            index++
        }

        val elapsed = System.currentTimeMillis() - startTime
        DebugLogger.stage("INCREMENTAL_ANALYSIS", elapsed)
        
        return cumulativeResult?.copy(tokensConsumed = totalTokens) ?: SmartStanceResult(
            overallStance = com.najmi.falco.domain.model.Stance.NEUTRAL,
            overallConfidence = 0.3f,
            excerptAnalyses = emptyList(),
            reasoning = "Failed to analyze any chunks",
            chunksUsed = 0
        )
    }

    private suspend fun analyzeBatch(
        sortedChunks: List<EvidenceChunk>,
        claim: String,
        paper: Paper,
        preferredProvider: LlmProvider?,
        startTime: Long
    ): SmartStanceResult {
        val batchPrompt = assembler.assemble(
            claim = claim,
            paperTitle = paper.title,
            paperYear = paper.year,
            chunks = sortedChunks
        )

        val provider = preferredProvider ?: providerRouter.selectProvider(batchPrompt.estimatedInputTokens)
        
        val routeResult = providerRouter.routeWithFastFallback(
            prompt = batchPrompt.prompt,
            tokenCount = batchPrompt.estimatedInputTokens
        )

        val elapsed = System.currentTimeMillis() - startTime
        DebugLogger.stage("SMART_STANCE_ANALYSIS", elapsed)

        return routeResult.getOrNull()?.let { result ->
            SmartStanceParser.parseWithFallback(result.response.text)
                .copy(
                    providerUsed = result.provider.name,
                    tokensConsumed = result.response.usage.totalTokens
                )
        } ?: SmartStanceResult(
            overallStance = com.najmi.falco.domain.model.Stance.NEUTRAL,
            overallConfidence = 0.3f,
            excerptAnalyses = emptyList(),
            reasoning = "Failed to analyze paper",
            chunksUsed = 0
        )
    }

    private fun updateAnalysisState(result: SmartStanceResult) {
        analysisState = analysisState.withResult(result)
    }

    fun getAnalysisState(): StanceAnalysisState = analysisState

    fun shouldContinueAnalysis(): Boolean {
        return earlyStopEvaluator.shouldContinue(analysisState)
    }

    fun resetAnalysisState() {
        analysisState = StanceAnalysisState()
    }

    fun getStopDecision(): EarlyStopEvaluator.StopDecision {
        return earlyStopEvaluator.evaluate(analysisState)
    }

    fun canStopEarly(): Boolean {
        return analysisState.canStopEarly()
    }

    fun isClearCutCase(): Boolean {
        return earlyStopEvaluator.isClearCutCase(analysisState)
    }

    fun estimateRemainingPapers(): Int? {
        return earlyStopEvaluator.estimateStoppingPoint(analysisState)?.let { stoppingPoint ->
            (stoppingPoint - analysisState.analyzedCount).coerceAtLeast(0)
        }
    }

    private fun estimateTokens(text: String): Int {
        val wordCount = text.split(Regex("\\s+")).size
        return (wordCount * 1.3f).toInt()
    }

    private fun estimateTokens(chunks: List<EvidenceChunk>): Int {
        return chunks.sumOf { estimateTokens(it.content) }
    }

    suspend fun getQuotaStatus(): Map<LlmProvider, FreeTierQuotaManager.QuotaStatus> {
        return LlmProvider.entries.associateWith { provider ->
            quotaManager.hasQuota(provider)
        }
    }

    suspend fun getAvailableProviders(): List<LlmProvider> {
        return LlmProvider.entries.filter { provider ->
            quotaManager.hasQuota(provider).available
        }
    }

    fun getRecommendedProvider(tokenCount: Int): LlmProvider {
        return providerRouter.selectProvider(tokenCount)
    }

    fun getProviderInfo(provider: LlmProvider): String {
        return providerRouter.getProviderTier(provider)
    }
}

class StanceActorFactory @Inject constructor(
    private val smartStanceActor: SmartStanceActor,
    private val userPreferencesRepository: com.najmi.falco.data.local.UserPreferencesRepository
) {
    suspend fun createStanceActorInput(
        claimText: String,
        paper: Paper
    ): SmartStanceActorInput {
        val prefs = userPreferencesRepository.preferences.first()
        return SmartStanceActorInput(
            claimText = claimText,
            paper = paper,
            enableSmartChunking = prefs.enableSmartChunking
        )
    }
}
