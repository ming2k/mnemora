package com.hihusky.mnema.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hihusky.mnema.data.local.db.entity.QuestionPoolEntity

@Dao
interface QuestionPoolDao {
    @Query("SELECT * FROM question_pool WHERE id = :id")
    suspend fun getById(id: Int): QuestionPoolEntity?

    @Query("SELECT * FROM question_pool WHERE sourceBookId = :bookId AND sourceQuestionId = :questionId")
    suspend fun getBySource(bookId: Int, questionId: Int): QuestionPoolEntity?

    @Query("SELECT * FROM question_pool WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<QuestionPoolEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: QuestionPoolEntity): Long

    @Query("DELETE FROM question_pool WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM question_pool WHERE sourceBookId = :bookId")
    suspend fun deleteByBookId(bookId: Int)
}
