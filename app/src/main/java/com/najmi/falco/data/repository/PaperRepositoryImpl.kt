package com.najmi.falco.data.repository

import com.najmi.falco.data.remote.semanticscholar.SemanticScholarClient
import com.najmi.falco.domain.model.Paper
import com.najmi.falco.domain.repository.IPaperRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaperRepositoryImpl @Inject constructor(
    private val semanticScholar: SemanticScholarClient
) : IPaperRepository {

    override suspend fun search(query: String, limit: Int): List<Paper> {
        return semanticScholar.searchPapers(query, limit)
    }

    override suspend fun searchAll(queries: List<String>): List<Paper> {
        return queries.flatMap { query ->
            semanticScholar.searchPapers(query, limit = 5)
        }.let { papers -> deduplicate(papers) }
    }

    private fun deduplicate(papers: List<Paper>): List<Paper> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Paper>()
        for (paper in papers) {
            val key = paper.doi ?: paper.title.lowercase()
                .replace(Regex("[^a-z0-9 ]"), "")
                .split(" ")
                .filter { it.length > 3 }
                .take(6)
                .joinToString(" ")
            if (seen.add(key)) result.add(paper)
        }
        return result
    }
}
