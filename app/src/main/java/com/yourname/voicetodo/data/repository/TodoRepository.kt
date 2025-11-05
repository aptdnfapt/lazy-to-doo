package com.yourname.voicetodo.data.repository

import com.yourname.voicetodo.data.local.TodoDao
import com.yourname.voicetodo.data.local.TodoEntity
import com.yourname.voicetodo.domain.model.Todo
import com.yourname.voicetodo.domain.model.TodoSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoRepository @Inject constructor(
    private val todoDao: TodoDao
) {
    
    fun getAllTodos(): Flow<List<Todo>> {
        return todoDao.getAllTodos().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    fun getTodosBySection(section: TodoSection): Flow<List<Todo>> {
        return todoDao.getTodosBySection(section.name).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    suspend fun getTodoById(id: String): Todo? {
        return todoDao.getTodoById(id)?.toDomainModel()
    }
    
    suspend fun insertTodo(todo: Todo) {
        todoDao.insertTodo(todo.toEntity())
    }
    
    suspend fun addTodo(
        title: String,
        description: String? = null,
        section: TodoSection = TodoSection.TODO,
        reminderTime: Long? = null
    ): Todo {
        val todo = Todo(
            id = UUID.randomUUID().toString(),
            description = description?.ifBlank { null } ?: title,
            section = section,
            reminderTime = reminderTime
        )
        insertTodo(todo)
        return todo
    }
    
    suspend fun updateTodo(todo: Todo) {
        todoDao.updateTodo(todo.toEntity())
    }
    
    suspend fun deleteTodo(todo: Todo) {
        todoDao.deleteTodo(todo.toEntity())
    }
    
    suspend fun deleteTodoById(id: String) {
        todoDao.deleteTodoById(id)
    }
    
    suspend fun updateTodoSection(id: String, section: TodoSection) {
        todoDao.updateTodoSection(id, section.name)
    }
    
    suspend fun updateTodoReminder(id: String, reminderTime: Long?) {
        todoDao.updateTodoReminder(id, reminderTime)
    }
    
    suspend fun deleteAllTodos() {
        todoDao.deleteAllTodos()
    }
    
    suspend fun getTodoCount(): Int {
        return todoDao.getTodoCount()
    }
}

// Extension functions for converting between Entity and Domain models
private fun TodoEntity.toDomainModel(): Todo {
    return Todo(
        id = id,
        description = description,
        section = TodoSection.valueOf(section),
        createdAt = createdAt,
        reminderTime = reminderTime
    )
}

private fun Todo.toEntity(): TodoEntity {
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