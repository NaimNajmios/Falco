package com.najmi.falco.data.remote.semanticscholar

import com.najmi.falco.data.remote.semanticscholar.dto.SsSearchResponse
import com.najmi.falco.domain.model.Paper
import com.najmi.falco.domain.model.PaperSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SemanticScholarClient @Inject constructor(
    private val httpClient: HttpClient
) {
    private val baseUrl = "https://api.semanticscholar.org/graph/v1"
    private val fields = listOf(
        "paperId,title,abstract,authors,year,citationCount," +
        "isOpenAccess,externalIds,fieldsOfStudy"
    ).joinToString(",")

    suspend fun searchPapers(query: String, limit: Int = 5): List<Paper> {
        val response: SsSearchResponse = httpClient.get("$baseUrl/paper/search") {
            parameter("query", query)
            parameter("limit", limit)
            parameter("fields", fields)
        }.body()

        return response.data.mapNotNull { it.toPaper() }
    }

    private fun com.najmi.falco.data.remote.semanticscholar.dto.SsPaperDto.toPaper(): Paper? {
        if (abstract.isNullOrBlank()) return null

        return Paper(
            id = paperId,
            title = title,
            abstract = abstract ?: "",
            authors = authors.map { it.name },
            year = year,
            citationCount = citationCount,
            isOpenAccess = isOpenAccess,
            doi = externalIds?.DOI,
            url = "https://www.semanticscholar.org/paper/$paperId",
            source = PaperSource.SEMANTIC_SCHOLAR,
            fieldsOfStudy = fieldsOfStudy
        )
    }
}
