package com.yourname.voicetodo.di

import android.content.Context
import com.yourname.voicetodo.ai.agent.TodoAgent
import com.yourname.voicetodo.ai.execution.RetryableToolExecutor
import com.yourname.voicetodo.ai.permission.ToolPermissionManager
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
    fun provideToolPermissionManager(
        userPreferences: com.yourname.voicetodo.data.preferences.UserPreferences
    ): ToolPermissionManager {
        return ToolPermissionManager(userPreferences)
    }

    @Provides
    @Singleton
    fun provideRetryableToolExecutor(
        permissionManager: ToolPermissionManager
    ): RetryableToolExecutor {
        return RetryableToolExecutor(permissionManager)
    }

    @Provides
    @Singleton
    fun provideTodoTools(
        todoRepository: com.yourname.voicetodo.data.repository.TodoRepository,
        permissionManager: ToolPermissionManager,
        retryableToolExecutor: RetryableToolExecutor
    ): TodoTools {
        return TodoTools(todoRepository, permissionManager, retryableToolExecutor)
    }

    @Provides
    @Singleton
    fun provideTodoAgent(
        todoTools: TodoTools,
        userPreferences: com.yourname.voicetodo.data.preferences.UserPreferences,
        permissionManager: ToolPermissionManager,
        retryableToolExecutor: RetryableToolExecutor
    ): TodoAgent {
        return TodoAgent(todoTools, userPreferences, permissionManager, retryableToolExecutor)
    }
}