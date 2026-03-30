package com.najmi.falco.chunking

import com.najmi.falco.domain.model.Stance

data class SmartStanceResult(
    val overallStance: Stance,
    val overallConfidence: Float,
    val excerptAnalyses: List<ExcerptAnalysis>,
    val reasoning: String,
    val chunksUsed: Int,
    val providerUsed: String? = null,
    val tokensConsumed: Int = 0
) {
    data class ExcerptAnalysis(
        val chunkId: Int,
        val stance: Stance,
        val confidence: Float,
        val reasoning: String,
        val keyEvidence: String? = null
    )

    fun isHighConfidence(): Boolean = overallConfidence >= 0.85f

    fun isLowConfidence(): Boolean = overallConfidence < 0.5f

    fun stanceAgreement(): Float {
        if (excerptAnalyses.isEmpty()) return 0f
        
        val stanceCounts = excerptAnalyses.groupingBy { it.stance }.eachCount()
        val dominantCount = stanceCounts.values.maxOrNull() ?: 0
        
        return dominantCount.toFloat() / excerptAnalyses.size
    }

    fun hasMixedStances(): Boolean {
        if (excerptAnalyses.isEmpty()) return false
        return excerptAnalyses.map { it.stance }.toSet().size > 1
    }

    fun dominantExcerptStance(): Stance? {
        if (excerptAnalyses.isEmpty()) return null
        
        return excerptAnalyses.groupingBy { it.stance }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    fun lowestConfidenceChunk(): ExcerptAnalysis? {
        return excerptAnalyses.minByOrNull { it.confidence }
    }

    fun highestConfidenceChunk(): ExcerptAnalysis? {
        return excerptAnalyses.maxByOrNull { it.confidence }
    }

    fun toPaperStance(paper: com.najmi.falco.domain.model.Paper): com.najmi.falco.domain.model.PaperStance {
        val topAnalysis = excerptAnalyses.maxByOrNull { it.confidence }
            ?: ExcerptAnalysis(0, overallStance, overallConfidence, reasoning)
        
        return com.najmi.falco.domain.model.PaperStance(
            paper = paper,
            actorStance = overallStance,
            actorReasoning = reasoning,
            confidence = overallConfidence,
            keyEvidence = topAnalysis.keyEvidence ?: topAnalysis.reasoning,
            relevanceScore = overallConfidence
        )
    }
}

object SmartStanceParser {
    
    fun parse(raw: String): SmartStanceResult? {
        val cleaned = cleanJson(raw) ?: return null
        
        return try {
            val stance = parseOverallStance(cleaned) ?: return null
            val confidence = parseConfidence(cleaned)
            val excerpts = parseExcerptAnalyses(cleaned)
            val reasoning = parseReasoning(cleaned)
            
            SmartStanceResult(
                overallStance = stance,
                overallConfidence = confidence,
                excerptAnalyses = excerpts,
                reasoning = reasoning,
                chunksUsed = excerpts.size
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanJson(raw: String): String? {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        
        return if (start >= 0 && end > start) {
            trimmed.substring(start, end + 1)
        } else null
    }

    private fun parseOverallStance(json: String): Stance? {
        val pattern = """"overall_stance"\s*:\s*"(\w+)"""".toRegex()
        val match = pattern.find(json) ?: return null
        
        return try {
            Stance.valueOf(match.groupValues[1].uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun parseConfidence(json: String): Float {
        val pattern = """"overall_confidence"\s*:\s*([\d.]+)""".toRegex()
        val match = pattern.find(json) ?: return 0.5f
        
        return match.groupValues[1].toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f
    }

    private fun parseReasoning(json: String): String {
        val pattern = """"reasoning"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(json)
        return match?.groupValues?.getOrNull(1)?.let { unescapeJson(it) } ?: "No reasoning provided."
    }

    private fun parseExcerptAnalyses(json: String): List<SmartStanceResult.ExcerptAnalysis> {
        val analyses = mutableListOf<SmartStanceResult.ExcerptAnalysis>()
        
        val excerptArrayPattern = """"excerpt_analyses"\s*:\s*\[([\s\S]*?)]""".toRegex()
        val arrayMatch = excerptArrayPattern.find(json) ?: return analyses
        
        val objectPattern = """\{[^}]*"chunk_id"\s*:\s*(\d+)[^}]*"stance"\s*:\s*"(\w+)"[^}]*"confidence"\s*:\s*([\d.]+)[^}]*"reasoning"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        objectPattern.findAll(arrayMatch.groupValues[1]).forEach { match ->
            val chunkId = match.groupValues[1].toIntOrNull() ?: return@forEach
            val stance = try {
                Stance.valueOf(match.groupValues[2].uppercase())
            } catch (e: Exception) {
                return@forEach
            }
            val confidence = match.groupValues[3].toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f
            val reasoning = unescapeJson(match.groupValues[4])
            
            analyses.add(SmartStanceResult.ExcerptAnalysis(
                chunkId = chunkId,
                stance = stance,
                confidence = confidence,
                reasoning = reasoning
            ))
        }
        
        return analyses
    }

    private fun unescapeJson(s: String): String {
        return s
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    fun parseWithFallback(raw: String, defaultStance: Stance = Stance.NEUTRAL): SmartStanceResult {
        return parse(raw) ?: SmartStanceResult(
            overallStance = defaultStance,
            overallConfidence = 0.3f,
            excerptAnalyses = emptyList(),
            reasoning = "Failed to parse LLM response",
            chunksUsed = 0
        )
    }
}
