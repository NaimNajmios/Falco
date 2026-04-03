package com.najmi.falco.domain.model

enum class VerificationStage(val label: String) {
    CLASSIFYING("Identifying claim type"),
    EXPANDING("Generating academic search queries"),
    RETRIEVING("Searching academic databases"),
    QUALITY_GATING("Filtering papers by quality"),
    TEMPORAL_CHECK("Checking evidence freshness"),
    ACTOR_CLASSIFICATION("Classifying paper stances"),
    CRITIC_REVIEW("Reviewing with critic"),
    GROUNDING("Verifying reasoning against abstracts"),
    AGGREGATING("Building verdict")
}

sealed class VerificationState {
    object Idle : VerificationState()
    data class InProgress(
        val stage: VerificationStage,
        val message: String,
        val processedCount: Int = 0,
        val totalCount: Int = 0
    ) : VerificationState() {
        val progress: Float
            get() = if (totalCount > 0) processedCount.toFloat() / totalCount else 0f
        
        val progressText: String
            get() = if (totalCount > 0) "$processedCount/$totalCount" else ""
    }
    data class Success(val verdict: Verdict) : VerificationState()
    data class Error(val stage: VerificationStage?, val message: String) : VerificationState()
}
