package com.yourname.voicetodo.domain.model

data class Todo(
    val id: String,
    val description: String,
    val section: TodoSection = TodoSection.TODO,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderTime: Long? = null
)