package com.najmi.falco.agent

import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.domain.model.Claim
import com.najmi.falco.domain.model.ClaimType
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
    override val defaultProvider = LlmProvider.GROQ

    override suspend fun execute(claim: Claim, preferredProvider: LlmProvider?): Result<List<String>> {
        val provider = preferredProvider ?: defaultProvider
        return try {
            val prompt = buildPrompt(claim)
            val routeResult = router.routeFor(prompt, provider)
            
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

        Generate 3 academic search queries with these CRITICAL requirements:
        - Include key terms from the claim in EVERY query
        - Use domain-specific operators where applicable
        - For supporting query: combine claim keywords with terms like "evidence", "confirms", "demonstrates"
        - For opposing query: combine claim keywords with "contradicts", "debunks", "refutes", "misconception"
        - For foundational query: focus on the core mechanism or principle in the claim

        DOMAIN-SPECIFIC GUIDANCE:
        ${domainGuidance(claim)}

        IMPORTANT: Generic queries that don't include claim-specific terms will retrieve irrelevant papers.

        Return: ["supporting_query_with_claim_terms", "opposing_query_with_claim_terms", "foundational_query_with_claim_terms"]
    """.trimIndent()

    private fun domainGuidance(claim: Claim): String = when (claim.type) {
        ClaimType.SCIENTIFIC ->
            """
            - For scientific claims: include specific mechanisms (e.g., "blackbody radiation", "thermal conductivity", "spectroscopic absorption", "convection", "Stefan-Boltzmann law")
            - Include the specific property being claimed (e.g., for color-heat claims: "spectral absorptivity", "thermal emissivity", "solar reflectance")
            - Do NOT use generic "heat absorption" — be specific about the mechanism and material
            - Include "peer-reviewed", "empirical study", "controlled experiment"
            """
        ClaimType.STATISTICAL ->
            """
            - Include population specifics (e.g., "children", "adults", "clinical trial", "longitudinal study")
            - Include study type (e.g., "meta-analysis", "randomized controlled trial", "survey")
            - Include statistical terms (e.g., "p-value", "confidence interval", "sample size")
            """
        ClaimType.CURRENT_EVENT ->
            """
            - Focus on recent sources (use year filters where possible)
            - Include "news", "report", "official statement", "press release"
            - Verify publication date is recent
            """
        ClaimType.COMPARATIVE ->
            """
            - Include both subjects being compared
            - Include comparison metrics and context
            """
        else ->
            """
            - Include the primary topic, mechanism, or phenomenon explicitly
            - Include relevant domain-specific vocabulary
            """
    }

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
