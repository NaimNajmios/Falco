package com.najmi.falco.di

import com.najmi.falco.BuildConfig
import com.najmi.falco.data.local.DebugLogger
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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

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
        if (DebugLogger.isEnabled()) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        val summary = message
                            .lines()
                            .filter { it.isNotBlank() }
                            .take(3)
                            .joinToString(" | ")
                            .take(300)
                        DebugLogger.d("[NET] $summary")
                    }
                }
                level = LogLevel.HEADERS
            }
        }
        engine {
            connectTimeout = 30_000
            socketTimeout = 60_000
        }
    }

    @Provides @Singleton
    fun provideSemanticScholarClient(httpClient: HttpClient): SemanticScholarClient = 
        SemanticScholarClient(httpClient)

    @Provides @Singleton
    fun provideOpenAlexClient(httpClient: HttpClient): OpenAlexClient = 
        OpenAlexClient(httpClient)
}
