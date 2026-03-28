package com.najmi.falco.agent

import com.najmi.falco.data.remote.LlmProvider

interface IFalcoAgent<I, O> {
    val agentName: String
    val preferredProvider: LlmProvider
    suspend fun execute(input: I): O
}
