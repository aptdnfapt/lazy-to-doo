package com.yourname.voicetodo.ui.screens.todos.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourname.voicetodo.domain.model.Category
import com.yourname.voicetodo.domain.model.Todo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodoCard(
    todo: Todo,
    categories: List<Category>,
    onClick: () -> Unit,
    onMoveToStatus: (com.yourname.voicetodo.domain.model.TodoStatus) -> Unit,
    onMoveToCategory: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.titleMedium.lineHeight
                )
                todo.description?.let { desc ->
                    if (desc.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = desc.take(100) + if (desc.length > 100) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
                }
                if (todo.reminderTime != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Due: ${formatTimestamp(todo.reminderTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Move to status
                    Text(
                        "Move to Status",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    com.yourname.voicetodo.domain.model.TodoStatus.values()
                        .filter { it != todo.status }
                        .forEach { status ->
                            DropdownMenuItem(
                                text = { Text(getStatusDisplayNameForCard(status)) },
                                onClick = {
                                    onMoveToStatus(status)
                                    showMenu = false
                                }
                            )
                        }

                    androidx.compose.material3.HorizontalDivider()

                    // Move to category (if multiple categories exist)
                    if (categories.filter { it.id != todo.categoryId }.isNotEmpty()) {
                        Text(
                            "Move to Category",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        categories.filter { it.id != todo.categoryId }.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row {
                                        category.icon?.let { Text("$it ") }
                                        Text(category.displayName)
                                    }
                                },
                                onClick = {
                                    onMoveToCategory(category.id)
                                    showMenu = false
                                }
                            )
                        }

                        androidx.compose.material3.HorizontalDivider()
                    }

                    // Delete option
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete",
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
                            showDeleteDialog = true
                            showMenu = false
                        }
                    )
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Todo?") },
                text = { Text("Are you sure you want to delete \"${todo.title}\"? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDelete()
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getStatusDisplayNameForCard(status: com.yourname.voicetodo.domain.model.TodoStatus): String {
    return when (status) {
        com.yourname.voicetodo.domain.model.TodoStatus.TODO -> "To Do"
        com.yourname.voicetodo.domain.model.TodoStatus.IN_PROGRESS -> "In Progress"
        com.yourname.voicetodo.domain.model.TodoStatus.DONE -> "Done"
        com.yourname.voicetodo.domain.model.TodoStatus.DO_LATER -> "Do Later"
    }
}