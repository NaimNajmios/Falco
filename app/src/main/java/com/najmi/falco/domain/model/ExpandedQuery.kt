package com.najmi.falco.domain.model

enum class QueryIntent {
    BROAD,
    NARROW,
    CONTRASTIVE
}

data class ExpandedQuery(
    val text: String,
    val intent: QueryIntent,
    val resultsFound: Int = 0
)