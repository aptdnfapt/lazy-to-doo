package com.yourname.voicetodo.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.voicetodo.ai.agent.TodoAgent
import com.yourname.voicetodo.ai.transcription.RecorderManager
import com.yourname.voicetodo.ai.transcription.WhisperTranscriber
import com.yourname.voicetodo.data.preferences.UserPreferences
import com.yourname.voicetodo.domain.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agent: TodoAgent,
    private val transcriber: WhisperTranscriber,
    private val recorder: RecorderManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

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

    // Get LLM config from preferences
    private val llmConfig = combine(
        userPreferences.getLlmBaseUrl(),
        userPreferences.getLlmApiKey(),
        userPreferences.getLlmModelName()
    ) { baseUrl, apiKey, modelName ->
        Triple(baseUrl, apiKey, modelName)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Triple("", "", ""))

    private var currentAudioFile: File? = null

    init {
        // Set up amplitude monitoring
        recorder.setOnUpdateMicrophoneAmplitude { amplitude ->
            _amplitude.value = amplitude
        }
        
        // Add welcome message
        addMessage(
            content = "Hi! I'm your voice-controlled todo assistant. Please configure your LLM provider in Settings first, then tap the microphone button and speak to add, edit, or manage your todos.",
            isFromUser = false
        )
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
            val agentResponse = agent.runAgent(userMessage)
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

    private fun addMessage(content: String, isFromUser: Boolean) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            content = content,
            isFromUser = isFromUser
        )
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(message)
        _messages.value = currentMessages
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