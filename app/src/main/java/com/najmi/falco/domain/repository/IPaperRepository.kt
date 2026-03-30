package com.najmi.falco.domain.repository

import com.najmi.falco.domain.model.Paper

data class PaperSearchResult(
    val papers: List<Paper>,
    val databasesQueried: List<String>
)

interface IPaperRepository {
    suspend fun search(query: String, limit: Int = 5): List<Paper>
    suspend fun searchAll(queries: List<String>): PaperSearchResult
}
