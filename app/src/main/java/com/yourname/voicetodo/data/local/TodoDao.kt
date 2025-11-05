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
    
    @Query("SELECT * FROM todos WHERE section = :section ORDER BY createdAt DESC")
    fun getTodosBySection(section: String): Flow<List<TodoEntity>>
    
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
    
    @Query("DELETE FROM todos")
    suspend fun deleteAllTodos()
    
    @Query("UPDATE todos SET section = :newSection WHERE id = :id")
    suspend fun updateTodoSection(id: String, newSection: String)
    
    @Query("UPDATE todos SET reminderTime = :reminderTime WHERE id = :id")
    suspend fun updateTodoReminder(id: String, reminderTime: Long?)
    
    @Query("SELECT COUNT(*) FROM todos")
    suspend fun getTodoCount(): Int
}