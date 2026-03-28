package com.najmi.falco.domain.model

enum class VerificationStage(val label: String) {
    CLASSIFYING("Identifying claim type"),
    EXPANDING_QUERIES("Generating academic search queries"),
    RETRIEVING_PAPERS("Searching academic databases"),
    ACTOR_CLASSIFICATION("Classifying paper stances"),
    AGGREGATING("Building verdict")
}

sealed class VerificationState {
    object Idle : VerificationState()
    data class InProgress(
        val stage: VerificationStage,
        val message: String
    ) : VerificationState()
    data class Success(val verdict: Verdict) : VerificationState()
    data class Error(val message: String) : VerificationState()
}
