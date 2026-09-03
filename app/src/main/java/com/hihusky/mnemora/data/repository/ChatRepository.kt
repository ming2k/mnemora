package com.hihusky.mnemora.data.repository

import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.ChatHistoryEntity
import com.hihusky.mnemora.data.local.db.entity.ChatSessionEntity
import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.data.model.ChatScrollPosition
import com.hihusky.mnemora.data.model.ChatSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository
    @Inject
    constructor(
        private val db: AppDatabase,
    ) {
        suspend fun getChatSessions(questionId: Int): List<ChatSession> =
            db.chatSessionDao().getByQuestionId(questionId).map {
                ChatSession(
                    id = it.id,
                    questionId = it.questionId,
                    title = it.title ?: "",
                    createdAt = it.createdAt,
                    lastScrollPosition =
                        ChatScrollPosition(
                            firstVisibleItemIndex = it.lastScrollIndex,
                            firstVisibleItemScrollOffset = it.lastScrollOffset,
                            isAtBottom = it.lastScrollAtBottom,
                        ),
                )
            }

        suspend fun createChatSession(
            questionId: Int,
            title: String,
        ): ChatSession {
            val now = System.currentTimeMillis()
            val id =
                db
                    .chatSessionDao()
                    .insert(
                        ChatSessionEntity(questionId = questionId, title = title, createdAt = now),
                    ).toInt()
            return ChatSession(id = id, questionId = questionId, title = title, createdAt = now)
        }

        suspend fun updateChatSessionTitle(
            sessionId: Int,
            title: String,
        ) {
            val existing = db.chatSessionDao().getById(sessionId) ?: return
            db.chatSessionDao().update(existing.copy(title = title))
        }

        suspend fun deleteChatSession(sessionId: Int) {
            db.chatSessionDao().deleteById(sessionId)
        }

        suspend fun getChatHistory(sessionId: Int): List<ChatMessage> =
            db.chatHistoryDao().getBySessionId(sessionId).map {
                ChatMessage(
                    id = it.id,
                    text = it.text,
                    isUser = it.isUser == 1,
                    timestamp = it.timestamp,
                    isInterrupted = it.isInterrupted == 1,
                )
            }

        suspend fun saveChatMessage(
            sessionId: Int,
            message: ChatMessage,
        ): ChatMessage {
            val id =
                db.chatHistoryDao().insert(
                    ChatHistoryEntity(
                        id = message.id,
                        sessionId = sessionId,
                        text = message.text,
                        isUser = if (message.isUser) 1 else 0,
                        timestamp = message.timestamp,
                        isInterrupted = if (message.isInterrupted) 1 else 0,
                    ),
                )
            return message.copy(id = if (message.id == 0) id.toInt() else message.id)
        }

        suspend fun updateChatMessage(
            sessionId: Int,
            message: ChatMessage,
        ) {
            db.chatHistoryDao().update(
                ChatHistoryEntity(
                    id = message.id,
                    sessionId = sessionId,
                    text = message.text,
                    isUser = if (message.isUser) 1 else 0,
                    timestamp = message.timestamp,
                    isInterrupted = if (message.isInterrupted) 1 else 0,
                ),
            )
        }

        suspend fun deleteChatMessage(messageId: Int) {
            db.chatHistoryDao().deleteById(messageId)
        }

        suspend fun saveScrollPosition(
            sessionId: Int,
            position: ChatScrollPosition,
        ) {
            db.chatSessionDao().updateScrollPosition(
                sessionId = sessionId,
                index = position.firstVisibleItemIndex,
                offset = position.firstVisibleItemScrollOffset,
                isAtBottom = position.isAtBottom,
            )
        }

        suspend fun clearBookChats(bookId: Int) {
            db.chatHistoryDao().deleteByBookId(bookId)
            db.chatSessionDao().deleteByBookId(bookId)
        }
    }
