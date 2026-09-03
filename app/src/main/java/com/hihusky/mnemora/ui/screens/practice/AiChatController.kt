package com.hihusky.mnemora.ui.screens.practice

import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.data.model.ChatScrollPosition
import com.hihusky.mnemora.data.model.ChatSession
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.domain.service.AiProviderCatalog
import com.hihusky.mnemora.domain.service.AiService
import com.hihusky.mnemora.domain.usecase.practice.AiChatUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val CHAT_STREAM_THROTTLE_MS = 40L
private const val SESSION_TITLE_MAX_LENGTH = 30

data class AiChatUiState(
    val questionId: Int? = null,
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: Int? = null,
    val history: List<ChatMessage> = emptyList(),
    val loadingSessionIds: Set<Int> = emptySet(),
    val streamingResponses: Map<Int, String> = emptyMap(),
    val scrollPosition: ChatScrollPosition = ChatScrollPosition(),
    val aiModel: String = "",
    val aiProvider: String = "",
) {
    val isLoading: Boolean get() = currentSessionId?.let { it in loadingSessionIds } ?: false
    val streamingResponse: String get() = currentSessionId?.let { streamingResponses[it] } ?: ""
}

/**
 * Owns the AI chat bottom-sheet state: sessions, streaming jobs, scroll
 * persistence and the active model/provider labels. Created by
 * [PracticeViewModel] with its scope so all streaming work is cancelled
 * with the screen.
 */
