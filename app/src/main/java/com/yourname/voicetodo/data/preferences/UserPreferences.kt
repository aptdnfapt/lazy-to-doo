package com.yourname.voicetodo.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.yourname.voicetodo.domain.model.LLMProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // LLM Provider Settings
    fun getLlmProvider(): Flow<LLMProvider> = dataStore.data.map { preferences ->
        val provider = preferences[PreferencesKeys.LLM_PROVIDER] ?: "OPENAI"
        LLMProvider.valueOf(provider)
    }

    suspend fun setLlmProvider(provider: LLMProvider) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LLM_PROVIDER] = provider.name
        }
    }

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

    // Theme preference
    fun getThemeMode(): Flow<ThemeMode> = dataStore.data.map { preferences ->
        val mode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
        ThemeMode.valueOf(mode)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    enum class ThemeMode {
        LIGHT,
        DARK,
        SYSTEM  // Follow system theme
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

    // Tool Permissions
    fun getAllowedTools(): Flow<List<String>> = dataStore.data.map { preferences ->
        val json = preferences[PreferencesKeys.ALLOWED_TOOLS] ?: "[]"
        Json.decodeFromString<List<String>>(json)
    }

    suspend fun setAllowedTools(tools: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALLOWED_TOOLS] = Json.encodeToString(tools)
        }
    }
}