package com.yourname.voicetodo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yourname.voicetodo.domain.model.TodoSection

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val section: String, // TodoSection enum as String
    val createdAt: Long,
    val completedAt: Long?,
    val reminderTime: Long?
)