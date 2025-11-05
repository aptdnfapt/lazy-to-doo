package com.yourname.voicetodo.ai.transcription

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

private const val MEDIA_RECORDER_CONSTRUCTOR_DEPRECATION_API_LEVEL = 31

class RecorderManager(private val context: Context) {
    companion object {
        fun requiredPermissions() = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        
        private const val AMPLITUDE_REPORT_PERIOD = 100L // 100ms
    }

    private var recorder: MediaRecorder? = null
    private var onUpdateMicrophoneAmplitude: (Int) -> Unit = { }
    private var microphoneAmplitudeUpdateJob: Job? = null
    private var currentAudioFile: File? = null

    fun startRecording(): File {
        recorder?.apply {
            stop()
            release()
        }

        // Create a temporary audio file
        val audioFile = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
        currentAudioFile = audioFile
        
        if (audioFile.exists()) {
            audioFile.delete()
        }

        recorder = if (Build.VERSION.SDK_INT >= MEDIA_RECORDER_CONSTRUCTOR_DEPRECATION_API_LEVEL) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        recorder!!.apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(audioFile.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("VoiceTodo", "MediaRecorder prepare() failed", e)
                throw e
            }

            start()
        }

        // Start a job to periodically report current amplitude
        microphoneAmplitudeUpdateJob?.cancel()
        microphoneAmplitudeUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (recorder != null) {
                val amplitude = recorder?.maxAmplitude ?: 0
                onUpdateMicrophoneAmplitude(amplitude)
                delay(AMPLITUDE_REPORT_PERIOD)
            }
        }
        
        return audioFile
    }

    fun stopRecording(): File? {
        recorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                Log.e("VoiceTodo", "Error stopping recorder", e)
            }
            release()
        }
        recorder = null

        microphoneAmplitudeUpdateJob?.cancel()
        microphoneAmplitudeUpdateJob = null
        
        return currentAudioFile
    }

    // Assign onUpdateMicrophoneAmplitude callback
    fun setOnUpdateMicrophoneAmplitude(onUpdateMicrophoneAmplitude: (Int) -> Unit) {
        this.onUpdateMicrophoneAmplitude = onUpdateMicrophoneAmplitude
    }

    // Returns whether all of the permissions are granted.
    fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions()) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }
}