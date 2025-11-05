package com.yourname.voicetodo.di

import android.content.Context
import com.yourname.voicetodo.ai.agent.TodoAgent
import com.yourname.voicetodo.ai.tools.TodoTools
import com.yourname.voicetodo.ai.transcription.RecorderManager
import com.yourname.voicetodo.ai.transcription.WhisperTranscriber
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {
    
    @Provides
    @Singleton
    fun provideRecorderManager(@ApplicationContext context: Context): RecorderManager {
        return RecorderManager(context)
    }

    @Provides
    @Singleton
    fun provideWhisperTranscriber(@ApplicationContext context: Context): WhisperTranscriber {
        return WhisperTranscriber(context)
    }

    @Provides
    @Singleton
    fun provideTodoTools(
        todoRepository: com.yourname.voicetodo.data.repository.TodoRepository
    ): TodoTools {
        return TodoTools(todoRepository)
    }

    @Provides
    @Singleton
    fun provideTodoAgent(
        todoTools: TodoTools,
        userPreferences: com.yourname.voicetodo.data.preferences.UserPreferences
    ): TodoAgent {
        return TodoAgent(todoTools, userPreferences)
    }
}