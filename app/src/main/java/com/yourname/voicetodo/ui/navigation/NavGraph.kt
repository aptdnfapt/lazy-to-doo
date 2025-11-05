package com.yourname.voicetodo.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourname.voicetodo.ui.screens.chat.ChatScreen
import com.yourname.voicetodo.ui.screens.settings.SettingsScreen
import com.yourname.voicetodo.ui.screens.todos.TodoListScreen

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
            startDestination = Screen.Chat.route,
            modifier = modifier.padding(paddingValues)
        ) {
            composable(Screen.Chat.route) {
                ChatScreen()
            }
            composable(Screen.Todos.route) {
                TodoListScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}