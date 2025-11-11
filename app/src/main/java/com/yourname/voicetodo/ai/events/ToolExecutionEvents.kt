package com.yourname.voicetodo.ai.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object ToolExecutionEvents {

    data class ToolCallRequest(
        val toolName: String,
        val arguments: Map<String, Any?>,
        val onResponse: (Boolean) -> Unit
    )

    data class ToolCallCompletion(
        val toolName: String,
        val arguments: Map<String, Any?>,
        val success: Boolean,
        val result: String? = null,
        val error: String? = null
    )

    private val _pendingRequests = MutableSharedFlow<ToolCallRequest>()
    val pendingRequests: SharedFlow<ToolCallRequest> = _pendingRequests

    private val _completions = MutableSharedFlow<ToolCallCompletion>()
    val completions: SharedFlow<ToolCallCompletion> = _completions

    suspend fun requestPermission(toolName: String, arguments: Map<String, Any?>): Boolean {
        return suspendCancellableCoroutine { continuation ->
            CoroutineScope(continuation.context).launch {
                _pendingRequests.emit(
                    ToolCallRequest(
                        toolName = toolName,
                        arguments = arguments,
                        onResponse = { granted ->
                            continuation.resume(granted)
                        }
                    )
                )
            }
        }
    }

    suspend fun notifyCompletion(toolName: String, arguments: Map<String, Any?>, success: Boolean, result: String? = null, error: String? = null) {
        _completions.emit(
            ToolCallCompletion(
                toolName = toolName,
                arguments = arguments,
                success = success,
                result = result,
                error = error
            )
        )
    }
}