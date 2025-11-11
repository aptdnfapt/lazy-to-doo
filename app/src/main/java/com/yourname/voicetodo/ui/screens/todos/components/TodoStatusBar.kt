package com.yourname.voicetodo.ui.screens.todos.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusOutlinedButton(
                    status = TodoStatus.TODO,
                    isSelected = currentStatus == TodoStatus.TODO,
                    onClick = { onStatusChange(TodoStatus.TODO) },
                    modifier = Modifier.weight(1f)
                )
                StatusOutlinedButton(
                    status = TodoStatus.IN_PROGRESS,
                    isSelected = currentStatus == TodoStatus.IN_PROGRESS,
                    onClick = { onStatusChange(TodoStatus.IN_PROGRESS) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusOutlinedButton(
                    status = TodoStatus.DONE,
                    isSelected = currentStatus == TodoStatus.DONE,
                    onClick = { onStatusChange(TodoStatus.DONE) },
                    modifier = Modifier.weight(1f)
                )
                StatusOutlinedButton(
                    status = TodoStatus.DO_LATER,
                    isSelected = currentStatus == TodoStatus.DO_LATER,
                    onClick = { onStatusChange(TodoStatus.DO_LATER) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatusOutlinedButton(
    status: TodoStatus,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        border = if (!isSelected) 
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        else null,
        modifier = modifier
    ) {
        Text(
            text = getStatusDisplayName(status),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
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