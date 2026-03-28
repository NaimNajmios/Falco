package com.najmi.falco.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val provider: String = "unknown",
    val model: String? = null
)
