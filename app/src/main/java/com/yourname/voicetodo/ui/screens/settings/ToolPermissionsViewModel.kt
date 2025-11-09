package com.yourname.voicetodo.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.voicetodo.ai.permission.ToolPermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolPermissionItem(
    val toolName: String,
    val displayName: String,
    val description: String,
    val isAllowed: Boolean
)

@HiltViewModel
class ToolPermissionsViewModel @Inject constructor(
    private val permissionManager: ToolPermissionManager
) : ViewModel() {

    private val _toolPermissions = MutableStateFlow<List<ToolPermissionItem>>(emptyList())
    val toolPermissions: StateFlow<List<ToolPermissionItem>> = _toolPermissions.asStateFlow()

    // Define all 10 tools
    private val allTools = listOf(
        ToolPermissionItem(
            toolName = "addTodo",
            displayName = "Add Todo",
            description = "Create new todo items",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "editTitle",
            displayName = "Edit Title",
            description = "Change todo titles",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "editDescription",
            displayName = "Edit Description",
            description = "Update todo descriptions",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "removeTodo",
            displayName = "Remove Todo",
            description = "Delete todo items",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "markComplete",
            displayName = "Mark Complete",
            description = "Mark todos as done",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "markInProgress",
            displayName = "Mark In Progress",
            description = "Mark todos as in progress",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "markDoLater",
            displayName = "Mark Do Later",
            description = "Mark todos to do later",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "setReminder",
            displayName = "Set Reminder",
            description = "Schedule reminders for todos",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "listTodos",
            displayName = "List Todos",
            description = "View all todos",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "readOutLoud",
            displayName = "Read Out Loud",
            description = "Text-to-speech for todos",
            isAllowed = false
        )
    )

    init {
        loadPermissions()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            permissionManager.init()
            _toolPermissions.value = allTools.map { tool ->
                tool.copy(isAllowed = permissionManager.isToolAlwaysAllowed(tool.toolName))
            }
        }
    }

    fun toggleToolPermission(toolName: String) {
        viewModelScope.launch {
            val currentPermission = permissionManager.isToolAlwaysAllowed(toolName)
            permissionManager.setToolAlwaysAllowed(toolName, !currentPermission)
            loadPermissions()  // Reload to update UI
        }
    }
}