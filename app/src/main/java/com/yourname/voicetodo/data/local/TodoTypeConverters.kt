package com.yourname.voicetodo.data.local

import androidx.room.TypeConverter
import com.yourname.voicetodo.domain.model.Subtask
import com.yourname.voicetodo.domain.model.TodoStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TodoTypeConverters {

    @TypeConverter
    fun fromTodoStatus(status: TodoStatus): String {
        return status.name
    }

    @TypeConverter
    fun toTodoStatus(status: String): TodoStatus {
        return TodoStatus.valueOf(status)
    }

    @TypeConverter
    fun fromSubtasks(subtasks: List<Subtask>?): String? {
        return subtasks?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toSubtasks(subtasksJson: String?): List<Subtask> {
        return subtasksJson?.let { Json.decodeFromString(it) } ?: emptyList()
    }
}