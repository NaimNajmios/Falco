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
data class RoutewayRequest(
    val model: String = "minimax-m2:free",
    val messages: List<RoutewayMessage>,
    val temperature: Float = 0.3f,
    @SerialName("max_tokens") val maxTokens: Int = 1024
)

@Serializable
data class RoutewayMessage(val role: String, val content: String)

@Serializable
data class RoutewayResponse(
    val choices: List<RoutewayChoice>,
    val usage: RoutewayUsage? = null,
    val model: String? = null
)

@Serializable
data class RoutewayUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)

@Serializable
data class RoutewayChoice(val message: RoutewayMessageResponse)

@Serializable
data class RoutewayMessageResponse(val content: String)

@Serializable
data class RoutewayErrorResponse(val error: RoutewayError)

@Serializable
data class RoutewayError(val message: String, val type: String? = null, val code: String? = null)

@Singleton
class RoutewayClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val apiKeyProvider: ApiKeyProvider,
    private val tokenSteward: TokenSteward
) : LlmClient {

    companion object {
        private const val BASE_URL = "https://api.routeway.ai/v1/chat/completions"
        private const val DEFAULT_MODEL = "minimax-m2:free"
    }

    override suspend fun canMakeRequest(): Boolean {
        return tokenSteward.hasRequestQuota(LlmProvider.ROUTEWAY)
    }

    override suspend fun chat(prompt: String): LlmResponse {
        if (!canMakeRequest()) {
            throw RateLimitException("ROUTEWAY", "Daily request limit reached for Routeway (180 requests/day)")
        }

        val startTime = System.currentTimeMillis()
        val apiKey = apiKeyProvider.getKey(LlmProvider.ROUTEWAY)
        DebugLogger.d("[ROUTEWAY] Request: prompt=${prompt.length} chars")

        val response = httpClient.post(BASE_URL) {
            headers.append("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(RoutewayRequest(messages = listOf(RoutewayMessage(role = "user", content = prompt))))
        }

        val latency = System.currentTimeMillis() - startTime
        val status = response.status
        val responseBody = response.bodyAsText()

        if (status == HttpStatusCode.TooManyRequests) {
            DebugLogger.e("[ROUTEWAY] Rate limit exceeded")
            throw RateLimitException("ROUTEWAY")
        }

        if (status != HttpStatusCode.OK) {
            DebugLogger.e("[ROUTEWAY] Error ($status): ${responseBody.take(100)}")
            val errorMessage = try {
                json.decodeFromString<RoutewayErrorResponse>(responseBody).error.message
            } catch (e: Exception) {
                responseBody.take(200)
            }
            throw Exception("Routeway error ($status): $errorMessage")
        }

        val routewayResponse: RoutewayResponse = try {
            json.decodeFromString<RoutewayResponse>(responseBody)
        } catch (e: Exception) {
            val errorMsg = try {
                json.decodeFromString<RoutewayErrorResponse>(responseBody).error.message
            } catch (ignored: Exception) {
                responseBody.take(200)
            }
            DebugLogger.e("[ROUTEWAY] Parse error / API error: $errorMsg")
            throw Exception("Routeway returned an error: $errorMsg")
        }

        if (routewayResponse.choices.isNullOrEmpty()) {
            DebugLogger.e("[ROUTEWAY] Empty choices in response")
            throw Exception("Routeway returned an empty response")
        }

        val text = routewayResponse.choices.first().message.content

        val usage = TokenUsage(
            promptTokens = routewayResponse.usage?.promptTokens ?: 0,
            completionTokens = routewayResponse.usage?.completionTokens ?: 0,
            totalTokens = routewayResponse.usage?.totalTokens ?: 0,
            provider = "ROUTEWAY",
            model = routewayResponse.model
        )

        DebugLogger.llm("ROUTEWAY", routewayResponse.model ?: DEFAULT_MODEL, usage.totalTokens, latency)
        return LlmResponse(text, usage)
    }
}
