package com.hihusky.mnemora.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hihusky.mnemora.data.local.db.entity.ChatHistoryEntity
import com.hihusky.mnemora.data.local.db.entity.ChatSessionEntity

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM ai_chat_sessions WHERE questionId = :questionId ORDER BY createdAt DESC")
    suspend fun getByQuestionId(questionId: Int): List<ChatSessionEntity>

    @Query("SELECT * FROM ai_chat_sessions WHERE id = :id")
    suspend fun getById(id: Int): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ChatSessionEntity): Long

    @Update
    suspend fun update(session: ChatSessionEntity)

    @Query("DELETE FROM ai_chat_sessions WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface ChatHistoryDao {
    @Query("SELECT * FROM ai_chat_history WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySessionId(sessionId: Int): List<ChatHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatHistoryEntity): Long

    @Query("DELETE FROM ai_chat_history WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Int)
}
