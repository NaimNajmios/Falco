package com.najmi.falco.data.remote.openapi.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAlexWorksResponse(
    val meta: OpenAlexMeta? = null,
    val results: List<OpenAlexWorkDto> = emptyList()
)

@Serializable
data class OpenAlexMeta(
    val count: Int? = null,
    @SerialName("per_page")
    val perPage: Int? = null,
    val page: Int? = null
)

@Serializable
data class OpenAlexWorkDto(
    val id: String? = null,
    val doi: String? = null,
    val title: String? = null,
    @SerialName("abstract_inverted_index")
    val abstractInvertedIndex: Map<String, List<Int>>? = null,
    @SerialName("publication_year")
    val publicationYear: Int? = null,
    @SerialName("cited_by_count")
    val citedByCount: Int? = 0,
    @SerialName("open_access")
    val openAccess: OpenAlexOpenAccess? = null,
    @SerialName("primary_location")
    val primaryLocation: OpenAlexPrimaryLocation? = null,
    val authorships: List<OpenAlexAuthorship>? = null,
    val concepts: List<OpenAlexConcept>? = null
)

@Serializable
data class OpenAlexOpenAccess(
    val isOa: Boolean? = false,
    @SerialName("oa_status")
    val oaStatus: String? = null
)

@Serializable
data class OpenAlexPrimaryLocation(
    val source: OpenAlexSource? = null,
    val url: String? = null
)

@Serializable
data class OpenAlexSource(
    val id: String? = null,
    val displayName: String? = null
)

@Serializable
data class OpenAlexAuthorship(
    val author: OpenAlexAuthor? = null,
    val authorPosition: String? = null
)

@Serializable
data class OpenAlexAuthor(
    val id: String? = null,
    val displayName: String? = null
)

@Serializable
data class OpenAlexConcept(
    val id: String? = null,
    val displayName: String? = null,
    val score: Float? = null
)
