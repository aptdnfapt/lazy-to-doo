package com.yourname.voicetodo.domain.model

import com.yourname.voicetodo.data.local.TodoEntity

fun TodoEntity.toDomainModel(): Todo {
    return Todo(
        id = id,
        title = title,
        description = description.takeIf { it.isNotBlank() },
        categoryId = categoryId,
        status = TodoStatus.valueOf(status),
        createdAt = createdAt,
        reminderTime = reminderTime,
        subtasks = emptyList()  // TODO: Parse from JSON
    )
}

fun Todo.toEntity(): TodoEntity {
    return TodoEntity(
        id = id,
        title = title,
        description = description ?: "",
        categoryId = categoryId,
        status = status.name,
        createdAt = createdAt,
        completedAt = if (status == TodoStatus.DONE) System.currentTimeMillis() else null,
        reminderTime = reminderTime,
        subtasks = null  // TODO: Serialize to JSON
    )
}