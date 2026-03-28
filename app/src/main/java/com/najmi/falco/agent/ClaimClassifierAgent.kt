package com.najmi.falco.agent

import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.domain.model.Claim
import com.najmi.falco.domain.model.ClaimType
import com.najmi.falco.provider.ProviderRouter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ClaimClassification(
    val type: String,
    val cleanedClaim: String,
    val keyTerms: List<String>
)

@Singleton
class ClaimClassifierAgent @Inject constructor(
    private val router: ProviderRouter,
    private val json: Json
) : IFalcoAgent<String, Claim> {

    override val agentName = "ClaimClassifier"
    override val preferredProvider = LlmProvider.GROQ

    override suspend fun execute(claimText: String): Result<Claim> {
        return try {
            val prompt = buildPrompt(claimText)
            val routeResult = router.routeFor(prompt, preferredProvider)
            
            routeResult.fold(
                onSuccess = { response ->
                    Result.success(parseResponse(claimText, response.text))
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(claim: String) = """
        You are an academic claim classifier. 
        Analyze the claim and return ONLY a JSON object with no preamble or markdown.

        Claim: "$claim"

        Classify this claim and return:
        {
          "type": "EMPIRICAL" | "COMPARATIVE" | "CAUSAL" | "DEFINITIONAL" | "STATISTICAL",
          "cleanedClaim": "<cleaned, grammatically precise version of the claim>",
          "keyTerms": ["<term1>", "<term2>", "<term3>"]
        }
    """.trimIndent()

    private fun parseResponse(raw: String, claimText: String): Claim {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        return try {
            val result = json.decodeFromString<ClaimClassification>(cleaned)
            val type = try {
                ClaimType.valueOf(result.type.uppercase())
            } catch (e: Exception) {
                ClaimType.EMPIRICAL
            }
            Claim(text = result.cleanedClaim.ifBlank { claimText }, type = type)
        } catch (e: Exception) {
            Claim(text = claimText, type = ClaimType.EMPIRICAL)
        }
    }
}
