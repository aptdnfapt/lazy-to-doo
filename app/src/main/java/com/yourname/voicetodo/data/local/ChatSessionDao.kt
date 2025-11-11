package com.yourname.voicetodo.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ChatSessionWithMessageCount(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int
)

@Dao
interface ChatSessionDao {

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllChatSessions(): Flow<List<ChatSessionEntity>>

    @Query("""
        SELECT cs.id, cs.title, cs.createdAt, cs.updatedAt, COUNT(m.id) as messageCount
        FROM chat_sessions cs
        LEFT JOIN messages m ON cs.id = m.sessionId
        GROUP BY cs.id, cs.title, cs.createdAt, cs.updatedAt
        ORDER BY cs.updatedAt DESC
    """)
    fun getAllChatSessionsWithMessageCount(): Flow<List<ChatSessionWithMessageCount>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getChatSessionById(id: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSessionEntity)

    @Update
    suspend fun updateChatSession(session: ChatSessionEntity)

    @Delete
    suspend fun deleteChatSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteChatSessionById(id: String)

    @Query("UPDATE chat_sessions SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateChatSessionTimestamp(id: String, updatedAt: Long)

    @Query("""
        SELECT cs.id, cs.title, cs.createdAt, cs.updatedAt, COUNT(m.id) as messageCount
        FROM chat_sessions cs
        LEFT JOIN messages m ON cs.id = m.sessionId
        WHERE cs.title LIKE '%' || :query || '%'
        GROUP BY cs.id, cs.title, cs.createdAt, cs.updatedAt
        ORDER BY cs.updatedAt DESC
    """)
    suspend fun searchChatSessionsByTitle(query: String): List<ChatSessionWithMessageCount>
}