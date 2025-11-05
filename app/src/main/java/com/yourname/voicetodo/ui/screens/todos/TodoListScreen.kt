package com.yourname.voicetodo.ui.screens.todos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourname.voicetodo.domain.model.Todo
import com.yourname.voicetodo.domain.model.TodoSection
import com.yourname.voicetodo.ui.screens.todos.components.AddTodoDialog
import com.yourname.voicetodo.ui.screens.todos.components.TodoItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    onBackClick: () -> Unit = {},
    viewModel: TodoListViewModel = hiltViewModel()
) {
    val todos by viewModel.todos.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val editingTodo by viewModel.editingTodo.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todos") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Todo")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (todos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No todos yet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the + button to add your first todo",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TodoSection.values().forEach { section ->
                        val sectionTodos = todos.filter { it.section == section }
                        if (sectionTodos.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = getSectionDisplayName(section),
                                    count = sectionTodos.size
                                )
                            }
                            items(sectionTodos) { todo ->
                                TodoItem(
                                    todo = todo,
                                    onEdit = { viewModel.editTodo(it) },
                                    onDelete = { viewModel.deleteTodo(todo.id) },
                                    onSectionChange = { newSection ->
                                        viewModel.moveTodo(todo.id, newSection)
                                    }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Add/Edit Dialog
        if (showDialog) {
            AddTodoDialog(
                todo = editingTodo,
                onDismiss = { viewModel.hideDialog() },
                onConfirm = { title, description, section ->
                    if (editingTodo != null) {
                        val updatedTodo = editingTodo!!.copy(
                            description = title,
                            section = section
                        )
                        viewModel.updateTodo(updatedTodo)
                    } else {
                        viewModel.addTodo(title, description, section)
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$count items",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getSectionDisplayName(section: TodoSection): String {
    return when (section) {
        TodoSection.TODO -> "To Do"
        TodoSection.IN_PROGRESS -> "In Progress"
        TodoSection.DONE -> "Done"
        TodoSection.DO_LATER -> "Do Later"
    }
}