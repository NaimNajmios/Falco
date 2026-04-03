package com.najmi.falco.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.falco.data.local.DebugLogger
import com.najmi.falco.data.local.UserPreferences
import com.najmi.falco.data.local.UserPreferencesRepository
import com.najmi.falco.data.remote.LlmClient
import com.najmi.falco.data.remote.LlmProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val editingProvider: LlmProvider? = null,
    val keyValidationStatus: Map<LlmProvider, KeyValidationStatus> = emptyMap(),
    val validatingProviders: Set<LlmProvider> = emptySet()
)

enum class KeyValidationStatus {
    VALID,
    INVALID,
    VALIDATING,
    UNKNOWN
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val clients: Map<LlmProvider, @JvmSuppressWildcards LlmClient>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val providers = LlmProvider.entries.toList()

    init {
        viewModelScope.launch {
            userPreferencesRepository.preferences.collect { prefs ->
                _uiState.value = _uiState.value.copy(preferences = prefs, isLoading = false)
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkMode(enabled)
        }
    }

    fun setDebugMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDebugMode(enabled)
        }
    }

    fun setPreferredProvider(provider: LlmProvider) {
        viewModelScope.launch {
            userPreferencesRepository.setPreferredProvider(provider.name)
        }
    }

    fun setUserApiKey(provider: LlmProvider, key: String) {
        viewModelScope.launch {
            if (key.isBlank()) {
                userPreferencesRepository.setUserApiKey(provider, null)
                _uiState.value = _uiState.value.copy(
                    keyValidationStatus = _uiState.value.keyValidationStatus - provider
                )
            } else {
                userPreferencesRepository.setUserApiKey(provider, key)
            }
        }
    }

    fun validateAndSaveKeys(keys: Map<LlmProvider, String>) {
        viewModelScope.launch {
            val validating = keys.filter { it.value.isNotBlank() }.keys
            _uiState.value = _uiState.value.copy(
                validatingProviders = validating,
                keyValidationStatus = _uiState.value.keyValidationStatus.mapValues { (provider, status) ->
                    if (provider in validating) KeyValidationStatus.VALIDATING else status
                }
            )

            keys.forEach { (provider, key) ->
                if (key.isNotBlank()) {
                    userPreferencesRepository.setUserApiKey(provider, key)
                    val result = validateApiKey(provider, key)
                    _uiState.value = _uiState.value.copy(
                        keyValidationStatus = _uiState.value.keyValidationStatus + (provider to result),
                        validatingProviders = _uiState.value.validatingProviders - provider
                    )
                }
            }
        }
    }

    private suspend fun validateApiKey(provider: LlmProvider, key: String): KeyValidationStatus {
        return withContext(Dispatchers.IO) {
            try {
                val client = clients[provider]
                if (client == null) {
                    DebugLogger.w("[Settings] No client for provider: $provider")
                    return@withContext KeyValidationStatus.UNKNOWN
                }

                val testPrompt = "Hi"
                val response = client.chat("test: $testPrompt")
                
                if (response.text.isNotBlank()) {
                    KeyValidationStatus.VALID
                } else {
                    KeyValidationStatus.INVALID
                }
            } catch (e: Exception) {
                val errorMsg = e.message?.lowercase() ?: ""
                when {
                    errorMsg.contains("invalid") || errorMsg.contains("unauthorized") || 
                    errorMsg.contains("api key") || errorMsg.contains("401") ||
                    errorMsg.contains("403") || errorMsg.contains("malformed") -> {
                        DebugLogger.d("[Settings] Key validation failed for $provider: ${e.message}")
                        KeyValidationStatus.INVALID
                    }
                    else -> {
                        DebugLogger.w("[Settings] Key validation error for $provider: ${e.message}")
                        KeyValidationStatus.UNKNOWN
                    }
                }
            }
        }
    }

    fun startEditingKey(provider: LlmProvider) {
        _uiState.value = _uiState.value.copy(editingProvider = provider)
    }

    fun stopEditingKey() {
        _uiState.value = _uiState.value.copy(editingProvider = null)
    }

    fun getCurrentKey(provider: LlmProvider): String {
        return when (provider) {
            LlmProvider.GEMINI -> _uiState.value.preferences.userGeminiKey ?: ""
            LlmProvider.GROQ -> _uiState.value.preferences.userGroqKey ?: ""
            LlmProvider.CEREBRAS -> _uiState.value.preferences.userCerebrasKey ?: ""
            LlmProvider.OPENROUTER -> _uiState.value.preferences.userOpenRouterKey ?: ""
            LlmProvider.MISTRAL -> _uiState.value.preferences.userMistralKey ?: ""
            LlmProvider.COHERE -> _uiState.value.preferences.userCohereKey ?: ""
            LlmProvider.ROUTEWAY -> _uiState.value.preferences.userRoutewayKey ?: ""
        }
    }

    fun hasUserKey(provider: LlmProvider): Boolean {
        return when (provider) {
            LlmProvider.GEMINI -> !_uiState.value.preferences.userGeminiKey.isNullOrBlank()
            LlmProvider.GROQ -> !_uiState.value.preferences.userGroqKey.isNullOrBlank()
            LlmProvider.CEREBRAS -> !_uiState.value.preferences.userCerebrasKey.isNullOrBlank()
            LlmProvider.OPENROUTER -> !_uiState.value.preferences.userOpenRouterKey.isNullOrBlank()
            LlmProvider.MISTRAL -> !_uiState.value.preferences.userMistralKey.isNullOrBlank()
            LlmProvider.COHERE -> !_uiState.value.preferences.userCohereKey.isNullOrBlank()
            LlmProvider.ROUTEWAY -> !_uiState.value.preferences.userRoutewayKey.isNullOrBlank()
        }
    }

    fun getValidationStatus(provider: LlmProvider): KeyValidationStatus {
        return _uiState.value.keyValidationStatus[provider] ?: KeyValidationStatus.UNKNOWN
    }

    fun isValidating(provider: LlmProvider): Boolean {
        return provider in _uiState.value.validatingProviders
    }

    fun clearAllKeys() {
        viewModelScope.launch {
            userPreferencesRepository.clearAllUserKeys()
            _uiState.value = _uiState.value.copy(keyValidationStatus = emptyMap())
        }
    }
}
