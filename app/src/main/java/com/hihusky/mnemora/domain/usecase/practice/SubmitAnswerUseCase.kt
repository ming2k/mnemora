package com.hihusky.mnemora.domain.usecase.practice

import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.SrsRating
import com.hihusky.mnemora.data.model.UserAnswer
import com.hihusky.mnemora.data.repository.SrsRepository
import com.hihusky.mnemora.data.repository.UserAnswerRepository
import com.hihusky.mnemora.domain.service.FeedbackService
import javax.inject.Inject

class SubmitAnswerUseCase
    @Inject
    constructor(
        private val userAnswerRepository: UserAnswerRepository,
        private val feedbackService: FeedbackService,
        private val srsRepository: SrsRepository,
    ) {
        suspend operator fun invoke(
            bookId: Int,
            question: Question,
            option: String,
        ): UserAnswer {
            val isCorrect = option.uppercase() == question.answer.uppercase()
            val answer = UserAnswer(selected = option, isCorrect = isCorrect)
            userAnswerRepository.saveUserAnswer(bookId, question.id, answer)

            // Every answered question feeds the spaced-repetition scheduler so
            // due-date filters and book statistics reflect real review history.
            srsRepository.applyRating(
                bookId = bookId,
                questionId = question.id,
                rating = if (isCorrect) SrsRating.Good else SrsRating.Again,
            )

            if (isCorrect) {
                feedbackService.incrementStreak()
                feedbackService.playCorrect()
            } else {
                feedbackService.resetStreak()
                feedbackService.playWrong()
            }

            return answer
        }
    }
