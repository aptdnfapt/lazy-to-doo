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
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh


import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

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
import com.yourname.voicetodo.ui.screens.chat.components.ToolCallBubble
import com.yourname.voicetodo.ui.screens.chat.components.CategoryDropdown
import com.yourname.voicetodo.ui.screens.chat.components.ChatInputBar
import com.yourname.voicetodo.domain.model.MessageType
import com.yourname.voicetodo.domain.model.ToolCallStatus
import kotlinx.serialization.json.Json
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
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()



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
        // Header with category dropdown
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Checklist,
                    contentDescription = "Checklist",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Assistant", style = MaterialTheme.typography.headlineSmall)

            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category selection dropdown
            CategoryDropdown(
                selectedCategoryId = selectedCategoryId,
                categories = categories,
                onCategorySelected = { viewModel.setSelectedCategory(it) },
                modifier = Modifier.fillMaxWidth()
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
                if (message.messageType == MessageType.TEXT) {
                    // Regular message bubble
                    MessageBubble(message = message)
                } else {
                    // Tool call bubble
                    val toolCall = message.toToolCallMessage()
                    ToolCallBubble(
                        toolCall = toolCall,
                        onApproveAlways = {
                            viewModel.onToolCallApproveAlways(message.id, toolCall.toolName)
                        },
                        onApproveOnce = {
                            viewModel.onToolCallApproveOnce(message.id)
                        },
                        onDeny = {
                            viewModel.onToolCallDeny(message.id)
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
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
            OutlinedCard(
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

        // Input bar (redesigned)
        ChatInputBar(
            textInput = textInput,
            onTextChange = { textInput = it },
            onSendClick = { message ->
                viewModel.sendTextMessage(message)
                textInput = ""
            },
            onMicClick = {
                if (isRecording) {
                    viewModel.stopRecording()
                } else {
                    viewModel.startRecording()
                }
            },
            isRecording = isRecording,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )


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

// Extension function to convert Message to ToolCallMessage
private fun com.yourname.voicetodo.domain.model.Message.toToolCallMessage(): com.yourname.voicetodo.domain.model.ToolCallMessage {
    // Parse JSON to Map<String, String> for display purposes
    val arguments = try {
        this.toolArguments?.let {
            Json.decodeFromString<Map<String, String>>(it)
        } ?: emptyMap()
    } catch (e: Exception) {
        emptyMap()
    }

    val status = try {
        this.toolStatus?.let { ToolCallStatus.valueOf(it) } ?: ToolCallStatus.PENDING_APPROVAL
    } catch (e: Exception) {
        ToolCallStatus.PENDING_APPROVAL
    }

    return com.yourname.voicetodo.domain.model.ToolCallMessage(
        id = this.id,
        toolName = this.toolName ?: "",
        arguments = arguments,
        status = status,
        result = this.toolResult,
        timestamp = this.timestamp,
        autoApproved = this.approved // Mark as auto-approved if it was previously approved
    )
}

