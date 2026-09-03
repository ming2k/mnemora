package com.hihusky.mnemora.domain.service

import com.hihusky.mnemora.data.model.SrsRating
import com.hihusky.mnemora.data.model.SrsReviewState
import com.hihusky.mnemora.data.model.SrsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsServiceTest {
    private val now = 1700000000000L
    private val defaultState =
        SrsState(
            questionId = 1,
            bookId = 1,
            intervalDays = 0,
            easeFactor = 2.5,
            repetitions = 0,
            lapses = 0,
            dueDate = null,
            lastReviewed = null,
            reviewState = SrsReviewState.New,
        )

    @Test
    fun `Again resets interval and increases lapses`() {
        val state = defaultState.copy(repetitions = 3, intervalDays = 10)
        val result = SrsService.review(state, SrsRating.Again, now)

        assertEquals(1, result.intervalDays)
        assertEquals(0, result.repetitions)
        assertEquals(1, result.lapses)
        assertEquals(SrsReviewState.Relearning, result.reviewState)
        assertEquals(now + 60_000L, result.dueDate)
        assertEquals(2.3, result.easeFactor, 0.01)
    }

    @Test
    fun `Again does not lower easeFactor below minimum`() {
        val state = defaultState.copy(easeFactor = 1.3)
        val result = SrsService.review(state, SrsRating.Again, now)

        assertEquals(1.3, result.easeFactor, 0.01)
    }

    @Test
    fun `Good on first review sets interval to 1 day`() {
        val result = SrsService.review(defaultState, SrsRating.Good, now)

        assertEquals(1, result.intervalDays)
        assertEquals(1, result.repetitions)
        assertEquals(SrsReviewState.Review, result.reviewState)
        assertEquals(now + SrsService.DAY_MS, result.dueDate)
    }

    @Test
    fun `Good on second review sets interval to 6 days`() {
        val state = defaultState.copy(repetitions = 1, intervalDays = 1)
        val result = SrsService.review(state, SrsRating.Good, now)

        assertEquals(6, result.intervalDays)
        assertEquals(2, result.repetitions)
    }

    @Test
    fun `Good on third review multiplies interval by easeFactor`() {
        val state = defaultState.copy(repetitions = 2, intervalDays = 6, easeFactor = 2.5)
        val result = SrsService.review(state, SrsRating.Good, now)

        assertEquals(15, result.intervalDays) // 6 * 2.5 = 15
        assertEquals(3, result.repetitions)
    }

    @Test
    fun `Easy on first review sets interval to 4 days`() {
        val result = SrsService.review(defaultState, SrsRating.Easy, now)

        assertEquals(4, result.intervalDays)
        assertEquals(1, result.repetitions)
        assertEquals(2.5, result.easeFactor, 0.01) // capped at MAX_EASE
    }

    @Test
    fun `Easy increases easeFactor`() {
        val state = defaultState.copy(repetitions = 3, intervalDays = 15, easeFactor = 2.3)
        val result = SrsService.review(state, SrsRating.Easy, now)

        assertEquals(2.45, result.easeFactor, 0.01)
        assertEquals(4, result.repetitions)
    }

    @Test
    fun `Easy does not increase easeFactor above maximum`() {
        val state = defaultState.copy(easeFactor = 2.5)
        val result = SrsService.review(state, SrsRating.Easy, now)

        assertEquals(2.5, result.easeFactor, 0.01)
    }

    @Test
    fun `Hard reduces easeFactor`() {
        val state = defaultState.copy(repetitions = 3, intervalDays = 15, easeFactor = 2.5)
        val result = SrsService.review(state, SrsRating.Hard, now)

        assertEquals(2.35, result.easeFactor, 0.01)
        assertEquals(4, result.repetitions)
    }

    @Test
    fun `Hard multiplies interval by hard multiplier`() {
        val state = defaultState.copy(repetitions = 3, intervalDays = 10, easeFactor = 2.5)
        val result = SrsService.review(state, SrsRating.Hard, now)

        assertEquals(12, result.intervalDays) // 10 * 1.2 = 12
    }

    @Test
    fun `rating does not mutate original state`() {
        val original = defaultState.copy(repetitions = 2, intervalDays = 6, easeFactor = 2.5)
        SrsService.review(original, SrsRating.Good, now)

        assertEquals(2, original.repetitions)
        assertEquals(6, original.intervalDays)
        assertEquals(2.5, original.easeFactor, 0.01)
    }

    @Test
    fun `lastReviewed is set to now`() {
        val result = SrsService.review(defaultState, SrsRating.Good, now)
        assertEquals(now, result.lastReviewed)
    }

    @Test
    fun `intervalLabel for 1 day`() {
        val state = defaultState.copy(repetitions = 3, intervalDays = 10)
        val label = SrsService.intervalLabel(state, SrsRating.Good)
        assertTrue(label.contains("day"))
    }

    @Test
    fun `intervalLabel for multiple days`() {
        val state = defaultState.copy(repetitions = 4, intervalDays = 60, easeFactor = 2.0)
        val label = SrsService.intervalLabel(state, SrsRating.Good)
        assertTrue(label.contains("month") || label.contains("day"))
    }

    @Test
    fun `intervalLabel for Again reflects the sub-day relearning step`() {
        val state = defaultState.copy(repetitions = 3, intervalDays = 10)
        val label = SrsService.intervalLabel(state, SrsRating.Again)
        assertEquals("< 1 day", label)
    }

    @Test
    fun `intervalLabel for first Good shows one day`() {
        val label = SrsService.intervalLabel(defaultState, SrsRating.Good)
        assertEquals("1 day", label)
    }
}
