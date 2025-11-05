package com.yourname.voicetodo.ui.navigation

sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Todos : Screen("todos")
    object Settings : Screen("settings")
}