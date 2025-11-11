package com.yourname.voicetodo.ai.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.yourname.voicetodo.ai.events.ToolExecutionEvents
import com.yourname.voicetodo.ai.execution.RetryableToolExecutor
import com.yourname.voicetodo.ai.permission.ToolPermissionManager
import com.yourname.voicetodo.data.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@LLMDescription("Manage todo categories (Work, Life, Study, etc.)")
@Singleton
class CategoryTools @Inject constructor(
    private val categoryRepository: CategoryRepository,
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
    @LLMDescription("Create a new category")
    suspend fun createCategory(
        @LLMDescription("Category name (e.g., 'Work', 'Personal')") name: String,
        @LLMDescription("Display name") displayName: String = name,
        @LLMDescription("Hex color code") color: String = "#137fec",
        @LLMDescription("Optional icon emoji") icon: String? = null
    ): String = executeWithPermission(
        toolName = "createCategory",
        arguments = mapOf("name" to name, "displayName" to displayName, "color" to color, "icon" to icon)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "createCategory",
                arguments = mapOf("name" to name, "displayName" to displayName, "color" to color, "icon" to icon),
                executeFunction = {
                    try {
                        val category = categoryRepository.createCategory(name, displayName, color, icon)
                        "✅ Created category: ${category.displayName}"
                    } catch (e: Exception) {
                        "❌ Failed to create category: ${e.message}"
                    }
                }
            ),
            checkPermission = { true } // Already checked above
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("List all categories")
    suspend fun listCategories(): String = executeWithPermission(
        toolName = "listCategories",
        arguments = emptyMap()
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "listCategories",
                arguments = emptyMap(),
                executeFunction = {
                    try {
                        val categories = runBlocking { categoryRepository.getAllCategories().first() }
                        if (categories.isEmpty()) {
                            "📁 No categories found"
                        } else {
                            val list = categories.joinToString("\n") { category ->
                                "${category.icon ?: "📁"} ${category.displayName} [${category.id}] - ${category.todoCount} todos"
                            }
                            "📋 Categories:\n$list"
                        }
                    } catch (e: Exception) {
                        "❌ Failed to list categories: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }

    @Tool
    @LLMDescription("Delete a category (cannot delete default categories)")
    suspend fun deleteCategory(
        @LLMDescription("Category ID") categoryId: String
    ): String = executeWithPermission(
        toolName = "deleteCategory",
        arguments = mapOf("categoryId" to categoryId)
    ) {
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "deleteCategory",
                arguments = mapOf("categoryId" to categoryId),
                executeFunction = {
                    try {
                        categoryRepository.deleteCategory(categoryId)
                        "✅ Deleted category"
                    } catch (e: IllegalArgumentException) {
                        "❌ Cannot delete default category"
                    } catch (e: Exception) {
                        "❌ Failed to delete category: ${e.message}"
                    }
                }
            ),
            checkPermission = { true }
        ).result ?: throw Exception("Tool execution failed")
    }
}