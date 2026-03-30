package com.najmi.falco.pipeline

import com.najmi.falco.domain.model.Paper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow

class PaperBackpressureQueue(
    private val capacity: Int = DEFAULT_CAPACITY
) {
    companion object {
        const val DEFAULT_CAPACITY = 15
    }

    private val channel = Channel<Paper>(capacity)

    val papersFlow: Flow<Paper> = channel.consumeAsFlow()

    suspend fun send(paper: Paper) {
        channel.send(paper)
    }

    suspend fun sendAll(papers: List<Paper>) {
        papers.forEach { paper ->
            channel.send(paper)
        }
    }

    fun close() {
        channel.close()
    }
}
