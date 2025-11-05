package com.yourname.voicetodo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VoiceTodoApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}