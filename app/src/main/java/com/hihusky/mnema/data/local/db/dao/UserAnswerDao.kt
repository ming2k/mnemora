package com.hihusky.mnema.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hihusky.mnema.data.local.db.entity.UserAnswerEntity

@Dao
interface UserAnswerDao {
    @Query("SELECT * FROM user_answers WHERE bookId = :bookId")
    suspend fun getByBookId(bookId: Int): List<UserAnswerEntity>

    @Query("SELECT questionId FROM user_answers WHERE bookId = :bookId AND isMarked = 1")
    suspend fun getMarkedQuestionIds(bookId: Int): List<Int>

    @Query("SELECT questionId FROM user_answers WHERE bookId = :bookId AND isCorrect = 0")
    suspend fun getWrongQuestionIds(bookId: Int): List<Int>

    @Query("SELECT questionId FROM user_answers WHERE bookId = :bookId")
    suspend fun getAnsweredQuestionIds(bookId: Int): List<Int>

    @Query("SELECT * FROM user_answers WHERE questionId = :questionId")
    suspend fun getByQuestionId(questionId: Int): UserAnswerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(answer: UserAnswerEntity)

    @Update
    suspend fun update(answer: UserAnswerEntity)

    @Query("UPDATE user_answers SET selected = NULL, isCorrect = NULL WHERE questionId = :questionId")
    suspend fun clearAnswer(questionId: Int)

    @Query("UPDATE user_answers SET isMarked = :isMarked, timestamp = :timestamp WHERE questionId = :questionId")
    suspend fun setMark(questionId: Int, isMarked: Int, timestamp: Long)

    @Query("DELETE FROM user_answers WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: Int)
}
