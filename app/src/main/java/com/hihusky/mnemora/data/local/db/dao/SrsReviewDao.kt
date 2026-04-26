package com.hihusky.mnemora.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hihusky.mnemora.data.local.db.entity.SrsReviewEntity

@Dao
interface SrsReviewDao {
    @Query("SELECT * FROM srs_reviews WHERE bookId = :bookId")
    suspend fun getByBookId(bookId: Int): List<SrsReviewEntity>

    @Query("SELECT * FROM srs_reviews WHERE bookId = :bookId AND dueDate <= :now")
    suspend fun getDueByBookId(bookId: Int, now: Long): List<SrsReviewEntity>

    @Query("SELECT questionId FROM srs_reviews WHERE bookId = :bookId AND dueDate <= :now")
    suspend fun getDueQuestionIds(bookId: Int, now: Long): List<Int>

    @Query("SELECT * FROM srs_reviews WHERE questionId = :questionId")
    suspend fun getByQuestionId(questionId: Int): SrsReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: SrsReviewEntity)

    @Update
    suspend fun update(review: SrsReviewEntity)

    @Query("DELETE FROM srs_reviews WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: Int)

    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN reviewState = 0 THEN 1 ELSE 0 END) as newCards,
            SUM(CASE WHEN reviewState IN (1, 3) THEN 1 ELSE 0 END) as learning,
            SUM(CASE WHEN reviewState = 2 THEN 1 ELSE 0 END) as review,
            SUM(CASE WHEN dueDate <= :now THEN 1 ELSE 0 END) as dueToday
        FROM srs_reviews WHERE bookId = :bookId
    """)
    suspend fun getStats(bookId: Int, now: Long): SrsStatsRow?
}

data class SrsStatsRow(
    val total: Int = 0,
    val newCards: Int = 0,
    val learning: Int = 0,
    val review: Int = 0,
    val dueToday: Int = 0
)
