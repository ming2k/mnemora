package com.hihusky.mnema.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hihusky.mnema.data.local.db.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions WHERE bookId = :bookId AND mode = :mode AND isActive = 1 LIMIT 1")
    suspend fun getActiveSession(bookId: Int, mode: String): StudySessionEntity?

    @Query("SELECT * FROM study_sessions WHERE bookId = :bookId AND isActive = 1 ORDER BY lastActiveTime DESC LIMIT 1")
    suspend fun getMostRecentActiveSession(bookId: Int): StudySessionEntity?

    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE bookId = :bookId ORDER BY startTime DESC")
    fun getSessionsByBook(bookId: Int): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE bookId = :bookId ORDER BY startTime DESC")
    suspend fun getSessionsByBookOnce(bookId: Int): List<StudySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySessionEntity): Long

    @Update
    suspend fun update(session: StudySessionEntity)

    @Query("UPDATE study_sessions SET isActive = 0 WHERE bookId = :bookId AND mode = :mode AND isActive = 1")
    suspend fun deactivateSessions(bookId: Int, mode: String)

    @Query("UPDATE study_sessions SET isActive = 0 WHERE id = :sessionId")
    suspend fun deactivateSession(sessionId: Long)

    @Query("""
        UPDATE study_sessions 
        SET currentIndex = :currentIndex, 
            lastActiveTime = :lastActiveTime, 
            totalQuestions = :totalQuestions,
            isCompleted = :isCompleted,
            isActive = :isActive
        WHERE id = :sessionId
    """)
    suspend fun updateProgress(
        sessionId: Long,
        currentIndex: Int,
        lastActiveTime: Long,
        totalQuestions: Int,
        isCompleted: Boolean,
        isActive: Boolean
    )

    @Query("DELETE FROM study_sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: Long)

    @Query("SELECT * FROM study_sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: Long): StudySessionEntity?
}
