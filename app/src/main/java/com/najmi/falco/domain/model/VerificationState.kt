package com.najmi.falco.domain.model

enum class VerificationStage(val label: String) {
    CLASSIFYING("Identifying claim type"),
    EXPANDING_QUERIES("Generating academic search queries"),
    RETRIEVING_PAPERS("Searching academic databases"),
    QUALITY_GATING("Filtering papers by quality"),
    TEMPORAL_CHECK("Checking evidence freshness"),
    ACTOR_CLASSIFICATION("Classifying paper stances"),
    CRITIC_REVIEW("Reviewing with critic"),
    GROUNDING("Verifying reasoning against abstracts"),
    RAG_VERIFICATION("Verifying facts against sources"),
    TEMPORAL_OVERRIDE("Checking temporal consistency"),
    AGGREGATING("Building verdict")
}

sealed class VerificationState {
    object Idle : VerificationState()
    data class InProgress(
        val stage: VerificationStage,
        val message: String
    ) : VerificationState()
    data class Success(val verdict: Verdict) : VerificationState()
    data class Error(val stage: VerificationStage?, val message: String) : VerificationState()
}
