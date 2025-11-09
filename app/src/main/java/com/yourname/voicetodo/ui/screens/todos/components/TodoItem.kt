package com.yourname.voicetodo.ui.screens.todos.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourname.voicetodo.domain.model.Todo
import com.yourname.voicetodo.domain.model.TodoSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TodoItem(
    todo: Todo,
    onEdit: (Todo) -> Unit,
    onDelete: (String) -> Unit,
    onSectionChange: (TodoSection) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onEdit(todo) },
                onLongClick = { /* Can be used for context menu if needed */ }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    todo.description?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatTimestamp(todo.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    // Section dropdown
                    ExposedDropdownMenuBox(
                        expanded = sectionExpanded,
                        onExpandedChange = { sectionExpanded = !sectionExpanded }
                    ) {
                        OutlinedTextField(
                            value = getSectionDisplayName(todo.section),
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier
                                .width(120.dp)
                                .menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) }
                        )
                        DropdownMenu(
                            expanded = sectionExpanded,
                            onDismissRequest = { sectionExpanded = false }
                        ) {
                            TodoSection.values().forEach { section ->
                                DropdownMenuItem(
                                    text = { Text(getSectionDisplayName(section)) },
                                    onClick = {
                                        onSectionChange(section)
                                        sectionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Edit button
                    IconButton(onClick = { onEdit(todo) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }

                    // Delete button
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Todo") },
            text = { Text("Are you sure you want to delete this todo?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(todo.id)
                        showDeleteDialog = false
                    }
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

private fun getSectionDisplayName(section: TodoSection): String {
    return when (section) {
        TodoSection.TODO -> "To Do"
        TodoSection.IN_PROGRESS -> "In Progress"
        TodoSection.DONE -> "Done"
        TodoSection.DO_LATER -> "Do Later"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}