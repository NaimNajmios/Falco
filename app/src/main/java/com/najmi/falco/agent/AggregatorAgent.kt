package com.najmi.falco.agent

import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.domain.model.ClaimType
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.domain.model.Verdict
import com.najmi.falco.provider.ProviderRouter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class AggregatorInput(
    val claimId: String,
    val claimText: String,
    val claimType: ClaimType,
    val stances: List<PaperStance>,
    val totalRetrieved: Int,
    val totalPapersPassedGate: Int = stances.size,
    val temporalWarning: String? = null,
    val supportingCount: Int = stances.count { it.finalStance == Stance.SUPPORTS },
    val opposingCount: Int = stances.count { it.finalStance == Stance.OPPOSES },
    val neutralCount: Int = stances.count { it.finalStance == Stance.NEUTRAL }
)

@Serializable
data class AggregatorResponse(
    val lean: String,
    val confidence: Float,
    val summary: String,
    val supportingCount: Int,
    val opposingCount: Int,
    val neutralCount: Int,
    val dominantField: String,
    val caveat: String? = null
)

@Singleton
class AggregatorAgent @Inject constructor(
    private val router: ProviderRouter,
    private val json: Json
) : IFalcoAgent<AggregatorInput, Verdict> {

    override val agentName = "Aggregator"
    override val preferredProvider = LlmProvider.GEMINI

    override suspend fun execute(input: AggregatorInput): Verdict {
        val prompt = buildPrompt(input)
        val response = router.routeFor(prompt, preferredProvider)
        return parseResponse(response.text, input)
    }

    private fun buildPrompt(input: AggregatorInput): String {
        val papersJson = input.stances.joinToString(",\n") { stance ->
            val finalStance = stance.finalStance ?: stance.actorStance
            val groundingScore = stance.groundingScore ?: stance.confidence
            """{"title": "${escapeJson(stance.paper.title)}","year": ${stance.paper.year ?: "null"},"citationCount": ${stance.paper.citationCount},"stance": "${finalStance.name}","reasoning": "${escapeJson(stance.actorReasoning)}","groundingScore": ${groundingScore}}"""
        }

        return """
            You are a research synthesis AI. You receive a list of academic paper stance evaluations
            for a hypothesis. Produce a calibrated verdict with a clear confidence level.
            Be conservative: if evidence is mixed, reflect that in the confidence score.
            Return ONLY a JSON object.

            CLAIM: "${input.claimText}"
            CLAIM TYPE: ${input.claimType.name}
            
            SUMMARY STATS:
            - Total papers retrieved: ${input.totalRetrieved}
            - Papers passed quality gate: ${input.totalPapersPassedGate}
            - Supporting: ${input.supportingCount}
            - Opposing: ${input.opposingCount}
            - Neutral: ${input.neutralCount}
            ${input.temporalWarning?.let { "\n- TEMPORAL WARNING: $it" } ?: ""}

            PAPER STANCES:
            [$papersJson]

            Produce a synthesis:
            {
              "lean": "SUPPORTS" | "OPPOSES" | "NEUTRAL",
              "confidence": 0.0-1.0,
              "summary": "<2-3 sentences synthesizing the evidence landscape>",
              "supportingCount": <integer>,
              "opposingCount": <integer>,
              "neutralCount": <integer>,
              "dominantField": "<primary field of study from the papers>",
              "caveat": "<optional caveat or null>"
            }
        """.trimIndent()
    }

    private fun parseResponse(raw: String, input: AggregatorInput): Verdict {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        return try {
            val parsed = json.decodeFromString<JsonObject>(cleaned)
            val leanStr = parsed["lean"]?.jsonPrimitive?.content ?: "NEUTRAL"
            val lean = try { Stance.valueOf(leanStr.uppercase()) } catch (e: Exception) { Stance.NEUTRAL }
            val confidence = parsed["confidence"]?.jsonPrimitive?.floatOrNull?.coerceIn(0f, 1f) ?: 0.5f
            val summary = parsed["summary"]?.jsonPrimitive?.content ?: "Unable to generate summary."
            val supporting = parsed["supportingCount"]?.jsonPrimitive?.intOrNull ?: input.supportingCount
            val opposing = parsed["opposingCount"]?.jsonPrimitive?.intOrNull ?: input.opposingCount
            val neutral = parsed["neutralCount"]?.jsonPrimitive?.intOrNull ?: input.neutralCount
            val field = parsed["dominantField"]?.jsonPrimitive?.content
                ?: input.stances.firstOrNull()?.paper?.fieldsOfStudy?.firstOrNull()
                ?: "Unknown"

            Verdict(
                claimId = input.claimId,
                claim = input.claimText,
                lean = lean,
                confidence = confidence,
                summary = summary,
                stances = input.stances,
                totalPapersRetrieved = input.totalRetrieved,
                totalPapersPassedGate = input.totalPapersPassedGate,
                supportingCount = supporting,
                opposingCount = opposing,
                neutralCount = neutral,
                dominantField = field,
                temporalWarning = input.temporalWarning
            )
        } catch (e: Exception) {
            Verdict(
                claimId = input.claimId,
                claim = input.claimText,
                lean = Stance.NEUTRAL,
                confidence = 0.3f,
                summary = "Unable to generate verdict. Please try again.",
                stances = input.stances,
                totalPapersRetrieved = input.totalRetrieved,
                totalPapersPassedGate = input.totalPapersPassedGate,
                supportingCount = input.supportingCount,
                opposingCount = input.opposingCount,
                neutralCount = input.neutralCount,
                dominantField = "Unknown",
                temporalWarning = input.temporalWarning
            )
        }
    }

    private fun escapeJson(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
