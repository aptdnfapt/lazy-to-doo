package com.yourname.voicetodo.ui.screens.todos.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownRenderer(
    content: String,
    onCheckboxToggle: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedLines = remember(content) { parseMarkdown(content) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        parsedLines.forEachIndexed { index, line ->
            when (line) {
                is MarkdownLine.Heading -> {
                    Text(
                        text = line.text,
                        style = when (line.level) {
                            1 -> MaterialTheme.typography.headlineMedium
                            2 -> MaterialTheme.typography.headlineSmall
                            else -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                is MarkdownLine.Checkbox -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = line.checked,
                            onCheckedChange = { onCheckboxToggle(index, it) }
                        )
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (line.checked) TextDecoration.LineThrough else null,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                is MarkdownLine.ListItem -> {
                    Row {
                        Text("• ", style = MaterialTheme.typography.bodyLarge)
                        Text(line.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is MarkdownLine.Plain -> {
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

// Markdown parsing utility
sealed class MarkdownLine {
    data class Heading(val level: Int, val text: String) : MarkdownLine()
    data class Checkbox(val checked: Boolean, val text: String) : MarkdownLine()
    data class ListItem(val text: String) : MarkdownLine()
    data class Plain(val text: String) : MarkdownLine()
}

fun parseMarkdown(content: String): List<MarkdownLine> {
    return content.lines().mapNotNull { line ->
        when {
            line.startsWith("# ") -> MarkdownLine.Heading(1, line.removePrefix("# "))
            line.startsWith("## ") -> MarkdownLine.Heading(2, line.removePrefix("## "))
            line.startsWith("### ") -> MarkdownLine.Heading(3, line.removePrefix("### "))
            line.trim().startsWith("- [ ]") -> MarkdownLine.Checkbox(false, line.trim().removePrefix("- [ ]").trim())
            line.trim().startsWith("- [x]") || line.trim().startsWith("- [X]") ->
                MarkdownLine.Checkbox(true, line.trim().removePrefix("- [x]").removePrefix("- [X]").trim())
            line.trim().startsWith("- ") -> MarkdownLine.ListItem(line.trim().removePrefix("- "))
            line.isNotBlank() -> MarkdownLine.Plain(line)
            else -> null
        }
    }
}