package com.najmi.falco.agent

import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.domain.model.Claim
import com.najmi.falco.provider.ProviderRouter
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueryExpansionAgent @Inject constructor(
    private val router: ProviderRouter,
    private val json: Json
) : IFalcoAgent<Claim, List<String>> {

    override val agentName = "QueryExpansion"
    override val preferredProvider = LlmProvider.GROQ

    override suspend fun execute(claim: Claim): Result<List<String>> {
        return try {
            val prompt = buildPrompt(claim)
            val routeResult = router.routeFor(prompt, preferredProvider)
            
            routeResult.fold(
                onSuccess = { response ->
                    Result.success(parseResponse(response.text))
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(claim: Claim) = """
        You are an academic search query engineer.
        Generate search queries that would be entered into Google Scholar or Semantic Scholar.
        Return ONLY a JSON array of 3 strings. No numbering, no markdown.

        Claim type: ${claim.type.name}
        Claim: "${claim.text}"

        Generate 3 academic search queries:
        - Query 1: Should retrieve papers that could CONFIRM the claim
        - Query 2: Should retrieve papers that could CHALLENGE or REFUTE the claim
        - Query 3: Should retrieve foundational/methodology papers about the topic

        Return: ["query1", "query2", "query3"]
    """.trimIndent()

    private fun parseResponse(raw: String): List<String> {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        return try {
            json.decodeFromString<List<String>>(cleaned).take(3)
        } catch (e: Exception) {
            listOf(cleaned.trim().removeSurrounding("[\"", "\"]").removeSurrounding("\"", "\""))
                .filter { it.isNotBlank() }
        }
    }
}
