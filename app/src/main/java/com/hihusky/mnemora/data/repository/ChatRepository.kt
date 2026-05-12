package com.hihusky.mnemora.data.repository

import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.ChatHistoryEntity
import com.hihusky.mnemora.data.local.db.entity.ChatSessionEntity
import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.data.model.ChatSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val db: AppDatabase
) {
    suspend fun getChatSessions(questionId: Int): List<ChatSession> {
        return db.chatSessionDao().getByQuestionId(questionId).map {
            ChatSession(
                id = it.id,
                questionId = it.questionId,
                title = it.title ?: "",
                createdAt = it.createdAt
            )
        }
    }

    suspend fun createChatSession(questionId: Int, title: String): ChatSession {
        val now = System.currentTimeMillis()
        val id = db.chatSessionDao().insert(
            ChatSessionEntity(questionId = questionId, title = title, createdAt = now)
        ).toInt()
        return ChatSession(id = id, questionId = questionId, title = title, createdAt = now)
    }

    suspend fun updateChatSessionTitle(sessionId: Int, title: String) {
        val existing = db.chatSessionDao().getById(sessionId) ?: return
        db.chatSessionDao().update(existing.copy(title = title))
    }

    suspend fun deleteChatSession(sessionId: Int) {
        db.chatSessionDao().deleteById(sessionId)
    }

    suspend fun getChatHistory(sessionId: Int): List<ChatMessage> {
        return db.chatHistoryDao().getBySessionId(sessionId).map {
            ChatMessage(text = it.text, isUser = it.isUser == 1, timestamp = it.timestamp)
        }
    }

    suspend fun saveChatMessage(sessionId: Int, message: ChatMessage) {
        db.chatHistoryDao().insert(
            ChatHistoryEntity(
                sessionId = sessionId,
                text = message.text,
                isUser = if (message.isUser) 1 else 0,
                timestamp = message.timestamp
            )
        )
    }

    suspend fun clearBookChats(bookId: Int) {
        db.chatHistoryDao().deleteByBookId(bookId)
        db.chatSessionDao().deleteByBookId(bookId)
    }
}
