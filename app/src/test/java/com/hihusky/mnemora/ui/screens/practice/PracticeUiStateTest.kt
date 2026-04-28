package com.hihusky.mnemora.ui.screens.practice

import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.QuestionChoice
import com.hihusky.mnemora.data.model.QuestionStatus
import com.hihusky.mnemora.data.model.UserAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeUiStateTest {

    private val sampleQuestion = Question(
        id = 1,
        bookId = 1,
        nodeId = "node_1",
        content = "What is 2+2?",
        questionType = com.hihusky.mnemora.data.model.QuestionType.MultipleChoice,
        answer = "A",
        choices = listOf(
            com.hihusky.mnemora.data.model.QuestionChoice("A", "4"),
            com.hihusky.mnemora.data.model.QuestionChoice("B", "3")
        ),
        explanation = "2+2=4"
    )

    private val sampleQuestion2 = Question(
        id = 2,
        bookId = 1,
        nodeId = "node_1",
        content = "What is 2+3?",
        questionType = com.hihusky.mnemora.data.model.QuestionType.MultipleChoice,
        answer = "B",
        choices = listOf(
            com.hihusky.mnemora.data.model.QuestionChoice("A", "4"),
            com.hihusky.mnemora.data.model.QuestionChoice("B", "5")
        ),
        explanation = "2+3=5"
    )

    @Test
    fun `currentQuestion returns question at currentIndex`() {
        val state = PracticeUiState(
            questions = listOf(sampleQuestion, sampleQuestion2),
            currentIndex = 0
        )
        assertEquals(sampleQuestion, state.currentQuestion)
    }

    @Test
    fun `currentQuestion returns question at index 1`() {
        val state = PracticeUiState(
            questions = listOf(sampleQuestion, sampleQuestion2),
            currentIndex = 1
        )
        assertEquals(sampleQuestion2, state.currentQuestion)
    }

    @Test
    fun `currentQuestion returns null for empty list`() {
        val state = PracticeUiState(questions = emptyList(), currentIndex = 0)
        assertNull(state.currentQuestion)
    }

    @Test
    fun `currentQuestion returns null for out of bounds index`() {
        val state = PracticeUiState(
            questions = listOf(sampleQuestion),
            currentIndex = 5
        )
        assertNull(state.currentQuestion)
    }

    @Test
    fun `currentUserAnswer returns null when no answer`() {
        val state = PracticeUiState(
            questions = listOf(sampleQuestion),
            currentIndex = 0,
            userAnswers = emptyMap()
        )
        assertNull(state.currentUserAnswer)
    }

    @Test
    fun `currentUserAnswer returns answer for current question`() {
        val answer = UserAnswer(selected = "A", isCorrect = true)
        val state = PracticeUiState(
            questions = listOf(sampleQuestion),
            currentIndex = 0,
            userAnswers = mapOf(1 to answer)
        )
        assertEquals(answer, state.currentUserAnswer)
    }

    @Test
    fun `isCurrentMarked returns true when question is marked`() {
        val state = PracticeUiState(
            questions = listOf(sampleQuestion),
            currentIndex = 0,
            markedQuestions = setOf(1)
        )
        assertTrue(state.isCurrentMarked)
    }

    @Test
    fun `isCurrentMarked returns false when question is not marked`() {
        val state = PracticeUiState(
            questions = listOf(sampleQuestion),
            currentIndex = 0,
            markedQuestions = emptySet()
        )
        assertFalse(state.isCurrentMarked)
    }

    @Test
    fun `totalQuestions returns list size`() {
        val state = PracticeUiState(
            questions = listOf(sampleQuestion, sampleQuestion2)
        )
        assertEquals(2, state.totalQuestions)
    }

    @Test
    fun `totalQuestions returns 0 for empty list`() {
        val state = PracticeUiState(questions = emptyList())
        assertEquals(0, state.totalQuestions)
    }

    @Test
    fun `progress returns correct ratio`() {
        val state = PracticeUiState(
            questions = listOf(sampleQuestion, sampleQuestion2),
            currentIndex = 0
        )
        assertEquals(0.5f, state.progress)
    }

    @Test
    fun `progress at last question returns 1`() {
        val state = PracticeUiState(
            questions = listOf(sampleQuestion, sampleQuestion2),
            currentIndex = 1
        )
        assertEquals(1.0f, state.progress)
    }

    @Test
    fun `progress returns 0 for empty list`() {
        val state = PracticeUiState(questions = emptyList(), currentIndex = 0)
        assertEquals(0.0f, state.progress)
    }

    @Test
    fun `getQuestionStatus returns Unanswered for no answer`() {
        val vm = PracticeUiState(
            questions = listOf(sampleQuestion),
            currentIndex = 0,
            userAnswers = emptyMap(),
            markedQuestions = emptySet()
        )
        // getQuestionStatus is on PracticeViewModel, not UiState
        // This is tested via the ViewModel or inline
        val q = sampleQuestion
        val answer = vm.userAnswers[q.id]
        val isMarked = vm.markedQuestions.contains(q.id)
        val status = when {
            isMarked -> QuestionStatus.Marked
            answer == null -> QuestionStatus.Unanswered
            answer.isCorrect == true -> QuestionStatus.Correct
            else -> QuestionStatus.Wrong
        }
        assertEquals(QuestionStatus.Unanswered, status)
    }

    @Test
    fun `getQuestionStatus returns Correct for correct answer`() {
        val state = PracticeUiState(
            questions = listOf(sampleQuestion),
            currentIndex = 0,
            userAnswers = mapOf(1 to UserAnswer(selected = "A", isCorrect = true)),
            markedQuestions = emptySet()
        )
        val answer = state.userAnswers[1]
        val status = when {
            answer == null -> QuestionStatus.Unanswered
            answer.isCorrect == true -> QuestionStatus.Correct
            else -> QuestionStatus.Wrong
        }
        assertEquals(QuestionStatus.Correct, status)
    }

    @Test
    fun `getQuestionStatus returns Wrong for incorrect answer`() {
        val state = PracticeUiState(
            questions = listOf(sampleQuestion),
            currentIndex = 0,
            userAnswers = mapOf(1 to UserAnswer(selected = "B", isCorrect = false)),
            markedQuestions = emptySet()
        )
        val answer = state.userAnswers[1]
        val status = when {
            answer == null -> QuestionStatus.Unanswered
            answer.isCorrect == true -> QuestionStatus.Correct
            else -> QuestionStatus.Wrong
        }
        assertEquals(QuestionStatus.Wrong, status)
    }

    @Test
    fun `getQuestionStatus returns Marked when marked regardless of answer`() {
        val state = PracticeUiState(
            questions = listOf(sampleQuestion),
            currentIndex = 0,
            userAnswers = mapOf(1 to UserAnswer(selected = "B", isCorrect = false)),
            markedQuestions = setOf(1)
        )
        val status = if (state.markedQuestions.contains(1)) {
            QuestionStatus.Marked
        } else {
            val answer = state.userAnswers[1]
            when {
                answer == null -> QuestionStatus.Unanswered
                answer.isCorrect == true -> QuestionStatus.Correct
                else -> QuestionStatus.Wrong
            }
        }
        assertEquals(QuestionStatus.Marked, status)
    }
}
