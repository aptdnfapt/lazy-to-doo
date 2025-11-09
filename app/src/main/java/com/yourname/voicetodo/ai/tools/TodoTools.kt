package com.yourname.voicetodo.ai.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.yourname.voicetodo.ai.events.ToolExecutionEvents
import com.yourname.voicetodo.ai.execution.RetryableToolExecutor
import com.yourname.voicetodo.ai.permission.ToolPermissionManager
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
    private val repository: TodoRepository,
    private val permissionManager: ToolPermissionManager,
    private val retryableToolExecutor: RetryableToolExecutor
) : ToolSet {

    // Helper function for permission-aware execution
    private suspend fun <T> executeWithPermission(
        toolName: String,
        arguments: Map<String, Any?>,
        block: suspend () -> T
    ): T {
        // Check if always allowed
        if (permissionManager.isToolAlwaysAllowed(toolName)) {
            return block()
        }

        // Request permission
        val granted = ToolExecutionEvents.requestPermission(toolName, arguments)
        if (!granted) {
            throw SecurityException("Permission denied for tool: $toolName")
        }

        return block()
    }

    @Tool
    @LLMDescription("Add a new todo item")
    suspend fun addTodo(
        @LLMDescription("Title of the todo") title: String,
        @LLMDescription("Optional description") description: String? = null,
        @LLMDescription("Section to place the todo in (todo, in_progress, done, do_later)") section: String = "todo"
    ): String = executeWithPermission(
        toolName = "addTodo",
        arguments = mapOf("title" to title, "description" to description, "section" to section)
    ) {
        // Original implementation with retry
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "addTodo",
                arguments = mapOf("title" to title, "description" to description, "section" to section),
                executeFunction = {
                    try {
                        val todoSection = try {
                            TodoSection.valueOf(section.uppercase().replace(" ", "_"))
                        } catch (e: IllegalArgumentException) {
                            TodoSection.TODO
                        }
                        val todo = repository.addTodo(
                            title = title,
                            description = description,
                            section = todoSection
                        )
                        "🔧 Tool Call: addTodo\n✅ Added todo: ${todo.title} in ${todoSection.name.replace("_", " ").lowercase()}"
                    } catch (e: Exception) {
                        "🔧 Tool Call: addTodo\n❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true } // Already checked above
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("Remove a todo item")
    suspend fun removeTodo(
        @LLMDescription("ID of todo to remove") todoId: String
    ): String = executeWithPermission(
        toolName = "removeTodo",
        arguments = mapOf("todoId" to todoId)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "removeTodo",
                arguments = mapOf("todoId" to todoId),
                executeFunction = {
                    try {
                        val todo = repository.getTodoById(todoId)
                        if (todo != null) {
                            repository.deleteTodoById(todoId)
                            "🔧 Tool Call: removeTodo\n✅ Removed todo: ${todo.title}"
                        } else {
                            "🔧 Tool Call: removeTodo\n❌ Todo with ID $todoId not found"
                        }
                    } catch (e: Exception) {
                        "🔧 Tool Call: removeTodo\n❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("Edit todo description")
    suspend fun editDescription(
        @LLMDescription("Todo ID") todoId: String,
        @LLMDescription("New description") description: String
    ): String = executeWithPermission(
        toolName = "editDescription",
        arguments = mapOf("todoId" to todoId, "description" to description)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "editDescription",
                arguments = mapOf("todoId" to todoId, "description" to description),
                executeFunction = {
                    try {
                        val todo = repository.getTodoById(todoId)
                        if (todo != null) {
                            val updatedTodo = todo.copy(description = description)
                            repository.updateTodo(updatedTodo)
                            "🔧 Tool Call: editDescription\n✅ Updated todo description to: $description"
                        } else {
                            "🔧 Tool Call: editDescription\n❌ Todo with ID $todoId not found"
                        }
                    } catch (e: Exception) {
                        "🔧 Tool Call: editDescription\n❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("Mark todo as complete")
    suspend fun markComplete(
        @LLMDescription("Todo ID") todoId: String
    ): String = executeWithPermission(
        toolName = "markComplete",
        arguments = mapOf("todoId" to todoId)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "markComplete",
                arguments = mapOf("todoId" to todoId),
                executeFunction = {
                    try {
                        val todo = repository.getTodoById(todoId)
                        if (todo != null) {
                            repository.updateTodoSection(todoId, TodoSection.DONE)
                            "🔧 Tool Call: markComplete\n✅ Marked todo as complete: ${todo.title}"
                        } else {
                            "🔧 Tool Call: markComplete\n❌ Todo with ID $todoId not found"
                        }
                    } catch (e: Exception) {
                        "🔧 Tool Call: markComplete\n❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("Mark todo as in progress")
    suspend fun markInProgress(
        @LLMDescription("Todo ID") todoId: String
    ): String = executeWithPermission(
        toolName = "markInProgress",
        arguments = mapOf("todoId" to todoId)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "markInProgress",
                arguments = mapOf("todoId" to todoId),
                executeFunction = {
                    try {
                        val todo = repository.getTodoById(todoId)
                        if (todo != null) {
                            repository.updateTodoSection(todoId, TodoSection.IN_PROGRESS)
                            "🔧 Tool Call: markInProgress\n🔄 Marked todo as in progress: ${todo.title}"
                        } else {
                            "🔧 Tool Call: markInProgress\n❌ Todo with ID $todoId not found"
                        }
                    } catch (e: Exception) {
                        "🔧 Tool Call: markInProgress\n❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("Mark todo to do later")
    suspend fun markDoLater(
        @LLMDescription("Todo ID") todoId: String
    ): String = executeWithPermission(
        toolName = "markDoLater",
        arguments = mapOf("todoId" to todoId)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "markDoLater",
                arguments = mapOf("todoId" to todoId),
                executeFunction = {
                    try {
                        val todo = repository.getTodoById(todoId)
                        if (todo != null) {
                            repository.updateTodoSection(todoId, TodoSection.DO_LATER)
                            "🔧 Tool Call: markDoLater\n⏰ Marked todo to do later: ${todo.title}"
                        } else {
                            "🔧 Tool Call: markDoLater\n❌ Todo with ID $todoId not found"
                        }
                    } catch (e: Exception) {
                        "🔧 Tool Call: markDoLater\n❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("Create new section")
    suspend fun createSection(
        @LLMDescription("Section name") name: String
    ): String = executeWithPermission(
        toolName = "createSection",
        arguments = mapOf("name" to name)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "createSection",
                arguments = mapOf("name" to name),
                executeFunction = {
                    try {
                        // Note: TodoSection is an enum, so we can't create new sections dynamically
                        // Available sections are: TODO, IN_PROGRESS, DONE, DO_LATER
                        val availableSections = TodoSection.values().joinToString(", ") { it.name.lowercase().replace("_", " ") }
                        "🔧 Tool Call: createSection\nℹ️ Available sections are: $availableSections. Please use one of these sections when adding or moving todos."
                    } catch (e: Exception) {
                        "🔧 Tool Call: createSection\n❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("Set reminder for todo")
    suspend fun setReminder(
        @LLMDescription("Todo ID") todoId: String,
        @LLMDescription("Reminder time in milliseconds since epoch") time: Long
    ): String = executeWithPermission(
        toolName = "setReminder",
        arguments = mapOf("todoId" to todoId, "time" to time)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "setReminder",
                arguments = mapOf("todoId" to todoId, "time" to time),
                executeFunction = {
                    try {
                        val todo = repository.getTodoById(todoId)
                        if (todo != null) {
                            repository.updateTodoReminder(todoId, time)
                            val reminderDate = java.util.Date(time)
                            "🔧 Tool Call: setReminder\n⏰ Set reminder for todo '${todo.title}' at $reminderDate"
                        } else {
                            "🔧 Tool Call: setReminder\n❌ Todo with ID $todoId not found"
                        }
                    } catch (e: Exception) {
                        "🔧 Tool Call: setReminder\n❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("Read text out loud")
    suspend fun readOutLoud(
        @LLMDescription("Text to read") text: String
    ): String = executeWithPermission(
        toolName = "readOutLoud",
        arguments = mapOf("text" to text)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "readOutLoud",
                arguments = mapOf("text" to text),
                executeFunction = {
                    try {
                        // Note: In a real implementation, this would use Android's TTS (Text-to-Speech)
                        // For now, we'll just return the text as if it was read
                        "🔧 Tool Call: readOutLoud\n🔊 Reading: $text"
                    } catch (e: Exception) {
                        "🔧 Tool Call: readOutLoud\n❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("List all todos")
    suspend fun listTodos(
        @LLMDescription("Section to filter by (todo, in_progress, done, do_later, or 'all' for all todos)") section: String = "all"
    ): String = executeWithPermission(
        toolName = "listTodos",
        arguments = mapOf("section" to section)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "listTodos",
                arguments = mapOf("section" to section),
                executeFunction = {
                    try {
                        val todos = if (section.lowercase() == "all") {
                            runBlocking { repository.getAllTodos().first() }
                        } else {
                            val todoSection = try {
                                TodoSection.valueOf(section.uppercase().replace(" ", "_"))
                            } catch (e: IllegalArgumentException) {
                                return@ToolExecutionRequest "❌ Invalid section. Available sections: todo, in_progress, done, do_later, or 'all'"
                            }
                            runBlocking { repository.getTodosBySection(todoSection).first() }
                        }

                        if (todos.isEmpty()) {
                            return@ToolExecutionRequest "🔧 Tool Call: listTodos\n📝 No todos found"
                        }

                        val todoList = todos.joinToString("\n") { todo ->
                            val status = when (todo.section) {
                                TodoSection.TODO -> "📝"
                                TodoSection.IN_PROGRESS -> "🔄"
                                TodoSection.DONE -> "✅"
                                TodoSection.DO_LATER -> "⏰"
                            }
                            val desc = todo.description?.let { "\n   Description: \"$it\"" } ?: ""
                            "$status ${todo.title} [${todo.id}]$desc"
                        }

                        "🔧 Tool Call: listTodos\n📋 Todos:\n$todoList"
                    } catch (e: Exception) {
                        "🔧 Tool Call: listTodos\n❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }
}