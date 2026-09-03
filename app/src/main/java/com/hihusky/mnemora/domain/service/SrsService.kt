package com.hihusky.mnemora.domain.service

import com.hihusky.mnemora.data.model.SrsRating
import com.hihusky.mnemora.data.model.SrsReviewState
import com.hihusky.mnemora.data.model.SrsState

object SrsService {
    private const val MIN_EASE = 1.3
    private const val MAX_EASE = 2.5
    private const val AGAIN_EASE_DELTA = 0.2
    private const val HARD_EASE_DELTA = 0.15
    private const val EASY_EASE_DELTA = 0.15
    private const val HARD_INTERVAL_MULTIPLIER = 1.2
    private const val EASY_INTERVAL_MULTIPLIER = 1.3

    private const val FIRST_INTERVAL_AGAIN = 1
    private const val FIRST_INTERVAL_GOOD = 1
    private const val FIRST_INTERVAL_EASY = 4
    private const val SECOND_INTERVAL_GOOD = 6

    private const val MINUTE_MS = 60_000L
    internal const val DAY_MS = 86_400_000L
    private const val MAX_INTERVAL_DAYS = 36500

    private const val DAYS_PER_MONTH = 30
    private const val DAYS_PER_YEAR = 365

    fun review(
        state: SrsState,
        rating: SrsRating,
        now: Long = System.currentTimeMillis(),
    ): SrsState =
        when (rating) {
            SrsRating.Again -> handleAgain(state, now)
            SrsRating.Hard -> handleHard(state, now)
            SrsRating.Good -> handleGood(state, now)
            SrsRating.Easy -> handleEasy(state, now)
        }

    private fun handleAgain(
        state: SrsState,
        now: Long,
    ): SrsState {
        val newEase = (state.easeFactor - AGAIN_EASE_DELTA).coerceIn(MIN_EASE, MAX_EASE)
        return state.copyWith(
            intervalDays = FIRST_INTERVAL_AGAIN,
            easeFactor = newEase,
            repetitions = 0,
            lapses = state.lapses + 1,
            reviewState = SrsReviewState.Relearning,
            dueDate = now + MINUTE_MS,
            lastReviewed = now,
        )
    }

    private fun handleHard(
        state: SrsState,
        now: Long,
    ): SrsState {
        val newEase = (state.easeFactor - HARD_EASE_DELTA).coerceIn(MIN_EASE, MAX_EASE)
        val newInterval =
            (state.intervalDays * HARD_INTERVAL_MULTIPLIER)
                .toInt()
                .coerceIn(1, MAX_INTERVAL_DAYS)
        return state.copyWith(
            intervalDays = newInterval,
            easeFactor = newEase,
            repetitions = state.repetitions + 1,
            reviewState = SrsReviewState.Review,
            dueDate = now + newInterval * DAY_MS,
            lastReviewed = now,
        )
    }

    private fun handleGood(
        state: SrsState,
        now: Long,
    ): SrsState {
        val newInterval =
            when (state.repetitions) {
                0 -> FIRST_INTERVAL_GOOD
                1 -> SECOND_INTERVAL_GOOD
                else -> (state.intervalDays * state.easeFactor).toInt()
            }.coerceIn(1, MAX_INTERVAL_DAYS)
        return state.copyWith(
            intervalDays = newInterval,
            easeFactor = state.easeFactor.coerceIn(MIN_EASE, MAX_EASE),
            repetitions = state.repetitions + 1,
            reviewState = SrsReviewState.Review,
            dueDate = now + newInterval * DAY_MS,
            lastReviewed = now,
        )
    }

    private fun handleEasy(
        state: SrsState,
        now: Long,
    ): SrsState {
        val newEase = (state.easeFactor + EASY_EASE_DELTA).coerceIn(MIN_EASE, MAX_EASE)
        val newInterval =
            when (state.repetitions) {
                0 -> FIRST_INTERVAL_EASY
                else -> (state.intervalDays * state.easeFactor * EASY_INTERVAL_MULTIPLIER).toInt()
            }.coerceIn(1, MAX_INTERVAL_DAYS)
        return state.copyWith(
            intervalDays = newInterval,
            easeFactor = newEase,
            repetitions = state.repetitions + 1,
            reviewState = SrsReviewState.Review,
            dueDate = now + newInterval * DAY_MS,
            lastReviewed = now,
        )
    }

    /**
     * Human-readable preview of the interval that `rating` would schedule,
     * derived from the actual due-date delta so sub-day relearning steps are
     * not reported as full days.
     */
    fun intervalLabel(
        state: SrsState,
        rating: SrsRating,
        now: Long = System.currentTimeMillis(),
    ): String {
        val dueDate = review(state, rating, now).dueDate ?: return "-"
        val dueInDays = (dueDate - now) / DAY_MS
        return when {
            dueInDays < 1 -> "< 1 day"
            dueInDays == 1L -> "1 day"
            dueInDays < DAYS_PER_MONTH -> "$dueInDays days"
            dueInDays < DAYS_PER_YEAR -> "${dueInDays / DAYS_PER_MONTH} months"
            else -> "${dueInDays / DAYS_PER_YEAR} years"
        }
    }
}
