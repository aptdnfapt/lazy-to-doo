package com.yourname.voicetodo.domain.model

import com.yourname.voicetodo.data.local.TodoEntity

fun TodoEntity.toDomainModel(): Todo {
    return Todo(
        id = id,
        description = description,
        section = TodoSection.valueOf(section),
        createdAt = createdAt,
        reminderTime = reminderTime
    )
}

fun Todo.toEntity(): TodoEntity {
    return TodoEntity(
        id = id,
        title = description, // Use description as title for simplicity
        description = description,
        section = section.name,
        createdAt = createdAt,
        completedAt = if (section == TodoSection.DONE) System.currentTimeMillis() else null,
        reminderTime = reminderTime
    )
}