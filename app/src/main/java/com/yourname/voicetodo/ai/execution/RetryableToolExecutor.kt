package com.yourname.voicetodo.ai.execution

import com.yourname.voicetodo.ai.permission.ToolPermissionManager
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetryableToolExecutor @Inject constructor(
    private val permissionManager: ToolPermissionManager
) {

    data class ToolExecutionRequest(
        val toolName: String,
        val arguments: Map<String, Any?>,
        val executeFunction: suspend () -> String
    )

    data class ToolExecutionResult(
        val success: Boolean,
        val result: String? = null,
        val error: String? = null,
        val retryCount: Int = 0,
        val permissionDenied: Boolean = false
    )

    suspend fun executeWithRetry(
        request: ToolExecutionRequest,
        maxRetries: Int = 3,
        checkPermission: suspend (ToolExecutionRequest) -> Boolean
    ): ToolExecutionResult {

        // Check permission first
        val hasPermission = checkPermission(request)
        if (!hasPermission) {
            return ToolExecutionResult(
                success = false,
                error = "Permission denied by user",
                permissionDenied = true
            )
        }

        // Execute with retry
        var lastError: String? = null
        var retryCount = 0

        for (attempt in 0 until maxRetries) {
            try {
                val result = request.executeFunction()
                return ToolExecutionResult(
                    success = true,
                    result = result,
                    retryCount = attempt
                )
            } catch (e: Exception) {
                retryCount = attempt + 1
                lastError = e.message ?: "Unknown error"

                // Check if it's a retryable error (500, network issues)
                if (isRetryableError(e) && attempt < maxRetries - 1) {
                    // Exponential backoff: 1s, 2s, 4s
                    val delayMs = (1000L * (1 shl attempt))
                    delay(delayMs)
                    // Log retry attempt
                    android.util.Log.w(
                        "RetryableToolExecutor",
                        "Retry attempt ${attempt + 1}/$maxRetries for ${request.toolName}: $lastError"
                    )
                } else {
                    // Not retryable or max retries reached
                    break
                }
            }
        }

        return ToolExecutionResult(
            success = false,
            error = lastError,
            retryCount = retryCount
        )
    }

    private fun isRetryableError(error: Exception): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("500") ||
                message.contains("502") ||
                message.contains("503") ||
                message.contains("504") ||
                message.contains("timeout") ||
                message.contains("connection")
    }
}