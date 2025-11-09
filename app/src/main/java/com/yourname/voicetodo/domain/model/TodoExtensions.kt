package com.yourname.voicetodo.domain.model

import com.yourname.voicetodo.data.local.TodoEntity

fun TodoEntity.toDomainModel(): Todo {
    return Todo(
        id = id,
        title = title,
        description = description.takeIf { it.isNotBlank() },
        section = TodoSection.valueOf(section),
        createdAt = createdAt,
        reminderTime = reminderTime
    )
}

fun Todo.toEntity(): TodoEntity {
    return TodoEntity(
        id = id,
        title = title,
        description = description ?: "",
        section = section.name,
        createdAt = createdAt,
        completedAt = if (section == TodoSection.DONE) System.currentTimeMillis() else null,
        reminderTime = reminderTime
    )
}