package com.najmi.falco.pipeline

import com.najmi.falco.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemporalFreshnessAnalyzer @Inject constructor() {

    fun analyze(qualityPapers: List<PaperQuality>, claimType: ClaimType): List<PaperQuality> {
        return qualityPapers
    }

    fun generateTemporalWarning(qualityPapers: List<PaperQuality>): String? {
        if (qualityPapers.isEmpty()) return null
        
        val staleCount = qualityPapers.count { it.freshnessFlag == FreshnessFlag.STALE }
        val totalCount = qualityPapers.size
        val staleRatio = staleCount.toFloat() / totalCount
        
        return if (staleRatio > 0.5f) {
            val avgYear = qualityPapers
                .filter { it.paper.year != null }
                .mapNotNull { it.paper.year }
                .average()
                .takeIf { !it.isNaN() }
                ?.toInt()
            
            "⚠ Temporal Note: Most supporting evidence predates ${avgYear ?: "recent years"}. " +
            "This field evolves rapidly. Findings may have been superseded by more recent work."
        } else null
    }

    fun computeFieldFreshness(qualityPapers: List<PaperQuality>): String {
        if (qualityPapers.isEmpty()) return "Unknown"
        
        return qualityPapers
            .flatMap { it.paper.fieldsOfStudy }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "Unknown"
    }
}
