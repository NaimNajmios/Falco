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
    val neutralCount: Int = stances.count { it.finalStance == Stance.NEUTRAL },
    val confidenceThreshold: Float = 0.5f
)

data class AggregatorOutput(
    val verdict: Verdict,
    val needsMorePapers: Boolean,
    val suggestedQueries: List<String>?
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
    val caveat: String? = null,
    val needsMorePapers: Boolean = false,
    val suggestedQueries: List<String>? = null
)

@Singleton
class AggregatorAgent @Inject constructor(
    private val router: ProviderRouter,
    private val json: Json
) : IFalcoAgent<AggregatorInput, AggregatorOutput> {

    override val agentName = "Aggregator"
    override val defaultProvider = LlmProvider.GEMINI

    override suspend fun execute(input: AggregatorInput, preferredProvider: LlmProvider?): Result<AggregatorOutput> {
        val provider = preferredProvider ?: defaultProvider
        return try {
            val prompt = buildPrompt(input)
            val routeResult = router.routeFor(prompt, provider)
            
            routeResult.fold(
                onSuccess = { response ->
                    Result.success(parseResponse(response.text, input))
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(input: AggregatorInput): String {
        val papersJson = input.stances.joinToString(",\n") { stance ->
            val finalStance = stance.finalStance ?: stance.actorStance
            val groundingScore = stance.groundingScore ?: stance.confidence
            val consensusFlag = if (stance.isConsensus) " (consensus)" else if (stance.isOutlier) " (outlier)" else ""
            """{"title": "${escapeJson(stance.paper.title)}","year": ${stance.paper.year ?: "null"},"citationCount": ${stance.paper.citationCount},"stance": "${finalStance.name}","reasoning": "${escapeJson(stance.actorReasoning)}","groundingScore": ${groundingScore}$consensusFlag}"""
        }

        val adaptivePrompt = if (input.confidenceThreshold > 0 && input.stances.size < 15) {
            """
            
            ADAPTIVE RETRIEVAL:
            If confidence is below ${input.confidenceThreshold} OR evidence is highly mixed (no clear consensus),
            set "needsMorePapers": true and provide "suggestedQueries" (1-2 new search terms to find additional evidence).
            Otherwise, set "needsMorePapers": false and "suggestedQueries": null.
            """
        } else ""

        return """
            You are a research synthesis AI. You receive a list of academic paper stance evaluations
            for a hypothesis. Produce a calibrated verdict with a clear confidence level.
            Be conservative: if evidence is mixed, reflect that in the confidence score.
            RETURN ONLY A JSON OBJECT.

            CLAIM: "${input.claimText}"
            CLAIM TYPE: ${input.claimType.name}
            
            SUMMARY STATS:
            - Total papers retrieved: ${input.totalRetrieved}
            - Papers passed quality gate: ${input.totalPapersPassedGate}
            - Supporting: ${input.supportingCount}
            - Opposing: ${input.opposingCount}
            - Neutral: ${input.neutralCount}
            ${input.temporalWarning?.let { "\n- TEMPORAL WARNING: $it" } ?: ""}
            $adaptivePrompt

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
              "caveat": "<optional caveat or null>",
              "needsMorePapers": <boolean>,
              "suggestedQueries": <array of strings or null>
            }
        """.trimIndent()
    }

    private fun parseResponse(raw: String, input: AggregatorInput): AggregatorOutput {
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
            
            val needsMorePapers = parsed["needsMorePapers"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
            val suggestedQueries = parsed["suggestedQueries"]?.let { element ->
                if (element is kotlinx.serialization.json.JsonArray) {
                    element.mapNotNull { it.jsonPrimitive.content }.takeIf { it.isNotEmpty() }
                } else null
            }

            val verdict = Verdict(
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
            
            AggregatorOutput(verdict, needsMorePapers, suggestedQueries)
        } catch (e: Exception) {
            val verdict = Verdict(
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
            AggregatorOutput(verdict, false, null)
        }
    }

    private fun escapeJson(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
