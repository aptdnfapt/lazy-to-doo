package com.yourname.voicetodo.ai.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.yourname.voicetodo.data.repository.TodoRepository
import com.yourname.voicetodo.domain.model.TodoSection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@LLMDescription("Manage todo items and sections")
@Singleton
class TodoTools @Inject constructor(
    private val repository: TodoRepository
) : ToolSet {

    @Tool
    @LLMDescription("Add a new todo item")
    suspend fun addTodo(
        @LLMDescription("Title of the todo") title: String,
        @LLMDescription("Optional description") description: String? = null,
        @LLMDescription("Section to place the todo in (todo, in_progress, done, do_later)") section: String = "todo"
    ): String {
        return try {
            val todoSection = try {
                TodoSection.valueOf(section.uppercase().replace(" ", "_"))
            } catch (e: IllegalArgumentException) {
                TodoSection.TODO
            }
            
            val todo = repository.addTodo(
                title = title,
                description = description ?: title,
                section = todoSection
            )
            "✅ Added todo: ${todo.description} in ${todoSection.name.replace("_", " ").lowercase()}"
        } catch (e: Exception) {
            "❌ Failed to add todo: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("Remove a todo item")
    suspend fun removeTodo(
        @LLMDescription("ID of todo to remove") todoId: String
    ): String {
        return try {
            val todo = repository.getTodoById(todoId)
            if (todo != null) {
                repository.deleteTodoById(todoId)
                "✅ Removed todo: ${todo.description}"
            } else {
                "❌ Todo with ID $todoId not found"
            }
        } catch (e: Exception) {
            "❌ Failed to remove todo: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("Edit todo description")
    suspend fun editDescription(
        @LLMDescription("Todo ID") todoId: String,
        @LLMDescription("New description") description: String
    ): String {
        return try {
            val todo = repository.getTodoById(todoId)
            if (todo != null) {
                val updatedTodo = todo.copy(description = description)
                repository.updateTodo(updatedTodo)
                "✅ Updated todo description to: $description"
            } else {
                "❌ Todo with ID $todoId not found"
            }
        } catch (e: Exception) {
            "❌ Failed to update todo: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("Mark todo as complete")
    suspend fun markComplete(
        @LLMDescription("Todo ID") todoId: String
    ): String {
        return try {
            val todo = repository.getTodoById(todoId)
            if (todo != null) {
                repository.updateTodoSection(todoId, TodoSection.DONE)
                "✅ Marked todo as complete: ${todo.description}"
            } else {
                "❌ Todo with ID $todoId not found"
            }
        } catch (e: Exception) {
            "❌ Failed to mark todo as complete: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("Mark todo as in progress")
    suspend fun markInProgress(
        @LLMDescription("Todo ID") todoId: String
    ): String {
        return try {
            val todo = repository.getTodoById(todoId)
            if (todo != null) {
                repository.updateTodoSection(todoId, TodoSection.IN_PROGRESS)
                "🔄 Marked todo as in progress: ${todo.description}"
            } else {
                "❌ Todo with ID $todoId not found"
            }
        } catch (e: Exception) {
            "❌ Failed to mark todo as in progress: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("Mark todo to do later")
    suspend fun markDoLater(
        @LLMDescription("Todo ID") todoId: String
    ): String {
        return try {
            val todo = repository.getTodoById(todoId)
            if (todo != null) {
                repository.updateTodoSection(todoId, TodoSection.DO_LATER)
                "⏰ Marked todo to do later: ${todo.description}"
            } else {
                "❌ Todo with ID $todoId not found"
            }
        } catch (e: Exception) {
            "❌ Failed to mark todo to do later: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("Create new section")
    suspend fun createSection(
        @LLMDescription("Section name") name: String
    ): String {
        return try {
            // Note: TodoSection is an enum, so we can't create new sections dynamically
            // Available sections are: TODO, IN_PROGRESS, DONE, DO_LATER
            val availableSections = TodoSection.values().joinToString(", ") { it.name.lowercase().replace("_", " ") }
            "ℹ️ Available sections are: $availableSections. Please use one of these sections when adding or moving todos."
        } catch (e: Exception) {
            "❌ Failed: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("Set reminder for todo")
    suspend fun setReminder(
        @LLMDescription("Todo ID") todoId: String,
        @LLMDescription("Reminder time in milliseconds since epoch") time: Long
    ): String {
        return try {
            val todo = repository.getTodoById(todoId)
            if (todo != null) {
                repository.updateTodoReminder(todoId, time)
                val reminderDate = java.util.Date(time)
                "⏰ Set reminder for todo '${todo.description}' at $reminderDate"
            } else {
                "❌ Todo with ID $todoId not found"
            }
        } catch (e: Exception) {
            "❌ Failed to set reminder: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("Read text out loud")
    suspend fun readOutLoud(
        @LLMDescription("Text to read") text: String
    ): String {
        return try {
            // Note: In a real implementation, this would use Android's TTS (Text-to-Speech)
            // For now, we'll just return the text as if it was read
            "🔊 Reading: $text"
        } catch (e: Exception) {
            "❌ Failed to read text: ${e.message}"
        }
    }

    @Tool
    @LLMDescription("List all todos")
    suspend fun listTodos(
        @LLMDescription("Section to filter by (todo, in_progress, done, do_later, or 'all' for all todos)") section: String = "all"
    ): String {
        return try {
            val todos = if (section.lowercase() == "all") {
                runBlocking { repository.getAllTodos().first() }
            } else {
                val todoSection = try {
                    TodoSection.valueOf(section.uppercase().replace(" ", "_"))
                } catch (e: IllegalArgumentException) {
                    return "❌ Invalid section. Available sections: todo, in_progress, done, do_later, or 'all'"
                }
                runBlocking { repository.getTodosBySection(todoSection).first() }
            }
            
            if (todos.isEmpty()) {
                return "📝 No todos found"
            }
            
            val todoList = todos.joinToString("\n") { todo ->
                val status = when (todo.section) {
                    TodoSection.TODO -> "📝"
                    TodoSection.IN_PROGRESS -> "🔄"
                    TodoSection.DONE -> "✅"
                    TodoSection.DO_LATER -> "⏰"
                }
                "$status [${todo.id.take(8)}] ${todo.description}"
            }
            
            "📋 Todos:\n$todoList"
        } catch (e: Exception) {
            "❌ Failed to list todos: ${e.message}"
        }
    }
}