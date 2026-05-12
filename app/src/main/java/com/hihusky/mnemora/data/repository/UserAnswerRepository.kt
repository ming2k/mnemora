package com.hihusky.mnemora.data.repository

import com.hihusky.mnemora.data.local.db.AppDatabase
import com.hihusky.mnemora.data.local.db.entity.UserAnswerEntity
import com.hihusky.mnemora.data.model.UserAnswer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAnswerRepository @Inject constructor(
    private val db: AppDatabase
) {
    suspend fun getUserAnswers(bookId: Int): Map<Int, UserAnswer> {
        return db.userAnswerDao().getByBookId(bookId).associate {
            it.questionId to UserAnswer(
                selected = it.selected,
                isCorrect = it.isCorrect?.let { v -> v == 1 },
                isMarked = it.isMarked == 1,
                timestamp = it.timestamp
            )
        }
    }

    suspend fun saveUserAnswer(bookId: Int, questionId: Int, answer: UserAnswer) {
        db.userAnswerDao().insert(
            UserAnswerEntity(
                questionId = questionId,
                bookId = bookId,
                selected = answer.selected,
                isCorrect = answer.isCorrect?.let { if (it) 1 else 0 },
                isMarked = if (answer.isMarked) 1 else 0,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun getMarkedQuestions(bookId: Int): Set<Int> {
        return db.userAnswerDao().getMarkedQuestionIds(bookId).toSet()
    }

    suspend fun setUserMark(bookId: Int, questionId: Int, isMarked: Boolean) {
        val existing = db.userAnswerDao().getByQuestionId(questionId)
        if (existing == null) {
            db.userAnswerDao().insert(
                UserAnswerEntity(
                    questionId = questionId,
                    bookId = bookId,
                    isMarked = if (isMarked) 1 else 0,
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            db.userAnswerDao().setMark(questionId, if (isMarked) 1 else 0, System.currentTimeMillis())
        }
    }

    suspend fun deleteUserAnswer(questionId: Int) {
        db.userAnswerDao().clearAnswer(questionId)
    }

    suspend fun clearBookProgress(bookId: Int) {
        db.userAnswerDao().deleteByBookId(bookId)
        db.srsReviewDao().deleteByBookId(bookId)
    }

    suspend fun getWrongQuestionIds(bookId: Int): List<Int> {
        return db.userAnswerDao().getWrongQuestionIds(bookId)
    }

    suspend fun getAnsweredQuestionIds(bookId: Int): List<Int> {
        return db.userAnswerDao().getAnsweredQuestionIds(bookId)
    }
}
