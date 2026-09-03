package com.hihusky.mnemora.domain.usecase.practice

import com.hihusky.mnemora.data.repository.SrsRepository
import com.hihusky.mnemora.data.repository.StudySessionRepository
import com.hihusky.mnemora.data.repository.UserAnswerRepository
import javax.inject.Inject

class ManageProgressUseCase
    @Inject
    constructor(
        private val userAnswerRepository: UserAnswerRepository,
        private val studySessionRepository: StudySessionRepository,
        private val srsRepository: SrsRepository,
    ) {
        suspend fun saveSessionProgress(
            sessionId: Long,
            currentIndex: Int,
            totalQuestions: Int,
        ) {
            if (sessionId > 0) {
                studySessionRepository.updateSessionProgress(sessionId, currentIndex, totalQuestions)
            }
        }

        suspend fun toggleMark(
            bookId: Int,
            questionId: Int,
            isMarked: Boolean,
        ) {
            userAnswerRepository.setUserMark(bookId, questionId, isMarked)
        }

        suspend fun resetCurrentQuestion(questionId: Int) {
            userAnswerRepository.deleteUserAnswer(questionId)
        }

        suspend fun resetAllProgress(
            bookId: Int,
            currentSessionId: Long,
        ) {
            userAnswerRepository.clearBookProgress(bookId)
            srsRepository.resetBookProgress(bookId)
            if (currentSessionId > 0) {
                studySessionRepository.deactivateSession(currentSessionId)
            }
        }
    }
