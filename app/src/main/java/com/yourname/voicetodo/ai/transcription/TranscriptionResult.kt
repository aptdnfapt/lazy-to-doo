package com.yourname.voicetodo.ai.transcription

// Transcription result wrapper
data class TranscriptionResult(
    val text: String,
    val confidence: Float = 1.0f,
    val language: String? = null
)