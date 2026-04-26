package com.hihusky.mnema.ui.screens.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnema.data.local.db.entity.StudySessionEntity
import com.hihusky.mnema.data.model.Question
import com.hihusky.mnema.data.model.UserAnswer
import com.hihusky.mnema.data.repository.DatabaseRepository
import com.hihusky.mnema.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dbRepository: DatabaseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val bookId: Int = checkNotNull(savedStateHandle["bookId"])
    private val resumeSessionId: Long = savedStateHandle["sessionId"] ?: -1L

    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null
    private var currentSessionId: Long = -1L

    init {
        loadTest()
    }

    private fun loadTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val allQuestions = dbRepository.getQuestions(bookId)
                    .filter { it.isAnswerable }
                    .shuffled()
                val count = settingsRepository.testQuestionCount.first()
                    .coerceAtMost(allQuestions.size)
                    .coerceAtLeast(1)
                val questions = allQuestions.take(count)

                val session = if (resumeSessionId > 0) {
                    dbRepository.getSessionById(resumeSessionId)
                } else null

                val startIndex = session?.currentIndex?.coerceIn(0, questions.size - 1) ?: 0
                currentSessionId = session?.id ?: dbRepository.saveSession(
                    StudySessionEntity(
                        bookId = bookId,
                        mode = "Test",
                        startTime = System.currentTimeMillis(),
                        lastActiveTime = System.currentTimeMillis(),
                        currentIndex = 0,
                        totalQuestions = questions.size
                    )
                )

                _uiState.update {
                    it.copy(
                        questions = questions,
                        currentIndex = startIndex,
                        isLoading = false,
                        isRunning = true
                    )
                }
                startTimer()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
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

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    fun answerQuestion(option: String) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        val isCorrect = option.uppercase() == question.answer.uppercase()
        val answer = UserAnswer(selected = option, isCorrect = isCorrect)

        _uiState.update {
            it.copy(
                userAnswers = it.userAnswers.toMutableMap().apply { put(question.id, answer) }
            )
        }
    }

    fun goToQuestion(index: Int) {
        if (index < 0 || index >= _uiState.value.questions.size) return
        _uiState.update { it.copy(currentIndex = index) }
        saveSessionProgress(index)
    }

    fun nextQuestion() {
        goToQuestion(_uiState.value.currentIndex + 1)
    }

    fun previousQuestion() {
        goToQuestion(_uiState.value.currentIndex - 1)
    }

    fun finishTest() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, showResults = true) }
        viewModelScope.launch {
            if (currentSessionId > 0) {
                dbRepository.updateSessionProgress(
                    sessionId = currentSessionId,
                    currentIndex = _uiState.value.currentIndex,
                    totalQuestions = _uiState.value.questions.size,
                    isCompleted = true,
                    isActive = false
                )
            }
        }
    }

    fun resetTest() {
        timerJob?.cancel()
        if (currentSessionId > 0) {
            viewModelScope.launch {
                dbRepository.deactivateSession(currentSessionId)
            }
        }
        _uiState.update { TestUiState() }
        loadTest()
    }
}

data class TestUiState(
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val userAnswers: Map<Int, UserAnswer> = emptyMap(),
    val elapsedSeconds: Int = 0,
    val isLoading: Boolean = false,
    val isRunning: Boolean = false,
    val showResults: Boolean = false,
    val error: String? = null
) {
    val currentQuestion: Question? get() = questions.getOrNull(currentIndex)
    val totalQuestions: Int get() = questions.size
    val correctCount: Int get() = userAnswers.count { it.value.isCorrect == true }
    val wrongCount: Int get() = userAnswers.count { it.value.isCorrect == false }
    val unansweredCount: Int get() = totalQuestions - userAnswers.size
    val progress: Float get() = if (totalQuestions > 0) (currentIndex + 1).toFloat() / totalQuestions else 0f
    val formattedTime: String get() {
        val m = elapsedSeconds / 60
        val s = elapsedSeconds % 60
        return "%02d:%02d".format(m, s)
    }
}
