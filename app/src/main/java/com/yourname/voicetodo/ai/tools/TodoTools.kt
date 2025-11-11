package com.yourname.voicetodo.ai.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.yourname.voicetodo.ai.events.ToolExecutionEvents
import com.yourname.voicetodo.ai.execution.RetryableToolExecutor
import com.yourname.voicetodo.ai.permission.ToolPermissionManager
import com.yourname.voicetodo.data.repository.TodoRepository
import com.yourname.voicetodo.domain.model.TodoStatus
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
            try {
                val result = block()
                ToolExecutionEvents.notifyCompletion(toolName, arguments, true, result.toString())
                return result
            } catch (e: Exception) {
                ToolExecutionEvents.notifyCompletion(toolName, arguments, false, null, e.message)
                throw e
            }
        }

        // Request permission
        val granted = ToolExecutionEvents.requestPermission(toolName, arguments)
        if (!granted) {
            ToolExecutionEvents.notifyCompletion(toolName, arguments, false, null, "Permission denied")
            throw SecurityException("Permission denied for tool: $toolName")
        }

        try {
            val result = block()
            ToolExecutionEvents.notifyCompletion(toolName, arguments, true, result.toString())
            return result
        } catch (e: Exception) {
            ToolExecutionEvents.notifyCompletion(toolName, arguments, false, null, e.message)
            throw e
        }
    }

    @Tool
    @LLMDescription("Add a new todo item")
    suspend fun addTodo(
        @LLMDescription("Title of the todo") title: String,
        @LLMDescription("Optional markdown description with subtasks") description: String? = null,
        @LLMDescription("Category ID (use listCategories to see available categories)") categoryId: String,
        @LLMDescription("Status: todo, in_progress, done, do_later") status: String = "todo"
    ): String = executeWithPermission(
        toolName = "addTodo",
        arguments = mapOf("title" to title, "description" to description, "categoryId" to categoryId, "status" to status)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "addTodo",
                arguments = mapOf("title" to title, "description" to description, "categoryId" to categoryId, "status" to status),
                executeFunction = {
                    try {
                        val todoStatus = try {
                            TodoStatus.valueOf(status.uppercase().replace(" ", "_"))
                        } catch (e: IllegalArgumentException) {
                            TodoStatus.TODO
                        }

                        val todo = repository.addTodo(
                            title = title,
                            description = description,
                            categoryId = categoryId,
                            status = todoStatus
                        )

                        """
                        Tool: addTodo
                        Status: SUCCESS
                        Result: Added todo "${todo.title}" to ${categoryId}
                        Todo ID: ${todo.id}
                        """.trimIndent()
                    } catch (e: Exception) {
                        """
                        Tool: addTodo
                        Status: FAILED
                        Error: ${e.message}
                        Suggestion: Verify categoryId exists using listCategories
                        """.trimIndent()
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
    @LLMDescription("Edit the title of an existing todo")
    suspend fun editTitle(
        @LLMDescription("Todo ID") todoId: String,
        @LLMDescription("New title") newTitle: String
    ): String = executeWithPermission(
        toolName = "editTitle",
        arguments = mapOf("todoId" to todoId, "newTitle" to newTitle)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "editTitle",
                arguments = mapOf("todoId" to todoId, "newTitle" to newTitle),
                executeFunction = {
                    try {
                        val todo = repository.getTodoById(todoId)
                        if (todo != null) {
                            val updatedTodo = todo.copy(title = newTitle)
                            repository.updateTodo(updatedTodo)
                            "🔧 Tool Call: editTitle\n✅ Updated todo title to: $newTitle"
                        } else {
                            "🔧 Tool Call: editTitle\n❌ Todo with ID $todoId not found"
                        }
                    } catch (e: Exception) {
                        "🔧 Tool Call: editTitle\n❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("Update todo content (description and subtasks)")
    suspend fun updateTodoContent(
        @LLMDescription("Todo ID") todoId: String,
        @LLMDescription("New markdown content with description and subtasks") content: String
    ): String = executeWithPermission(
        toolName = "updateTodoContent",
        arguments = mapOf("todoId" to todoId, "content" to content)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "updateTodoContent",
                arguments = mapOf("todoId" to todoId, "content" to content),
                executeFunction = {
                    try {
                        val todo = repository.getTodoById(todoId)
                        if (todo != null) {
                            val updatedTodo = todo.copy(description = content)
                            repository.updateTodo(updatedTodo)
                            "🔧 Tool Call: updateTodoContent\n✅ Updated todo content"
                        } else {
                            "🔧 Tool Call: updateTodoContent\n❌ Todo with ID $todoId not found"
                        }
                    } catch (e: Exception) {
                        "🔧 Tool Call: updateTodoContent\n❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("Move a todo to a different category")
    suspend fun moveTodoToCategory(
        @LLMDescription("Todo ID") todoId: String,
        @LLMDescription("Target category ID") targetCategoryId: String
    ): String = executeWithPermission(
        toolName = "moveTodoToCategory",
        arguments = mapOf("todoId" to todoId, "targetCategoryId" to targetCategoryId)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "moveTodoToCategory",
                arguments = mapOf("todoId" to todoId, "targetCategoryId" to targetCategoryId),
                executeFunction = {
                    try {
                        val todo = repository.getTodoById(todoId)
                        if (todo != null) {
                            repository.updateTodoCategory(todoId, targetCategoryId)
                            "✅ Moved todo '${todo.title}' to category $targetCategoryId"
                        } else {
                            "❌ Todo with ID $todoId not found"
                        }
                    } catch (e: Exception) {
                        "❌ Failed to move todo: ${e.message}"
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
                            repository.updateTodoStatus(todoId, TodoStatus.DONE)
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
                            repository.updateTodoStatus(todoId, TodoStatus.IN_PROGRESS)
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
                            repository.updateTodoStatus(todoId, TodoStatus.DO_LATER)
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
    @LLMDescription("List todos with optional filtering")
    suspend fun listTodos(
        @LLMDescription("Category ID to filter by, or 'all'") categoryId: String = "all",
        @LLMDescription("Status to filter by: todo, in_progress, done, do_later, or 'all'") status: String = "all"
    ): String = executeWithPermission(
        toolName = "listTodos",
        arguments = mapOf("categoryId" to categoryId, "status" to status)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "listTodos",
                arguments = mapOf("categoryId" to categoryId, "status" to status),
                executeFunction = {
                    try {
                        val todos = when {
                            categoryId == "all" && status == "all" -> {
                                runBlocking { repository.getAllTodos().first() }
                            }
                            categoryId == "all" -> {
                                val todoStatus = TodoStatus.valueOf(status.uppercase().replace(" ", "_"))
                                runBlocking { repository.getTodosByStatus(todoStatus).first() }
                            }
                            status == "all" -> {
                                runBlocking { repository.getTodosByCategory(categoryId).first() }
                            }
                            else -> {
                                val todoStatus = TodoStatus.valueOf(status.uppercase().replace(" ", "_"))
                                runBlocking { repository.getTodosByCategoryAndStatus(categoryId, todoStatus).first() }
                            }
                        }

                        if (todos.isEmpty()) {
                            return@ToolExecutionRequest "📝 No todos found"
                        }

                        val todoList = todos.joinToString("\n") { todo ->
                            val statusIcon = when (todo.status) {
                                TodoStatus.TODO -> "📝"
                                TodoStatus.IN_PROGRESS -> "🔄"
                                TodoStatus.DONE -> "✅"
                                TodoStatus.DO_LATER -> "⏰"
                            }
                            val desc = todo.description?.let { "\n   Description: \"${it.take(50)}...\"" } ?: ""
                            "$statusIcon ${todo.title} [${todo.id}] (Category: ${todo.categoryId})$desc"
                        }

                        "📋 Todos:\n$todoList"
                    } catch (e: Exception) {
                        "❌ Failed to list todos: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }
}