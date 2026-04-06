package com.najmi.falco.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ClaimAnalysis(
    val claimType: ClaimType,
    val confidence: Float = 0f,
    val isAmbiguous: Boolean = false,
    val ambiguityReason: String? = null,
    val subClaims: List<String> = emptyList(),
    val restatedClaim: String = ""
)

@Serializable
enum class VerdictLabel {
    SUPPORTED,
    LIKELY_SUPPORTED,
    CONTESTED,
    LIKELY_REFUTED,
    REFUTED,
    INSUFFICIENT_EVIDENCE
}

enum class VerdictFactor {
    EVIDENCE_VOLUME,
    SOURCE_DIVERSITY,
    CONSENSUS_STRENGTH,
    EVIDENCE_RECENCY
}

@Serializable
data class VerdictDetails(
    val label: VerdictLabel = VerdictLabel.INSUFFICIENT_EVIDENCE,
    val overallConfidence: Float = 0f,
    val verdictNarrative: String = "",
    val weightedScore: Float = 0f,
    val unweightedScore: Float = 0f,
    val factorScores: Map<VerdictFactor, Float> = emptyMap(),
    val conflictDetected: Boolean = false
)