package com.najmi.falco.di

import com.najmi.falco.data.local.UserPreferencesRepository
import com.najmi.falco.data.remote.LlmProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ApiKeyProvider @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @Named("gemini") private val defaultGeminiKey: String,
    @Named("groq") private val defaultGroqKey: String,
    @Named("cerebras") private val defaultCerebrasKey: String,
    @Named("openrouter") private val defaultOpenRouterKey: String
) {
    private val defaultKeys = mapOf(
        LlmProvider.GEMINI to defaultGeminiKey,
        LlmProvider.GROQ to defaultGroqKey,
        LlmProvider.CEREBRAS to defaultCerebrasKey,
        LlmProvider.OPENROUTER to defaultOpenRouterKey
    )

    suspend fun getKey(provider: LlmProvider): String {
        val prefs = userPreferencesRepository.preferences.first()
        
        return if (prefs.useUserKeys) {
            val userKey = when (provider) {
                LlmProvider.GEMINI -> prefs.userGeminiKey
                LlmProvider.GROQ -> prefs.userGroqKey
                LlmProvider.CEREBRAS -> prefs.userCerebrasKey
                LlmProvider.OPENROUTER -> prefs.userOpenRouterKey
            }
            
            if (!userKey.isNullOrBlank()) {
                userKey
            } else {
                defaultKeys[provider] ?: throw IllegalStateException("No key available for ${provider.name}")
            }
        } else {
            defaultKeys[provider] ?: throw IllegalStateException("No key available for ${provider.name}")
        }
    }

    fun hasUserKey(provider: LlmProvider): Boolean {
        return userPreferencesRepository.getApiKey(provider) != null
    }

    fun hasAnyUserKey(): Boolean {
        return LlmProvider.entries.any { hasUserKey(it) }
    }
}
