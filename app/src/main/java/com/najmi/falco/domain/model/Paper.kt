package com.najmi.falco.domain.model

data class Paper(
    val id: String,
    val title: String,
    val abstract: String,
    val authors: List<String>,
    val year: Int?,
    val citationCount: Int,
    val isOpenAccess: Boolean,
    val doi: String?,
    val url: String?,
    val source: PaperSource,
    val fieldsOfStudy: List<String>
)
