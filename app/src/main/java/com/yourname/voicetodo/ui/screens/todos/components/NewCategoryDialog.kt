package com.yourname.voicetodo.ui.screens.todos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NewCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#137fec") } // Default blue color
    var icon by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Category") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    placeholder = { Text("Unique identifier (e.g., 'work', 'personal')") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name *") },
                    placeholder = { Text("Human-readable name (e.g., 'Work', 'Personal')") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                )

                // Color picker section
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Color preview
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(color)))
                    )

                    // Color input field
                    OutlinedTextField(
                        value = color,
                        onValueChange = { color = it },
                        label = { Text("Hex Color") },
                        placeholder = { Text("#137fec") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Preset colors
                Text(
                    text = "Quick Colors",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presetColors = listOf(
                        "#137fec", // Blue
                        "#10b981", // Green
                        "#f59e0b", // Yellow
                        "#ef4444", // Red
                        "#8b5cf6", // Purple
                        "#f97316", // Orange
                        "#06b6d4", // Cyan
                        "#84cc16"  // Lime
                    )

                    presetColors.forEach { presetColor ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(presetColor)))
                                .clickable { color = presetColor }
                        )
                    }
                }

                // Icon field (optional)
                OutlinedTextField(
                    value = icon ?: "",
                    onValueChange = { icon = it.ifBlank { null } },
                    label = { Text("Icon (optional)") },
                    placeholder = { Text("Material icon name (e.g., 'work', 'home')") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && displayName.isNotBlank()) {
                        onConfirm(name, displayName, color, icon)
                    }
                },
                enabled = name.isNotBlank() && displayName.isNotBlank()
            ) {
                Text("Create")
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