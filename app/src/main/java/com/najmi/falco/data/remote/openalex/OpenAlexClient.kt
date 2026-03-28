package com.najmi.falco.data.remote.openapi

import com.najmi.falco.data.remote.openapi.dto.OpenAlexWorkDto
import com.najmi.falco.data.remote.openapi.dto.OpenAlexWorksResponse
import com.najmi.falco.domain.model.Paper
import com.najmi.falco.domain.model.PaperSource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAlexClient @Inject constructor(
    private val httpClient: HttpClient
) {
    private val baseUrl = "https://api.openalex.org"
    private val email = "naim@example.com"

    private val fields = listOf(
        "id,doi,title,abstract_inverted_index,publication_year,cited_by_count," +
        "open_access,primary_location,authorships,concepts"
    ).joinToString(",")

    suspend fun searchPapers(query: String, limit: Int = 5): List<Paper> {
        val response: OpenAlexWorksResponse = httpClient.get("$baseUrl/works") {
            parameter("search", query)
            parameter("filter", "has_abstract:true")
            parameter("per_page", limit)
            parameter("select", fields)
            parameter("mailto", email)
        }.body()

        return response.results.mapNotNull { it.toPaper() }
    }

    private fun OpenAlexWorkDto.toPaper(): Paper? {
        val abstract = reconstructAbstract(abstractInvertedIndex)
        if (abstract.isBlank()) return null

        return Paper(
            id = id ?: doi ?: title.hashCode().toString(),
            title = title ?: "",
            abstract = abstract,
            authors = authorships?.mapNotNull { it.author?.displayName } ?: emptyList(),
            year = publicationYear,
            citationCount = citedByCount ?: 0,
            isOpenAccess = openAccess?.isOa ?: false,
            doi = doi,
            url = primaryLocation?.url ?: doi?.let { "https://doi.org/$it" },
            source = PaperSource.OPEN_ALEX,
            fieldsOfStudy = concepts?.mapNotNull { it.displayName } ?: emptyList()
        )
    }

    private fun reconstructAbstract(invertedIndex: Map<String, List<Int>>?): String {
        if (invertedIndex.isNullOrEmpty()) return ""
        
        val maxPos = invertedIndex.values.flatten().maxOrNull() ?: return ""
        if (maxPos < 0) return ""
        
        val words = Array(maxPos + 1) { "" }
        invertedIndex.forEach { (word, positions) ->
            positions.forEach { pos ->
                if (pos in words.indices) {
                    words[pos] = word
                }
            }
        }
        
        return words.joinToString(" ").trim()
    }
}
