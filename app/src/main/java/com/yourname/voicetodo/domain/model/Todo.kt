package com.yourname.voicetodo.domain.model

data class Todo(
    val id: String,
    val title: String,
    val description: String? = null,
    val section: TodoSection = TodoSection.TODO,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderTime: Long? = null
)