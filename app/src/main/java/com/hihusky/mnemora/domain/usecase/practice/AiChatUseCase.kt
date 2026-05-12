package com.hihusky.mnemora.domain.usecase.practice

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.data.model.ChatSession
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.repository.ChatRepository
import com.hihusky.mnemora.domain.service.AiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AiChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val aiService: AiService
) {
    suspend fun getChatSessions(questionId: Int): List<ChatSession> {
        return chatRepository.getChatSessions(questionId)
    }

    suspend fun getChatHistory(sessionId: Int): List<ChatMessage> {
        return chatRepository.getChatHistory(sessionId)
    }

    suspend fun createChatSession(questionId: Int, title: String): ChatSession {
        return chatRepository.createChatSession(questionId, title)
    }

    suspend fun deleteChatSession(sessionId: Int) {
        chatRepository.deleteChatSession(sessionId)
    }

    suspend fun saveUserMessage(sessionId: Int, message: String): ChatMessage {
        val userMsg = ChatMessage(text = message, isUser = true)
        chatRepository.saveChatMessage(sessionId, userMsg)
        return userMsg
    }

    suspend fun streamAiResponse(
        sessionId: Int,
        question: Question,
        userMessage: String,
        history: List<ChatMessage>
    ): Flow<String> {
        return aiService.explain(
            questionStem = question.content,
            options = question.choices.associate { it.key to it.content },
            correctAnswer = question.answer,
            explanation = question.explanation,
            userQuestion = userMessage,
            history = history
        )
    }

    suspend fun saveScrollPosition(sessionId: Int, index: Int, offset: Int) {
        chatRepository.saveScrollPosition(sessionId, index, offset)
    }

    suspend fun saveBotMessage(sessionId: Int, response: String): ChatMessage {
        val botMsg = ChatMessage(text = response, isUser = false)
        chatRepository.saveChatMessage(sessionId, botMsg)
        return botMsg
    }
}
