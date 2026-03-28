package com.najmi.falco.domain.model

data class PaperStance(
    val paper: Paper,
    val actorStance: Stance,
    val actorReasoning: String,
    val confidence: Float,
    val keyEvidence: String,
    val relevanceScore: Float,
    val criticStance: Stance? = null,
    val criticChallenge: String? = null,
    val finalStance: Stance? = null,
    val groundingScore: Float? = null,
    val ragScore: Float? = null
)
