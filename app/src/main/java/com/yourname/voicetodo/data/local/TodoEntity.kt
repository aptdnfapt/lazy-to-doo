package com.yourname.voicetodo.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todos",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,       // CHANGE: Will be markdown formatted
    val categoryId: String,        // NEW: Foreign key to CategoryEntity
    val status: String,            // RENAME: from 'section' to 'status' (TodoStatus enum)
    val createdAt: Long,
    val completedAt: Long?,
    val reminderTime: Long?,
    val subtasks: String? = null   // NEW: JSON array of subtask objects
)