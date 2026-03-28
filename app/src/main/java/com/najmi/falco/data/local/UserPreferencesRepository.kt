package com.najmi.falco.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.najmi.falco.data.remote.LlmProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "falco_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
        val PREFERRED_PROVIDER = stringPreferencesKey("preferred_provider")
        val KEYS_REVISION = longPreferencesKey("keys_revision")
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "falco_encrypted_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            isDarkMode = prefs[Keys.DARK_MODE] ?: true,
            isDebugMode = prefs[Keys.DEBUG_MODE] ?: false,
            preferredProvider = prefs[Keys.PREFERRED_PROVIDER] ?: "GROQ",
            userGeminiKey = getEncryptedKey("user_gemini_key"),
            userGroqKey = getEncryptedKey("user_groq_key"),
            userMistralKey = getEncryptedKey("user_mistral_key"),
            userCohereKey = getEncryptedKey("user_cohere_key"),
            userCerebrasKey = getEncryptedKey("user_cerebras_key"),
            userOpenRouterKey = getEncryptedKey("user_openrouter_key")
        )
    }

    private fun getEncryptedKey(keyName: String): String? {
        return encryptedPrefs.getString(keyName, null)
    }

    private fun setEncryptedKey(keyName: String, value: String?) {
        if (value.isNullOrBlank()) {
            encryptedPrefs.edit().remove(keyName).apply()
        } else {
            encryptedPrefs.edit().putString(keyName, value).apply()
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setDebugMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEBUG_MODE] = enabled }
        DebugLogger.setEnabled(enabled)
    }

    suspend fun setPreferredProvider(provider: String) {
        context.dataStore.edit { it[Keys.PREFERRED_PROVIDER] = provider }
    }

    suspend fun setUserApiKey(provider: LlmProvider, key: String?) {
        val keyName = when (provider) {
            LlmProvider.GEMINI -> "user_gemini_key"
            LlmProvider.GROQ -> "user_groq_key"
            LlmProvider.MISTRAL -> "user_mistral_key"
            LlmProvider.COHERE -> "user_cohere_key"
            LlmProvider.CEREBRAS -> "user_cerebras_key"
            LlmProvider.OPENROUTER -> "user_openrouter_key"
        }
        setEncryptedKey(keyName, key)
        context.dataStore.edit { it[Keys.KEYS_REVISION] = System.currentTimeMillis() }
    }

    fun getApiKey(provider: LlmProvider): String? {
        return when (provider) {
            LlmProvider.GEMINI -> getEncryptedKey("user_gemini_key")
            LlmProvider.GROQ -> getEncryptedKey("user_groq_key")
            LlmProvider.MISTRAL -> getEncryptedKey("user_mistral_key")
            LlmProvider.COHERE -> getEncryptedKey("user_cohere_key")
            LlmProvider.CEREBRAS -> getEncryptedKey("user_cerebras_key")
            LlmProvider.OPENROUTER -> getEncryptedKey("user_openrouter_key")
        }
    }

    suspend fun clearAllUserKeys() {
        setEncryptedKey("user_gemini_key", null)
        setEncryptedKey("user_groq_key", null)
        setEncryptedKey("user_mistral_key", null)
        setEncryptedKey("user_cohere_key", null)
        setEncryptedKey("user_cerebras_key", null)
        setEncryptedKey("user_openrouter_key", null)
        context.dataStore.edit { it[Keys.KEYS_REVISION] = System.currentTimeMillis() }
    }
}
