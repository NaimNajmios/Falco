package com.najmi.falco.data.remote

import com.najmi.falco.domain.model.TokenUsage

data class LlmResponse(
    val text: String,
    val usage: TokenUsage
)

interface LlmClient {
    suspend fun chat(prompt: String): LlmResponse
}
