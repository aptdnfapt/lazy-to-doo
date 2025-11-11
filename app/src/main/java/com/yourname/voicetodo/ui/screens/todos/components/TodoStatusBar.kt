package com.yourname.voicetodo.ui.screens.todos.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourname.voicetodo.domain.model.TodoStatus

@Composable
fun TodoStatusBar(
    currentStatus: TodoStatus,
    onStatusChange: (TodoStatus) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TodoStatus.values().forEach { status ->
                StatusButton(
                    status = status,
                    isSelected = currentStatus == status,
                    onClick = { onStatusChange(status) }
                )
            }
        }
    }
}

@Composable
fun StatusButton(
    status: TodoStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = getStatusDisplayName(status),
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun getStatusDisplayName(status: TodoStatus): String {
    return when (status) {
        TodoStatus.TODO -> "To-Do"
        TodoStatus.IN_PROGRESS -> "In Progress"
        TodoStatus.DONE -> "Done"
        TodoStatus.DO_LATER -> "To Later"
    }
}