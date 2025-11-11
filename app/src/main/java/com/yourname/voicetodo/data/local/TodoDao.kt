package com.yourname.voicetodo.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    
    @Query("SELECT * FROM todos ORDER BY createdAt DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>
    
    // NEW: Filter by category
    @Query("SELECT * FROM todos WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getTodosByCategory(categoryId: String): Flow<List<TodoEntity>>

    // UPDATED: Rename from getTodosBySection
    @Query("SELECT * FROM todos WHERE status = :status ORDER BY createdAt DESC")
    fun getTodosByStatus(status: String): Flow<List<TodoEntity>>

    // NEW: Filter by category AND status
    @Query("SELECT * FROM todos WHERE categoryId = :categoryId AND status = :status ORDER BY createdAt DESC")
    fun getTodosByCategoryAndStatus(categoryId: String, status: String): Flow<List<TodoEntity>>
    
    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: String): TodoEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<TodoEntity>)
    
    @Update
    suspend fun updateTodo(todo: TodoEntity)
    
    @Delete
    suspend fun deleteTodo(todo: TodoEntity)
    
    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: String)

    @Query("DELETE FROM todos WHERE categoryId = :categoryId")
    suspend fun deleteTodosByCategory(categoryId: String)

    @Query("DELETE FROM todos")
    suspend fun deleteAllTodos()
    
    // NEW: Update category
    @Query("UPDATE todos SET categoryId = :categoryId WHERE id = :id")
    suspend fun updateTodoCategory(id: String, categoryId: String)

    // UPDATED: Rename from updateTodoSection
    @Query("UPDATE todos SET status = :status WHERE id = :id")
    suspend fun updateTodoStatus(id: String, status: String)
    
    @Query("UPDATE todos SET reminderTime = :reminderTime WHERE id = :id")
    suspend fun updateTodoReminder(id: String, reminderTime: Long?)
    
    @Query("SELECT COUNT(*) FROM todos")
    suspend fun getTodoCount(): Int
}