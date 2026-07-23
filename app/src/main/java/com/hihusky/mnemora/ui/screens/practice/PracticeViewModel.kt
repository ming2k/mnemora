package com.hihusky.mnemora.ui.screens.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnemora.data.model.Book
import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.data.model.ChatSession
import com.hihusky.mnemora.data.model.Node
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.QuestionStatus
import com.hihusky.mnemora.data.model.UserAnswer
import com.hihusky.mnemora.data.repository.SettingsRepository
import com.hihusky.mnemora.domain.service.AiProviderCatalog
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

@HiltViewModel
class PracticeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadPracticeSessionUseCase: LoadPracticeSessionUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val manageCollectionUseCase: ManageCollectionUseCase,
    private val manageProgressUseCase: ManageProgressUseCase,
    private val aiChatUseCase: AiChatUseCase,
    private val settingsRepository: SettingsRepository,
    private val aiService: AiService,
    private val feedbackService: FeedbackService
) : ViewModel() {

    private val navBookId: Int = checkNotNull(savedStateHandle["bookId"])
    private val initialNodeId: String = savedStateHandle["nodeId"] ?: ""
    private val collectionId: Int = savedStateHandle["collectionId"] ?: -1
    private val filter: String = savedStateHandle["filter"] ?: ""
    private val mode: String = savedStateHandle["mode"] ?: "Practice"

    private val _uiState = MutableStateFlow(PracticeUiState(isPreviewMode = mode == "Preview"))
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    var imageBasePath: String? = null
        private set

    private val chatJobs = mutableMapOf<Int, Job>()
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
                val questionCollectionIds = manageCollectionUseCase.getQuestionCollectionIds(
                    question.bookId, question.id
                )
                _uiState.update {
                    it.copy(
                        availableCollections = collections,
                        questionCollectionIds = questionCollectionIds
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun toggleQuestionInCollection(collectionId: Int) {
        val question = _uiState.value.currentQuestion ?: return
        val isIn = collectionId in _uiState.value.questionCollectionIds
        _uiState.update { state ->
            val updated = if (isIn) state.questionCollectionIds - collectionId else state.questionCollectionIds + collectionId
            state.copy(questionCollectionIds = updated)
        }
        viewModelScope.launch {
            try {
                manageCollectionUseCase.toggleQuestionInCollection(collectionId, question.id, isIn)
            } catch (_: Exception) {
                _uiState.update { state ->
                    val reverted = if (isIn) state.questionCollectionIds + collectionId else state.questionCollectionIds - collectionId
                    state.copy(questionCollectionIds = reverted)
                }
            }
        }
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            try {
                manageCollectionUseCase.createCollection(effectiveBookId, name)
                loadCollectionData()
            } catch (_: Exception) {}
        }
    }

    fun deleteCollection(collectionId: Int) {
        viewModelScope.launch {
            try {
                manageCollectionUseCase.deleteCollection(collectionId)
                _uiState.update { state ->
                    state.copy(
                        availableCollections = state.availableCollections.filter { it.id != collectionId },
                        questionCollectionIds = state.questionCollectionIds - collectionId
                    )
                }
            } catch (_: Exception) {}
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
        viewModelScope.launch {
            aiService.config.collect { config ->
                _uiState.update {
                    it.copy(
                        aiModel = AiProviderCatalog.modelDisplayFor(config.provider, config.model),
                        aiProvider = AiProviderCatalog.displayFor(config.provider),
                    )
                }
            }
        }
    }

    // ── Session lifecycle ────────────────────────────────────────────

    private fun loadBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val data = loadPracticeSessionUseCase(
                    navBookId = navBookId,
                    collectionId = collectionId,
                    filter = filter,
                    initialNodeId = initialNodeId,
                    mode = mode
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
                        isLoading = false
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
                val data = loadPracticeSessionUseCase(
                    navBookId = effectiveBookId,
                    collectionId = -1,
                    filter = "",
                    initialNodeId = if (nodeId == "all") "" else nodeId,
                    mode = mode
                )
                _uiState.update {
                    it.copy(
                        questions = data.questions,
                        currentIndex = 0,
                        currentPartitionId = nodeId
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
                    userAnswers = it.userAnswers.toMutableMap().apply { put(question.id, answer) }
                )
            }

            if (answer.isCorrect == true) {
                if (_uiState.value.confettiEnabled) {
                    _uiState.update { it.copy(confettiId = System.currentTimeMillis()) }
                    confettiJob?.cancel()
                    confettiJob = launch {
                        delay(2800)
                        _uiState.update { it.copy(confettiId = 0L) }
                    }
                }
            }

            if (answer.isCorrect == true && state.autoAdvance && state.currentIndex < state.questions.size - 1) {
                delay(1000)
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
                    userAnswers = it.userAnswers.toMutableMap().apply { remove(question.id) }
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
                    currentIndex = 0
                )
            }
        }
    }

    private fun saveSessionProgress(index: Int, total: Int? = null, debounceMs: Long = 0L) {
        saveProgressJob?.cancel()
        saveProgressJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            val state = _uiState.value
            manageProgressUseCase.saveSessionProgress(
                sessionId = currentSessionId,
                currentIndex = index,
                totalQuestions = total ?: state.questions.size
            )
        }
    }

    // ── AI Chat ──────────────────────────────────────────────────────

    fun chatLoadHistory() {
        val question = _uiState.value.currentQuestion ?: return
        val questionId = question.id
        // Already loaded for this question: keep the in-memory state (including the live scroll
        // position) untouched. Reloading here would overwrite the just-saved scroll offset with a
        // stale DB read and lose the user's place on reopen.
        if (_uiState.value.chat.questionId == questionId) return
        _uiState.update { state ->
            state.chatUpdate(
                questionId = questionId,
                sessions = emptyList(),
                currentId = null,
                history = emptyList(),
                scrollIndex = 0,
                scrollOffset = 0
            )
        }
        viewModelScope.launch {
            val sessions = aiChatUseCase.getChatSessions(questionId)
            val current = sessions.firstOrNull()
            val currentId = current?.id
            val history = currentId?.let { aiChatUseCase.getChatHistory(it) } ?: emptyList()
            if (_uiState.value.currentQuestion?.id != questionId) return@launch
            _uiState.update {
                it.chatUpdate(
                    questionId = questionId,
                    sessions = sessions,
                    currentId = currentId,
                    history = history,
                    scrollIndex = current?.lastScrollIndex ?: 0,
                    scrollOffset = current?.lastScrollOffset ?: 0
                )
            }
        }
    }

    fun chatCreateSession(title: String = "New Chat") {
        val question = _uiState.value.currentQuestion ?: return
        viewModelScope.launch {
            val session = aiChatUseCase.createChatSession(question.id, title)
            _uiState.update { state ->
                state.chatUpdate(
                    sessions = listOf(session) + state.chat.sessions,
                    currentId = session.id,
                    history = emptyList(),
                    scrollIndex = 0,
                    scrollOffset = 0
                )
            }
        }
    }

    fun chatSwitchSession(sessionId: Int) {
        viewModelScope.launch {
            val history = aiChatUseCase.getChatHistory(sessionId)
            val targetSession = _uiState.value.chat.sessions.find { it.id == sessionId }
            _uiState.update { state ->
                state.chatUpdate(
                    sessions = state.chat.sessions,
                    currentId = sessionId,
                    history = history,
                    scrollIndex = targetSession?.lastScrollIndex ?: 0,
                    scrollOffset = targetSession?.lastScrollOffset ?: 0
                )
            }
        }
    }

    fun chatSaveScrollPosition(index: Int, offset: Int) {
        _uiState.update { state -> state.chatUpdate(scrollIndex = index, scrollOffset = offset) }
        val sessionId = _uiState.value.chat.currentSessionId ?: return
        viewModelScope.launch {
            aiChatUseCase.saveScrollPosition(sessionId, index, offset)
        }
    }

    fun chatDeleteSession() {
        val sessionId = _uiState.value.chat.currentSessionId ?: return
        val questionId = _uiState.value.currentQuestion?.id ?: return
        viewModelScope.launch {
            aiChatUseCase.deleteChatSession(sessionId)
            val remaining = aiChatUseCase.getChatSessions(questionId)
            val newCurrent = remaining.firstOrNull()
            val newHistory = newCurrent?.let { aiChatUseCase.getChatHistory(it.id) } ?: emptyList()
            _uiState.update { state ->
                state.chatUpdate(
                    sessions = remaining,
                    currentId = newCurrent?.id,
                    history = newHistory,
                    scrollIndex = newCurrent?.lastScrollIndex ?: 0,
                    scrollOffset = newCurrent?.lastScrollOffset ?: 0
                )
            }
        }
    }

    fun chatSendMessage(message: String) {
        val question = _uiState.value.currentQuestion ?: return
        viewModelScope.launch {
            val sessionId = _uiState.value.chat.currentSessionId ?: run {
                val session = aiChatUseCase.createChatSession(question.id, message.take(30))
                _uiState.update { state ->
                    state.chatUpdate(
                        sessions = listOf(session) + state.chat.sessions,
                        currentId = session.id,
                        history = emptyList()
                    )
                }
                session.id
            }

            val userMsg = aiChatUseCase.saveUserMessage(sessionId, message)
            _uiState.update { state ->
                state.chatUpdate(
                    history = state.chat.history + userMsg,
                    loadingSessionIds = state.chat.loadingSessionIds + sessionId
                )
            }

            chatJobs[sessionId]?.cancel()
            val captionHistory = _uiState.value.chat.history
            chatJobs[sessionId] = launch {
                try {
                    val stream = aiChatUseCase.streamAiResponse(
                        sessionId = sessionId,
                        question = question,
                        userMessage = message,
                        history = captionHistory
                    )
                    var response = ""
                    stream.collect { chunk ->
                        response += chunk
                        _uiState.update { state ->
                            state.chatUpdate(
                                streamingResponses = state.chat.streamingResponses + (sessionId to response)
                            )
                        }
                    }
                    val botMsg = aiChatUseCase.saveBotMessage(sessionId, response)
                    _uiState.update { state ->
                        val chat = state.chat
                        state.chatUpdate(
                            history = if (chat.currentSessionId == sessionId) chat.history + botMsg else chat.history,
                            streamingResponses = chat.streamingResponses - sessionId,
                            loadingSessionIds = chat.loadingSessionIds - sessionId
                        )
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    val botMsg = aiChatUseCase.saveBotMessage(sessionId, "Error: ${e.message}")
                    _uiState.update { state ->
                        state.chatUpdate(
                            history = if (state.chat.currentSessionId == sessionId) state.chat.history + botMsg else state.chat.history,
                            streamingResponses = state.chat.streamingResponses - sessionId,
                            loadingSessionIds = state.chat.loadingSessionIds - sessionId
                        )
                    }
                } finally {
                    chatJobs.remove(sessionId)
                }
            }
        }
    }

    fun chatCancel() {
        val sessionId = _uiState.value.chat.currentSessionId ?: return
        chatJobs[sessionId]?.cancel()
        chatJobs.remove(sessionId)
        _uiState.update { state ->
            state.chatUpdate(
                loadingSessionIds = state.chat.loadingSessionIds - sessionId,
                streamingResponses = state.chat.streamingResponses - sessionId
            )
        }
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

data class AiChatUiState(
    val questionId: Int? = null,
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: Int? = null,
    val history: List<ChatMessage> = emptyList(),
    val loadingSessionIds: Set<Int> = emptySet(),
    val streamingResponses: Map<Int, String> = emptyMap(),
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0
) {
    val isLoading: Boolean get() = currentSessionId?.let { it in loadingSessionIds } ?: false
    val streamingResponse: String get() = currentSessionId?.let { streamingResponses[it] } ?: ""
}

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
    val chat: AiChatUiState = AiChatUiState(),
    val availableCollections: List<com.hihusky.mnemora.data.model.Collection> = emptyList(),
    val questionCollectionIds: Set<Int> = emptySet(),
    val showProgressBar: Boolean = true,
    val confettiEnabled: Boolean = true,
    val confettiId: Long = 0L,
    val autoAdvance: Boolean = true,
    val aiModel: String = "",
    val aiProvider: String = "",
    val isPreviewMode: Boolean = false
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

    /** Convenience for `copy(chat = chat.copy(...))`. */
    fun chatUpdate(
        questionId: Int? = chat.questionId,
        sessions: List<ChatSession> = chat.sessions,
        currentId: Int? = chat.currentSessionId,
        history: List<ChatMessage> = chat.history,
        loadingSessionIds: Set<Int> = chat.loadingSessionIds,
        streamingResponses: Map<Int, String> = chat.streamingResponses,
        scrollIndex: Int = chat.scrollIndex,
        scrollOffset: Int = chat.scrollOffset
    ): PracticeUiState = copy(
        chat = AiChatUiState(
            questionId = questionId,
            sessions = sessions,
            currentSessionId = currentId,
            history = history,
            loadingSessionIds = loadingSessionIds,
            streamingResponses = streamingResponses,
            scrollIndex = scrollIndex,
            scrollOffset = scrollOffset
        )
    )
}
