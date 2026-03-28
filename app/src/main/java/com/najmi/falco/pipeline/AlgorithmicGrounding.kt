package com.najmi.falco.pipeline

import com.najmi.falco.domain.model.PaperStance
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlgorithmicGrounding @Inject constructor() {

    fun verify(stances: List<PaperStance>): List<PaperStance> {
        return stances.map { stance ->
            val groundingScore = computeGroundingScore(
                reasoning = stance.actorReasoning,
                abstract = stance.paper.abstract
            )
            stance.copy(groundingScore = groundingScore)
        }
    }

    private fun computeGroundingScore(reasoning: String, abstract: String): Float {
        if (reasoning.isBlank() || abstract.isBlank()) return 0.0f
        
        val reasoningWords = extractKeyTerms(reasoning)
        val abstractWords = extractKeyTerms(abstract)
        
        if (reasoningWords.isEmpty()) return 0.0f
        
        val matches = reasoningWords.count { term ->
            abstractWords.any { abstractTerm ->
                abstractTerm.contains(term, ignoreCase = true) ||
                term.contains(abstractTerm, ignoreCase = true)
            }
        }
        
        return (matches.toFloat() / reasoningWords.size).coerceIn(0.0f, 1.0f)
    }

    private fun extractKeyTerms(text: String): List<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "must", "shall", "can", "need", "dare",
            "to", "of", "in", "for", "on", "with", "at", "by", "from", "as",
            "into", "through", "during", "before", "after", "above", "below",
            "between", "under", "again", "further", "then", "once", "here",
            "there", "when", "where", "why", "how", "all", "each", "few",
            "more", "most", "other", "some", "such", "no", "nor", "not",
            "only", "own", "same", "so", "than", "too", "very", "just",
            "also", "now", "and", "or", "but", "if", "because", "until",
            "while", "although", "that", "this", "these", "those", "it",
            "its", "they", "their", "them", "we", "our", "you", "your", "he",
            "she", "him", "her", "his", "i", "me", "my", "show", "shows",
            "found", "study", "paper", "research", "result", "results",
            "method", "methods", "approach", "propose", "proposed", "use",
            "using", "used", "based", "however", "therefore", "thus",
            "shows", "demonstrate", "demonstrates", "demonstrated"
        )
        
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 3 }
            .filter { it !in stopWords }
            .distinct()
            .toList()
    }
}
