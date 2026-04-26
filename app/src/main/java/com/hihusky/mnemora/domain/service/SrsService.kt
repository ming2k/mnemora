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

    fun review(state: SrsState, rating: SrsRating, now: Long = System.currentTimeMillis()): SrsState {
        return when (rating) {
            SrsRating.Again -> handleAgain(state, now)
            SrsRating.Hard -> handleHard(state, now)
            SrsRating.Good -> handleGood(state, now)
            SrsRating.Easy -> handleEasy(state, now)
        }
    }

    private fun handleAgain(state: SrsState, now: Long): SrsState {
        val newEase = (state.easeFactor - AGAIN_EASE_DELTA).coerceIn(MIN_EASE, MAX_EASE)
        return state.copyWith(
            intervalDays = FIRST_INTERVAL_AGAIN,
            easeFactor = newEase,
            repetitions = 0,
            lapses = state.lapses + 1,
            reviewState = SrsReviewState.Relearning,
            dueDate = now + 60_000, // 1 minute
            lastReviewed = now
        )
    }

    private fun handleHard(state: SrsState, now: Long): SrsState {
        val newEase = (state.easeFactor - HARD_EASE_DELTA).coerceIn(MIN_EASE, MAX_EASE)
        val newInterval = (state.intervalDays * HARD_INTERVAL_MULTIPLIER).toInt()
            .coerceIn(1, 36500)
        return state.copyWith(
            intervalDays = newInterval,
            easeFactor = newEase,
            repetitions = state.repetitions + 1,
            reviewState = SrsReviewState.Review,
            dueDate = now + newInterval * 86400000L,
            lastReviewed = now
        )
    }

    private fun handleGood(state: SrsState, now: Long): SrsState {
        val newInterval = when (state.repetitions) {
            0 -> FIRST_INTERVAL_GOOD
            1 -> SECOND_INTERVAL_GOOD
            else -> (state.intervalDays * state.easeFactor).toInt()
        }.coerceIn(1, 36500)
        return state.copyWith(
            intervalDays = newInterval,
            easeFactor = state.easeFactor.coerceIn(MIN_EASE, MAX_EASE),
            repetitions = state.repetitions + 1,
            reviewState = SrsReviewState.Review,
            dueDate = now + newInterval * 86400000L,
            lastReviewed = now
        )
    }

    private fun handleEasy(state: SrsState, now: Long): SrsState {
        val newEase = (state.easeFactor + EASY_EASE_DELTA).coerceIn(MIN_EASE, MAX_EASE)
        val newInterval = when (state.repetitions) {
            0 -> FIRST_INTERVAL_EASY
            else -> (state.intervalDays * state.easeFactor * EASY_INTERVAL_MULTIPLIER).toInt()
        }.coerceIn(1, 36500)
        return state.copyWith(
            intervalDays = newInterval,
            easeFactor = newEase,
            repetitions = state.repetitions + 1,
            reviewState = SrsReviewState.Review,
            dueDate = now + newInterval * 86400000L,
            lastReviewed = now
        )
    }

    fun intervalLabel(state: SrsState, rating: SrsRating): String {
        val next = review(state, rating)
        val days = next.intervalDays
        return when {
            days == 0 -> "< 1 min"
            days == 1 -> "1 day"
            days < 30 -> "$days days"
            days < 365 -> "${(days / 30)} months"
            else -> "${(days / 365)} years"
        }
    }
}
