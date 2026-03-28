package com.najmi.falco.data.local

data class UserPreferences(
    val isDarkMode: Boolean = true,
    val isDebugMode: Boolean = false,
    val preferredProvider: String = "GROQ",
    val userGeminiKey: String? = null,
    val userGroqKey: String? = null,
    val userMistralKey: String? = null,
    val userCohereKey: String? = null,
    val userCerebrasKey: String? = null,
    val userOpenRouterKey: String? = null
)
