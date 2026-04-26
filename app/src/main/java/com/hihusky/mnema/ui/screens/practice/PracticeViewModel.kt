package com.hihusky.mnema.ui.screens.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hihusky.mnema.data.local.db.entity.CollectionItemEntity
import com.hihusky.mnema.data.local.db.entity.StudySessionEntity
import com.hihusky.mnema.data.model.Book
import com.hihusky.mnema.data.model.ChatMessage
import com.hihusky.mnema.data.model.ChatSession
import com.hihusky.mnema.data.model.Node
import com.hihusky.mnema.data.model.Question
import com.hihusky.mnema.data.model.QuestionStatus
import com.hihusky.mnema.data.model.UserAnswer
import com.hihusky.mnema.data.repository.DatabaseRepository
import com.hihusky.mnema.data.repository.SettingsRepository
import com.hihusky.mnema.domain.service.AiService
import com.hihusky.mnema.domain.service.FeedbackService
import com.hihusky.mnema.domain.service.PackageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dbRepository: DatabaseRepository,
    private val settingsRepository: SettingsRepository,
    private val aiService: AiService,
    private val feedbackService: FeedbackService,
    private val packageService: PackageService
) : ViewModel() {

    private val navBookId: Int = checkNotNull(savedStateHandle["bookId"])
    private val initialNodeId: String = savedStateHandle["nodeId"] ?: ""
    private val collectionId: Int = savedStateHandle["collectionId"] ?: -1
    private val filter: String = savedStateHandle["filter"] ?: ""
    private val mode: String = savedStateHandle["mode"] ?: "Practice"

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    var imageBasePath: String? = null
        private set

    private var aiJob: Job? = null
    private var currentSessionId: Long = -1L
    private var effectiveBookId: Int = navBookId

    fun loadCollectionData() {
        val question = _uiState.value.currentQuestion ?: return
        viewModelScope.launch {
            try {
                val collections = dbRepository.getCustomCollections()
                val questionCollectionIds = dbRepository.getCollectionIdsForQuestion(
                    question.bookId, question.id
                ).toSet()
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
                if (isIn) {
                    dbRepository.deleteCollectionItemBySource(collectionId, question.bookId, question.id)
                } else {
                    val poolQuestionId = dbRepository.ensureQuestionInPool(question.bookId, question.id)
                    dbRepository.insertCollectionItem(
                        CollectionItemEntity(
                            collectionId = collectionId,
                            poolQuestionId = poolQuestionId,
                            sourceBookId = question.bookId,
                            sourceQuestionId = question.id,
                            addedAt = System.currentTimeMillis()
                        )
                    )
                }
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
                val now = System.currentTimeMillis()
                dbRepository.insertCollection(
                    com.hihusky.mnema.data.local.db.entity.CollectionEntity(
                        kind = com.hihusky.mnema.data.model.CollectionKind.Custom.name.lowercase(),
                        behavior = com.hihusky.mnema.data.model.CollectionBehavior.Manual.name.lowercase(),
                        name = name,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                loadCollectionData()
            } catch (_: Exception) {}
        }
    }

    fun deleteCollection(collectionId: Int) {
        viewModelScope.launch {
            try {
                dbRepository.deleteCollection(collectionId)
                _uiState.update { state ->
                    state.copy(
                        availableCollections = state.availableCollections.filter { it.id != collectionId },
                        questionCollectionIds = state.questionCollectionIds - collectionId
                    )
                }
            } catch (_: Exception) {}
        }
    }

    init {
        observePracticeProgressPreference()
        loadBook()
        _uiState.update {
            it.copy(aiModel = aiService.model, aiProvider = aiService.provider)
        }
    }

    private fun observePracticeProgressPreference() {
        viewModelScope.launch {
            settingsRepository.showPracticeProgress.collect { showProgress ->
                _uiState.update { it.copy(showProgressBar = showProgress) }
            }
        }
    }

    private fun loadBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val allQuestions = dbRepository.getQuestions(navBookId).filter { it.isAnswerable }

                val questions = when {
                    collectionId > 0 -> {
                        val poolEntities = dbRepository.getPoolQuestionsByCollection(collectionId)
                        dbRepository.poolQuestionsToDomain(poolEntities).filter { it.isAnswerable }
                    }
                    filter.isNotBlank() -> {
                        applyFilter(allQuestions, filter)
                    }
                    initialNodeId.isNotBlank() -> {
                        allQuestions.filter { it.nodeId == initialNodeId }
                    }
                    else -> allQuestions
                }

                effectiveBookId = if (collectionId > 0 && questions.isNotEmpty()) {
                    questions.first().bookId
                } else navBookId

                val book = dbRepository.getBookById(effectiveBookId)
                imageBasePath = book?.let { packageService.getPackageImagePath(it.filename) }
                val nodes = if (book != null) dbRepository.getNodes(effectiveBookId) else emptyList()
                val answers = dbRepository.getUserAnswers(effectiveBookId)
                val marks = dbRepository.getMarkedQuestions(effectiveBookId)

                val partitionId = when {
                    collectionId > 0 -> "collection_$collectionId"
                    filter.isNotBlank() -> "filter_$filter"
                    initialNodeId.isNotBlank() -> initialNodeId
                    else -> "all"
                }

                val session = dbRepository.getActiveSession(effectiveBookId, mode)
                val startIndex = session?.currentIndex?.coerceIn(0, questions.size - 1) ?: 0
                currentSessionId = session?.id ?: -1L

                if (session == null && questions.isNotEmpty()) {
                    currentSessionId = dbRepository.saveSession(
                        StudySessionEntity(
                            bookId = effectiveBookId,
                            mode = mode,
                            startTime = System.currentTimeMillis(),
                            lastActiveTime = System.currentTimeMillis(),
                            currentIndex = 0,
                            totalQuestions = questions.size,
                            collectionId = collectionId.takeIf { it > 0 },
                            nodeId = initialNodeId.takeIf { it.isNotBlank() }
                        )
                    )
                }

                _uiState.update {
                    it.copy(
                        book = book,
                        nodes = nodes,
                        questions = questions,
                        userAnswers = answers,
                        markedQuestions = marks,
                        currentIndex = startIndex,
                        currentPartitionId = partitionId,
                        isLoading = false
                    )
                }
                loadChatHistory()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private suspend fun applyFilter(questions: List<Question>, filter: String): List<Question> {
        return when (filter.lowercase()) {
            "marked" -> {
                val markedIds = dbRepository.getMarkedQuestions(navBookId)
                questions.filter { it.id in markedIds }
            }
            "wrong" -> {
                val wrongIds = dbRepository.getWrongQuestionIds(navBookId).toSet()
                questions.filter { it.id in wrongIds }
            }
            "unanswered" -> {
                val answeredIds = dbRepository.getAnsweredQuestionIds(navBookId).toSet()
                questions.filter { it.id !in answeredIds }
            }
            "srs_due" -> {
                val dueIds = dbRepository.getSrsDueQuestionIds(navBookId).toSet()
                questions.filter { it.id in dueIds }
            }
            else -> questions
        }
    }

    fun selectNode(nodeId: String) {
        viewModelScope.launch {
            val allQuestions = dbRepository.getQuestions(effectiveBookId).filter { it.isAnswerable }
            val filtered = if (nodeId == "all") {
                allQuestions
            } else {
                allQuestions.filter { it.nodeId == nodeId }
            }
            _uiState.update {
                it.copy(
                    questions = filtered,
                    currentIndex = 0,
                    currentPartitionId = nodeId
                )
            }
            saveSessionProgress(0, filtered.size)
            loadChatHistory()
        }
    }

    fun goToQuestion(index: Int) {
        if (index < 0 || index >= _uiState.value.questions.size) return
        _uiState.update { it.copy(currentIndex = index) }
        saveSessionProgress(index)
        loadChatHistory()
    }

    fun nextQuestion() {
        goToQuestion(_uiState.value.currentIndex + 1)
    }

    fun previousQuestion() {
        goToQuestion(_uiState.value.currentIndex - 1)
    }

    fun answerQuestion(option: String) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        val isCorrect = option.uppercase() == question.answer.uppercase()
        val answer = UserAnswer(selected = option, isCorrect = isCorrect)

        viewModelScope.launch {
            dbRepository.saveUserAnswer(effectiveBookId, question.id, answer)
            _uiState.update {
                it.copy(
                    userAnswers = it.userAnswers.toMutableMap().apply { put(question.id, answer) }
                )
            }

            if (isCorrect) {
                feedbackService.incrementStreak()
                feedbackService.playCorrect()
            } else {
                feedbackService.resetStreak()
                feedbackService.playWrong()
            }

            val autoAdvance = settingsRepository.autoAdvance.first()
            if (isCorrect && autoAdvance && state.currentIndex < state.questions.size - 1) {
                delay(1000)
                nextQuestion()
            }
        }
    }

    fun toggleMark() {
        val question = _uiState.value.currentQuestion ?: return
        val isMarked = !_uiState.value.markedQuestions.contains(question.id)
        viewModelScope.launch {
            dbRepository.setUserMark(effectiveBookId, question.id, isMarked)
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
            dbRepository.deleteUserAnswer(question.id)
            _uiState.update {
                it.copy(
                    userAnswers = it.userAnswers.toMutableMap().apply { remove(question.id) }
                )
            }
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            dbRepository.clearBookProgress(effectiveBookId)
            if (currentSessionId > 0) {
                dbRepository.deactivateSession(currentSessionId)
                currentSessionId = -1L
            }
            _uiState.update {
                it.copy(
                    userAnswers = emptyMap(),
                    markedQuestions = emptySet(),
                    currentIndex = 0
                )
            }
        }
    }

    private fun saveSessionProgress(index: Int, total: Int? = null) {
        viewModelScope.launch {
            if (currentSessionId > 0) {
                val state = _uiState.value
                dbRepository.updateSessionProgress(
                    sessionId = currentSessionId,
                    currentIndex = index,
                    totalQuestions = total ?: state.questions.size
                )
            }
        }
    }

    //region AI Chat

    private fun loadChatHistory() {
        val question = _uiState.value.currentQuestion ?: return
        viewModelScope.launch {
            val sessions = dbRepository.getChatSessions(question.id)
            val currentSessionId = sessions.firstOrNull()?.id
            val history = currentSessionId?.let { dbRepository.getChatHistory(it) } ?: emptyList()
            _uiState.update {
                it.copy(
                    chatSessions = sessions,
                    currentChatSessionId = currentSessionId,
                    chatHistory = history
                )
            }
        }
    }

    fun createChatSession(title: String = "New Chat") {
        val question = _uiState.value.currentQuestion ?: return
        viewModelScope.launch {
            val session = dbRepository.createChatSession(question.id, title)
            _uiState.update {
                it.copy(
                    chatSessions = listOf(session) + it.chatSessions,
                    currentChatSessionId = session.id,
                    chatHistory = emptyList()
                )
            }
        }
    }

    fun switchChatSession(sessionId: Int) {
        viewModelScope.launch {
            val history = dbRepository.getChatHistory(sessionId)
            _uiState.update {
                it.copy(currentChatSessionId = sessionId, chatHistory = history)
            }
        }
    }

    fun deleteChatSession() {
        val sessionId = _uiState.value.currentChatSessionId ?: return
        viewModelScope.launch {
            dbRepository.deleteChatSession(sessionId)
            val remaining = dbRepository.getChatSessions(
                _uiState.value.currentQuestion?.id ?: return@launch
            )
            val newCurrentId = remaining.firstOrNull()?.id
            val newHistory = newCurrentId?.let { dbRepository.getChatHistory(it) } ?: emptyList()
            _uiState.update {
                it.copy(
                    chatSessions = remaining,
                    currentChatSessionId = newCurrentId,
                    chatHistory = newHistory
                )
            }
        }
    }

    fun sendAiMessage(message: String) {
        val question = _uiState.value.currentQuestion ?: return
        viewModelScope.launch {
            val sessionId = _uiState.value.currentChatSessionId ?: run {
                val session = dbRepository.createChatSession(question.id, message.take(30))
                _uiState.update {
                    it.copy(
                        chatSessions = listOf(session) + it.chatSessions,
                        currentChatSessionId = session.id,
                        chatHistory = emptyList()
                    )
                }
                session.id
            }

            val userMsg = ChatMessage(text = message, isUser = true)
            dbRepository.saveChatMessage(sessionId, userMsg)
            _uiState.update {
                it.copy(chatHistory = it.chatHistory + userMsg, isAiLoading = true, aiError = null)
            }

            aiJob?.cancel()
            val history = _uiState.value.chatHistory
            aiJob = launch {
                try {
                    val stream = aiService.explain(
                        questionStem = question.content,
                        options = question.choices.associate { it.key to it.content },
                        correctAnswer = question.answer,
                        userQuestion = message,
                        history = history
                    )
                    var response = ""
                    stream.collect { chunk ->
                        response += chunk
                        _uiState.update { it.copy(aiStreamingResponse = response) }
                    }
                    val botMsg = ChatMessage(text = response, isUser = false)
                    dbRepository.saveChatMessage(sessionId, botMsg)
                    _uiState.update {
                        it.copy(
                            chatHistory = it.chatHistory + botMsg,
                            aiStreamingResponse = "",
                            isAiLoading = false
                        )
                    }
                } catch (e: Exception) {
                    val errorMsg = "Error: ${e.message}"
                    val botMsg = ChatMessage(text = errorMsg, isUser = false)
                    dbRepository.saveChatMessage(sessionId, botMsg)
                    _uiState.update {
                        it.copy(
                            chatHistory = it.chatHistory + botMsg,
                            aiStreamingResponse = "",
                            isAiLoading = false,
                            aiError = e.message
                        )
                    }
                }
            }
        }
    }

    fun cancelAiChat() {
        aiJob?.cancel()
        aiJob = null
        _uiState.update { it.copy(isAiLoading = false, aiStreamingResponse = "") }
    }

    //endregion

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
    val chatSessions: List<ChatSession> = emptyList(),
    val currentChatSessionId: Int? = null,
    val chatHistory: List<ChatMessage> = emptyList(),
    val isAiLoading: Boolean = false,
    val aiStreamingResponse: String = "",
    val aiError: String? = null,
    val availableCollections: List<com.hihusky.mnema.data.model.Collection> = emptyList(),
    val questionCollectionIds: Set<Int> = emptySet(),
    val showProgressBar: Boolean = true,
    val aiModel: String = "",
    val aiProvider: String = ""
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
