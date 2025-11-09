package com.yourname.voicetodo.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.yourname.voicetodo.ui.screens.chat.components.MessageBubble
import com.yourname.voicetodo.ui.screens.chat.components.MicButton
import com.yourname.voicetodo.ui.screens.chat.components.ToolPermissionDialog
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    sessionId: String,
    navController: NavHostController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()

    // NEW: Tool activity state
    val toolActivities by viewModel.toolActivities.collectAsState()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Initialize session
    LaunchedEffect(sessionId) {
        viewModel.initializeSession(sessionId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Clear error messages after a delay
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Column(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "Voice Todo Assistant",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Using: $currentModel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message
                )
            }

            // NEW: Tool activities section
            item {
                if (toolActivities.isNotEmpty()) {
                    ToolActivitiesSection(
                        activities = toolActivities,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        // Status indicators
        if (isTranscribing) {
            StatusIndicator("Transcribing audio...")
        }
        if (isProcessing) {
            StatusIndicator("Processing your request...")
        }

        // Error message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Text input
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Type a message...") },
                enabled = !isRecording && !isTranscribing && !isProcessing,
                modifier = Modifier.weight(1f),
                maxLines = 3,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send button
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendTextMessage(textInput)
                        textInput = ""
                    }
                },
                enabled = textInput.isNotBlank() && !isRecording && !isTranscribing && !isProcessing,
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Microphone button
            MicButton(
                isRecording = isRecording,
                isTranscribing = isTranscribing,
                isProcessing = isProcessing,
                amplitude = amplitude,
                onRecordingStart = { viewModel.startRecording() },
                onRecordingStop = { viewModel.stopRecording() }
            )
        }

        // NEW: Permission dialog
        showPermissionDialog?.let { activity ->
            ToolPermissionDialog(
                toolName = activity.toolName,
                toolArguments = activity.arguments,
                onDismiss = { viewModel.onPermissionDeny(activity.id) },
                onAllowOnce = { viewModel.onPermissionAllowOnce(activity.id) },
                onAlwaysAllow = { viewModel.onPermissionAlwaysAllow(activity.id, activity.toolName) },
                onDeny = { viewModel.onPermissionDeny(activity.id) }
            )
        }
    }
}

@Composable
private fun StatusIndicator(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToolActivitiesSection(
    activities: List<ChatViewModel.ToolActivity>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Agent Activity",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        activities.forEach { activity ->
            ToolActivityItem(activity = activity)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ToolActivityItem(activity: ChatViewModel.ToolActivity) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (activity.status) {
                ChatViewModel.ToolStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                ChatViewModel.ToolStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                ChatViewModel.ToolStatus.DENIED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Icon(
                imageVector = when (activity.status) {
                    ChatViewModel.ToolStatus.PENDING_PERMISSION -> Icons.Default.Lock
                    ChatViewModel.ToolStatus.EXECUTING -> Icons.Default.Refresh
                    ChatViewModel.ToolStatus.RETRYING -> Icons.Default.Refresh
                    ChatViewModel.ToolStatus.SUCCESS -> Icons.Default.Check
                    ChatViewModel.ToolStatus.FAILED -> Icons.Default.Close
                    ChatViewModel.ToolStatus.DENIED -> Icons.Default.Close
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.toolName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = when (activity.status) {
                        ChatViewModel.ToolStatus.PENDING_PERMISSION -> "Waiting for permission..."
                        ChatViewModel.ToolStatus.EXECUTING -> "Executing..."
                        ChatViewModel.ToolStatus.RETRYING -> "Retrying..."
                        ChatViewModel.ToolStatus.SUCCESS -> activity.result ?: "Success"
                        ChatViewModel.ToolStatus.FAILED -> activity.result ?: "Failed"
                        ChatViewModel.ToolStatus.DENIED -> "Permission denied"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (activity.status == ChatViewModel.ToolStatus.EXECUTING ||
                activity.status == ChatViewModel.ToolStatus.RETRYING
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}