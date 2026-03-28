package com.najmi.falco.agent

import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.domain.model.Paper
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.provider.ProviderRouter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.floatOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class StanceActorInput(
    val claimText: String,
    val paper: Paper
)

@Singleton
class StanceActorAgent @Inject constructor(
    private val router: ProviderRouter,
    private val json: Json
) : IFalcoAgent<StanceActorInput, PaperStance> {

    override val agentName = "StanceActor"
    override val preferredProvider = LlmProvider.GROQ

    override suspend fun execute(input: StanceActorInput): Result<PaperStance> {
        return try {
            val prompt = buildPrompt(input)
            val routeResult = router.routeFor(prompt, preferredProvider)
            
            routeResult.fold(
                onSuccess = { response ->
                    Result.success(parseResponse(response.text, input.paper))
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(input: StanceActorInput) = """
        You are a rigorous academic stance classifier.
        You read paper abstracts and determine whether they support, oppose, or are neutral 
        toward a given research claim.
        Return ONLY a JSON object. No preamble, no markdown.

        CLAIM: "${input.claimText}"

        PAPER:
        Title: "${input.paper.title}"
        Year: ${input.paper.year ?: "Unknown"}
        Citation Count: ${input.paper.citationCount}
        Abstract: "${escapeJson(input.paper.abstract)}"

        Analyze whether this paper's abstract SUPPORTS, OPPOSES, or is NEUTRAL toward the claim.

        Return:
        {
          "stance": "SUPPORTS" | "OPPOSES" | "NEUTRAL",
          "reasoning": "<one to two sentences citing specific content from the abstract>",
          "relevanceScore": 0.0-1.0,
          "keyEvidence": "<the specific phrase or finding in the abstract that drives the stance>"
        }
    """.trimIndent()

    private fun parseResponse(raw: String, paper: Paper): PaperStance {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        return try {
            val parsed = json.decodeFromString<JsonObject>(cleaned)
            val stanceStr = parsed["stance"]?.jsonPrimitive?.content ?: "NEUTRAL"
            val stance = try { Stance.valueOf(stanceStr.uppercase()) } catch (e: Exception) { Stance.NEUTRAL }
            val reasoning = parsed["reasoning"]?.jsonPrimitive?.content ?: "No reasoning provided."
            val confidence = parsed["relevanceScore"]?.jsonPrimitive?.floatOrNull?.coerceIn(0f, 1f) ?: 0.5f
            val keyEvidence = parsed["keyEvidence"]?.jsonPrimitive?.content ?: ""

            PaperStance(
                paper = paper,
                actorStance = stance,
                actorReasoning = reasoning,
                confidence = confidence,
                keyEvidence = keyEvidence,
                relevanceScore = confidence
            )
        } catch (e: Exception) {
            PaperStance(
                paper = paper,
                actorStance = Stance.NEUTRAL,
                actorReasoning = "Failed to classify paper stance.",
                confidence = 0.3f,
                keyEvidence = "",
                relevanceScore = 0.3f
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
