package com.yourname.voicetodo.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [TodoEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(TodoTypeConverters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}

@Module
@InstallIn(SingletonComponent::class)
object TodoDatabaseModule {
    
    @Provides
    @Singleton
    fun provideTodoDatabase(@ApplicationContext context: Context): TodoDatabase {
        return Room.databaseBuilder(
            context,
            TodoDatabase::class.java,
            "todo_database"
        ).build()
    }
    
    @Provides
    fun provideTodoDao(database: TodoDatabase): TodoDao {
        return database.todoDao()
    }
}