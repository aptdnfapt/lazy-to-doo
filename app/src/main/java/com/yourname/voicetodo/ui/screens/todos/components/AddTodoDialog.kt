package com.yourname.voicetodo.ui.screens.todos.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourname.voicetodo.domain.model.Todo
import com.yourname.voicetodo.domain.model.TodoSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    todo: Todo? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, TodoSection) -> Unit
) {
    var title by remember(todo) { mutableStateOf(todo?.description ?: "") }
    var description by remember(todo) { mutableStateOf("") }
    var selectedSection by remember(todo) { mutableStateOf(todo?.section ?: TodoSection.TODO) }
    var sectionExpanded by remember { mutableStateOf(false) }

    val isEditing = todo != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Todo" else "Add Todo") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    placeholder = { Text("Enter todo title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Enter additional details") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                ExposedDropdownMenuBox(
                    expanded = sectionExpanded,
                    onExpandedChange = { sectionExpanded = !sectionExpanded }
                ) {
                    OutlinedTextField(
                        value = getSectionDisplayName(selectedSection),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Section") },
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    selectedSection = section
                                    sectionExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, description.ifBlank { null }, selectedSection)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (isEditing) "Update" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier.padding(8.dp)
    )
}

private fun getSectionDisplayName(section: TodoSection): String {
    return when (section) {
        TodoSection.TODO -> "To Do"
        TodoSection.IN_PROGRESS -> "In Progress"
        TodoSection.DONE -> "Done"
        TodoSection.DO_LATER -> "Do Later"
    }
}