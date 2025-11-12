package com.yourname.voicetodo.data.repository

import com.yourname.voicetodo.data.local.ChatSessionDao
import com.yourname.voicetodo.data.local.ChatSessionEntity
import com.yourname.voicetodo.data.local.ChatSessionWithMessageCount
import com.yourname.voicetodo.data.local.MessageDao
import com.yourname.voicetodo.data.local.MessageEntity
import com.yourname.voicetodo.domain.model.ChatSession
import com.yourname.voicetodo.domain.model.Message
import com.yourname.voicetodo.domain.model.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatSessionDao: ChatSessionDao,
    private val messageDao: MessageDao
) {

    fun getAllChatSessions(): Flow<List<ChatSession>> {
        return chatSessionDao.getAllChatSessionsWithMessageCount().map { sessions ->
            sessions.map { session ->
                ChatSession(
                    id = session.id,
                    title = session.title,
                    createdAt = session.createdAt,
                    updatedAt = session.updatedAt,
                    messageCount = session.messageCount
                )
            }
        }
    }

    suspend fun getChatSessionById(id: String): ChatSession? {
        val entity = chatSessionDao.getChatSessionById(id)
        return entity?.let {
            val messageCount = messageDao.getMessageCountForSession(id)
            ChatSession(
                id = it.id,
                title = it.title,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                messageCount = messageCount
            )
        }
    }

    suspend fun createChatSession(title: String = "New Chat"): ChatSession {
        val now = System.currentTimeMillis()
        val sessionId = UUID.randomUUID().toString()
        return createChatSessionWithId(sessionId, title)
    }

    suspend fun createChatSessionWithId(sessionId: String, title: String): ChatSession {
        val now = System.currentTimeMillis()
        val entity = ChatSessionEntity(
            id = sessionId,
            title = title,
            createdAt = now,
            updatedAt = now
        )
        chatSessionDao.insertChatSession(entity)
        return ChatSession(
            id = sessionId,
            title = title,
            createdAt = now,
            updatedAt = now,
            messageCount = 0
        )
    }

    suspend fun updateChatSessionTitle(id: String, title: String) {
        val entity = chatSessionDao.getChatSessionById(id)
        entity?.let {
            val updatedEntity = it.copy(title = title, updatedAt = System.currentTimeMillis())
            chatSessionDao.updateChatSession(updatedEntity)
        }
    }

    suspend fun deleteChatSession(id: String) {
        chatSessionDao.deleteChatSessionById(id)
        messageDao.deleteMessagesForSession(id)
    }

    fun getMessagesForSession(sessionId: String): Flow<List<Message>> {
        return messageDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { entity ->
                Message(
                    id = entity.id,
                    sessionId = entity.sessionId,
                    content = entity.content,
                    isFromUser = entity.isFromUser,
                    timestamp = entity.timestamp,
                    messageType = try { MessageType.valueOf(entity.messageType) } catch (e: Exception) { MessageType.TEXT },
                    toolName = entity.toolName,
                    toolArguments = entity.toolArguments,
                    toolStatus = entity.toolStatus,
                    toolResult = entity.toolResult,
                    approved = entity.approved
                )
            }
        }
    }

    suspend fun addMessage(sessionId: String, content: String, isFromUser: Boolean): Message {
        val message = Message(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            content = content,
            isFromUser = isFromUser
        )
        val entity = MessageEntity(
            id = message.id,
            sessionId = message.sessionId,
            content = message.content,
            isFromUser = message.isFromUser,
            timestamp = message.timestamp
        )
        messageDao.insertMessage(entity)
        chatSessionDao.updateChatSessionTimestamp(sessionId, System.currentTimeMillis())
        return message
    }

    suspend fun addMessage(message: Message): Message {
        val entity = MessageEntity(
            id = message.id,
            sessionId = message.sessionId,
            content = message.content,
            isFromUser = message.isFromUser,
            timestamp = message.timestamp,
            messageType = message.messageType.name,
            toolName = message.toolName,
            toolArguments = message.toolArguments,
            toolStatus = message.toolStatus,
            toolResult = message.toolResult,
            approved = message.approved
        )
        messageDao.insertMessage(entity)
        chatSessionDao.updateChatSessionTimestamp(message.sessionId, System.currentTimeMillis())
        return message
    }

    suspend fun updateToolCallMessageStatus(messageId: String, status: String, result: String? = null) {
        messageDao.updateToolCallMessageStatus(messageId, status, result)
    }

    suspend fun deleteMessage(messageId: String) {
        val entity = messageDao.getMessageById(messageId)
        entity?.let { messageDao.deleteMessage(it) }
    }

    suspend fun searchChatSessions(query: String): List<ChatSession> {
        if (query.isBlank()) {
            return getAllChatSessions().first()
        }

        val sessionsByTitle = chatSessionDao.searchChatSessionsByTitle(query)
        val sessionIdsByMessage = messageDao.getSessionIdsWithMessageContent(query)

        val allSessionIds = (sessionsByTitle.map { it.id } + sessionIdsByMessage).distinct()

        return allSessionIds.mapNotNull { sessionId ->
            val session = chatSessionDao.getChatSessionById(sessionId)
            session?.let {
                val messageCount = messageDao.getMessageCountForSession(sessionId)
                ChatSession(
                    id = it.id,
                    title = it.title,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    messageCount = messageCount
                )
            }
        }.sortedByDescending { it.updatedAt }
    }
}