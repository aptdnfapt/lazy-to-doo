package com.yourname.voicetodo.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.voicetodo.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferences
) : ViewModel() {

    // LLM Provider Settings
    val llmBaseUrl = preferences.getLlmBaseUrl()
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val llmApiKey = preferences.getLlmApiKey()
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val llmModelName = preferences.getLlmModelName()
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    // Voice-to-Text Settings
    val geminiApiKey = preferences.getGeminiApiKey()
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val voiceInputEnabled = preferences.getVoiceInputEnabled()
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    // General Settings
    val themeMode = preferences.getThemeMode()
        .stateIn(viewModelScope, SharingStarted.Lazily, UserPreferences.ThemeMode.SYSTEM)

    val ttsEnabled = preferences.getTtsEnabled()
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val autoExecute = preferences.getAutoExecute()
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // Update functions
    fun updateLlmBaseUrl(url: String) {
        viewModelScope.launch {
            preferences.setLlmBaseUrl(url.trim())
        }
    }

    fun updateLlmApiKey(apiKey: String) {
        viewModelScope.launch {
            preferences.setLlmApiKey(apiKey)
        }
    }

    fun updateLlmModelName(modelName: String) {
        viewModelScope.launch {
            preferences.setLlmModelName(modelName.trim())
        }
    }

    fun updateGeminiApiKey(apiKey: String) {
        viewModelScope.launch {
            preferences.setGeminiApiKey(apiKey)
        }
    }

    fun updateVoiceInputEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setVoiceInputEnabled(enabled)
        }
    }

    fun updateThemeMode(mode: UserPreferences.ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun updateTtsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setTtsEnabled(enabled)
        }
    }

    fun updateAutoExecute(autoExecute: Boolean) {
        viewModelScope.launch {
            preferences.setAutoExecute(autoExecute)
        }
    }
}