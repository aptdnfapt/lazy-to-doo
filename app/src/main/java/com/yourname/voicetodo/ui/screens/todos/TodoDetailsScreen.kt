package com.yourname.voicetodo.ui.screens.todos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.yourname.voicetodo.ui.screens.todos.components.MarkdownEditor
import com.yourname.voicetodo.ui.screens.todos.components.MarkdownRenderer
import com.yourname.voicetodo.ui.screens.todos.components.TodoStatusBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailsScreen(
    todoId: String,
    navController: NavHostController,
    viewModel: TodoDetailsViewModel = hiltViewModel()
) {
    val todo by viewModel.todo.collectAsState()
    val category by viewModel.category.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val markdownContent by viewModel.markdownContent.collectAsState()
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    androidx.compose.foundation.layout.Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit title") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    showEditTitleDialog = true
                                    showOptionsMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            todo?.let {
                TodoStatusBar(
                    currentStatus = it.status,
                    onStatusChange = { viewModel.updateStatus(it) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category tag
            category?.let { cat ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(android.graphics.Color.parseColor(cat.color)).copy(alpha = 0.2f),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Label,
                            contentDescription = null,
                            tint = Color(android.graphics.Color.parseColor(cat.color))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sector: ${cat.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(android.graphics.Color.parseColor(cat.color))
                        )
                    }
                }
            }

            // AI update indicator
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AI updated 5 mins ago. Saved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Markdown editor/viewer
            if (isEditing) {
                MarkdownEditor(
                    content = markdownContent,
                    onContentChange = { viewModel.updateMarkdownContent(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )
            } else {
                MarkdownRenderer(
                    content = markdownContent,
                    onCheckboxToggle = { index, checked ->
                        viewModel.toggleSubtask(index, checked)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                )
            }

            // Edit/Save button
            Button(
                onClick = {
                    if (isEditing) viewModel.saveMarkdown() else viewModel.startEditing()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(if (isEditing) "Save" else "Edit")
            }
        }

        // Edit title dialog
        if (showEditTitleDialog) {
            var titleText by remember { mutableStateOf(todo?.title ?: "") }
            AlertDialog(
                onDismissRequest = { showEditTitleDialog = false },
                title = { Text("Edit Title") },
                text = {
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("Todo title") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateTitle(titleText)
                            showEditTitleDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditTitleDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}