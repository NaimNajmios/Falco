package com.najmi.falco.data.remote.semanticscholar

import android.util.Log
import com.najmi.falco.data.remote.semanticscholar.dto.SsSearchResponse
import com.najmi.falco.domain.model.Paper
import com.najmi.falco.domain.model.PaperSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
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

    companion object {
        private const val TAG = "SemanticScholarClient"
    }

    suspend fun searchPapers(query: String, limit: Int = 5): List<Paper> {
        val response = httpClient.get("$baseUrl/paper/search") {
            parameter("query", query)
            parameter("limit", limit)
            parameter("fields", fields)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val searchResponse: SsSearchResponse = response.body()
                return searchResponse.data.mapNotNull { it.toPaper() }
            }
            HttpStatusCode.TooManyRequests -> {
                Log.w(TAG, "Rate limit exceeded for Semantic Scholar API")
                throw RateLimitException("Semantic Scholar rate limit exceeded")
            }
            else -> {
                Log.e(TAG, "Semantic Scholar API error: ${response.status}")
                throw SearchException("Semantic Scholar API returned ${response.status}")
            }
        }
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

class RateLimitException(message: String) : Exception(message)
class SearchException(message: String) : Exception(message)
