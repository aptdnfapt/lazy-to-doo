package com.yourname.voicetodo.data.repository

import com.yourname.voicetodo.data.local.TodoDao
import com.yourname.voicetodo.data.local.TodoEntity
import com.yourname.voicetodo.domain.model.Subtask
import com.yourname.voicetodo.domain.model.Todo
import com.yourname.voicetodo.domain.model.TodoStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    
    // NEW methods
    fun getTodosByCategory(categoryId: String): Flow<List<Todo>> =
        todoDao.getTodosByCategory(categoryId).map { it.map { entity -> entity.toDomainModel() } }

    fun getTodosByCategoryAndStatus(categoryId: String, status: TodoStatus): Flow<List<Todo>> =
        todoDao.getTodosByCategoryAndStatus(categoryId, status.name)
            .map { it.map { entity -> entity.toDomainModel() } }

    // UPDATED: Rename from getTodosBySection
    fun getTodosByStatus(status: TodoStatus): Flow<List<Todo>> =
        todoDao.getTodosByStatus(status.name).map { it.map { entity -> entity.toDomainModel() } }
    
    suspend fun getTodoById(id: String): Todo? {
        return todoDao.getTodoById(id)?.toDomainModel()
    }
    
    suspend fun insertTodo(todo: Todo) {
        todoDao.insertTodo(todo.toEntity())
    }
    
    // UPDATED: Add categoryId parameter
    suspend fun addTodo(
        title: String,
        description: String? = null,
        categoryId: String,
        status: TodoStatus = TodoStatus.TODO,
        reminderTime: Long? = null
    ): Todo {
        val todo = Todo(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description?.ifBlank { null },
            categoryId = categoryId,
            status = status,
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

    suspend fun deleteTodosByCategory(categoryId: String) {
        todoDao.deleteTodosByCategory(categoryId)
    }

    suspend fun updateTodoCategory(todoId: String, categoryId: String) {
        todoDao.updateTodoCategory(todoId, categoryId)
    }

    // UPDATED: Rename from updateTodoSection
    suspend fun updateTodoStatus(todoId: String, status: TodoStatus) {
        todoDao.updateTodoStatus(todoId, status.name)
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
        title = title,
        description = description,
        categoryId = categoryId,
        status = TodoStatus.valueOf(status),
        createdAt = createdAt,
        reminderTime = reminderTime,
        subtasks = subtasks?.let { Json.decodeFromString<List<Subtask>>(it) } ?: emptyList()
    )
}

private fun Todo.toEntity(): TodoEntity {
    return TodoEntity(
        id = id,
        title = title,
        description = description ?: "",
        categoryId = categoryId,
        status = status.name,
        createdAt = createdAt,
        completedAt = if (status == TodoStatus.DONE) System.currentTimeMillis() else null,
        reminderTime = reminderTime,
        subtasks = if (subtasks.isNotEmpty()) Json.encodeToString(subtasks) else null
    )
}