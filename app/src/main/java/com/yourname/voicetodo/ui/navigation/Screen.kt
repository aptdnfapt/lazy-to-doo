package com.yourname.voicetodo.ui.navigation

sealed class Screen(val route: String) {
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{sessionId}") {
        fun createRoute(sessionId: String) = "chat/$sessionId"
    }
    object Todos : Screen("todos")
    object TodoDetails : Screen("todo_details/{todoId}") {
        fun createRoute(todoId: String) = "todo_details/$todoId"
    }
    object Settings : Screen("settings")
    object ToolPermissions : Screen("tool_permissions")
}