package com.hihusky.mnemora.data.repository

import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.SrsReviewEntity
import com.hihusky.mnemora.data.model.SrsRating
import com.hihusky.mnemora.data.model.SrsReviewState
import com.hihusky.mnemora.data.model.SrsState
import com.hihusky.mnemora.data.model.SrsStats
import com.hihusky.mnemora.domain.service.SrsService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SrsRepository
    @Inject
    constructor(
        private val db: AppDatabase,
    ) {
        suspend fun getSrsReviews(bookId: Int): List<SrsReviewEntity> = db.srsReviewDao().getByBookId(bookId)

        suspend fun getSrsDueQuestionIds(
            bookId: Int,
            now: Long = System.currentTimeMillis(),
        ): List<Int> = db.srsReviewDao().getDueQuestionIds(bookId, now)

        suspend fun getSrsStats(
            bookId: Int,
            now: Long = System.currentTimeMillis(),
        ): SrsStats {
            val row = db.srsReviewDao().getStats(bookId, now)
            return SrsStats(
                total = row?.total ?: 0,
                newCards = row?.newCards ?: 0,
                learning = row?.learning ?: 0,
                review = row?.review ?: 0,
                dueToday = row?.dueToday ?: 0,
            )
        }

        suspend fun saveSrsReview(review: SrsReviewEntity) {
            db.srsReviewDao().insert(review)
        }

        /**
         * Advances (or initializes) the SRS state of a question with the given
         * rating and persists the result. This is the single write path that
         * keeps the `srs_reviews` table alive; without it the `srs_due` filter
         * and book statistics would always see an empty table.
         */
        suspend fun applyRating(
            bookId: Int,
            questionId: Int,
            rating: SrsRating,
            now: Long = System.currentTimeMillis(),
        ) {
            val state =
                db.srsReviewDao().getByQuestionId(questionId)?.toSrsState()
                    ?: SrsState(questionId = questionId, bookId = bookId)
            db.srsReviewDao().insert(SrsService.review(state, rating, now).toEntity())
        }

        suspend fun resetBookProgress(bookId: Int) {
            db.srsReviewDao().deleteByBookId(bookId)
        }
    }

private fun SrsReviewEntity.toSrsState(): SrsState =
    SrsState(
        questionId = questionId,
        bookId = bookId,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        repetitions = repetitions,
        lapses = lapses,
        dueDate = dueDate,
        lastReviewed = lastReviewed,
        reviewState = SrsReviewState.entries.getOrElse(reviewState) { SrsReviewState.New },
    )

private fun SrsState.toEntity(): SrsReviewEntity =
    SrsReviewEntity(
        questionId = questionId,
        bookId = bookId,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        repetitions = repetitions,
        lapses = lapses,
        dueDate = dueDate,
        lastReviewed = lastReviewed,
        reviewState = reviewState.ordinal,
    )
