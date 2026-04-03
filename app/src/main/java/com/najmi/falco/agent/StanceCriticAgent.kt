package com.najmi.falco.agent

import android.util.Log
import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.domain.model.PaperStance
import com.najmi.falco.domain.model.Stance
import com.najmi.falco.provider.ProviderRouter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

data class StanceCriticInput(
    val claimText: String,
    val paperStance: PaperStance
)

@Singleton
class StanceCriticAgent @Inject constructor(
    private val router: ProviderRouter,
    private val json: Json
) : IFalcoAgent<StanceCriticInput, PaperStance> {

    companion object {
        private const val TAG = "StanceCriticAgent"
    }

    override val agentName = "StanceCritic"
    override val defaultProvider = LlmProvider.GEMINI

    override suspend fun execute(input: StanceCriticInput, preferredProvider: LlmProvider?): Result<PaperStance> {
        val provider = preferredProvider ?: defaultProvider
        return try {
            val prompt = buildPrompt(input)
            val routeResult = router.routeFor(prompt, provider)
            
            routeResult.fold(
                onSuccess = { response ->
                    val providerUsed = response.usage.provider
                    Log.d(TAG, "Critic request succeeded with provider: $providerUsed (preferred: ${provider.name})")
                    Result.success(parseResponse(response.text, input.paperStance, providerUsed))
                },
                onFailure = { error ->
                    Log.e(TAG, "Critic request failed with provider ${provider.name}: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(input: StanceCriticInput): String {
        val ps = input.paperStance
        return """
            You are an academic peer-reviewer acting as devil's advocate.
            You receive a stance classification for a research paper and must critically evaluate it.
            Your job is to challenge overconfident classifications and correct misreadings of abstracts.
            Return ONLY a JSON object. No preamble, no markdown.

            CLAIM: "${input.claimText}"

            PAPER ABSTRACT: "${escapeJson(ps.paper.abstract)}"

            ACTOR CLASSIFICATION:
            - Stance: ${ps.actorStance.name}
            - Reasoning: "${escapeJson(ps.actorReasoning)}"
            - Key Evidence: "${escapeJson(ps.keyEvidence)}"

            Critically evaluate this classification:
            1. Is the actor's reasoning actually grounded in what the abstract says?
            2. Does the abstract address the claim directly, or is the actor over-inferring?
            3. Are there nuances the actor missed?

            Return:
            {
              "agreedWithActor": true | false,
              "revisedStance": "SUPPORTS" | "OPPOSES" | "NEUTRAL",
              "challenge": "<one sentence: your critique of the actor or confirmation of it>",
              "finalReasoning": "<the definitive one-sentence reasoning for the revised stance>"
            }
        """.trimIndent()
    }

    private fun parseResponse(raw: String, original: PaperStance, providerUsed: String? = null): PaperStance {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        return try {
            val parsed = json.decodeFromString<JsonObject>(cleaned)
            val agreedWithActor = parsed["agreedWithActor"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true
            val revisedStanceStr = parsed["revisedStance"]?.jsonPrimitive?.content ?: original.actorStance.name
            val revisedStance = try { Stance.valueOf(revisedStanceStr.uppercase()) } catch (e: Exception) { original.actorStance }
            val challenge = parsed["challenge"]?.jsonPrimitive?.content ?: ""
            val finalReasoning = parsed["finalReasoning"]?.jsonPrimitive?.content ?: original.actorReasoning

            val finalStance = if (agreedWithActor) original.actorStance else revisedStance

            original.copy(
                criticStance = revisedStance,
                criticChallenge = challenge,
                finalStance = finalStance,
                criticProviderUsed = providerUsed
            )
        } catch (e: Exception) {
            original.copy(
                criticStance = original.actorStance,
                criticChallenge = "Critic evaluation failed, defaulting to actor stance.",
                finalStance = original.actorStance,
                criticProviderUsed = providerUsed
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
