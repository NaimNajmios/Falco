package com.najmi.falco.di

import android.util.Log
import com.najmi.falco.BuildConfig
import com.najmi.falco.data.remote.openapi.OpenAlexClient
import com.najmi.falco.data.remote.semanticscholar.SemanticScholarClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private fun loadLocalProp(key: String): String {
        return try {
            val props = Properties()
            val file = File("local.properties")
            if (file.exists()) {
                props.load(file.inputStream())
            }
            props.getProperty(key, "")
        } catch (e: Exception) {
            Log.w("NetworkModule", "Could not load $key from local.properties: ${e.message}")
            ""
        }
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("KtorClient", message.take(500))
                }
            }
            level = LogLevel.BODY
        }
        engine {
            connectTimeout = 30_000
            socketTimeout = 60_000
        }
    }

    @Provides @Singleton @Named("gemini") fun provideGeminiApiKey(): String = loadLocalProp("GEMINI_API_KEY")
    @Provides @Singleton @Named("groq") fun provideGroqApiKey(): String = loadLocalProp("GROQ_API_KEY")
    @Provides @Singleton @Named("cerebras") fun provideCerebrasApiKey(): String = loadLocalProp("CEREBRAS_API_KEY")
    @Provides @Singleton @Named("openrouter") fun provideOpenRouterApiKey(): String = loadLocalProp("OPENROUTER_API_KEY")

    @Provides @Singleton
    fun provideSemanticScholarClient(httpClient: HttpClient): SemanticScholarClient = 
        SemanticScholarClient(httpClient)

    @Provides @Singleton
    fun provideOpenAlexClient(httpClient: HttpClient): OpenAlexClient = 
        OpenAlexClient(httpClient)
}
