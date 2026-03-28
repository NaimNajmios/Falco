package com.najmi.falco.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.najmi.falco.data.local.UserPreferences
import com.najmi.falco.data.local.UserPreferencesRepository
import com.najmi.falco.data.remote.LlmProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val editingProvider: LlmProvider? = null,
    val keyValidationStatus: Map<LlmProvider, KeyValidationStatus> = emptyMap()
)

enum class KeyValidationStatus {
    VALID,
    INVALID,
    VALIDATING,
    UNKNOWN
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
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

    fun setUseUserKeys(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setUseUserKeys(enabled)
        }
    }

    fun setUserApiKey(provider: LlmProvider, key: String) {
        viewModelScope.launch {
            if (key.isBlank()) {
                userPreferencesRepository.setUserApiKey(provider, null)
            } else {
                userPreferencesRepository.setUserApiKey(provider, key)
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
        }
    }

    fun hasUserKey(provider: LlmProvider): Boolean {
        return when (provider) {
            LlmProvider.GEMINI -> !_uiState.value.preferences.userGeminiKey.isNullOrBlank()
            LlmProvider.GROQ -> !_uiState.value.preferences.userGroqKey.isNullOrBlank()
            LlmProvider.CEREBRAS -> !_uiState.value.preferences.userCerebrasKey.isNullOrBlank()
            LlmProvider.OPENROUTER -> !_uiState.value.preferences.userOpenRouterKey.isNullOrBlank()
        }
    }

    fun clearAllKeys() {
        viewModelScope.launch {
            userPreferencesRepository.clearAllUserKeys()
        }
    }
}
