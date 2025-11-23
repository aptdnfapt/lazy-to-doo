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
            // For auto-approved tools, we still go through the permission request flow
            // but automatically approve it, so the ChatViewModel can create the tool call message
            val granted = ToolExecutionEvents.requestPermission(toolName, arguments)
            // The ChatViewModel will automatically approve this request and create the tool call message
            
            if (!granted) {
                // This shouldn't happen for auto-approved tools, but handle it just in case
                ToolExecutionEvents.notifyCompletion(toolName, arguments, false, null, "Permission denied")
                throw SecurityException("Permission denied for auto-approved tool: $toolName")
            }
            
            try {
                val result = block()
                // Small delay to ensure UI shows the executing state for auto-approved tools
                kotlinx.coroutines.delay(500)
                ToolExecutionEvents.notifyCompletion(toolName, arguments, true, result.toString())
                return result
            } catch (e: Exception) {
                ToolExecutionEvents.notifyCompletion(toolName, arguments, false, null, e.message)
                throw e
            }
        }

        // Request permission for non-auto-approved tools
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
    @LLMDescription("Add a new todo item with title and optional description. Use description exactly as provided by user without asking for clarification. Only create new todos when no existing related todos are found.")
    suspend fun addTodo(
        @LLMDescription("Title of the todo") title: String,
        @LLMDescription("Optional markdown description with subtasks - use exactly as provided by user") description: String? = null,
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
                        ✅ **Todo Added Successfully!**
                        
                        📝 **Title:** ${todo.title}
                        🏷️ **Category:** ${categoryId}
                        📊 **Status:** ${todo.status.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}
                        🔑 **Todo ID:** ${todo.id}
                        
                        ${if (description != null) "📝 **Description:** $description\n" else ""}
                        
                        💡 *You can refer to this todo by its ID "${todo.id}" for future updates or ask me to show all todos in the ${categoryId} category.*
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
    @LLMDescription("Add a subtask to an existing todo using clean markdown checkbox format")
    suspend fun addSubtask(
        @LLMDescription("Todo ID to add subtask to") todoId: String,
        @LLMDescription("Subtask description") subtask: String
    ): String = executeWithPermission(
        toolName = "addSubtask",
        arguments = mapOf("todoId" to todoId, "subtask" to subtask)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "addSubtask",
                arguments = mapOf("todoId" to todoId, "subtask" to subtask),
                executeFunction = {
                    try {
                        val todo = repository.getTodoById(todoId)
                        if (todo != null) {
                            val currentDescription = todo.description ?: ""
                            val newDescription = if (currentDescription.isBlank()) {
                                "[] $subtask"
                            } else {
                                "$currentDescription\n[] $subtask"
                            }
                            val updatedTodo = todo.copy(description = newDescription)
                            repository.updateTodo(updatedTodo)
                            "🔧 Tool Call: addSubtask\n✅ Added subtask to '${todo.title}': $subtask"
                        } else {
                            "🔧 Tool Call: addSubtask\n❌ Todo with ID $todoId not found"
                        }
                    } catch (e: Exception) {
                        "🔧 Tool Call: addSubtask\n❌ Failed: ${e.message}"
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
                            """
                            ✅ **Todo Completed Successfully!**
                            
                            📝 **Completed:** ${todo.title}
                            🏷️ **Category:** ${todo.categoryId}
                            🕐 **Completed At:** ${java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm").format(java.util.Date())}
                            
                            🎉 Great job! You've completed "${todo.title}". This todo has been moved to the Done section.
                            
                            💡 *You can ask me to show your completed todos or add new tasks to keep the momentum going!*
                            """.trimIndent()
                        } else {
                            """
                            ❌ **Todo Not Found**
                            
                            🔑 **Todo ID:** $todoId
                            
                            I couldn't find a todo with that ID. Please check the ID and try again, or ask me to list your todos to see available options.
                            """.trimIndent()
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
                            """
                            🔄 **Todo Status Updated!**
                            
                            📝 **Task:** ${todo.title}
                            🏷️ **Category:** ${todo.categoryId}
                            📊 **New Status:** In Progress
                            
                            💪 You're making progress on "${todo.title}"! It's now marked as In Progress.
                            
                            💡 *You can ask me to add subtasks, update the description, or mark it as complete when you're done!*
                            """.trimIndent()
                        } else {
                            """
                            ❌ **Todo Not Found**
                            
                            🔑 **Todo ID:** $todoId
                            
                            I couldn't find a todo with that ID. Please check the ID and try again, or ask me to list your todos to see available options.
                            """.trimIndent()
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
    @LLMDescription("Find todos that might be related to a topic or keywords")
    suspend fun findRelatedTodos(
        @LLMDescription("Keywords to search for in todo titles and descriptions") keywords: String
    ): String = executeWithPermission(
        toolName = "findRelatedTodos",
        arguments = mapOf("keywords" to keywords)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "findRelatedTodos",
                arguments = mapOf("keywords" to keywords),
                executeFunction = {
                    try {
                        val allTodos = runBlocking { repository.getAllTodos().first() }
                        val searchTerms = keywords.lowercase().split("\\s+".toRegex())

                        val relatedTodos = allTodos.filter { todo ->
                            val searchableText = "${todo.title} ${todo.description ?: ""}".lowercase()
                            searchTerms.any { term -> searchableText.contains(term) }
                        }

                        if (relatedTodos.isEmpty()) {
                            return@ToolExecutionRequest "📝 No todos found related to: $keywords"
                        }

                        val todoList = relatedTodos.joinToString("\n\n") { todo ->
                            val statusIcon = when (todo.status) {
                                TodoStatus.TODO -> "📝"
                                TodoStatus.IN_PROGRESS -> "🔄"
                                TodoStatus.DONE -> "✅"
                                TodoStatus.DO_LATER -> "⏰"
                            }
                            
                            val statusText = when (todo.status) {
                                TodoStatus.TODO -> "To Do"
                                TodoStatus.IN_PROGRESS -> "In Progress"
                                TodoStatus.DONE -> "Done"
                                TodoStatus.DO_LATER -> "Do Later"
                            }
                            
                            val lines = mutableListOf<String>()
                            lines.add("$statusIcon **${todo.title}** [${todo.id}]")
                            lines.add("   Status: $statusText")
                            lines.add("   Category: ${todo.categoryId}")
                            
                            // Add description if available
                            if (!todo.description.isNullOrBlank()) {
                                val cleanDescription = todo.description.replace("\n", " ").trim()
                                lines.add("   Description: \"$cleanDescription\"")
                            }
                            
                            // Add subtasks if any
                            if (todo.subtasks.isNotEmpty()) {
                                lines.add("   Subtasks (${todo.subtasks.count { it.completed }}/ ${todo.subtasks.size}):")
                                todo.subtasks.forEach { subtask ->
                                    val checkmark = if (subtask.completed) "✅" else "☐"
                                    lines.add("     $checkmark ${subtask.text}")
                                }
                            }
                            
                            lines.joinToString("\n")
                        }

                        """
                        🔍 **Related Todos for "$keywords"**: ${relatedTodos.size}
                        
                        $todoList
                        
                        💡 *Found ${relatedTodos.size} todos matching your search. Ask me to update any of these todos or filter by status.*
                        """.trimIndent()
                    } catch (e: Exception) {
                        "❌ Failed to search todos: ${e.message}"
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
        @LLMDescription("Status to filter by: todo, in_progress, done, do_later, active, or 'all'") status: String = "all"
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
                            categoryId == "all" && status == "active" -> {
                                // Active status: TODO + IN_PROGRESS (excluding DONE)
                                val todoStatuses = listOf(TodoStatus.TODO, TodoStatus.IN_PROGRESS)
                                val allTodos = runBlocking { repository.getAllTodos().first() }
                                allTodos.filter { todo -> todo.status in todoStatuses }
                            }
                            categoryId == "all" -> {
                                val todoStatus = TodoStatus.valueOf(status.uppercase().replace(" ", "_"))
                                runBlocking { repository.getTodosByStatus(todoStatus).first() }
                            }
                            status == "all" -> {
                                runBlocking { repository.getTodosByCategory(categoryId).first() }
                            }
                            status == "active" -> {
                                // Active todos in specific category: TODO + IN_PROGRESS
                                val todoStatuses = listOf(TodoStatus.TODO, TodoStatus.IN_PROGRESS)
                                val categoryTodos = runBlocking { repository.getTodosByCategory(categoryId).first() }
                                categoryTodos.filter { todo -> todo.status in todoStatuses }
                            }
                            else -> {
                                val todoStatus = TodoStatus.valueOf(status.uppercase().replace(" ", "_"))
                                runBlocking { repository.getTodosByCategoryAndStatus(categoryId, todoStatus).first() }
                            }
                        }

                        if (todos.isEmpty()) {
                            return@ToolExecutionRequest "📝 No todos found"
                        }

                        val todoList = todos.joinToString("\n\n") { todo ->
                            val statusIcon = when (todo.status) {
                                TodoStatus.TODO -> "📝"
                                TodoStatus.IN_PROGRESS -> "🔄"
                                TodoStatus.DONE -> "✅"
                                TodoStatus.DO_LATER -> "⏰"
                            }
                            
                            val statusText = when (todo.status) {
                                TodoStatus.TODO -> "To Do"
                                TodoStatus.IN_PROGRESS -> "In Progress"
                                TodoStatus.DONE -> "Done"
                                TodoStatus.DO_LATER -> "Do Later"
                            }
                            
                            val lines = mutableListOf<String>()
                            lines.add("$statusIcon **${todo.title}** [${todo.id}]")
                            lines.add("   Status: $statusText")
                            lines.add("   Category: ${todo.categoryId}")
                            
                            // Add description if available
                            if (!todo.description.isNullOrBlank()) {
                                val cleanDescription = todo.description.replace("\n", " ").trim()
                                lines.add("   Description: \"$cleanDescription\"")
                            }
                            
                            // Add subtasks if any
                            if (todo.subtasks.isNotEmpty()) {
                                lines.add("   Subtasks (${todo.subtasks.count { it.completed }}/ ${todo.subtasks.size}):")
                                todo.subtasks.forEach { subtask ->
                                    val checkmark = if (subtask.completed) "✅" else "☐"
                                    lines.add("     $checkmark ${subtask.text}")
                                }
                            }
                            
                            // Add reminder if set
                            if (todo.reminderTime != null) {
                                val reminderDate = java.util.Date(todo.reminderTime)
                                lines.add("   Reminder: ${java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm").format(reminderDate)}")
                            }
                            
                            lines.joinToString("\n")
                        }

                        """
                        📋 **Todos Found**: ${todos.size}
                        
                        $todoList
                        
                        💡 *Use specific todo IDs for updates, or ask me to filter by status/category*
                        """.trimIndent()
                    } catch (e: Exception) {
                        "❌ Failed to list todos: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }
}