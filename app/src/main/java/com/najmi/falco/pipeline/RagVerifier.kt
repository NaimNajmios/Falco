package com.najmi.falco.pipeline

import com.najmi.falco.domain.model.Paper
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.Verdict
import javax.inject.Inject
import javax.inject.Singleton

data class RagVerificationResult(
    val verifiedFacts: List<GroundedFact>,
    val unverifiedFacts: List<String>,
    val groundingScore: Float,
    val finalExplanation: String
)

data class GroundedFact(
    val fact: String,
    val sourceTitle: String,
    val matchedText: String,
    val isGrounded: Boolean
)

@Singleton
class RagVerifier @Inject constructor() {

    fun verify(
        verdict: Verdict,
        papers: List<Paper>
    ): RagVerificationResult {
        val facts = extractFacts(verdict.summary)
        
        if (facts.isEmpty() || papers.isEmpty()) {
            return RagVerificationResult(
                verifiedFacts = emptyList(),
                unverifiedFacts = emptyList(),
                groundingScore = 1.0f,
                finalExplanation = verdict.summary
            )
        }

        val paperTexts = papers.map { it.title to (it.abstract ?: "") }
        
        val verifiedFacts = mutableListOf<GroundedFact>()
        val unverifiedFacts = mutableListOf<String>()
        
        for (fact in facts) {
            val match = findBestMatch(fact, paperTexts)
            if (match != null) {
                verifiedFacts.add(
                    GroundedFact(
                        fact = fact,
                        sourceTitle = match.first,
                        matchedText = match.second,
                        isGrounded = true
                    )
                )
            } else {
                unverifiedFacts.add(fact)
            }
        }
        
        val groundingScore = if (facts.isNotEmpty()) {
            verifiedFacts.size.toFloat() / facts.size
        } else {
            1.0f
        }

        val finalExplanation = if (unverifiedFacts.isNotEmpty() && groundingScore < 0.7f) {
            buildFinalExplanation(verdict.summary, unverifiedFacts, groundingScore)
        } else {
            verdict.summary
        }

        return RagVerificationResult(
            verifiedFacts = verifiedFacts,
            unverifiedFacts = unverifiedFacts,
            groundingScore = groundingScore,
            finalExplanation = finalExplanation
        )
    }

    fun verifyStances(
        stances: List<PaperStance>,
        papers: List<Paper>
    ): List<PaperStance> {
        return stances.map { stance ->
            val paper = stance.paper
            val paperText = "${paper.title} ${paper.abstract ?: ""}".lowercase()
            val reasoning = stance.actorReasoning.lowercase()
            
            val ragScore = computeRagScore(reasoning, paperText)
            stance.copy(ragScore = ragScore)
        }
    }

    private fun computeRagScore(reasoning: String, paperText: String): Float {
        val reasoningKeywords = extractKeywords(reasoning)
        if (reasoningKeywords.isEmpty()) return 0.5f

        val matches = reasoningKeywords.count { keyword ->
            keyword in paperText || paperText.contains(keyword)
        }

        return (matches.toFloat() / reasoningKeywords.size).coerceIn(0f, 1f)
    }

    private fun extractFacts(text: String): List<String> {
        val sentences = text.split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 20 }
        
        return sentences.map { sentence ->
            val numericValues = extractNumericClaims(sentence)
            if (numericValues.isNotEmpty()) {
                "$sentence [CONTAINS_DATA]"
            } else {
                sentence
            }
        }
    }

    private fun extractNumericClaims(text: String): List<String> {
        val numericPatterns = listOf(
            Regex("\\d+%"),
            Regex("\\d+\\.\\d+%"),
            Regex("\\d+\\s+(million|billion|thousand|hundred)"),
            Regex("\\d+\\s+(years?|months?|days?|percent)"),
            Regex("(increased|decreased|changed| grew | declined )\\s+\\d+")
        )
        
        return numericPatterns.flatMap { pattern ->
            pattern.findAll(text).map { it.value }
        }
    }

    private fun findBestMatch(fact: String, paperTexts: List<Pair<String, String>>): Pair<String, String>? {
        val factKeywords = extractKeywords(fact.lowercase())
        
        for ((title, abstract) in paperTexts) {
            val fullText = "$title $abstract".lowercase()
            val matches = factKeywords.count { keyword ->
                keyword in fullText
            }
            
            if (matches >= factKeywords.size * 0.5 && factKeywords.isNotEmpty()) {
                val matchedText = factKeywords
                    .filter { it in fullText }
                    .take(3)
                    .joinToString(", ")
                
                return title to matchedText
            }
        }
        
        return null
    }

    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "must", "shall", "can", "need", "that",
            "this", "these", "those", "it", "its", "they", "them", "their",
            "we", "our", "you", "your", "he", "she", "him", "her", "his",
            "i", "me", "my", "and", "or", "but", "if", "because", "until",
            "while", "although", "then", "so", "than", "too", "very", "just",
            "also", "now", "study", "research", "found", "show", "shows",
            "showed", "demonstrate", "demonstrated", "however", "therefore"
        )

        return text
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 3 }
            .filter { it !in stopWords }
            .distinct()
            .toList()
    }

    private fun buildFinalExplanation(
        originalSummary: String,
        unverifiedFacts: List<String>,
        groundingScore: Float
    ): String {
        val disclaimer = "\n\n[NOTE: This verdict has a grounding score of ${(groundingScore * 100).toInt()}%. Some claims could not be fully verified against available sources.]"
        
        if (groundingScore < 0.5f) {
            return originalSummary + disclaimer + "\n[CAUTION: Low confidence due to unverified facts.]"
        }
        
        return originalSummary + disclaimer
    }
}
