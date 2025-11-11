package com.yourname.voicetodo.domain.model

import kotlinx.serialization.Serializable

data class Todo(
    val id: String,
    val title: String,
    val description: String? = null,  // Markdown content
    val categoryId: String,            // NEW
    val status: TodoStatus = TodoStatus.TODO,  // RENAME from 'section'
    val createdAt: Long = System.currentTimeMillis(),
    val reminderTime: Long? = null,
    val subtasks: List<Subtask> = emptyList()  // NEW
)

@Serializable
data class Subtask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val completed: Boolean = false
)