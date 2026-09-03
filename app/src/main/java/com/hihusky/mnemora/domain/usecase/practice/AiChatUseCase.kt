package com.hihusky.mnemora.domain.usecase.practice

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.data.model.ChatScrollPosition
import com.hihusky.mnemora.data.model.ChatSession
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.repository.ChatRepository
import com.hihusky.mnemora.domain.service.AiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AiChatUseCase
    @Inject
    constructor(
        private val chatRepository: ChatRepository,
        private val aiService: AiService,
    ) {
        suspend fun getChatSessions(questionId: Int): List<ChatSession> = chatRepository.getChatSessions(questionId)

        suspend fun getChatHistory(sessionId: Int): List<ChatMessage> = chatRepository.getChatHistory(sessionId)

        suspend fun createChatSession(
            questionId: Int,
            title: String,
        ): ChatSession = chatRepository.createChatSession(questionId, title)

        suspend fun deleteChatSession(sessionId: Int) {
            chatRepository.deleteChatSession(sessionId)
        }

        suspend fun saveUserMessage(
            sessionId: Int,
            message: String,
        ): ChatMessage {
            val userMsg = ChatMessage(text = message, isUser = true)
            return chatRepository.saveChatMessage(sessionId, userMsg)
        }

        suspend fun streamAiResponse(
            sessionId: Int,
            question: Question,
            userMessage: String,
            history: List<ChatMessage>,
        ): Flow<String> =
            aiService.explain(
                questionStem = question.content,
                options = question.choices.associate { it.key to it.content },
                correctAnswer = question.answer,
                explanation = question.explanation,
                userQuestion = userMessage,
                history = history,
            )

        suspend fun saveScrollPosition(
            sessionId: Int,
            position: ChatScrollPosition,
        ) {
            chatRepository.saveScrollPosition(sessionId, position)
        }

        suspend fun saveBotMessage(
            sessionId: Int,
            response: String,
            isInterrupted: Boolean = false,
        ): ChatMessage {
            val botMsg = ChatMessage(text = response, isUser = false, isInterrupted = isInterrupted)
            return chatRepository.saveChatMessage(sessionId, botMsg)
        }

        suspend fun updateChatMessage(
            sessionId: Int,
            message: ChatMessage,
        ) {
            chatRepository.updateChatMessage(sessionId, message)
        }

        suspend fun deleteChatMessage(messageId: Int) {
            chatRepository.deleteChatMessage(messageId)
        }

        suspend fun streamAiContinuation(
            sessionId: Int,
            question: Question,
            history: List<ChatMessage>,
            interruptedText: String,
        ): Flow<String> {
            val continuationPrompt =
                "Continue directly and seamlessly from the exact point where the previous message ended. " +
                    "Do not repeat any previous text, do not add introductory phrases or acknowledgments. " +
                    "Continue from: \"${interruptedText.takeLast(100)}\""
            return aiService.explain(
                questionStem = question.content,
                options = question.choices.associate { it.key to it.content },
                correctAnswer = question.answer,
                explanation = question.explanation,
                userQuestion = continuationPrompt,
                history = history,
            )
        }
    }
