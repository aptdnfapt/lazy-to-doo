package com.yourname.voicetodo.data.local

import androidx.room.TypeConverter
import com.yourname.voicetodo.domain.model.TodoSection

class TodoTypeConverters {
    
    @TypeConverter
    fun fromTodoSection(section: TodoSection): String {
        return section.name
    }
    
    @TypeConverter
    fun toTodoSection(section: String): TodoSection {
        return TodoSection.valueOf(section)
    }
}