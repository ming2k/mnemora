package com.hihusky.mnemora.ui.screens.practice

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.data.model.ChatSession
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.QuestionChoice
import com.hihusky.mnemora.data.model.QuestionType
import com.hihusky.mnemora.domain.service.AiConfig
import com.hihusky.mnemora.domain.service.AiService
import com.hihusky.mnemora.domain.usecase.practice.AiChatUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AiChatControllerTest {
    private val aiChatUseCase = mockk<AiChatUseCase>()
    private val aiService = mockk<AiService>()

    private val question =
        Question(
            id = 1,
            bookId = 1,
            nodeId = "n1",
            content = "What is 2+2?",
            choices = listOf(QuestionChoice("A", "3"), QuestionChoice("B", "4")),
            answer = "B",
            questionType = QuestionType.MultipleChoice,
        )

    private fun controller(scope: kotlinx.coroutines.CoroutineScope): AiChatController {
        every { aiService.config } returns MutableStateFlow(AiConfig())
        return AiChatController(scope, aiChatUseCase, aiService, now = { 0L })
    }

    @Test
    fun `send message streams the response and persists the completed bot message`() =
        runTest {
            coEvery { aiChatUseCase.createChatSession(1, "Hello") } returns
                ChatSession(id = 7, questionId = 1, title = "Hello", createdAt = 0)
            coEvery { aiChatUseCase.saveUserMessage(7, "Hello") } returns
                ChatMessage(id = 101, text = "Hello", isUser = true)
            coEvery {
                aiChatUseCase.streamAiResponse(7, question, "Hello", any())
            } returns flowOf("Hel", "lo")
            coEvery { aiChatUseCase.saveBotMessage(7, "Hello", any()) } returns
                ChatMessage(id = 102, text = "Hello", isUser = false)

            val controller = controller(this)
            controller.sendMessage(question, "Hello")
            advanceUntilIdle()

            val chat = controller.state.value
            assertEquals(7, chat.currentSessionId)
            assertEquals(listOf("Hello", "Hello"), chat.history.map { it.text })
            assertTrue(chat.loadingSessionIds.isEmpty())
            assertTrue(chat.streamingResponses.isEmpty())

            coVerify { aiChatUseCase.saveBotMessage(7, "Hello", false) }
            controller.close()
        }

    @Test
    fun `send message keeps partial response as interrupted on stream failure`() =
        runTest {
            coEvery { aiChatUseCase.createChatSession(1, "Hello") } returns
                ChatSession(id = 7, questionId = 1, title = "Hello", createdAt = 0)
            coEvery { aiChatUseCase.saveUserMessage(7, "Hello") } returns
                ChatMessage(id = 101, text = "Hello", isUser = true)
            coEvery { aiChatUseCase.streamAiResponse(7, question, "Hello", any()) } returns
                flow {
                    emit("Par")
                    emit("tial")
                    throw IOException("boom")
                }
            val savedText = slot<String>()
            coEvery { aiChatUseCase.saveBotMessage(7, capture(savedText), any()) } returns
                ChatMessage(id = 102, text = "partial", isUser = false)

            val controller = controller(this)
            controller.sendMessage(question, "Hello")
            advanceUntilIdle()

            assertEquals("Partial", savedText.captured)
            coVerify { aiChatUseCase.saveBotMessage(7, "Partial", true) }
            val chat = controller.state.value
            assertEquals(listOf("Hello", "partial"), chat.history.map { it.text })
            assertTrue(chat.loadingSessionIds.isEmpty())
            controller.close()
        }

    @Test
    fun `send message without prior stream persists an error message`() =
        runTest {
            coEvery { aiChatUseCase.createChatSession(1, "Hello") } returns
                ChatSession(id = 7, questionId = 1, title = "Hello", createdAt = 0)
            coEvery { aiChatUseCase.saveUserMessage(7, "Hello") } returns
                ChatMessage(id = 101, text = "Hello", isUser = true)
            coEvery { aiChatUseCase.streamAiResponse(7, question, "Hello", any()) } returns
                flow<String> { throw IOException("network down") }
            coEvery { aiChatUseCase.saveBotMessage(7, any(), any()) } returns
                ChatMessage(id = 102, text = "Error: network down", isUser = false)

            val controller = controller(this)
            controller.sendMessage(question, "Hello")
            advanceUntilIdle()

            val chat = controller.state.value
            assertEquals(2, chat.history.size)
            assertEquals("Error: network down", chat.history.last().text)
            coVerify { aiChatUseCase.saveBotMessage(7, "Error: network down", true) }
            controller.close()
        }

    @Test
    fun `send message reuses the current session instead of creating one`() =
        runTest {
            coEvery { aiChatUseCase.getChatSessions(1) } returns emptyList()
            coEvery { aiChatUseCase.createChatSession(1, "Existing") } returns
                ChatSession(id = 9, questionId = 1, title = "Existing", createdAt = 0)

            val controller = controller(this)
            controller.loadHistory(question)
            advanceUntilIdle()
            // Seed an existing session as current.
            controller.createSession(question, "Existing")
            advanceUntilIdle()
            val sessionId = controller.state.value.currentSessionId!!

            coEvery { aiChatUseCase.saveUserMessage(sessionId, "Hi") } returns
                ChatMessage(id = 201, text = "Hi", isUser = true)
            coEvery { aiChatUseCase.streamAiResponse(sessionId, question, "Hi", any()) } returns flowOf("ok")
            coEvery { aiChatUseCase.saveBotMessage(sessionId, "ok", any()) } returns
                ChatMessage(id = 202, text = "ok", isUser = false)

            controller.sendMessage(question, "Hi")
            advanceUntilIdle()

            val chat = controller.state.value
            assertEquals(sessionId, chat.currentSessionId)
            assertEquals(listOf("Hi", "ok"), chat.history.map { it.text })
            controller.close()
        }
}
