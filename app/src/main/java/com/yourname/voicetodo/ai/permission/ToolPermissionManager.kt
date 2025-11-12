package com.yourname.voicetodo.ai.permission

import com.yourname.voicetodo.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolPermissionManager @Inject constructor(
    private val userPreferences: UserPreferences
) {

    // Store which tools are always allowed
    private val alwaysAllowedTools = mutableSetOf<String>()

    suspend fun init() {
        // Load saved permissions from DataStore
        userPreferences.getAllowedTools().first().let { savedTools ->
            alwaysAllowedTools.addAll(savedTools)
        }
    }

    suspend fun isToolAlwaysAllowed(toolName: String): Boolean {
        return alwaysAllowedTools.contains(toolName)
    }

    fun getCurrentAllowedTools(): Set<String> {
        return alwaysAllowedTools.toSet()
    }

    suspend fun setToolAlwaysAllowed(toolName: String, allowed: Boolean) {
        if (allowed) {
            alwaysAllowedTools.add(toolName)
        } else {
            alwaysAllowedTools.remove(toolName)
        }
        // Save to DataStore and ensure persistence
        userPreferences.setAllowedTools(alwaysAllowedTools.toList())
        // Verify the save was successful by reloading
        userPreferences.getAllowedTools().first().let { savedTools ->
            alwaysAllowedTools.clear()
            alwaysAllowedTools.addAll(savedTools)
        }
    }

    suspend fun clearAllPermissions() {
        alwaysAllowedTools.clear()
        userPreferences.setAllowedTools(emptyList())
    }
}