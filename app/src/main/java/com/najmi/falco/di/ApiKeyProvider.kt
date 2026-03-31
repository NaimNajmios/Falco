package com.najmi.falco.di

import com.najmi.falco.data.local.UserPreferencesRepository
import com.najmi.falco.data.remote.LlmProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyProvider @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend fun getKey(provider: LlmProvider): String {
        val prefs = userPreferencesRepository.preferences.first()
        
        val userKey = when (provider) {
            LlmProvider.GEMINI -> prefs.userGeminiKey
            LlmProvider.GROQ -> prefs.userGroqKey
            LlmProvider.MISTRAL -> prefs.userMistralKey
            LlmProvider.COHERE -> prefs.userCohereKey
            LlmProvider.CEREBRAS -> prefs.userCerebrasKey
            LlmProvider.OPENROUTER -> prefs.userOpenRouterKey
            LlmProvider.ROUTEWAY -> prefs.userRoutewayKey
        }
        
        return userKey ?: throw IllegalStateException("No API key configured for ${provider.name}. Please add your key in Settings.")
    }

    fun hasUserKey(provider: LlmProvider): Boolean {
        return userPreferencesRepository.getApiKey(provider) != null
    }

    fun hasAnyUserKey(): Boolean {
        return LlmProvider.entries.any { hasUserKey(it) }
    }
}
