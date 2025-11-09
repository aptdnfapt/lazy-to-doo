package com.yourname.voicetodo.domain.model

data class Message(
    val id: String,
    val sessionId: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),

    // NEW: For tool call messages
    val messageType: MessageType = MessageType.TEXT,  // TEXT or TOOL_CALL
    val toolName: String? = null,
    val toolArguments: String? = null,  // JSON string
    val toolStatus: String? = null,      // ToolCallStatus as string
    val toolResult: String? = null
)

enum class MessageType {
    TEXT,
    TOOL_CALL
}