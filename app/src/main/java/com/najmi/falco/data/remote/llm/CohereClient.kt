package com.najmi.falco.data.remote.llm

import android.util.Log
import com.najmi.falco.data.local.DebugLogger
import com.najmi.falco.data.remote.LlmClient
import com.najmi.falco.data.remote.LlmProvider
import com.najmi.falco.data.remote.LlmResponse
import com.najmi.falco.di.ApiKeyProvider
import com.najmi.falco.domain.model.TokenUsage
import com.najmi.falco.provider.RateLimitException
import com.najmi.falco.provider.TokenSteward
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CohereRequest(
    val model: String = "command-r",
    val message: String,
    val chatHistory: List<CohereChatMessage>? = null,
    val temperature: Float = 0.3f,
    @SerialName("max_tokens") val maxTokens: Int = 1024
)

@Serializable
data class CohereChatMessage(
    val role: String,
    val text: String
)

@Serializable
data class CohereResponse(
    val text: String? = null,
    val generationId: String? = null,
    val chatHistory: List<CohereChatMessage>? = null,
    val usage: CohereUsage? = null,
    val model: String? = null
)

@Serializable
data class CohereUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)

@Serializable
data class CohereErrorResponse(
    val message: String,
    val type: String? = null,
    val code: String? = null
)

@Singleton
class CohereClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val apiKeyProvider: ApiKeyProvider,
    private val tokenSteward: TokenSteward
) : LlmClient {
    companion object { private const val TAG = "CohereClient" }

    override suspend fun canMakeRequest(): Boolean {
        return tokenSteward.hasRequestQuota(LlmProvider.COHERE)
    }

    override suspend fun chat(prompt: String): LlmResponse {
        if (!canMakeRequest()) {
            val remaining = tokenSteward.getRemainingTokens(LlmProvider.COHERE)
            throw RateLimitException("COHERE", "Daily request limit reached. Requests: ${tokenSteward.getRequestLimit(LlmProvider.COHERE)}, Remaining: $remaining")
        }

        val startTime = System.currentTimeMillis()
        val apiKey = apiKeyProvider.getKey(LlmProvider.COHERE)
        DebugLogger.d("[COHERE] Request: prompt=${prompt.length} chars")

        val response = httpClient.post("https://api.cohere.ai/v1/chat") {
            headers.append("Authorization", "Bearer $apiKey")
            headers.append("Content-Type", "application/json")
            setBody(CohereRequest(message = prompt))
        }

        val latency = System.currentTimeMillis() - startTime
        val status = response.status
        val responseBody = response.bodyAsText()

        if (status == HttpStatusCode.TooManyRequests) {
            DebugLogger.e("[COHERE] Rate limit exceeded")
            throw RateLimitException("COHERE")
        }

        if (status != HttpStatusCode.OK) {
            DebugLogger.e("[COHERE] Error ($status): ${responseBody.take(200)}")
            val errorMessage = try {
                json.decodeFromString<CohereErrorResponse>(responseBody).message
            } catch (e: Exception) {
                responseBody.take(200)
            }
            throw Exception("Cohere error ($status): $errorMessage")
        }

        val cohereResponse: CohereResponse = try {
            json.decodeFromString<CohereResponse>(responseBody)
        } catch (e: Exception) {
            DebugLogger.e("[COHERE] Parse error: ${e.message}")
            DebugLogger.e("[COHERE] Response body: $responseBody")
            throw Exception("Invalid response from Cohere: ${e.message}")
        }

        val text = cohereResponse.text
            ?: throw Exception("Empty response from Cohere")

        val usage = TokenUsage(
            promptTokens = cohereResponse.usage?.promptTokens ?: 0,
            completionTokens = cohereResponse.usage?.completionTokens ?: 0,
            totalTokens = cohereResponse.usage?.totalTokens ?: 0,
            provider = "COHERE",
            model = cohereResponse.model
        )

        DebugLogger.llm("COHERE", cohereResponse.model ?: "command-r", usage.totalTokens, latency)
        return LlmResponse(text, usage)
    }
}
