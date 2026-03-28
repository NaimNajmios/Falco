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
data class MistralRequest(
    val model: String = "open-mistral-7b",
    val messages: List<MistralMessage>,
    val temperature: Float = 0.3f,
    @SerialName("max_tokens") val maxTokens: Int = 1024
)

@Serializable
data class MistralMessage(val role: String, val content: String)

@Serializable
data class MistralResponse(
    val choices: List<MistralChoice>,
    val usage: MistralUsage? = null,
    val model: String? = null
)

@Serializable
data class MistralUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)

@Serializable
data class MistralChoice(val message: MistralMessageResponse)

@Serializable
data class MistralMessageResponse(val content: String)

@Serializable
data class MistralErrorResponse(val message: String, val type: String? = null)

@Singleton
class MistralClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val apiKeyProvider: ApiKeyProvider,
    private val tokenSteward: TokenSteward
) : LlmClient {
    companion object { private const val TAG = "MistralClient" }

    override suspend fun canMakeRequest(): Boolean {
        return tokenSteward.hasRequestQuota(LlmProvider.MISTRAL)
    }

    override suspend fun chat(prompt: String): LlmResponse {
        if (!canMakeRequest()) {
            val remaining = tokenSteward.getRemainingTokens(LlmProvider.MISTRAL)
            throw RateLimitException("MISTRAL", "Daily request limit reached. Requests: ${tokenSteward.getRequestLimit(LlmProvider.MISTRAL)}, Remaining: $remaining")
        }

        val startTime = System.currentTimeMillis()
        val apiKey = apiKeyProvider.getKey(LlmProvider.MISTRAL)
        DebugLogger.d("[MISTRAL] API Key present: ${!apiKey.isNullOrBlank()}, length: ${apiKey?.length ?: 0}")
        DebugLogger.d("[MISTRAL] Request: prompt=${prompt.length} chars")

        val response = httpClient.post("https://api.mistral.ai/v1/chat/completions") {
            headers.append("Authorization", "Bearer $apiKey")
            headers.append("Content-Type", "application/json")
            setBody(MistralRequest(messages = listOf(MistralMessage(role = "user", content = prompt))))
        }

        val latency = System.currentTimeMillis() - startTime
        val status = response.status
        val responseBody = response.bodyAsText()

        if (status == HttpStatusCode.TooManyRequests) {
            DebugLogger.e("[MISTRAL] Rate limit exceeded")
            throw RateLimitException("MISTRAL")
        }

        if (status != HttpStatusCode.OK) {
            DebugLogger.e("[MISTRAL] Error ($status): ${responseBody.take(100)}")
            val errorMessage = try {
                json.decodeFromString<MistralErrorResponse>(responseBody).message
            } catch (e: Exception) {
                responseBody.take(200)
            }
            throw Exception("Mistral error ($status): $errorMessage")
        }

        val mistralResponse: MistralResponse = try {
            json.decodeFromString<MistralResponse>(responseBody)
        } catch (e: Exception) {
            DebugLogger.e("[MISTRAL] Parse error: ${e.message}")
            throw Exception("Invalid response from Mistral: ${e.message}")
        }

        val text = mistralResponse.choices.firstOrNull()?.message?.content
            ?: throw Exception("Empty response from Mistral")

        val usage = TokenUsage(
            promptTokens = mistralResponse.usage?.promptTokens ?: 0,
            completionTokens = mistralResponse.usage?.completionTokens ?: 0,
            totalTokens = mistralResponse.usage?.totalTokens ?: 0,
            provider = "MISTRAL",
            model = mistralResponse.model
        )

        DebugLogger.llm("MISTRAL", mistralResponse.model ?: "mistral-7b-instruct", usage.totalTokens, latency)
        return LlmResponse(text, usage)
    }
}
