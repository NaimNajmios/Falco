package com.najmi.falco.data.repository

import com.najmi.falco.domain.model.PaperSource
import javax.inject.Inject
import javax.inject.Singleton

data class OutletCredibility(
    val sourceName: String,
    val credibilityScore: Float,
    val biasRating: String,
    val isHighQuality: Boolean,
    val category: String
)

@Singleton
class OutletCredibilityRepository @Inject constructor() {

    private val credibilityCache = mutableMapOf<String, OutletCredibility>()

    private val knownOutlets = mapOf(
        "nature" to OutletCredibility("Nature", 0.95f, "Neutral", true, "Scientific Journal"),
        "science" to OutletCredibility("Science", 0.95f, "Neutral", true, "Scientific Journal"),
        "cell" to OutletCredibility("Cell", 0.93f, "Neutral", true, "Scientific Journal"),
        "lancet" to OutletCredibility("The Lancet", 0.94f, "Neutral", true, "Medical Journal"),
        "nejm" to OutletCredibility("NEJM", 0.94f, "Neutral", true, "Medical Journal"),
        "plos" to OutletCredibility("PLOS", 0.85f, "Neutral", true, "Open Access"),
        "biorxiv" to OutletCredibility("bioRxiv", 0.70f, "Neutral", false, "Preprint"),
        "medrxiv" to OutletCredibility("medRxiv", 0.70f, "Neutral", false, "Preprint"),
        "arxiv" to OutletCredibility("arXiv", 0.75f, "Neutral", false, "Preprint"),
        "ssrn" to OutletCredibility("SSRN", 0.70f, "Neutral", false, "Preprint"),
        "pubmed" to OutletCredibility("PubMed", 0.88f, "Neutral", true, "Database"),
        "semantic scholar" to OutletCredibility("Semantic Scholar", 0.82f, "Neutral", true, "Database"),
        "google scholar" to OutletCredibility("Google Scholar", 0.80f, "Neutral", true, "Database"),
        "ieee" to OutletCredibility("IEEE", 0.90f, "Neutral", true, "Technical Journal"),
        "acm" to OutletCredibility("ACM", 0.90f, "Neutral", true, "Technical Journal"),
        "springer" to OutletCredibility("Springer", 0.85f, "Neutral", true, "Publisher"),
        "elsevier" to OutletCredibility("Elsevier", 0.80f, "Neutral", true, "Publisher"),
        "wiley" to OutletCredibility("Wiley", 0.82f, "Neutral", true, "Publisher"),
        "oxford" to OutletCredibility("Oxford University Press", 0.88f, "Neutral", true, "Publisher"),
        "cambridge" to OutletCredibility("Cambridge University Press", 0.88f, "Neutral", true, "Publisher"),
        "mit press" to OutletCredibility("MIT Press", 0.87f, "Neutral", true, "Publisher"),
        "sage" to OutletCredibility("SAGE", 0.80f, "Neutral", true, "Publisher"),
        "tandf" to OutletCredibility("Taylor & Francis", 0.78f, "Neutral", true, "Publisher"),
        "reuters" to OutletCredibility("Reuters", 0.85f, "Neutral", true, "News"),
        "ap" to OutletCredibility("Associated Press", 0.85f, "Neutral", true, "News"),
        "bbc" to OutletCredibility("BBC", 0.82f, "Left-Center", true, "News"),
        "npr" to OutletCredibility("NPR", 0.80f, "Left-Center", true, "News"),
        "the guardian" to OutletCredibility("The Guardian", 0.72f, "Left", true, "News"),
        "the new york times" to OutletCredibility("NYT", 0.75f, "Left-Center", true, "News"),
        "washington post" to OutletCredibility("Washington Post", 0.75f, "Left-Center", true, "News"),
        "wall street journal" to OutletCredibility("WSJ", 0.75f, "Right-Center", true, "News"),
        "forbes" to OutletCredibility("Forbes", 0.65f, "Right-Center", false, "Magazine"),
        "huffpost" to OutletCredibility("HuffPost", 0.55f, "Left", false, "News"),
        "breitbart" to OutletCredibility("Breitbart", 0.30f, "Right", false, "News"),
        "fox news" to OutletCredibility("Fox News", 0.40f, "Right", false, "News"),
        "cnn" to OutletCredibility("CNN", 0.55f, "Left-Center", false, "News"),
        "msnbc" to OutletCredibility("MSNBC", 0.50f, "Left", false, "News")
    )

    suspend fun getCredibility(source: String): OutletCredibility {
        val normalizedSource = source.lowercase()
        
        credibilityCache[normalizedSource]?.let { return it }

        val matched = knownOutlets.entries.find { (key, _) ->
            normalizedSource.contains(key) || key in normalizedSource
        }

        val credibility = if (matched != null) {
            matched.value
        } else {
            OutletCredibility(
                sourceName = source,
                credibilityScore = 0.50f,
                biasRating = "Unknown",
                isHighQuality = false,
                category = "Unknown"
            )
        }

        credibilityCache[normalizedSource] = credibility
        return credibility
    }

    suspend fun isCredible(source: String, minimumScore: Float = 0.40f): Boolean {
        return getCredibility(source).credibilityScore >= minimumScore
    }

    fun getCredibilityThreshold(): Float = 0.40f

    fun getBiasLabel(score: Float): String {
        return when {
            score >= 0.85f -> "Very High"
            score >= 0.70f -> "High"
            score >= 0.50f -> "Medium"
            score >= 0.35f -> "Low"
            else -> "Very Low"
        }
    }
}
