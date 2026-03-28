package com.najmi.falco.pipeline

import com.najmi.falco.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class PaperQualityGate @Inject constructor() {

    fun filter(papers: List<Paper>, claimType: ClaimType): List<PaperQuality> {
        return papers.map { paper ->
            val citationTier = computeCitationTier(paper)
            val freshnessFlag = computeFreshnessFlag(paper)
            val score = compositeScore(paper, citationTier, freshnessFlag)
            val passes = passesGate(paper, score, citationTier, freshnessFlag)
            PaperQuality(paper, score, citationTier, freshnessFlag, passes)
        }.filter { it.passesGate }
    }

    private fun computeCitationTier(paper: Paper): CitationTier {
        val field = paper.fieldsOfStudy.firstOrNull() ?: "Unknown"
        val thresholds = CITATION_THRESHOLDS[field] ?: CITATION_THRESHOLDS["Unknown"]!!
        
        return when {
            paper.citationCount >= thresholds.high -> CitationTier.HIGH
            paper.citationCount >= thresholds.medium -> CitationTier.MEDIUM
            else -> CitationTier.LOW
        }
    }

    private fun computeFreshnessFlag(paper: Paper): FreshnessFlag {
        val currentYear = 2026
        val paperYear = paper.year ?: return FreshnessFlag.UNKNOWN
        val yearsOld = currentYear - paperYear
        
        val field = paper.fieldsOfStudy.firstOrNull() ?: "Unknown"
        
        val (recentThreshold, staleThreshold) = when {
            field.equals("Computer Science", ignoreCase = true) ||
            field.equals("Artificial Intelligence", ignoreCase = true) ||
            field.equals("Machine Learning", ignoreCase = true) -> Pair(1, 3)
            
            field.equals("Medicine", ignoreCase = true) ||
            field.equals("Biology", ignoreCase = true) ||
            field.equals("Pharmaceutical Sciences", ignoreCase = true) -> Pair(1, 2)
            
            field.equals("Physics", ignoreCase = true) ||
            field.equals("Chemistry", ignoreCase = true) -> Pair(2, 4)
            
            field.equals("Engineering", ignoreCase = true) ||
            field.equals("Electronics", ignoreCase = true) -> Pair(2, 4)
            
            field.equals("Social Sciences", ignoreCase = true) ||
            field.equals("Psychology", ignoreCase = true) -> Pair(3, 5)
            
            field.equals("History", ignoreCase = true) ||
            field.equals("Philosophy", ignoreCase = true) -> Pair(Int.MAX_VALUE, Int.MAX_VALUE)
            
            else -> Pair(2, 5)
        }

        return when {
            yearsOld >= staleThreshold -> FreshnessFlag.VERY_OLD
            yearsOld >= recentThreshold -> FreshnessFlag.STALE
            yearsOld > 0 -> FreshnessFlag.RECENT
            else -> FreshnessFlag.FRESH
        }
    }

    private fun compositeScore(paper: Paper, citationTier: CitationTier, freshnessFlag: FreshnessFlag): Float {
        val citationWeight = when (citationTier) {
            CitationTier.HIGH -> 1.0f
            CitationTier.MEDIUM -> 0.6f
            CitationTier.LOW -> 0.3f
            CitationTier.UNKNOWN -> 0.1f
        }
        
        val freshnessWeight = when (freshnessFlag) {
            FreshnessFlag.FRESH -> 1.0f
            FreshnessFlag.RECENT -> 0.8f
            FreshnessFlag.STALE -> 0.4f
            FreshnessFlag.VERY_OLD -> 0.1f
            FreshnessFlag.UNKNOWN -> 0.5f
        }
        
        val openAccessWeight = if (paper.isOpenAccess) 1.0f else 0.7f
        
        val abstractLengthWeight = when {
            paper.abstract.length >= 200 -> 1.0f
            paper.abstract.length >= 100 -> 0.7f
            paper.abstract.length >= 50 -> 0.4f
            else -> 0.1f
        }
        
        return (citationWeight * 0.4f) + (freshnessWeight * 0.3f) + 
               (openAccessWeight * 0.15f) + (abstractLengthWeight * 0.15f)
    }

    private fun passesGate(
        paper: Paper,
        score: Float,
        citationTier: CitationTier,
        freshnessFlag: FreshnessFlag
    ): Boolean {
        if (paper.abstract.isNullOrBlank() || paper.abstract.length < 80) return false
        if (score < 0.25f) return false
        if (citationTier == CitationTier.LOW && (freshnessFlag == FreshnessFlag.STALE || freshnessFlag == FreshnessFlag.VERY_OLD)) return false
        return true
    }

    companion object {
        private data class CitationThresholds(val high: Int, val medium: Int)
        
        private val CITATION_THRESHOLDS = mapOf(
            "Computer Science" to CitationThresholds(50, 10),
            "Artificial Intelligence" to CitationThresholds(50, 10),
            "Machine Learning" to CitationThresholds(50, 10),
            "Medicine" to CitationThresholds(100, 20),
            "Biology" to CitationThresholds(100, 20),
            "Pharmaceutical Sciences" to CitationThresholds(100, 20),
            "Social Sciences" to CitationThresholds(20, 5),
            "Psychology" to CitationThresholds(20, 5),
            "Engineering" to CitationThresholds(30, 5),
            "Electronics" to CitationThresholds(30, 5),
            "Physics" to CitationThresholds(30, 5),
            "Chemistry" to CitationThresholds(30, 5),
            "Unknown" to CitationThresholds(10, 3)
        )
    }
}
