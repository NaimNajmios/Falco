package com.najmi.falco.domain.model

data class PaperStance(
    val paper: Paper,
    val actorStance: Stance,
    val actorReasoning: String,
    val confidence: Float,
    val keyEvidence: String,
    val supportingExcerpt: String? = null,
    val relevanceScore: Float,
    val criticStance: Stance? = null,
    val criticChallenge: String? = null,
    val finalStance: Stance? = null,
    val groundingScore: Float? = null,
    val ragScore: Float? = null,
    val isConsensus: Boolean = false,
    val isOutlier: Boolean = false,
    val chunksAnalyzed: List<AnalyzedChunk> = emptyList(),
    val analysisDepth: AnalysisDepth = AnalysisDepth.STANDARD,
    val providerUsed: String = "unknown",
    val modelUsed: String? = null,
    val actorProviderUsed: String? = null,
    val criticProviderUsed: String? = null,
    val didStopEarly: Boolean = false,
    val confidenceFactors: List<ConfidenceFactor> = emptyList()
)
