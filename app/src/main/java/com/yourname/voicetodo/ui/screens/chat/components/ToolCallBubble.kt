package com.yourname.voicetodo.ui.screens.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.voicetodo.domain.model.ToolCallMessage
import com.yourname.voicetodo.domain.model.ToolCallStatus

@Composable
fun ToolCallBubble(
    toolCall: ToolCallMessage,
    onApproveAlways: () -> Unit = {},
    onApproveOnce: () -> Unit = {},
    onDeny: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (toolCall.status) {
        ToolCallStatus.SUCCESS -> Color(0xFF4CAF50)
        ToolCallStatus.FAILED, ToolCallStatus.DENIED -> Color(0xFFF44336)
        ToolCallStatus.EXECUTING, ToolCallStatus.RETRYING -> Color(0xFF2196F3)
        else -> Color(0xFFFFA726)
    }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth(0.85f),
        colors = CardDefaults.cardColors(
            containerColor = when (toolCall.status) {
                ToolCallStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                ToolCallStatus.FAILED, ToolCallStatus.DENIED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Tool name + expand button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status icon
                    Icon(
                        imageVector = when (toolCall.status) {
                            ToolCallStatus.PENDING_APPROVAL -> Icons.Default.Lock
                            ToolCallStatus.EXECUTING -> Icons.Default.Refresh
                            ToolCallStatus.RETRYING -> Icons.Default.Refresh
                            ToolCallStatus.SUCCESS -> Icons.Default.Check
                            ToolCallStatus.FAILED -> Icons.Default.Close
                            ToolCallStatus.DENIED -> Icons.Default.Close
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = statusColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = toolCall.toolName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getStatusText(toolCall.status),
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                }

                // Expand/collapse button
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                                      else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            // Expandable details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Arguments
                    if (toolCall.arguments.isNotEmpty()) {
                        Text(
                            text = "Arguments:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        toolCall.arguments.forEach { (key, value) ->
                            Text(
                                text = "  $key: ${value?.toString() ?: "null"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Result (if completed)
                    if (toolCall.result != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Result:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = toolCall.result,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action buttons (only show if pending approval and not auto-approved)
            if (toolCall.status == ToolCallStatus.PENDING_APPROVAL && !toolCall.autoApproved) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDeny,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Deny")
                    }
                    TextButton(onClick = onApproveOnce) {
                        Text("Allow Once")
                    }
                    OutlinedButton(onClick = onApproveAlways) {
                        Text("Always Allow")
                    }
                }
            }

            // Loading indicator
            if (toolCall.status == ToolCallStatus.EXECUTING ||
                toolCall.status == ToolCallStatus.RETRYING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = statusColor
                )
            }
        }
    }
}

private fun getStatusText(status: ToolCallStatus): String {
    return when (status) {
        ToolCallStatus.PENDING_APPROVAL -> "Waiting for approval..."
        ToolCallStatus.EXECUTING -> "Executing..."
        ToolCallStatus.RETRYING -> "Retrying..."
        ToolCallStatus.SUCCESS -> "Completed successfully"
        ToolCallStatus.FAILED -> "Failed"
        ToolCallStatus.DENIED -> "Denied by user"
    }
}