package com.najmi.falco.data.remote.semanticscholar.dto

import com.najmi.falco.domain.model.PaperSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SsPaperDto(
    val paperId: String,
    val title: String,
    val abstract: String? = null,
    val authors: List<SsAuthorDto> = emptyList(),
    val year: Int? = null,
    @SerialName("citationCount") val citationCount: Int = 0,
    @SerialName("isOpenAccess") val isOpenAccess: Boolean = false,
    @SerialName("externalIds") val externalIds: SsExternalIds? = null,
    @SerialName("fieldsOfStudy") val fieldsOfStudy: List<String> = emptyList()
)

@Serializable
data class SsAuthorDto(val name: String)

@Serializable
data class SsExternalIds(val DOI: String? = null)

@Serializable
data class SsSearchResponse(
    val total: Int,
    val data: List<SsPaperDto>
)
