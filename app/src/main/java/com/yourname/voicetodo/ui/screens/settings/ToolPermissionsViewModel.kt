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

    // Define all available tools
    private val allTools = listOf(
        // Todo management tools
        ToolPermissionItem(
            toolName = "addTodo",
            displayName = "Add Todo",
            description = "Create new todo items",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "removeTodo",
            displayName = "Remove Todo",
            description = "Delete todo items",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "editTitle",
            displayName = "Edit Title",
            description = "Change todo titles",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "addSubtask",
            displayName = "Add Subtask",
            description = "Add subtasks to existing todos",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "updateTodoContent",
            displayName = "Update Todo Content",
            description = "Update todo descriptions and subtasks",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "moveTodoToCategory",
            displayName = "Move Todo to Category",
            description = "Move todos between categories",
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
            description = "View all todos with filtering",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "findRelatedTodos",
            displayName = "Find Related Todos",
            description = "Search for todos by keywords",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "readOutLoud",
            displayName = "Read Out Loud",
            description = "Text-to-speech for todos",
            isAllowed = false
        ),
        // Category management tools
        ToolPermissionItem(
            toolName = "createCategory",
            displayName = "Create Category",
            description = "Create new todo categories",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "listCategories",
            displayName = "List Categories",
            description = "View all categories",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "deleteCategory",
            displayName = "Delete Category",
            description = "Delete existing categories",
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
            // Update UI immediately using the current state from manager
            val updatedAllowedTools = permissionManager.getCurrentAllowedTools()
            _toolPermissions.value = allTools.map { tool ->
                tool.copy(isAllowed = updatedAllowedTools.contains(tool.toolName))
            }
        }
    }
}