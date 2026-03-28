package com.najmi.falco.agent

import com.najmi.falco.data.remote.LlmProvider

interface IFalcoAgent<I, O> {
    val agentName: String
    val defaultProvider: LlmProvider
    suspend fun execute(input: I, preferredProvider: LlmProvider? = null): Result<O>
}
