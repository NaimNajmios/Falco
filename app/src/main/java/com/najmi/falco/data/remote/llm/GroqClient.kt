package com.najmi.falco.data.remote.llm

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
data class GroqRequest(
    val model: String = "llama-3.3-70b-versatile",
    val messages: List<GroqMessage>,
    val temperature: Float = 0.3f,
    @SerialName("max_tokens") val maxTokens: Int = 1024
)

@Serializable
data class GroqMessage(val role: String, val content: String)

@Serializable
data class GroqResponse(
    val choices: List<GroqChoice>,
    val usage: GroqUsage? = null,
    val model: String? = null
)

@Serializable
data class GroqUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)

@Serializable
data class GroqChoice(val message: GroqMessageResponse)

@Serializable
data class GroqMessageResponse(val content: String)

@Serializable
data class GroqErrorResponse(val error: GroqError)

@Serializable
data class GroqError(val message: String, val type: String? = null, val code: String? = null)

@Singleton
class GroqClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val apiKeyProvider: ApiKeyProvider,
    private val tokenSteward: TokenSteward
) : LlmClient {

    override suspend fun canMakeRequest(): Boolean {
        return tokenSteward.hasRequestQuota(LlmProvider.GROQ)
    }

    override suspend fun chat(prompt: String): LlmResponse {
        if (!canMakeRequest()) {
            val remaining = tokenSteward.getRemainingTokens(LlmProvider.GROQ)
            throw RateLimitException("GROQ", "Daily request limit reached. Requests: ${tokenSteward.getRequestLimit(LlmProvider.GROQ)}, Remaining: $remaining")
        }

        val startTime = System.currentTimeMillis()
        val apiKey = apiKeyProvider.getKey(LlmProvider.GROQ)
        DebugLogger.d("[GROQ] Request: prompt=${prompt.length} chars")

        val response = httpClient.post("https://api.groq.com/openai/v1/chat/completions") {
            headers.append("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(GroqRequest(messages = listOf(GroqMessage(role = "user", content = prompt))))
        }

        val latency = System.currentTimeMillis() - startTime
        val status = response.status
        val responseBody = response.bodyAsText()

        if (status == HttpStatusCode.TooManyRequests) {
            DebugLogger.e("[GROQ] Rate limit exceeded")
            throw RateLimitException("GROQ")
        }

        if (status != HttpStatusCode.OK) {
            DebugLogger.e("[GROQ] Error ($status): ${responseBody.take(100)}")
            val errorMessage = try {
                json.decodeFromString<GroqErrorResponse>(responseBody).error.message
            } catch (e: Exception) {
                responseBody.take(200)
            }
            throw Exception("Groq error ($status): $errorMessage")
        }

        val groqResponse: GroqResponse = try {
            json.decodeFromString<GroqResponse>(responseBody)
        } catch (e: Exception) {
            DebugLogger.e("[GROQ] Parse error: ${e.message}")
            throw Exception("Invalid response from Groq: ${e.message}")
        }

        val text = groqResponse.choices.firstOrNull()?.message?.content
            ?: throw Exception("Empty response from Groq")

        val usage = TokenUsage(
            promptTokens = groqResponse.usage?.promptTokens ?: 0,
            completionTokens = groqResponse.usage?.completionTokens ?: 0,
            totalTokens = groqResponse.usage?.totalTokens ?: 0,
            provider = "GROQ",
            model = groqResponse.model
        )

        DebugLogger.llm("GROQ", groqResponse.model ?: "llama-3.3-70b-versatile", usage.totalTokens, latency)
        return LlmResponse(text, usage)
    }
}
