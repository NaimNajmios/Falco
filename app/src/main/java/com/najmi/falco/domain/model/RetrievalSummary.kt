package com.najmi.falco.domain.model

import kotlinx.serialization.Serializable

enum class FailureReason {
    TIMEOUT,
    RATE_LIMITED,
    NO_RESULTS,
    UNKNOWN_ERROR
}

@Serializable
data class FailedSource(
    val name: String,
    val reason: FailureReason
)

@Serializable
data class RetrievalSummary(
    val totalFetched: Int = 0,
    val openAlexCount: Int = 0,
    val semanticScholarCount: Int = 0,
    val excludedCount: Int = 0,
    val failedSources: List<FailedSource> = emptyList(),
    val evidenceQualityScore: Float = 0f
)