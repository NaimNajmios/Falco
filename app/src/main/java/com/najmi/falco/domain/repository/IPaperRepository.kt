package com.najmi.falco.domain.repository

import com.najmi.falco.domain.model.Paper

interface IPaperRepository {
    suspend fun search(query: String, limit: Int = 5): List<Paper>
    suspend fun searchAll(queries: List<String>): List<Paper>
}
