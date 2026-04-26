package com.hihusky.mnema.data.model

enum class SrsRating {
    Again, Hard, Good, Easy
}

enum class SrsReviewState {
    New, Learning, Review, Relearning
}

data class SrsState(
    val questionId: Int,
    val bookId: Int,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val dueDate: Long? = null,
    val lastReviewed: Long? = null,
    val reviewState: SrsReviewState = SrsReviewState.New
) {
    fun copyWith(
        intervalDays: Int = this.intervalDays,
        easeFactor: Double = this.easeFactor,
        repetitions: Int = this.repetitions,
        lapses: Int = this.lapses,
        dueDate: Long? = this.dueDate,
        lastReviewed: Long? = this.lastReviewed,
        reviewState: SrsReviewState = this.reviewState
    ): SrsState {
        return SrsState(
            questionId = this.questionId,
            bookId = this.bookId,
            intervalDays = intervalDays,
            easeFactor = easeFactor,
            repetitions = repetitions,
            lapses = lapses,
            dueDate = dueDate,
            lastReviewed = lastReviewed,
            reviewState = reviewState
        )
    }
}

data class SrsStats(
    val total: Int = 0,
    val newCards: Int = 0,
    val learning: Int = 0,
    val review: Int = 0,
    val dueToday: Int = 0
)
