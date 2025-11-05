package com.yourname.voicetodo.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // LLM Provider Settings
    fun getLlmBaseUrl(): Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LLM_BASE_URL] ?: ""
    }

    suspend fun setLlmBaseUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LLM_BASE_URL] = url
        }
    }

    fun getLlmApiKey(): Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LLM_API_KEY] ?: ""
    }

    suspend fun setLlmApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LLM_API_KEY] = apiKey
        }
    }

    fun getLlmModelName(): Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LLM_MODEL_NAME] ?: ""
    }

    suspend fun setLlmModelName(modelName: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LLM_MODEL_NAME] = modelName
        }
    }

    // Voice-to-Text Settings
    fun getGeminiApiKey(): Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GEMINI_API_KEY] ?: ""
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY] = apiKey
        }
    }

    fun getVoiceInputEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.VOICE_INPUT_ENABLED] ?: true
    }

    suspend fun setVoiceInputEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOICE_INPUT_ENABLED] = enabled
        }
    }

    // General Settings
    fun getTheme(): Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME] ?: "system"
    }

    suspend fun setTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }

    fun getTtsEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TTS_ENABLED] ?: true
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_ENABLED] = enabled
        }
    }

    fun getAutoExecute(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_EXECUTE] ?: false
    }

    suspend fun setAutoExecute(autoExecute: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_EXECUTE] = autoExecute
        }
    }
}