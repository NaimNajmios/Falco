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
    val enableSmartChunking: Boolean = true
)

@Singleton
class SmartStanceActor @Inject constructor(
    private val chunker: ContentChunker,
    private val assembler: BatchAssembler,
    private val providerRouter: TieredProviderRouter,
    private val earlyStopEvaluator: EarlyStopEvaluator,
    private val quotaManager: FreeTierQuotaManager
) : IFalcoAgent<SmartStanceActorInput, PaperStance> {

    companion object {
        private const val TAG = "SmartStanceActor"
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

            val batchPrompt = assembler.assemble(
                claim = input.claimText,
                paperTitle = input.paper.title,
                paperYear = input.paper.year,
                chunks = chunks
            )

            val provider = preferredProvider ?: providerRouter.selectProvider(batchPrompt.estimatedInputTokens)
            
            val routeResult = providerRouter.routeWithFastFallback(
                prompt = batchPrompt.prompt,
                tokenCount = batchPrompt.estimatedInputTokens
            )

            val elapsed = System.currentTimeMillis() - startTime
            DebugLogger.stage("SMART_STANCE_ANALYSIS", elapsed)

            routeResult.fold(
                onSuccess = { result ->
                    val smartResult = SmartStanceParser.parseWithFallback(result.response.text)
                        .copy(
                            providerUsed = result.provider.name,
                            tokensConsumed = result.response.usage.totalTokens
                        )

                    updateAnalysisState(smartResult)

                    Log.d(TAG, "Smart stance analysis completed: ${smartResult.overallStance} " +
                            "(confidence: ${smartResult.overallConfidence}, " +
                            "chunks: ${smartResult.chunksUsed}, " +
                            "provider: ${result.provider.name}, " +
                            "tokens: ${result.response.usage.totalTokens}, " +
                            "time: ${elapsed}ms)")

                    Result.success(smartResult.toPaperStance(input.paper))
                },
                onFailure = { error ->
                    Log.e(TAG, "Smart stance analysis failed: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Smart stance actor error: ${e.message}", e)
            Result.failure(e)
        }
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
