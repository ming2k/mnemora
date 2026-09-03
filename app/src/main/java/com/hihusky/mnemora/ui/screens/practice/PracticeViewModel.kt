package com.hihusky.mnemora.ui.screens.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnemora.data.model.Book
import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.data.model.ChatScrollPosition
import com.hihusky.mnemora.data.model.Node
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.QuestionStatus
import com.hihusky.mnemora.data.model.UserAnswer
import com.hihusky.mnemora.data.repository.SettingsRepository
import com.hihusky.mnemora.domain.service.AiService
import com.hihusky.mnemora.domain.service.FeedbackService
import com.hihusky.mnemora.domain.usecase.practice.AiChatUseCase
import com.hihusky.mnemora.domain.usecase.practice.LoadPracticeSessionUseCase
import com.hihusky.mnemora.domain.usecase.practice.ManageCollectionUseCase
import com.hihusky.mnemora.domain.usecase.practice.ManageProgressUseCase
import com.hihusky.mnemora.domain.usecase.practice.SubmitAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CONFETTI_CLEAR_DELAY_MS = 2800L
private const val AUTO_ADVANCE_DELAY_MS = 1000L

@HiltViewModel
class PracticeViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val loadPracticeSessionUseCase: LoadPracticeSessionUseCase,
        private val submitAnswerUseCase: SubmitAnswerUseCase,
        private val manageCollectionUseCase: ManageCollectionUseCase,
        private val manageProgressUseCase: ManageProgressUseCase,
        aiChatUseCase: AiChatUseCase,
        private val settingsRepository: SettingsRepository,
        aiService: AiService,
        private val feedbackService: FeedbackService,
    ) : ViewModel() {
        private val navBookId: Int = checkNotNull(savedStateHandle["bookId"])
        private val initialNodeId: String = savedStateHandle["nodeId"] ?: ""
        private val collectionId: Int = savedStateHandle["collectionId"] ?: -1
        private val filter: String = savedStateHandle["filter"] ?: ""
        private val mode: String = savedStateHandle["mode"] ?: "Practice"

        private val _uiState = MutableStateFlow(PracticeUiState(isPreviewMode = mode == "Preview"))
        val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

        private val chatController = AiChatController(viewModelScope, aiChatUseCase, aiService)
        val chatState: StateFlow<AiChatUiState> get() = chatController.state

        var imageBasePath: String? = null
            private set

        private var confettiJob: Job? = null
        private var saveProgressJob: Job? = null
        private var currentSessionId: Long = -1L
        private var effectiveBookId: Int = navBookId

        init {
            observePreferences()
            loadBook()
        }

        // ── Collections ──────────────────────────────────────────────────

        fun loadCollectionData() {
            val question = _uiState.value.currentQuestion ?: return
            viewModelScope.launch {
                try {
                    val collections = manageCollectionUseCase.getAvailableCollections(question.bookId)
                    val questionCollectionIds =
                        manageCollectionUseCase.getQuestionCollectionIds(
                            question.bookId,
                            question.id,
                        )
                    _uiState.update {
                        it.copy(
                            availableCollections = collections,
                            questionCollectionIds = questionCollectionIds,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        fun toggleQuestionInCollection(collectionId: Int) {
            val question = _uiState.value.currentQuestion ?: return
            val isIn = collectionId in _uiState.value.questionCollectionIds
            _uiState.update { state ->
                val updated =
                    if (isIn) {
                        state.questionCollectionIds - collectionId
                    } else {
                        state.questionCollectionIds +
                            collectionId
                    }
                state.copy(questionCollectionIds = updated)
            }
            viewModelScope.launch {
                try {
                    manageCollectionUseCase.toggleQuestionInCollection(collectionId, question.id, isIn)
                } catch (e: Exception) {
                    // Roll back the optimistic update and surface the failure.
                    _uiState.update { state ->
                        val reverted =
                            if (isIn) {
                                state.questionCollectionIds + collectionId
                            } else {
                                state.questionCollectionIds -
                                    collectionId
                            }
                        state.copy(questionCollectionIds = reverted, error = e.message)
                    }
                }
            }
        }

        fun createCollection(name: String) {
            viewModelScope.launch {
                try {
                    manageCollectionUseCase.createCollection(effectiveBookId, name)
                    loadCollectionData()
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        fun deleteCollection(collectionId: Int) {
            viewModelScope.launch {
                try {
                    manageCollectionUseCase.deleteCollection(collectionId)
                    _uiState.update { state ->
                        state.copy(
                            availableCollections = state.availableCollections.filter { it.id != collectionId },
                            questionCollectionIds = state.questionCollectionIds - collectionId,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        // ── Preferences ──────────────────────────────────────────────────

        private fun observePreferences() {
            viewModelScope.launch {
                settingsRepository.showPracticeProgress.collect { showProgress ->
                    _uiState.update { it.copy(showProgressBar = showProgress) }
                }
            }
            viewModelScope.launch {
                settingsRepository.soundEffects.collect { enabled ->
                    feedbackService.soundEnabled = enabled
                }
            }
            viewModelScope.launch {
                settingsRepository.hapticFeedback.collect { enabled ->
                    feedbackService.hapticEnabled = enabled
                }
            }
            viewModelScope.launch {
                settingsRepository.continuousFeedback.collect { enabled ->
                    feedbackService.continuousFeedback = enabled
                }
            }
            viewModelScope.launch {
                settingsRepository.confettiEffect.collect { enabled ->
                    _uiState.update { it.copy(confettiEnabled = enabled) }
                }
            }
            viewModelScope.launch {
                settingsRepository.autoAdvance.collect { enabled ->
                    _uiState.update { it.copy(autoAdvance = enabled) }
                }
            }
        }

        // ── Session lifecycle ────────────────────────────────────────────

        private fun loadBook() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    val data =
                        loadPracticeSessionUseCase(
                            navBookId = navBookId,
                            collectionId = collectionId,
                            filter = filter,
                            initialNodeId = initialNodeId,
                            mode = mode,
                        )

                    effectiveBookId = data.effectiveBookId
                    currentSessionId = data.sessionId
                    imageBasePath = data.imageBasePath

                    _uiState.update {
                        it.copy(
                            book = data.book,
                            nodes = data.nodes,
                            questions = data.questions,
                            userAnswers = data.userAnswers,
                            markedQuestions = data.markedQuestions,
                            currentIndex = data.currentIndex,
                            currentPartitionId = data.currentPartitionId,
                            isLoading = false,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
            }
        }

        fun selectNode(nodeId: String) {
            viewModelScope.launch {
                try {
                    val data =
                        loadPracticeSessionUseCase(
                            navBookId = effectiveBookId,
                            collectionId = -1,
                            filter = "",
                            initialNodeId = if (nodeId == "all") "" else nodeId,
                            mode = mode,
                        )
                    _uiState.update {
                        it.copy(
                            questions = data.questions,
                            currentIndex = 0,
                            currentPartitionId = nodeId,
                        )
                    }
                    saveSessionProgress(0, data.questions.size)
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        // ── Navigation ───────────────────────────────────────────────────

        fun goToQuestion(index: Int) {
            if (index < 0 || index >= _uiState.value.questions.size) return
            _uiState.update { it.copy(currentIndex = index) }
            saveSessionProgress(index, debounceMs = 300L)
        }

        private fun nextQuestion() {
            goToQuestion(_uiState.value.currentIndex + 1)
        }

        // ── Answering ────────────────────────────────────────────────────

        fun answerQuestion(option: String) {
            val state = _uiState.value
            val question = state.currentQuestion ?: return

            viewModelScope.launch {
                val answer = submitAnswerUseCase(effectiveBookId, question, option)

                _uiState.update {
                    it.copy(
                        userAnswers = it.userAnswers.toMutableMap().apply { put(question.id, answer) },
                    )
                }

                if (answer.isCorrect == true) {
                    if (_uiState.value.confettiEnabled) {
                        _uiState.update { it.copy(confettiId = System.currentTimeMillis()) }
                        confettiJob?.cancel()
                        confettiJob =
                            launch {
                                delay(CONFETTI_CLEAR_DELAY_MS)
                                _uiState.update { it.copy(confettiId = 0L) }
                            }
                    }
                }

                if (answer.isCorrect == true && state.autoAdvance && state.currentIndex < state.questions.size - 1) {
                    delay(AUTO_ADVANCE_DELAY_MS)
                    nextQuestion()
                }
            }
        }

        // ── Progress management ──────────────────────────────────────────

        fun toggleMark() {
            val question = _uiState.value.currentQuestion ?: return
            val isMarked = !_uiState.value.markedQuestions.contains(question.id)
            viewModelScope.launch {
                manageProgressUseCase.toggleMark(effectiveBookId, question.id, isMarked)
                _uiState.update {
                    val marks = it.markedQuestions.toMutableSet()
                    if (isMarked) marks.add(question.id) else marks.remove(question.id)
                    it.copy(markedQuestions = marks)
                }
            }
        }

        fun resetCurrentQuestion() {
            val question = _uiState.value.currentQuestion ?: return
            viewModelScope.launch {
                manageProgressUseCase.resetCurrentQuestion(question.id)
                _uiState.update {
                    it.copy(
                        userAnswers = it.userAnswers.toMutableMap().apply { remove(question.id) },
                    )
                }
            }
        }

        fun resetAllProgress() {
            viewModelScope.launch {
                manageProgressUseCase.resetAllProgress(effectiveBookId, currentSessionId)
                currentSessionId = -1L
                _uiState.update {
                    it.copy(
                        userAnswers = emptyMap(),
                        markedQuestions = emptySet(),
                        currentIndex = 0,
                    )
                }
            }
        }

        private fun saveSessionProgress(
            index: Int,
            total: Int? = null,
            debounceMs: Long = 0L,
        ) {
            saveProgressJob?.cancel()
            saveProgressJob =
                viewModelScope.launch {
                    if (debounceMs > 0) delay(debounceMs)
                    val state = _uiState.value
                    manageProgressUseCase.saveSessionProgress(
                        sessionId = currentSessionId,
                        currentIndex = index,
                        totalQuestions = total ?: state.questions.size,
                    )
                }
        }

        // ── AI Chat (delegates to AiChatController) ──────────────────────

        fun chatLoadHistory() = withCurrentQuestion { chatController.loadHistory(it) }

        fun chatCreateSession(title: String = "New Chat") =
            withCurrentQuestion { chatController.createSession(it, title) }

        fun chatSwitchSession(sessionId: Int) {
            chatController.switchSession(sessionId)
        }

        fun chatSaveScrollPosition(
            sessionId: Int,
            position: ChatScrollPosition,
        ) {
            chatController.saveScrollPosition(sessionId, position)
        }

        fun chatDeleteSession() = withCurrentQuestion { chatController.deleteSession(it) }

        fun chatSendMessage(message: String) = withCurrentQuestion { chatController.sendMessage(it, message) }

        fun chatContinueMessage(targetMessage: ChatMessage) =
            withCurrentQuestion { chatController.continueMessage(it, targetMessage) }

        fun chatRegenerateMessage(targetMessage: ChatMessage) =
            withCurrentQuestion { chatController.regenerateMessage(it, targetMessage) }

        fun chatCancel() {
            chatController.cancel()
        }

        private fun withCurrentQuestion(block: (Question) -> Unit) {
            _uiState.value.currentQuestion?.let(block)
        }

        // ── Misc ─────────────────────────────────────────────────────────

        fun clearConfetti() {
            confettiJob?.cancel()
            _uiState.update { it.copy(confettiId = 0L) }
        }

        fun getQuestionStatus(index: Int): QuestionStatus {
            val state = _uiState.value
            if (index < 0 || index >= state.questions.size) return QuestionStatus.Unanswered
            val q = state.questions[index]
            if (state.markedQuestions.contains(q.id)) return QuestionStatus.Marked
            val answer = state.userAnswers[q.id]
            if (answer == null) return QuestionStatus.Unanswered
            return if (answer.isCorrect == true) QuestionStatus.Correct else QuestionStatus.Wrong
        }
    }

// ── State definitions ────────────────────────────────────────────────

data class PracticeUiState(
    val book: Book? = null,
    val nodes: List<Node> = emptyList(),
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val currentPartitionId: String = "all",
    val userAnswers: Map<Int, UserAnswer> = emptyMap(),
    val markedQuestions: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val availableCollections: List<com.hihusky.mnemora.data.model.Collection> = emptyList(),
    val questionCollectionIds: Set<Int> = emptySet(),
    val showProgressBar: Boolean = true,
    val confettiEnabled: Boolean = true,
    val confettiId: Long = 0L,
    val autoAdvance: Boolean = true,
    val isPreviewMode: Boolean = false,
) {
    val currentQuestion: Question?
        get() = questions.getOrNull(currentIndex)
    val currentUserAnswer: UserAnswer?
        get() = currentQuestion?.let { userAnswers[it.id] }
    val isCurrentMarked: Boolean
        get() = currentQuestion?.let { markedQuestions.contains(it.id) } ?: false
    val totalQuestions: Int get() = questions.size
    val progress: Float
        get() = if (totalQuestions > 0) (currentIndex + 1).toFloat() / totalQuestions else 0f
}
