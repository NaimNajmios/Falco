package com.najmi.falco.chunking

import com.najmi.falco.domain.model.Paper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentChunker @Inject constructor() {

    data class Config(
        val maxChunksPerPaper: Int = EvidenceChunk.MAX_CHUNKS_PER_PAPER,
        val abstractMaxTokens: Int = EvidenceChunk.ABSTRACT_MAX_TOKENS,
        val conclusionMaxTokens: Int = EvidenceChunk.CONCLUSION_MAX_TOKENS,
        val bodyParagraphMaxTokens: Int = EvidenceChunk.BODY_PARAGRAPH_MAX_TOKENS,
        val tokensPerWord: Float = 1.3f
    )

    private val defaultConfig = Config()

    fun chunk(
        paper: Paper,
        claim: String,
        config: Config = defaultConfig
    ): List<EvidenceChunk> {
        val keywords = extractKeywords(claim)
        val chunks = mutableListOf<EvidenceChunk>()
        var chunkId = 0

        val abstractChunk = extractAbstract(paper, chunkId++, config)
        if (abstractChunk != null) {
            chunks.add(abstractChunk)
        }

        val conclusionChunk = extractConclusion(paper, chunkId++, keywords, config)
        if (conclusionChunk != null) {
            chunks.add(conclusionChunk)
        }

        val bodyChunks = extractRelevantBodyParagraphs(paper, keywords, chunkId, config)
        chunks.addAll(bodyChunks)

        return chunks.sortedBy { it.priority }.take(config.maxChunksPerPaper)
    }

    private fun extractKeywords(claim: String): List<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "can", "this", "that", "these", "those",
            "i", "you", "he", "she", "it", "we", "they", "what", "which", "who",
            "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
            "by", "from", "as", "into", "through", "during", "before", "after",
            "above", "below", "between", "under", "again", "further", "then",
            "once", "here", "there", "when", "where", "why", "how", "all", "each",
            "few", "more", "most", "other", "some", "such", "no", "nor", "not",
            "only", "own", "same", "so", "than", "too", "very", "s", "t", "just"
        )

        return claim.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 3 && it !in stopWords }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
    }

    private fun extractAbstract(paper: Paper, id: Int, config: Config): EvidenceChunk? {
        val abstract = paper.abstract.trim()
        if (abstract.isBlank()) return null

        val truncated = truncateToTokens(abstract, config.abstractMaxTokens, config)
        val tokens = estimateTokens(truncated, config)

        return EvidenceChunk(
            id = id,
            priority = 0,
            label = "[ABSTRACT]",
            content = truncated,
            estimatedTokens = tokens,
            sourceSection = ChunkSource.ABSTRACT
        )
    }

    private fun extractConclusion(
        paper: Paper,
        id: Int,
        keywords: List<String>,
        config: Config
    ): EvidenceChunk? {
        val conclusionPatterns = listOf(
            "conclusion", "conclusions", "concluding", "summary",
            "in conclusion", "to conclude", "overall", "findings suggest",
            "results indicate", "we conclude", "in summary"
        )

        val abstract = paper.abstract.lowercase()
        var conclusionStart = -1

        for (pattern in conclusionPatterns) {
            val index = abstract.lastIndexOf(pattern)
            if (index > conclusionStart) {
                conclusionStart = index
            }
        }

        if (conclusionStart == -1) {
            return null
        }

        val paragraphStart = abstract.lastIndexOf('.', conclusionStart).let {
            if (it == -1) 0 else it + 2
        }

        val conclusionText = paper.abstract.substring(paragraphStart.coerceAtLeast(0)).trim()
        if (conclusionText.isBlank()) return null

        val truncated = truncateToTokens(conclusionText, config.conclusionMaxTokens, config)
        val tokens = estimateTokens(truncated, config)

        val relevanceScore = calculateRelevanceScore(truncated, keywords)

        return EvidenceChunk(
            id = id,
            priority = if (relevanceScore > 0.3) 1 else 3,
            label = "[CONCLUSION]",
            content = truncated,
            estimatedTokens = tokens,
            sourceSection = ChunkSource.CONCLUSION
        )
    }

    private fun extractRelevantBodyParagraphs(
        paper: Paper,
        keywords: List<String>,
        startId: Int,
        config: Config
    ): List<EvidenceChunk> {
        val paragraphs = splitIntoParagraphs(paper.abstract)
        if (paragraphs.size <= 2) return emptyList()

        val scoredParagraphs = paragraphs.mapIndexed { index, paragraph ->
            val score = calculateRelevanceScore(paragraph, keywords)
            val containsMethod = containsMethodology(paragraph)
            val containsResult = containsResults(paragraph)
            val adjustedScore = score + (if (containsMethod) 0.2f else 0f) + (if (containsResult) 0.15f else 0f)
            
            Triple(index, paragraph, adjustedScore)
        }.filter { it.third > 0.1f }
          .sortedByDescending { it.third }

        val maxBodyChunks = 2
        return scoredParagraphs.take(maxBodyChunks).mapIndexed { idx, (index, paragraph, _) ->
            val truncated = truncateToTokens(paragraph, config.bodyParagraphMaxTokens, config)
            val tokens = estimateTokens(truncated, config)

            EvidenceChunk(
                id = startId + idx,
                priority = 2 + idx,
                label = "[EXCERPT ${idx + 1}]",
                content = truncated,
                estimatedTokens = tokens,
                sourceSection = ChunkSource.BODY_PARAGRAPH,
                sourceParagraph = index
            )
        }
    }

    private fun splitIntoParagraphs(text: String): List<String> {
        return text.split(Regex("\\n\\s*\\n|\\.\\s+(?=[A-Z])"))
            .map { it.trim() }
            .filter { it.length > 50 }
    }

    private fun calculateRelevanceScore(text: String, keywords: List<String>): Float {
        if (keywords.isEmpty()) return 0.5f

        val textLower = text.lowercase()
        val wordCount = textLower.split(Regex("\\s+")).size
        if (wordCount == 0) return 0f

        val matchedKeywords = keywords.count { keyword ->
            textLower.contains(keyword)
        }

        return (matchedKeywords.toFloat() / keywords.size).coerceIn(0f, 1f)
    }

    private fun containsMethodology(text: String): Boolean {
        val methodPatterns = listOf(
            "method", "methodology", "approach", "study design",
            "participants", "subjects", "sample", "population",
            "experiment", "analysis", "measured", "assessed"
        )
        val textLower = text.lowercase()
        return methodPatterns.any { textLower.contains(it) }
    }

    private fun containsResults(text: String): Boolean {
        val resultPatterns = listOf(
            "result", "found", "showed", "demonstrated", "observed",
            "significant", "increased", "decreased", "effect",
            "correlation", "association", "p-value", "confidence"
        )
        val textLower = text.lowercase()
        return resultPatterns.any { textLower.contains(it) }
    }

    private fun estimateTokens(text: String, config: Config): Int {
        val wordCount = text.split(Regex("\\s+")).size
        return (wordCount * config.tokensPerWord).toInt()
    }

    private fun truncateToTokens(text: String, maxTokens: Int, config: Config): String {
        val words = text.split(Regex("\\s+"))
        val maxWords = (maxTokens / config.tokensPerWord).toInt()

        if (words.size <= maxWords) return text

        val truncatedWords = words.take(maxWords)
        var result = truncatedWords.joinToString(" ")

        val lastPeriod = result.lastIndexOf('.')
        if (lastPeriod > result.length * 0.7) {
            result = result.substring(0, lastPeriod + 1)
        }

        return result
    }
}
