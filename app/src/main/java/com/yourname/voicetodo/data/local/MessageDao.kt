package com.yourname.voicetodo.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId")
    suspend fun getMessageCountForSession(sessionId: String): Int

    @Query("UPDATE messages SET toolStatus = :status, toolResult = :result WHERE id = :messageId")
    suspend fun updateToolCallMessageStatus(messageId: String, status: String, result: String?)

    @Query("UPDATE messages SET approved = :approved WHERE id = :messageId")
    suspend fun updateToolCallMessageApproved(messageId: String, approved: Boolean)

    @Query("""
        SELECT DISTINCT sessionId FROM messages
        WHERE content LIKE '%' || :query || '%'
    """)
    suspend fun getSessionIdsWithMessageContent(query: String): List<String>
}