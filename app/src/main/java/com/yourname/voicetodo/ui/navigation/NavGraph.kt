package com.yourname.voicetodo.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourname.voicetodo.ui.screens.chat.ChatListScreen
import com.yourname.voicetodo.ui.screens.chat.ChatScreen
import com.yourname.voicetodo.ui.screens.settings.SettingsScreen
import com.yourname.voicetodo.ui.screens.settings.ToolPermissionsScreen
import com.yourname.voicetodo.ui.screens.todos.TodoListScreen
import com.yourname.voicetodo.ui.screens.todos.TodoDetailsScreen

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.ChatList.route,
            modifier = modifier.padding(paddingValues)
        ) {
            composable(Screen.ChatList.route) {
                ChatListScreen(navController = navController)
            }
            composable(Screen.Chat.route) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                ChatScreen(sessionId = sessionId, navController = navController)
            }
            composable(Screen.Todos.route) {
                TodoListScreen(navController = navController)
            }
            composable(Screen.TodoDetails.route) { backStackEntry ->
                val todoId = backStackEntry.arguments?.getString("todoId") ?: ""
                TodoDetailsScreen(todoId = todoId, navController = navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { /* No back button in bottom nav */ },
                    onNavigateToToolPermissions = {
                        navController.navigate(Screen.ToolPermissions.route)
                    }
                )
            }
            composable(Screen.ToolPermissions.route) {
                ToolPermissionsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}