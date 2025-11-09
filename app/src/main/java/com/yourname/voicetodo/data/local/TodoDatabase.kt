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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import javax.inject.Singleton

@Database(
    entities = [TodoEntity::class, ChatSessionEntity::class, MessageEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(TodoTypeConverters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun messageDao(): MessageDao
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
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).fallbackToDestructiveMigration().build()
    }
    
    @Provides
    fun provideTodoDao(database: TodoDatabase): TodoDao {
        return database.todoDao()
    }

    @Provides
    fun provideChatSessionDao(database: TodoDatabase): ChatSessionDao {
        return database.chatSessionDao()
    }

    @Provides
    fun provideMessageDao(database: TodoDatabase): MessageDao {
        return database.messageDao()
    }
}

// Migration from version 1 to 2 - Add chat tables
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create chat_sessions table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chat_sessions (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Create messages table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS messages (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                content TEXT NOT NULL,
                isFromUser INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Create index for messages
        database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_sessionId ON messages(sessionId)")
    }
}

// Migration from version 2 to 3 - Add tool call message fields
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns to messages table for tool call support
        database.execSQL("ALTER TABLE messages ADD COLUMN messageType TEXT DEFAULT 'TEXT'")
        database.execSQL("ALTER TABLE messages ADD COLUMN toolName TEXT")
        database.execSQL("ALTER TABLE messages ADD COLUMN toolArguments TEXT")
        database.execSQL("ALTER TABLE messages ADD COLUMN toolStatus TEXT")
        database.execSQL("ALTER TABLE messages ADD COLUMN toolResult TEXT")
    }
}