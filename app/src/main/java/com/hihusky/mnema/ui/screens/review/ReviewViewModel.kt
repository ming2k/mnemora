package com.hihusky.mnema.ui.screens.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnema.data.local.db.entity.SrsReviewEntity
import com.hihusky.mnema.data.local.db.entity.StudySessionEntity
import com.hihusky.mnema.data.model.Question
import com.hihusky.mnema.data.model.SrsRating
import com.hihusky.mnema.data.model.SrsReviewState
import com.hihusky.mnema.data.model.SrsState
import com.hihusky.mnema.data.repository.DatabaseRepository
import com.hihusky.mnema.domain.service.SrsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dbRepository: DatabaseRepository
) : ViewModel() {

    private val bookId: Int = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private var currentSessionId: Long = -1L

    init {
        loadDueQuestions()
    }

    private fun loadDueQuestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val dueIds = dbRepository.getSrsDueQuestionIds(bookId)
                val questions = dbRepository.getQuestionsByIds(dueIds)
                val reviews = dbRepository.getSrsReviews(bookId)
                val reviewMap = reviews.associateBy { it.questionId }

                val session = dbRepository.getActiveSession(bookId, "Review")
                val startIndex = session?.currentIndex?.coerceIn(0, questions.size - 1) ?: 0
                currentSessionId = session?.id ?: -1L

                if (session == null && questions.isNotEmpty()) {
                    currentSessionId = dbRepository.saveSession(
                        StudySessionEntity(
                            bookId = bookId,
                            mode = "Review",
                            startTime = System.currentTimeMillis(),
                            lastActiveTime = System.currentTimeMillis(),
                            currentIndex = 0,
                            totalQuestions = questions.size
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        questions = questions,
                        reviewMap = reviewMap,
                        currentIndex = startIndex,
                        isLoading = false,
                        isComplete = questions.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun rateCurrent(rating: SrsRating) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        val reviewEntity = state.reviewMap[question.id]

        viewModelScope.launch {
            val srsState = reviewEntity?.toSrsState() ?: SrsState(
                questionId = question.id,
                bookId = bookId
            )
            val nextState = SrsService.review(srsState, rating)

            dbRepository.saveSrsReview(
                SrsReviewEntity(
                    questionId = question.id,
                    bookId = bookId,
                    intervalDays = nextState.intervalDays,
                    easeFactor = nextState.easeFactor,
                    repetitions = nextState.repetitions,
                    lapses = nextState.lapses,
                    dueDate = nextState.dueDate,
                    lastReviewed = nextState.lastReviewed,
                    reviewState = nextState.reviewState.ordinal
                )
            )

            val nextIndex = state.currentIndex + 1
            if (state.currentIndex < state.questions.size - 1) {
                _uiState.update { it.copy(currentIndex = nextIndex) }
                saveSessionProgress(nextIndex)
            } else {
                _uiState.update { it.copy(isComplete = true) }
                viewModelScope.launch {
                    if (currentSessionId > 0) {
                        dbRepository.updateSessionProgress(
                            sessionId = currentSessionId,
                            currentIndex = state.questions.size,
                            totalQuestions = state.questions.size,
                            isCompleted = true,
                            isActive = false
                        )
                    }
                }
            }
        }
    }

    private fun saveSessionProgress(index: Int) {
        viewModelScope.launch {
            if (currentSessionId > 0) {
                val state = _uiState.value
                dbRepository.updateSessionProgress(
                    sessionId = currentSessionId,
                    currentIndex = index,
                    totalQuestions = state.questions.size
                )
            }
        }
    }

    fun getIntervalLabel(rating: SrsRating): String {
        val state = _uiState.value
        val question = state.currentQuestion ?: return ""
        val reviewEntity = state.reviewMap[question.id]
        val srsState = reviewEntity?.toSrsState() ?: SrsState(
            questionId = question.id,
            bookId = bookId
        )
        return SrsService.intervalLabel(srsState, rating)
    }
}

private fun SrsReviewEntity.toSrsState(): SrsState {
    return SrsState(
        questionId = questionId,
        bookId = bookId,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        repetitions = repetitions,
        lapses = lapses,
        dueDate = dueDate,
        lastReviewed = lastReviewed,
        reviewState = SrsReviewState.entries.getOrElse(reviewState) { SrsReviewState.New }
    )
}

data class ReviewUiState(
    val questions: List<Question> = emptyList(),
    val reviewMap: Map<Int, SrsReviewEntity> = emptyMap(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val currentQuestion: Question? get() = questions.getOrNull(currentIndex)
    val totalQuestions: Int get() = questions.size
    val progress: Float get() = if (totalQuestions > 0) currentIndex.toFloat() / totalQuestions else 0f
}
