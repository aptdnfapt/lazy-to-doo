package com.yourname.voicetodo.ui.screens.todos.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Markdown toolbar
        MarkdownToolbar(
            onInsertCheckbox = {
                val newContent = "$content\n- [ ] "
                onContentChange(newContent)
            },
            onInsertBold = {
                val newContent = "$content**bold text**"
                onContentChange(newContent)
            },
            onInsertItalic = {
                val newContent = "$content*italic text*"
                onContentChange(newContent)
            },
            onInsertList = {
                val newContent = "$content\n- "
                onContentChange(newContent)
            },
            onInsertHeading = {
                val newContent = "$content\n## Heading"
                onContentChange(newContent)
            }
        )

        // Text editor
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    if (content.isEmpty()) {
                        Text(
                            "Add task details, checklists, notes...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun MarkdownToolbar(
    onInsertCheckbox: () -> Unit,
    onInsertBold: () -> Unit,
    onInsertItalic: () -> Unit,
    onInsertList: () -> Unit,
    onInsertHeading: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ToolbarOutlinedButton(icon = Icons.Filled.Check, tooltip = "Checkbox", onClick = onInsertCheckbox)
            ToolbarOutlinedButton(icon = Icons.Filled.Star, tooltip = "Bold", onClick = onInsertBold)
            ToolbarOutlinedButton(icon = Icons.Filled.Info, tooltip = "Italic", onClick = onInsertItalic)
            ToolbarOutlinedButton(icon = Icons.Filled.List, tooltip = "List", onClick = onInsertList)
            ToolbarOutlinedButton(icon = Icons.Filled.TextFields, tooltip = "Heading", onClick = onInsertHeading)
        }
    }
}

@Composable
fun ToolbarOutlinedButton(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = tooltip,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onClick)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tooltip,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}