package com.yourname.voicetodo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(
    navController: NavController
) {
    val items = listOf(
        BottomNavItem(
            screen = Screen.ChatList,
            title = "Chat",
            icon = Icons.Default.Chat
        ),
        BottomNavItem(
            screen = Screen.Todos,
            title = "Dashboard",
            icon = Icons.Default.Dashboard
        ),
        BottomNavItem(
            screen = Screen.Settings,
            title = "Settings",
            icon = Icons.Default.List
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            val isSelected = when (item.screen) {
                is Screen.ChatList -> currentRoute == Screen.ChatList.route || currentRoute?.startsWith("chat/") == true
                else -> currentRoute == item.screen.route
            }
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = isSelected,
                onClick = {
                    val targetRoute = when (item.screen) {
                        is Screen.ChatList -> {
                            // If we're in a chat session, pop back to chat list
                            if (currentRoute?.startsWith("chat/") == true) {
                                navController.popBackStack(Screen.ChatList.route, false)
                                return@NavigationBarItem
                            }
                            Screen.ChatList.route
                        }
                        else -> item.screen.route
                    }
                    navController.navigate(targetRoute) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
)