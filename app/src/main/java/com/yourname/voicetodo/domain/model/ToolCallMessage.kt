package com.yourname.voicetodo.domain.model

data class ToolCallMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val toolName: String,
    val arguments: Map<String, String>,  // Changed from Any? to String for serialization
    val status: ToolCallStatus,
    val result: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val approved: Boolean = false,
    val denied: Boolean = false
)

enum class ToolCallStatus {
    PENDING_APPROVAL,   // Waiting for user to approve/deny
    EXECUTING,          // User approved, executing now
    RETRYING,           // Failed, retrying
    SUCCESS,            // Completed successfully
    FAILED,             // Failed after retries
    DENIED              // User denied
}