class AiChatController(
    parentScope: CoroutineScope,
    private val aiChatUseCase: AiChatUseCase,
    aiService: AiService,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow(AiChatUiState())
    val state: StateFlow<AiChatUiState> = _state.asStateFlow()

    private val chatJobs = mutableMapOf<Int, Job>()

    /**
     * Owns the config collector and every chat stream launched by this
     * controller. Children of the parent scope (viewModelScope in
     * production, the test scope under test) but isolated behind a
     * supervisor so a single misbehaving coroutine does not propagate
     * cancellation to siblings or to the host.
     */
    private val scope: CoroutineScope =
        CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))

    init {
        scope.launch {
            aiService.config.collect { config ->
                _state.update {
                    it.copy(
                        aiModel = AiProviderCatalog.modelDisplayFor(config.provider, config.model),
                        aiProvider = AiProviderCatalog.displayFor(config.provider),
                    )
                }
            }
        }
    }

    /**
     * Cancels every coroutine owned by this controller (config collector,
     * active streams). The host ViewModel should call this from
     * `onCleared()` so lingering streams don't outlive the screen.
     */
    fun close() {
        scope.cancel()
    }

    // ── Session lifecycle ──────────────────────────────────────────────

    fun loadHistory(question: Question) {
        val questionId = question.id
        // Already loaded for this question: keep the in-memory state (including the live scroll
        // position) untouched. Reloading here would overwrite the just-saved scroll offset with a
        // stale DB read and lose the user's place on reopen.
        if (_state.value.questionId == questionId) return
        _state.update { current ->
            // Fresh chat surface for the new question, keeping the AI labels
            // that the config collector already resolved.
            AiChatUiState(
                questionId = questionId,
                scrollPosition = ChatScrollPosition(),
                aiModel = current.aiModel,
                aiProvider = current.aiProvider,
            )
        }
        scope.launch {
            val sessions = aiChatUseCase.getChatSessions(questionId)
            val current = sessions.firstOrNull()
            val currentId = current?.id
            val history = currentId?.let { aiChatUseCase.getChatHistory(it) } ?: emptyList()
            if (_state.value.questionId != questionId) return@launch
            _state.update {
                it.copy(
                    sessions = sessions,
                    currentSessionId = currentId,
                    history = history,
                    scrollPosition = current?.lastScrollPosition ?: ChatScrollPosition(),
                )
            }
        }
    }

    fun createSession(
        question: Question,
        title: String = "New Chat",
    ) {
        scope.launch {
            val session = aiChatUseCase.createChatSession(question.id, title)
            _state.update { state ->
                state.copy(
                    sessions = listOf(session) + state.sessions,
                    currentSessionId = session.id,
                    history = emptyList(),
                    scrollPosition = ChatScrollPosition(),
                )
            }
        }
    }

    fun switchSession(sessionId: Int) {
        scope.launch {
            val history = aiChatUseCase.getChatHistory(sessionId)
            val targetSession = _state.value.sessions.find { it.id == sessionId }
            _state.update { state ->
                state.copy(
                    currentSessionId = sessionId,
                    history = history,
                    scrollPosition = targetSession?.lastScrollPosition ?: ChatScrollPosition(),
                )
            }
        }
    }

    fun saveScrollPosition(
        sessionId: Int,
        position: ChatScrollPosition,
    ) {
        _state.update { state ->
            val updatedSessions =
                state.sessions.map {
                    if (it.id == sessionId) it.copy(lastScrollPosition = position) else it
                }
            state.copy(
                sessions = updatedSessions,
                scrollPosition =
                    if (state.currentSessionId == sessionId) position else state.scrollPosition,
            )
        }
        scope.launch {
            aiChatUseCase.saveScrollPosition(sessionId, position)
        }
    }

    fun deleteSession(question: Question) {
        val sessionId = _state.value.currentSessionId ?: return
        scope.launch {
            aiChatUseCase.deleteChatSession(sessionId)
            val remaining = aiChatUseCase.getChatSessions(question.id)
            val newCurrent = remaining.firstOrNull()
            val newHistory = newCurrent?.let { aiChatUseCase.getChatHistory(it.id) } ?: emptyList()
            _state.update { state ->
                state.copy(
                    sessions = remaining,
                    currentSessionId = newCurrent?.id,
                    history = newHistory,
                    scrollPosition = newCurrent?.lastScrollPosition ?: ChatScrollPosition(),
                )
            }
        }
    }

    // ── Streaming ──────────────────────────────────────────────────────

    @Suppress("TooGenericExceptionCaught")
    fun sendMessage(
        question: Question,
        message: String,
    ) {
        scope.launch {
            val sessionId = ensureSession(question, message)
            val userMsg = aiChatUseCase.saveUserMessage(sessionId, message)
            _state.update { state ->
                state.copy(
                    history = state.history + userMsg,
                    loadingSessionIds = state.loadingSessionIds + sessionId,
                )
            }

            val captionHistory = _state.value.history
            startStreamJob(sessionId) {
                var response = ""
                try {
                    val stream =
                        aiChatUseCase.streamAiResponse(
                            sessionId = sessionId,
                            question = question,
                            userMessage = message,
                            history = captionHistory,
                        )
                    collectThrottled(sessionId, stream) { chunk ->
                        response += chunk
                        response
                    }
                    val botMsg = aiChatUseCase.saveBotMessage(sessionId, response, isInterrupted = false)
                    completeStreaming(sessionId) { history -> history + botMsg }
                } catch (e: Exception) {
                    // Any failure mode — cancellation, HTTP error, IO, parsing —
                    // must resolve the UI state rather than crash the chat.
                    handleSendFailure(sessionId, e, response)
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun continueMessage(
        question: Question,
        targetMessage: ChatMessage,
    ) {
        val sessionId = _state.value.currentSessionId ?: return
        if (sessionId in _state.value.loadingSessionIds) return

        _state.update { state ->
            state.copy(loadingSessionIds = state.loadingSessionIds + sessionId)
        }

        val currentHistory = _state.value.history
        val targetIndex = currentHistory.indexOfFirst { it.id == targetMessage.id }
        val priorHistory = if (targetIndex >= 0) currentHistory.take(targetIndex) else currentHistory
        val baseText = targetMessage.text

        startStreamJob(sessionId) {
            var continuation = ""
            try {
                val stream =
                    aiChatUseCase.streamAiContinuation(
                        sessionId = sessionId,
                        question = question,
                        history = priorHistory,
                        interruptedText = baseText,
                    )
                collectThrottled(sessionId, stream) { chunk ->
                    continuation += chunk
                    baseText + continuation
                }
                resumeInterruptedMessage(sessionId, targetMessage, baseText, continuation, isInterrupted = false)
            } catch (e: CancellationException) {
                resumeInterruptedMessage(sessionId, targetMessage, baseText, continuation, isInterrupted = true)
                throw e
            } catch (e: Exception) {
                // Whether the request failed on IO, HTTP, or parsing, whatever
                // arrived so far is kept on the original message and flagged
                // interrupted instead of crashing the chat.
                resumeInterruptedMessage(sessionId, targetMessage, baseText, continuation, isInterrupted = true)
            }
        }
    }

    fun regenerateMessage(
        question: Question,
        targetMessage: ChatMessage,
    ) {
        val sessionId = _state.value.currentSessionId ?: return
        val history = _state.value.history
        val messageIndex = history.indexOfFirst { it.id == targetMessage.id }
        val prevUserMsg = if (messageIndex >= 0) history.take(messageIndex).lastOrNull { it.isUser } else null
        if (prevUserMsg == null) return

        scope.launch {
            aiChatUseCase.deleteChatMessage(targetMessage.id)
            _state.update { state ->
                state.copy(history = state.history.filterNot { it.id == targetMessage.id })
            }
            sendMessage(question, prevUserMsg.text)
        }
    }

    fun cancel() {
        val sessionId = _state.value.currentSessionId ?: return
        chatJobs[sessionId]?.cancel()
        chatJobs.remove(sessionId)
    }

    // ── Internals ──────────────────────────────────────────────────────

    private suspend fun ensureSession(
        question: Question,
        firstMessage: String,
    ): Int {
        _state.value.currentSessionId?.let { return it }
        val session = aiChatUseCase.createChatSession(question.id, firstMessage.take(SESSION_TITLE_MAX_LENGTH))
        _state.update { state ->
            state.copy(
                sessions = listOf(session) + state.sessions,
                currentSessionId = session.id,
                history = emptyList(),
            )
        }
        return session.id
    }

    private fun startStreamJob(
        sessionId: Int,
        block: suspend () -> Unit,
    ) {
        chatJobs[sessionId]?.cancel()
        chatJobs[sessionId] =
            scope.launch {
                try {
                    block()
                } finally {
                    chatJobs.remove(sessionId)
                }
            }
    }

    /**
     * Collects the delta stream, refreshing the visible streaming text at
     * most every [CHAT_STREAM_THROTTLE_MS] to avoid recomposition storms.
     * [visibleText] folds each chunk into the full text the user should see.
     */
    private suspend fun collectThrottled(
        sessionId: Int,
        stream: Flow<String>,
        visibleText: (String) -> String,
    ) {
        var lastEmitTime = 0L
        stream.collect { chunk ->
            val text = visibleText(chunk)
            val timestamp = now()
            if (timestamp - lastEmitTime >= CHAT_STREAM_THROTTLE_MS) {
                lastEmitTime = timestamp
                _state.update { state ->
                    state.copy(streamingResponses = state.streamingResponses + (sessionId to text))
                }
            }
        }
    }

    /**
     * Applies a history transformation for the finished stream and clears
     * the streaming markers. Sessions the user has already left keep their
     * displayed history untouched.
     */
    private fun completeStreaming(
        sessionId: Int,
        transformHistory: (List<ChatMessage>) -> List<ChatMessage>,
    ) {
        _state.update { state ->
            val history =
                if (state.currentSessionId == sessionId) transformHistory(state.history) else state.history
            state.copy(
                history = history,
                streamingResponses = state.streamingResponses - sessionId,
                loadingSessionIds = state.loadingSessionIds - sessionId,
            )
        }
    }

    private fun replaceStreaming(
        sessionId: Int,
        messageId: Int,
        replacement: ChatMessage,
    ) {
        completeStreaming(sessionId) { history ->
            history.map { if (it.id == messageId) replacement else it }
        }
    }

    private suspend fun resumeInterruptedMessage(
        sessionId: Int,
        targetMessage: ChatMessage,
        baseText: String,
        continuation: String,
        isInterrupted: Boolean,
    ) {
        val updatedMessage =
            targetMessage.copy(
                text = (baseText + continuation).trimEnd(),
                isInterrupted = isInterrupted,
            )
        aiChatUseCase.updateChatMessage(sessionId, updatedMessage)
        replaceStreaming(sessionId, targetMessage.id, updatedMessage)
    }

    /**
     * Normalizes the outcome of a failed send: a partial response is kept and
     * flagged interrupted, a clean cancellation only clears markers, and real
     * errors surface as a persisted error message. Cancellation is rethrown
     * so the coroutine completes as cancelled.
     */
    private suspend fun handleSendFailure(
        sessionId: Int,
        error: Exception,
        partialResponse: String,
    ) {
        val isCancelled = error is CancellationException
        when {
            partialResponse.isNotBlank() -> {
                val partialMsg = aiChatUseCase.saveBotMessage(sessionId, partialResponse, isInterrupted = true)
                completeStreaming(sessionId) { history -> history + partialMsg }
            }

            !isCancelled -> {
                val errorMsg = aiChatUseCase.saveBotMessage(sessionId, "Error: ${error.message}", isInterrupted = true)
                completeStreaming(sessionId) { history -> history + errorMsg }
            }

            else -> {
                _state.update { state ->
                    state.copy(
                        streamingResponses = state.streamingResponses - sessionId,
                        loadingSessionIds = state.loadingSessionIds - sessionId,
                    )
                }
            }
        }
        if (isCancelled) throw error
    }
}
