package com.hihusky.mnemora.data.repository

import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.SrsReviewEntity
import com.hihusky.mnemora.data.model.SrsStats
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SrsRepository @Inject constructor(
    private val db: AppDatabase
) {
    suspend fun getSrsReviews(bookId: Int): List<SrsReviewEntity> {
        return db.srsReviewDao().getByBookId(bookId)
    }

    suspend fun getSrsDueQuestionIds(bookId: Int, now: Long = System.currentTimeMillis()): List<Int> {
        return db.srsReviewDao().getDueQuestionIds(bookId, now)
    }

    suspend fun getSrsStats(bookId: Int, now: Long = System.currentTimeMillis()): SrsStats {
        val row = db.srsReviewDao().getStats(bookId, now)
        return SrsStats(
            total = row?.total ?: 0,
            newCards = row?.newCards ?: 0,
            learning = row?.learning ?: 0,
            review = row?.review ?: 0,
            dueToday = row?.dueToday ?: 0
        )
    }

    suspend fun saveSrsReview(review: SrsReviewEntity) {
        db.srsReviewDao().insert(review)
    }
}
