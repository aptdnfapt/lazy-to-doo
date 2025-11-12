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
    entities = [TodoEntity::class, CategoryEntity::class, ChatSessionEntity::class, MessageEntity::class],
    version = 5,  // Bump version for approved field in messages
    exportSchema = false
)
@TypeConverters(TodoTypeConverters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun categoryDao(): CategoryDao
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
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .fallbackToDestructiveMigration()  // Alpha: Just recreate DB
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Insert default categories on first run
                db.execSQL("""
                    INSERT INTO categories (id, name, displayName, color, sortOrder, isDefault, createdAt)
                    VALUES
                        ('work', 'WORK', 'Work', '#137fec', 0, 1, ${System.currentTimeMillis()}),
                        ('life', 'LIFE', 'Life', '#4caf50', 1, 1, ${System.currentTimeMillis()}),
                        ('study', 'STUDY', 'Study', '#ff9800', 2, 1, ${System.currentTimeMillis()})
                """)
            }
        })
        .build()
    }
    
    @Provides
    fun provideTodoDao(database: TodoDatabase): TodoDao {
        return database.todoDao()
    }

    @Provides
    fun provideCategoryDao(database: TodoDatabase): CategoryDao {
        return database.categoryDao()
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

// Migration from version 3 to 4 - Add categories and update todos
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create categories table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS categories (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                displayName TEXT NOT NULL,
                color TEXT NOT NULL DEFAULT '#137fec',
                icon TEXT,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                isDefault INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """)

        // Add new columns to todos table
        database.execSQL("ALTER TABLE todos ADD COLUMN categoryId TEXT NOT NULL DEFAULT 'work'")
        database.execSQL("ALTER TABLE todos ADD COLUMN subtasks TEXT")

        // Rename section column to status
        database.execSQL("ALTER TABLE todos RENAME COLUMN section TO status")

        // Create index for categoryId
        database.execSQL("CREATE INDEX IF NOT EXISTS index_todos_categoryId ON todos(categoryId)")

        // Insert default categories
        val currentTime = System.currentTimeMillis()
        database.execSQL("""
            INSERT INTO categories (id, name, displayName, color, sortOrder, isDefault, createdAt)
            VALUES
                ('work', 'WORK', 'Work', '#137fec', 0, 1, $currentTime),
                ('life', 'LIFE', 'Life', '#4caf50', 1, 1, $currentTime),
                ('study', 'STUDY', 'Study', '#ff9800', 2, 1, $currentTime)
        """)
    }
}

// Migration from version 4 to 5 - Add approved field to messages table
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add approved column to messages table
        database.execSQL("ALTER TABLE messages ADD COLUMN approved INTEGER NOT NULL DEFAULT 0")
    }
}