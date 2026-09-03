package com.hihusky.mnemora.data.repository

import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudySessionRepository
    @Inject
    constructor(
        private val db: AppDatabase,
    ) {
        suspend fun getActiveSession(
            bookId: Int,
            mode: String,
        ): StudySessionEntity? = db.studySessionDao().getActiveSession(bookId, mode)

        suspend fun getMostRecentActiveSession(bookId: Int): StudySessionEntity? =
            db.studySessionDao().getMostRecentActiveSession(bookId)

        suspend fun getActiveSessionsForBooks(bookIds: List<Int>): Map<Int, StudySessionEntity> {
            if (bookIds.isEmpty()) return emptyMap()
            return db
                .studySessionDao()
                .getActiveSessionsForBooks(bookIds)
                .groupBy { it.bookId }
                .mapValues { it.value.first() }
        }

        suspend fun getActiveSessionsPerMode(bookIds: List<Int>): Map<Int, Map<String, StudySessionEntity>> {
            if (bookIds.isEmpty()) return emptyMap()
            return db
                .studySessionDao()
                .getActiveSessionsForBooks(bookIds)
                .groupBy { it.bookId }
                .mapValues { (_, sessions) ->
                    sessions.groupBy { it.mode }.mapValues { it.value.first() }
                }
        }

        fun getAllSessions(): Flow<List<StudySessionEntity>> = db.studySessionDao().getAllSessions()

        fun getSessionsByBook(bookId: Int): Flow<List<StudySessionEntity>> =
            db.studySessionDao().getSessionsByBook(bookId)

        suspend fun getSessionsByBookOnce(bookId: Int): List<StudySessionEntity> =
            db.studySessionDao().getSessionsByBookOnce(bookId)

        suspend fun saveSession(session: StudySessionEntity): Long = db.studySessionDao().insert(session)

        suspend fun updateSession(session: StudySessionEntity) {
            db.studySessionDao().update(session)
        }

        suspend fun updateSessionProgress(
            sessionId: Long,
            currentIndex: Int,
            totalQuestions: Int,
            isCompleted: Boolean = false,
            isActive: Boolean = true,
        ) {
            db.studySessionDao().updateProgress(
                sessionId = sessionId,
                currentIndex = currentIndex,
                lastActiveTime = System.currentTimeMillis(),
                totalQuestions = totalQuestions,
                isCompleted = isCompleted,
                isActive = isActive,
            )
        }

        suspend fun deactivateSessions(
            bookId: Int,
            mode: String,
        ) {
            db.studySessionDao().deactivateSessions(bookId, mode)
        }

        suspend fun deactivateSession(sessionId: Long) {
            db.studySessionDao().deactivateSession(sessionId)
        }

        suspend fun deleteSession(sessionId: Long) {
            db.studySessionDao().deleteById(sessionId)
        }

        suspend fun getSessionById(sessionId: Long): StudySessionEntity? = db.studySessionDao().getById(sessionId)

        suspend fun clearBookSessions(bookId: Int) {
            db.studySessionDao().deleteByBookId(bookId)
        }
    }
