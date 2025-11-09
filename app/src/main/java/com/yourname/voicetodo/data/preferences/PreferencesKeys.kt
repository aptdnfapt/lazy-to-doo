package com.yourname.voicetodo.data.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    // LLM Provider Settings
    val LLM_BASE_URL = stringPreferencesKey("llm_base_url")
    val LLM_API_KEY = stringPreferencesKey("llm_api_key")
    val LLM_MODEL_NAME = stringPreferencesKey("llm_model_name")
    
    // Voice-to-Text Settings
    val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    val VOICE_INPUT_ENABLED = booleanPreferencesKey("voice_input_enabled")
    
    // General Settings
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    val AUTO_EXECUTE = booleanPreferencesKey("auto_execute")

    // Tool Permissions
    val ALLOWED_TOOLS = stringPreferencesKey("allowed_tools")
}