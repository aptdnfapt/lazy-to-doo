package com.yourname.voicetodo.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,

    // NEW: For tool call messages
    val messageType: String = "TEXT",  // TEXT or TOOL_CALL
    val toolName: String? = null,
    val toolArguments: String? = null,  // JSON string
    val toolStatus: String? = null,      // ToolCallStatus as string
    val toolResult: String? = null,
    val approved: Boolean = false
)