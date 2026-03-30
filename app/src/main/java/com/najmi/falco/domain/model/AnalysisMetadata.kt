package com.najmi.falco.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisMetadata(
    val totalTokensAnalyzed: Int = 0,
    val estimatedFullTextTokens: Int? = null,
    val efficiencyComparison: String? = null,
    val analysisDurationMs: Long = 0,
    val databasesQueried: List<String> = emptyList(),
    val algorithmVersion: String = "Smart Chunking v1.2",
    val completedAt: Long = System.currentTimeMillis()
) {
    fun calculateEfficiency(): String? {
        val fullText = estimatedFullTextTokens ?: return null
        if (fullText <= 0 || totalTokensAnalyzed <= 0) return null
        
        val savings = ((fullText - totalTokensAnalyzed).toFloat() / fullText * 100).toInt()
        return if (savings > 0) {
            "$savings% more efficient than full-text analysis"
        } else {
            null
        }
    }
}

@Serializable
data class AnalyzedChunk(
    val content: String,
    val sourceSection: String,
    val estimatedTokens: Int,
    val keyEvidence: String? = null,
    val confidence: Float? = null
)

enum class AnalysisDepth {
    LIGHT,
    STANDARD,
    DEEP;

    companion object {
        fun fromChunks(chunksAnalyzed: Int): AnalysisDepth = when {
            chunksAnalyzed <= 1 -> LIGHT
            chunksAnalyzed == 2 -> STANDARD
            else -> DEEP
        }
    }
}

enum class CertaintyLevel {
    HIGH,
    MODERATE,
    LOW;

    companion object {
        fun fromConfidence(confidence: Float): CertaintyLevel = when {
            confidence >= 0.8f -> HIGH
            confidence >= 0.5f -> MODERATE
            else -> LOW
        }
    }
}

@Serializable
data class ConfidenceFactor(
    val type: String,
    val value: String,
    val impact: String
)

@Serializable
data class UncertaintyInfo(
    val gaps: List<String> = emptyList(),
    val qualityWarnings: List<String> = emptyList(),
    val recencyAlert: String? = null,
    val fundingDisclosure: String? = null
)

data class ConsensusInfo(
    val totalPapers: Int,
    val supportingCount: Int,
    val opposingCount: Int,
    val neutralCount: Int
) {
    fun getConsensusText(): String {
        return when {
            supportingCount == totalPapers && opposingCount == 0 && neutralCount == 0 ->
                "$totalPapers of $totalPapers papers agree"
            opposingCount == totalPapers && supportingCount == 0 && neutralCount == 0 ->
                "$totalPapers of $totalPapers papers disagree"
            else -> {
                val parts = mutableListOf<String>()
                if (supportingCount > 0) parts.add("$supportingCount support")
                if (opposingCount > 0) parts.add("$opposingCount refute")
                if (neutralCount > 0) parts.add("$neutralCount neutral")
                "Mixed evidence (${parts.joinToString(", ")})"
            }
        }
    }
}
