package com.yourname.voicetodo.di

import com.yourname.voicetodo.data.repository.TodoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideTodoRepository(
        todoDao: com.yourname.voicetodo.data.local.TodoDao
    ): TodoRepository {
        return TodoRepository(todoDao)
    }
}