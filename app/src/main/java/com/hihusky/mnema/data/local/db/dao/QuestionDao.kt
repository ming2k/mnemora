package com.hihusky.mnema.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hihusky.mnema.data.local.db.entity.QuestionEntity

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE bookId = :bookId ORDER BY id")
    suspend fun getByBookId(bookId: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE nodeId = :nodeId ORDER BY id")
    suspend fun getByNodeId(nodeId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id IN (:ids) ORDER BY id")
    suspend fun getByIds(ids: List<Int>): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: Int): QuestionEntity?

    @Query("SELECT id FROM questions WHERE bookId = :bookId AND questionType <> 'passage' ORDER BY id ASC")
    suspend fun getAnswerableQuestionIds(bookId: Int): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>): List<Long>

    @Query("DELETE FROM questions WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: Int)
}
