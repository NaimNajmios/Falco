package com.najmi.falco.chunking

import com.najmi.falco.domain.model.Stance
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatchAssembler @Inject constructor() {

    data class BatchPrompt(
        val prompt: String,
        val estimatedInputTokens: Int,
        val chunkCount: Int,
        val sections: List<Section>
    )

    data class Section(
        val label: String,
        val content: String,
        val estimatedTokens: Int
    )

    data class PromptValidation(
        val isValid: Boolean,
        val estimatedTokens: Int,
        val maxTokens: Int,
        val warnings: List<String>
    )

    fun assemble(
        claim: String,
        paperTitle: String,
        paperYear: Int?,
        chunks: List<EvidenceChunk>
    ): BatchPrompt {
        require(chunks.isNotEmpty()) { "At least one chunk is required" }

        val sections = chunks.map { chunk ->
            Section(
                label = chunk.label,
                content = chunk.content,
                estimatedTokens = chunk.estimatedTokens
            )
        }

        val prompt = buildPrompt(claim, paperTitle, paperYear, sections)
        val estimatedTokens = estimatePromptTokens(prompt)

        return BatchPrompt(
            prompt = prompt,
            estimatedInputTokens = estimatedTokens,
            chunkCount = chunks.size,
            sections = sections
        )
    }

    fun validate(
        chunks: List<EvidenceChunk>,
        estimatedPromptTokens: Int
    ): PromptValidation {
        val warnings = mutableListOf<String>()
        val maxTokens = EvidenceChunk.MAX_TOKENS_PER_PAPER

        if (chunks.size > EvidenceChunk.MAX_CHUNKS_PER_PAPER) {
            warnings.add("Exceeds max chunks: ${chunks.size} > ${EvidenceChunk.MAX_CHUNKS_PER_PAPER}")
        }

        if (estimatedPromptTokens > maxTokens) {
            warnings.add("Exceeds max tokens: $estimatedPromptTokens > $maxTokens")
        }

        val chunkTokens = chunks.sumOf { it.estimatedTokens }
        if (chunkTokens > maxTokens) {
            warnings.add("Chunk tokens exceed limit: $chunkTokens > $maxTokens")
        }

        return PromptValidation(
            isValid = warnings.isEmpty(),
            estimatedTokens = estimatedPromptTokens,
            maxTokens = maxTokens,
            warnings = warnings
        )
    }

    private fun buildPrompt(
        claim: String,
        paperTitle: String,
        paperYear: Int?,
        sections: List<Section>
    ): String {
        val sectionContent = sections.joinToString("\n\n") { section ->
            "${section.label}\n${section.content}"
        }

        return """
You are a rigorous academic stance classifier. Analyze evidence excerpts to determine whether a paper supports, opposes, or is neutral toward a research claim.

CLAIM: "$claim"

PAPER:
Title: "$paperTitle"
Year: ${paperYear ?: "Unknown"}

EVIDENCE EXCERPTS:
$sectionContent

TASK:
1. Analyze each excerpt INDEPENDENTLY for stance indicators
2. Evaluate the overall stance considering all excerpts
3. Assess your confidence in the classification

Return ONLY a valid JSON object with this exact structure:
{
  "overall_stance": "SUPPORTS" | "OPPOSES" | "NEUTRAL",
  "overall_confidence": 0.0-1.0,
  "excerpt_analyses": [
    {
      "chunk_id": 0,
      "stance": "SUPPORTS" | "OPPOSES" | "NEUTRAL",
      "confidence": 0.0-1.0,
      "reasoning": "<brief explanation citing specific content>"
    }
  ],
  "reasoning": "<overall reasoning combining all excerpts>"
}

IMPORTANT:
- Analyze EACH excerpt separately before determining overall stance
- Confidence 0.0 = no evidence, 1.0 = definitive evidence
- Quote specific phrases from excerpts in your reasoning
- If excerpts conflict, weigh by relevance to the claim
        """.trimIndent()
    }

    private fun estimatePromptTokens(prompt: String): Int {
        val wordCount = prompt.split(Regex("\\s+")).size
        return (wordCount * 1.3f).toInt()
    }

    companion object {
        fun parseExpectedStance(raw: String): Stance? {
            val cleaned = raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val stancePattern = """"overall_stance"\s*:\s*"(\w+)"""".toRegex()
            val match = stancePattern.find(cleaned)

            return match?.groupValues?.getOrNull(1)?.let { stanceStr ->
                try {
                    Stance.valueOf(stanceStr.uppercase())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }

        fun parseExpectedConfidence(raw: String): Float? {
            val cleaned = raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val confidencePattern = """"overall_confidence"\s*:\s*(-?[\d.]+)""".toRegex()
            val match = confidencePattern.find(cleaned)

            val value = match?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: return null
            return value.coerceIn(0f, 1f)
        }
    }
}
