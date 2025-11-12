package com.yourname.voicetodo.ui.screens.todos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.yourname.voicetodo.domain.model.TodoStatus
import com.yourname.voicetodo.ui.navigation.Screen
import com.yourname.voicetodo.ui.screens.todos.components.AddTodoDialog
import com.yourname.voicetodo.ui.screens.todos.components.CategoryAccordion
import com.yourname.voicetodo.ui.screens.todos.components.ExpandableFab
import com.yourname.voicetodo.ui.screens.todos.components.NewCategoryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    navController: NavHostController,
    viewModel: TodoListViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val todosByCategory by viewModel.todosByCategory.collectAsState()
    val expandedCategories by viewModel.expandedCategories.collectAsState()
    val selectedStatusByCategory by viewModel.selectedStatusByCategory.collectAsState()
    val isFabExpanded by viewModel.isFabExpanded.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val editingTodo by viewModel.editingTodo.collectAsState()
    val showCategoryDialog by viewModel.showCategoryDialog.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = { /* menu */ }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = null
                            )
                        }
                    },
                    // Removed profile button - not needed for open source app
                )
            },
            floatingActionButton = {
                ExpandableFab(
                    expanded = isFabExpanded,
                    onExpandedChange = { viewModel.toggleFabExpanded() },
                    onNewTodoClick = { viewModel.showNewTodoDialog() },
                    onNewCategoryClick = { viewModel.showNewCategoryDialog() },
                    onNewChatClick = { viewModel.createNewChatSession(navController) }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { category ->
                    CategoryAccordion(
                        category = category,
                        todos = todosByCategory[category.id] ?: emptyMap(),
                        isExpanded = expandedCategories.contains(category.id),
                        selectedStatus = selectedStatusByCategory[category.id] ?: TodoStatus.TODO,
                        onExpandChange = { viewModel.toggleCategoryExpanded(category.id) },
                        onStatusChange = { status -> viewModel.selectStatusForCategory(category.id, status) },
                        onTodoClick = { todo -> navController.navigate(Screen.TodoDetails.createRoute(todo.id)) },
                        onMoveToStatus = { todo, status ->
                            viewModel.moveTodo(todo.id, status)
                        },
                        onMoveToCategory = { todo, targetCategoryId ->
                            viewModel.moveTodoToCategory(todo.id, targetCategoryId)
                        },
                        onDeleteTodo = { todo -> viewModel.deleteTodo(todo.id) },
                        onDeleteCategory = { viewModel.deleteCategory(category.id) },
                        onEditCategory = { viewModel.editCategory(it) },
                        allCategories = categories
                    )
                }
            }
        }

        // Add/Edit Dialog
        if (showDialog) {
            AddTodoDialog(
                todo = editingTodo,
                categories = categories,
                onDismiss = { viewModel.hideDialog() },
                onConfirm = { title, description, categoryId, status ->
                    if (editingTodo != null) {
                        val updatedTodo = editingTodo!!.copy(
                            title = title,
                            description = description,
                            categoryId = categoryId,
                            status = status
                        )
                        viewModel.updateTodo(updatedTodo)
                    } else {
                        viewModel.addTodo(title, description, categoryId, status)
                    }
                }
            )
        }

        // New Category Dialog
        if (showCategoryDialog) {
            NewCategoryDialog(
                onDismiss = { viewModel.hideCategoryDialog() },
                onConfirm = { name, color ->
                    viewModel.createCategory(name, color)
                }
            )
        }
    }
}