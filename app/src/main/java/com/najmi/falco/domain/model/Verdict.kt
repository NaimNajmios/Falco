package com.najmi.falco.domain.model

data class Verdict(
    val claimId: String,
    val claim: String,
    val lean: Stance,
    val confidence: Float,
    val summary: String,
    val stances: List<PaperStance>,
    val totalPapersRetrieved: Int,
    val totalPapersPassedGate: Int,
    val supportingCount: Int,
    val opposingCount: Int,
    val neutralCount: Int,
    val dominantField: String,
    val temporalWarning: String?,
    val completedAt: Long = System.currentTimeMillis(),
    val analysisMetadata: AnalysisMetadata = AnalysisMetadata(),
    val uncertaintyInfo: UncertaintyInfo = UncertaintyInfo()
) {
    val consensusInfo: ConsensusInfo
        get() = ConsensusInfo(
            totalPapers = stances.size,
            supportingCount = supportingCount,
            opposingCount = opposingCount,
            neutralCount = neutralCount
        )

    val certaintyLevel: CertaintyLevel
        get() = CertaintyLevel.fromConfidence(confidence)
}
