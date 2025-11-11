package com.yourname.voicetodo.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.voicetodo.ai.agent.TodoAgent
import com.yourname.voicetodo.ai.events.ToolExecutionEvents
import com.yourname.voicetodo.ai.events.ToolExecutionEvents.ToolCallRequest
import com.yourname.voicetodo.ai.permission.ToolPermissionManager
import com.yourname.voicetodo.ai.transcription.RecorderManager
import com.yourname.voicetodo.ai.transcription.WhisperTranscriber
import com.yourname.voicetodo.data.preferences.UserPreferences
import com.yourname.voicetodo.data.repository.CategoryRepository
import com.yourname.voicetodo.domain.model.Category
import com.yourname.voicetodo.domain.model.Message
import com.yourname.voicetodo.domain.model.MessageType
import com.yourname.voicetodo.domain.model.ToolCallStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agent: TodoAgent,
    private val transcriber: WhisperTranscriber,
    private val recorder: RecorderManager,
    private val userPreferences: UserPreferences,
    private val chatRepository: com.yourname.voicetodo.data.repository.ChatRepository,
    private val categoryRepository: CategoryRepository,
    private val permissionManager: ToolPermissionManager
) : ViewModel() {

    private var currentSessionId: String = ""

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    // NEW: Category selection state
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)  // null = "All"
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    // Store pending permission request
    private var pendingPermissionRequest: ToolCallRequest? = null

    // Get LLM config from preferences
    private val llmConfig = combine(
        userPreferences.getLlmBaseUrl(),
        userPreferences.getLlmApiKey(),
        userPreferences.getLlmModelName()
    ) { baseUrl, apiKey, modelName ->
        Triple(baseUrl, apiKey, modelName)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Triple("", "", ""))

    val currentModel = combine(
        userPreferences.getLlmBaseUrl(),
        userPreferences.getLlmModelName()
    ) { baseUrl, modelName ->
        val base = if (baseUrl.isNotEmpty()) baseUrl else "OpenAI"
        val model = if (modelName.isNotEmpty()) modelName else "gpt-4o"
        "$model (${base.replace("https://", "").replace("/v1", "")})"
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "gpt-4o (OpenAI)")

    private var currentAudioFile: File? = null

    init {
        // Set up amplitude monitoring
        recorder.setOnUpdateMicrophoneAmplitude { amplitude ->
            _amplitude.value = amplitude
        }

        // Listen for tool permission requests
        viewModelScope.launch {
            ToolExecutionEvents.pendingRequests.collect { request ->
                pendingPermissionRequest = request
                addToolCallMessage(request.toolName, request.arguments, ToolCallStatus.PENDING_APPROVAL)
            }
        }

        // Load categories
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { _categories.value = it }
        }
    }

    fun initializeSession(sessionId: String) {
        if (currentSessionId != sessionId) {
            currentSessionId = sessionId
            loadMessagesForSession(sessionId)
        }
    }

    private fun loadMessagesForSession(sessionId: String) {
        viewModelScope.launch {
            try {
                // First check if session exists, create if not
                val session = chatRepository.getChatSessionById(sessionId)
                if (session == null) {
                    // Create new session with the provided ID if it doesn't exist
                    chatRepository.createChatSessionWithId(sessionId, "New Chat")
                }

                // Load messages for the session
                chatRepository.getMessagesForSession(sessionId).collect { messages ->
                    _messages.value = messages
                    if (messages.isEmpty()) {
                        // Add welcome message for new sessions
                        addMessage(
                            content = "Hi! I'm your voice-controlled todo assistant. Please configure your LLM provider in Settings first, then tap the microphone button and speak to add, edit, or manage your todos.",
                            isFromUser = false
                        )
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load chat session: ${e.message}"
            }
        }
    }

    fun startRecording() {
        if (_isRecording.value) return
        
        viewModelScope.launch {
            try {
                currentAudioFile = recorder.startRecording()
                _isRecording.value = true
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to start recording: ${e.message}"
            }
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        
        viewModelScope.launch {
            try {
                _isRecording.value = false
                val audioFile = recorder.stopRecording()
                
                if (audioFile != null && audioFile.exists()) {
                    currentAudioFile = audioFile
                    transcribeAudio(audioFile)
                } else {
                    _errorMessage.value = "No audio recorded"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to stop recording: ${e.message}"
            }
        }
    }

    private suspend fun transcribeAudio(audioFile: File) {
        _isTranscribing.value = true
        _errorMessage.value = null
        
        try {
            val geminiApiKey = userPreferences.getGeminiApiKey().first()
            
            if (geminiApiKey.isEmpty()) {
                _errorMessage.value = "Please set your Gemini API key in Settings for voice transcription"
                _isTranscribing.value = false
                return
            }

            val transcribedText = transcriber.transcribe(audioFile, geminiApiKey)
            
            if (transcribedText.isNotBlank()) {
                addMessage(transcribedText, isFromUser = true)
                processWithAgent(transcribedText)
            } else {
                _errorMessage.value = "No speech detected. Please try again."
            }
        } catch (e: Exception) {
            _errorMessage.value = "Transcription failed: ${e.message}"
        } finally {
            _isTranscribing.value = false
        }
    }

    private suspend fun processWithAgent(userMessage: String) {
        _isProcessing.value = true
        
        try {
            val (baseUrl, apiKey, modelName) = llmConfig.value
            
            if (apiKey.isEmpty()) {
                val errorMsg = "Please set your LLM API key in Settings"
                addMessage(errorMsg, isFromUser = false)
                _errorMessage.value = errorMsg
                _isProcessing.value = false
                return
            }

            if (baseUrl.isEmpty()) {
                val errorMsg = "Please set your LLM Base URL in Settings"
                addMessage(errorMsg, isFromUser = false)
                _errorMessage.value = errorMsg
                _isProcessing.value = false
                return
            }

            if (modelName.isEmpty()) {
                val errorMsg = "Please set your LLM Model Name in Settings"
                addMessage(errorMsg, isFromUser = false)
                _errorMessage.value = errorMsg
                _isProcessing.value = false
                return
            }
            
            // Use the agent's createAgent which reads from preferences
            // Pass current chat history for context (all messages except the current one being processed)
            val chatHistory = _messages.value
            val agentResponse = agent.runAgent(userMessage, chatHistory)
            addMessage(agentResponse, isFromUser = false)
        } catch (e: Exception) {
            val errorMsg = "Sorry, I encountered an error: ${e.message}"
            addMessage(errorMsg, isFromUser = false)
            _errorMessage.value = errorMsg
        } finally {
            _isProcessing.value = false
        }
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            addMessage(text, isFromUser = true)
            processWithAgent(text)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private suspend fun addMessage(content: String, isFromUser: Boolean) {
        if (currentSessionId.isEmpty()) {
            _errorMessage.value = "No active chat session"
            return
        }

        try {
            val message = chatRepository.addMessage(currentSessionId, content, isFromUser)
            // The messages will be updated via the Flow from the repository
        } catch (e: Exception) {
            _errorMessage.value = "Failed to save message: ${e.message}"
        }
    }

    // Add tool call message to chat
    private suspend fun addToolCallMessage(
        toolName: String,
        arguments: Map<String, Any?>,
        status: ToolCallStatus
    ) {
        if (currentSessionId.isEmpty()) return

        try {
            // Convert Map<String, Any?> to Map<String, String> for serialization
            val stringArguments = arguments.mapValues { (_, value) -> 
                value?.toString() ?: "null" 
            }
            
            val toolCallMessage = Message(
                id = UUID.randomUUID().toString(),
                sessionId = currentSessionId,
                content = "", // Not used for tool calls
                isFromUser = false,
                messageType = MessageType.TOOL_CALL,
                toolName = toolName,
                toolArguments = Json.encodeToString(stringArguments),
                toolStatus = status.name
            )

            chatRepository.addMessage(toolCallMessage)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to add tool call message: ${e.message}"
        }
    }

    // Update tool call message status
    private suspend fun updateToolCallMessageStatus(messageId: String, status: ToolCallStatus, result: String? = null) {
        try {
            chatRepository.updateToolCallMessageStatus(messageId, status.name, result)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to update tool call status: ${e.message}"
        }
    }

    // Handle inline tool call approvals
    fun onToolCallApproveOnce(messageId: String) {
        viewModelScope.launch {
            updateToolCallMessageStatus(messageId, ToolCallStatus.EXECUTING)
            // Respond to the pending request
            pendingPermissionRequest?.onResponse?.invoke(true)
            pendingPermissionRequest = null
        }
    }

    fun onToolCallApproveAlways(messageId: String, toolName: String) {
        viewModelScope.launch {
            permissionManager.setToolAlwaysAllowed(toolName, true)
            updateToolCallMessageStatus(messageId, ToolCallStatus.EXECUTING)
            // Respond to the pending request
            pendingPermissionRequest?.onResponse?.invoke(true)
            pendingPermissionRequest = null
        }
    }

    fun setSelectedCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun onToolCallDeny(messageId: String) {
        viewModelScope.launch {
            updateToolCallMessageStatus(messageId, ToolCallStatus.DENIED)
            // IMPORTANT: Stop agent execution completely
            pendingPermissionRequest?.onResponse?.invoke(false)
            pendingPermissionRequest = null

            // Clear any ongoing processing state
            _isProcessing.value = false

            // Add system message explaining denial
            addMessage(
                content = "⛔ Tool execution denied. The agent has stopped processing. Please provide more details or rephrase your request to continue.",
                isFromUser = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any ongoing recording
        if (_isRecording.value) {
            recorder.stopRecording()
        }
        transcriber.cancelTranscription()
    }
}