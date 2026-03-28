package com.najmi.falco.data.local

data class UserPreferences(
    val isDarkMode: Boolean = true,
    val isDebugMode: Boolean = false,
    val preferredProvider: String = "GROQ"
)
