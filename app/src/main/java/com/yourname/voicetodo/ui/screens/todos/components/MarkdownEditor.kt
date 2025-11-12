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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun insertAtCursor(
    textFieldValue: androidx.compose.ui.text.input.TextFieldValue,
    insertText: String,
    onContentChange: (String) -> Unit,
    onTextFieldValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit
) {
    val newText = buildString {
        append(textFieldValue.text.substring(0, textFieldValue.selection.start))
        append(insertText)
        append(textFieldValue.text.substring(textFieldValue.selection.end))
    }
    val newSelection = TextRange(
        textFieldValue.selection.start + insertText.length
    )
    val newTextFieldValue = textFieldValue.copy(
        text = newText,
        selection = newSelection
    )
    onContentChange(newText)
    onTextFieldValueChange(newTextFieldValue)
}

@Composable
fun MarkdownEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(content)) }

    // Update textFieldValue when content changes from external sources
    LaunchedEffect(content) {
        if (textFieldValue.text != content) {
            textFieldValue = textFieldValue.copy(text = content)
        }
    }

    Column(modifier = modifier) {
        // Markdown toolbar
        MarkdownToolbar(
            onInsertCheckbox = {
                insertAtCursor(textFieldValue, "\n[] ", onContentChange) { newTextFieldValue ->
                    textFieldValue = newTextFieldValue
                }
            },
            onInsertBold = {
                insertAtCursor(textFieldValue, "**bold text**", onContentChange) { newTextFieldValue ->
                    textFieldValue = newTextFieldValue
                }
            },
            onInsertItalic = {
                insertAtCursor(textFieldValue, "*italic text*", onContentChange) { newTextFieldValue ->
                    textFieldValue = newTextFieldValue
                }
            },
            onInsertList = {
                insertAtCursor(textFieldValue, "\n- ", onContentChange) { newTextFieldValue ->
                    textFieldValue = newTextFieldValue
                }
            },
            onInsertHeading = {
                insertAtCursor(textFieldValue, "\n## Heading", onContentChange) { newTextFieldValue ->
                    textFieldValue = newTextFieldValue
                }
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
                value = textFieldValue,
                onValueChange = { newTextFieldValue ->
                    textFieldValue = newTextFieldValue
                    onContentChange(newTextFieldValue.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary
                    )
                ),
                decorationBox = { innerTextField ->
                    if (textFieldValue.text.isEmpty()) {
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
            ToolbarOutlinedButton(icon = Icons.AutoMirrored.Filled.List, tooltip = "List", onClick = onInsertList)
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