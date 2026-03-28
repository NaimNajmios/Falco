package com.najmi.falco.domain.model

data class PaperQuality(
    val paper: Paper,
    val qualityScore: Float,
    val citationTier: CitationTier,
    val freshnessFlag: FreshnessFlag,
    val passesGate: Boolean
)
