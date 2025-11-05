package com.yourname.voicetodo.ai.transcription

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class WhisperTranscriber(private val context: Context) {
    companion object {
        private const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent"
    }

    private val TAG = "WhisperTranscriber"
    private var currentTranscriptionJob: Job? = null

    suspend fun transcribe(audioFile: File, apiKey: String): String {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isEmpty()) {
                    throw Exception("Gemini API key is not set")
                }

                val url = "$GEMINI_ENDPOINT?key=$apiKey"
                val request = buildGeminiRequest(audioFile, url)
                
                val client = okhttp3.OkHttpClient()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful || response.code / 100 != 2) {
                    throw Exception("Transcription failed: ${response.code} - ${response.body?.string()}")
                }

                val responseBody = response.body?.string()
                    ?: throw Exception("Empty response from transcription service")

                val jsonObject = JSONObject(responseBody)
                val text = jsonObject.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                
                // Clean up the audio file after transcription
                audioFile.delete()
                
                return@withContext text.trim()
            } catch (e: Exception) {
                Log.e(TAG, "Transcription failed", e)
                audioFile.delete() // Clean up even on failure
                throw e
            }
        }
    }

    fun cancelTranscription() {
        currentTranscriptionJob?.cancel()
        currentTranscriptionJob = null
    }

    private fun buildGeminiRequest(
        audioFile: File,
        url: String
    ): Request {
        val audioBytes = audioFile.readBytes()
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

        val prompt = "Transcribe this audio recording accurately. Return only the transcribed text without any additional commentary or formatting."

        val jsonPayload = JSONObject()
        val contentsObject = JSONObject()
        val partsArray = org.json.JSONArray()
        
        // Add the text prompt
        partsArray.put(JSONObject().put("text", prompt))
        
        // Add the audio data
        partsArray.put(JSONObject().put("inlineData", JSONObject()
            .put("mimeType", "audio/mp4")
            .put("data", base64Audio)))
        
        contentsObject.put("parts", partsArray)
        jsonPayload.put("contents", org.json.JSONArray().put(contentsObject))

        val requestBody = jsonPayload.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        return Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
    }
}