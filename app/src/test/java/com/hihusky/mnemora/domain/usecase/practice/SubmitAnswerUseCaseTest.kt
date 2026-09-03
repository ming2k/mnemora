package com.hihusky.mnemora.domain.usecase.practice

import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.QuestionChoice
import com.hihusky.mnemora.data.model.QuestionType
import com.hihusky.mnemora.data.model.SrsRating
import com.hihusky.mnemora.data.repository.SrsRepository
import com.hihusky.mnemora.data.repository.UserAnswerRepository
import com.hihusky.mnemora.domain.service.FeedbackService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitAnswerUseCaseTest {
    private val userAnswerRepository = mockk<UserAnswerRepository>(relaxed = true)
    private val feedbackService = mockk<FeedbackService>(relaxed = true)
    private val srsRepository = mockk<SrsRepository>(relaxed = true)
    private val useCase =
        SubmitAnswerUseCase(
            userAnswerRepository = userAnswerRepository,
            feedbackService = feedbackService,
            srsRepository = srsRepository,
        )

    private val question =
        Question(
            id = 42,
            bookId = 7,
            nodeId = "n1",
            content = "What is 2+2?",
            choices = listOf(QuestionChoice("A", "3"), QuestionChoice("B", "4")),
            answer = "B",
            questionType = QuestionType.MultipleChoice,
        )

    @Test
    fun `records the answer and persists it`() =
        runTest {
            val answer = useCase(7, question, "B")

            assertTrue(answer.isCorrect == true)
            assertEquals("B", answer.selected)
            coVerify {
                userAnswerRepository.saveUserAnswer(7, 42, match { it.selected == "B" && it.isCorrect == true })
            }
        }

    @Test
    fun `feeds the srs scheduler with Good on a correct answer`() =
        runTest {
            useCase(7, question, "B")

            coVerify { srsRepository.applyRating(bookId = 7, questionId = 42, rating = SrsRating.Good, now = any()) }
            coVerify { feedbackService.incrementStreak() }
            coVerify { feedbackService.playCorrect() }
            confirmVerified(srsRepository)
        }

    @Test
    fun `feeds the srs scheduler with Again on a wrong answer`() =
        runTest {
            val answer = useCase(7, question, "A")

            assertFalse(answer.isCorrect == true)
            coVerify { srsRepository.applyRating(bookId = 7, questionId = 42, rating = SrsRating.Again, now = any()) }
            coVerify { feedbackService.resetStreak() }
            coVerify { feedbackService.playWrong() }
            confirmVerified(srsRepository)
        }

    @Test
    fun `grading ignores case differences`() =
        runTest {
            coEvery { srsRepository.applyRating(any(), any(), any(), any()) } returns Unit

            val answer = useCase(7, question, "b")

            assertTrue(answer.isCorrect == true)
        }
}
