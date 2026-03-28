package com.najmi.falco.domain.model

data class PaperStance(
    val paper: Paper,
    val actorStance: Stance,
    val actorReasoning: String,
    val confidence: Float,
    val keyEvidence: String,
    val relevanceScore: Float
)
