package com.yourname.voicetodo.ui.screens.todos.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourname.voicetodo.domain.model.Category
import com.yourname.voicetodo.domain.model.Todo
import com.yourname.voicetodo.domain.model.TodoStatus

@Composable
fun CategoryAccordion(
    category: Category,
    todos: Map<TodoStatus, List<Todo>>,  // Todos grouped by status
    isExpanded: Boolean,
    selectedStatus: TodoStatus,
    onExpandChange: () -> Unit,
    onStatusChange: (TodoStatus) -> Unit,
    onTodoClick: (Todo) -> Unit,
    onMoveToStatus: (Todo, TodoStatus) -> Unit,
    onMoveToCategory: (Todo, String) -> Unit,
    onDeleteTodo: (Todo) -> Unit,
    onDeleteCategory: () -> Unit,
    allCategories: List<Category> = emptyList()  // NEW: All categories for move menu
) {
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.3f)),
        color = Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.05f),
        modifier = Modifier.animateContentSize()
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onExpandChange),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${category.todoCount} active tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                        Icon(
                            if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                }

                // 3-dot menu for category
                Box {
                    IconButton(onClick = { showCategoryMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Category options")
                    }

                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        if (!category.isDefault) {  // Can't delete default categories
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete category",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showDeleteCategoryDialog = true
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Delete category confirmation
            if (showDeleteCategoryDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteCategoryDialog = false },
                    title = { Text("Delete ${category.displayName}?") },
                    text = {
                        Text(
                            "This will permanently delete ${category.todoCount} todos in this category. " +
                            "This action cannot be undone."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onDeleteCategory()
                                showDeleteCategoryDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteCategoryDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Expanded content
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    // Status tabs
                    ScrollableTabRow(
                        selectedTabIndex = TodoStatus.values().indexOf(selectedStatus),
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 16.dp
                    ) {
                        TodoStatus.values().forEach { status ->
                            Tab(
                                selected = selectedStatus == status,
                                onClick = { onStatusChange(status) },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(getStatusDisplayNameForTab(status))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${todos[status]?.size ?: 0}",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier
                                                .background(
                                                    if (selectedStatus == status)
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    else Color.Transparent,
                                                    CircleShape
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // Todo list for selected status
                    val statusTodos = todos[selectedStatus] ?: emptyList()
                    if (statusTodos.isEmpty()) {
                        EmptyStatusMessage(status = selectedStatus)
                    } else {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            statusTodos.forEach { todo ->
                                TodoCard(
                                    todo = todo,
                                    categories = allCategories.filter { it.id != todo.categoryId },
                                    onClick = { onTodoClick(todo) },
                                    onMoveToStatus = { status -> onMoveToStatus(todo, status) },
                                    onMoveToCategory = { targetId -> onMoveToCategory(todo, targetId) },
                                    onDelete = { onDeleteTodo(todo) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStatusMessage(status: TodoStatus) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (status) {
                TodoStatus.TODO -> "No tasks to do yet"
                TodoStatus.IN_PROGRESS -> "No tasks in progress"
                TodoStatus.DONE -> "No completed tasks"
                TodoStatus.DO_LATER -> "No tasks to do later"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getStatusDisplayNameForTab(status: TodoStatus): String {
    return when (status) {
        TodoStatus.TODO -> "To-Do"
        TodoStatus.IN_PROGRESS -> "In Progress"
        TodoStatus.DONE -> "Done"
        TodoStatus.DO_LATER -> "To Later"
    }
}