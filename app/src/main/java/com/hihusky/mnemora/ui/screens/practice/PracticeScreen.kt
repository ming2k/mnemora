package com.hihusky.mnemora.ui.screens.practice

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hihusky.mnemora.data.model.Book
import com.hihusky.mnemora.data.model.Collection
import com.hihusky.mnemora.data.model.Node
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.QuestionChoice
import com.hihusky.mnemora.data.model.QuestionStatus
import com.hihusky.mnemora.data.model.QuestionType
import com.hihusky.mnemora.data.model.UserAnswer
import com.hihusky.mnemora.ui.components.AiChatSheet
import com.hihusky.mnemora.ui.components.CollectionSheet
import com.hihusky.mnemora.ui.components.ConfettiOverlay
import com.hihusky.mnemora.ui.components.DopamineProgressBar
import com.hihusky.mnemora.ui.components.MnemoraAlertDialog
import com.hihusky.mnemora.ui.components.NodeSheet
import com.hihusky.mnemora.ui.components.OverviewSheet
import com.hihusky.mnemora.ui.components.QuestionContent
import com.hihusky.mnemora.ui.components.topappbar.MnemoraTopAppBar
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.MnemoraTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PracticeScreen(
    onBack: () -> Unit,
    viewModel: PracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PracticeScreenContent(
        uiState = uiState,
        onBack = onBack,
        onToggleMark = viewModel::toggleMark,
        onResetCurrentQuestion = viewModel::resetCurrentQuestion,
        onAnswerQuestion = viewModel::answerQuestion,
        onGoToQuestion = viewModel::goToQuestion,
        onSelectNode = viewModel::selectNode,
        onLoadChatHistory = viewModel::chatLoadHistory,
        onLoadCollectionData = viewModel::loadCollectionData,
        onToggleQuestionInCollection = viewModel::toggleQuestionInCollection,
        onCreateCollection = viewModel::createCollection,
        onDeleteCollection = viewModel::deleteCollection,
        onSwitchChatSession = viewModel::chatSwitchSession,
        onCreateChatSession = viewModel::chatCreateSession,
        onDeleteChatSession = viewModel::chatDeleteSession,
        onSendAiMessage = viewModel::chatSendMessage,
        onCancelAiMessage = viewModel::chatCancel,
        onSaveChatScrollPosition = viewModel::chatSaveScrollPosition,
        onConfettiFinished = viewModel::clearConfetti,
        getQuestionStatus = viewModel::getQuestionStatus,
        imageBasePath = viewModel.imageBasePath
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun PracticeScreenContent(
    uiState: PracticeUiState,
    onBack: () -> Unit,
    onToggleMark: () -> Unit,
    onResetCurrentQuestion: () -> Unit,
    onAnswerQuestion: (String) -> Unit,
    onGoToQuestion: (Int) -> Unit,
    onSelectNode: (String) -> Unit,
    onLoadChatHistory: () -> Unit,
    onLoadCollectionData: () -> Unit,
    onToggleQuestionInCollection: (Int) -> Unit,
    onCreateCollection: (String) -> Unit,
    onDeleteCollection: (Int) -> Unit,
    onSwitchChatSession: (Int) -> Unit,
    onCreateChatSession: (String) -> Unit,
    onDeleteChatSession: () -> Unit,
    onSendAiMessage: (String) -> Unit,
    onCancelAiMessage: () -> Unit,
    onSaveChatScrollPosition: (Int, Int) -> Unit,
    onConfettiFinished: () -> Unit,
    getQuestionStatus: (Int) -> QuestionStatus,
    imageBasePath: String?
) {
    val pagerState = rememberPagerState(pageCount = { uiState.questions.size.coerceAtLeast(1) })
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val latestCurrentIndex by rememberUpdatedState(uiState.currentIndex)
    var showResetDialog by remember { mutableStateOf(false) }
    var showOverview by remember { mutableStateOf(false) }
    var showAiChat by remember { mutableStateOf(false) }
    var showNodeSelector by remember { mutableStateOf(false) }
    var showCollectionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(
        uiState.questions.size,
        uiState.questions.firstOrNull()?.id,
        uiState.currentPartitionId
    ) {
        if (uiState.questions.isNotEmpty()) {
            pagerState.scrollToPage(uiState.currentIndex.coerceIn(uiState.questions.indices))
        }
    }

    LaunchedEffect(uiState.currentIndex, uiState.questions.size) {
        if (uiState.questions.isNotEmpty() && pagerState.currentPage != uiState.currentIndex) {
            pagerState.animateScrollToPage(uiState.currentIndex.coerceIn(uiState.questions.indices))
        }
    }

    LaunchedEffect(pagerState, uiState.questions.size) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                if (page != latestCurrentIndex && page in uiState.questions.indices) {
                    onGoToQuestion(page)
                }
            }
    }

    fun navigateToPage(index: Int) {
        if (index !in uiState.questions.indices) return
        scope.launch {
            pagerState.animateScrollToPage(index)
        }
    }

    // Prefetch chat history whenever the question settles or when the chat opens,
    // ensuring the bottom sheet slides up smoothly with zero DB latency and no layout thrashing.
    LaunchedEffect(uiState.currentQuestion?.id, showAiChat) {
        if (uiState.currentQuestion != null && (showAiChat || uiState.chat.questionId != uiState.currentQuestion?.id)) {
            onLoadChatHistory()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MnemoraTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.book?.displayName ?: "Practice",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${uiState.currentIndex + 1} / ${uiState.totalQuestions}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showNodeSelector = true }) {
                            Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Nodes")
                        }
                        IconButton(onClick = { showOverview = true }) {
                            Icon(Icons.Default.GridView, contentDescription = "Overview")
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                PracticeBottomBar(
                    showPrev = uiState.currentIndex > 0,
                    showNext = uiState.currentIndex < uiState.totalQuestions - 1,
                    showAi = uiState.currentQuestion != null,
                    isMarked = uiState.isCurrentMarked,
                    showReset = uiState.currentUserAnswer != null && !uiState.isPreviewMode,
                    onPrevious = { navigateToPage(uiState.currentIndex - 1) },
                    onNext = { navigateToPage(uiState.currentIndex + 1) },
                    onAi = { showAiChat = true },
                    onToggleMark = onToggleMark,
                    onLongPressBookmark = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLoadCollectionData()
                        showCollectionSheet = true
                    },
                    onReset = { showResetDialog = true }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .onKeyEvent { event ->
                        when (event.key) {
                            Key.DirectionLeft -> {
                                navigateToPage(uiState.currentIndex - 1)
                                true
                            }
                            Key.DirectionRight -> {
                                navigateToPage(uiState.currentIndex + 1)
                                true
                            }
                            Key.A -> { showAiChat = true; true }
                            Key.M -> { onToggleMark(); true }
                            Key.R -> {
                                if (uiState.currentUserAnswer != null) onResetCurrentQuestion()
                                true
                            }
                            Key.One, Key.NumPad1 -> {
                                uiState.currentQuestion?.choices?.getOrNull(0)?.key
                                    ?.let { onAnswerQuestion(it) }
                                true
                            }
                            Key.Two, Key.NumPad2 -> {
                                uiState.currentQuestion?.choices?.getOrNull(1)?.key
                                    ?.let { onAnswerQuestion(it) }
                                true
                            }
                            Key.Three, Key.NumPad3 -> {
                                uiState.currentQuestion?.choices?.getOrNull(2)?.key
                                    ?.let { onAnswerQuestion(it) }
                                true
                            }
                            Key.Four, Key.NumPad4 -> {
                                uiState.currentQuestion?.choices?.getOrNull(3)?.key
                                    ?.let { onAnswerQuestion(it) }
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                if (uiState.showProgressBar) {
                    DopamineProgressBar(
                        progress = uiState.progress,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.questions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No questions available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    PracticePager(
                        pagerState = pagerState,
                        questions = uiState.questions,
                        userAnswers = uiState.userAnswers,
                        isPreviewMode = uiState.isPreviewMode,
                        onAnswerQuestion = onAnswerQuestion,
                        imageBasePath = imageBasePath
                    )
                }
            }
        }

        if (uiState.confettiId > 0L) {
            androidx.compose.runtime.key(uiState.confettiId) {
                ConfettiOverlay(onFinished = onConfettiFinished)
            }
        }
    }

    PracticeDialogs(
        showResetDialog = showResetDialog,
        onDismissReset = { showResetDialog = false },
        onConfirmReset = {
            onResetCurrentQuestion()
            showResetDialog = false
        },
        showOverview = showOverview,
        onDismissOverview = { showOverview = false },
        totalQuestions = uiState.totalQuestions,
        currentIndex = uiState.currentIndex,
        onQuestionSelected = {
            navigateToPage(it)
            showOverview = false
        },
        getQuestionStatus = getQuestionStatus,
        showAiChat = showAiChat,
        onDismissAiChat = { showAiChat = false },
        chat = uiState.chat,
        onSwitchChatSession = onSwitchChatSession,
        onCreateChatSession = onCreateChatSession,
        onSendAiMessage = onSendAiMessage,
        onCancelAiMessage = onCancelAiMessage,
        onDeleteChatSession = onDeleteChatSession,
        onSaveChatScrollPosition = onSaveChatScrollPosition,
        aiModel = uiState.aiModel,
        aiProvider = uiState.aiProvider,
        showNodeSelector = showNodeSelector,
        onDismissNodeSelector = { showNodeSelector = false },
        nodes = uiState.nodes,
        currentPartitionId = uiState.currentPartitionId,
        onSelectNode = { onSelectNode(it); showNodeSelector = false },
        showCollectionSheet = showCollectionSheet,
        onDismissCollection = { showCollectionSheet = false },
        collections = uiState.availableCollections,
        questionCollectionIds = uiState.questionCollectionIds,
        onToggleCollection = onToggleQuestionInCollection,
        onCreateCollection = onCreateCollection,
        onDeleteCollection = onDeleteCollection
    )
}

@Composable
private fun PracticePager(
    pagerState: PagerState,
    questions: List<Question>,
    userAnswers: Map<Int, UserAnswer>,
    isPreviewMode: Boolean,
    onAnswerQuestion: (String) -> Unit,
    imageBasePath: String?
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 2,
        contentPadding = PaddingValues(horizontal = 0.dp),
        key = { page -> questions[page].id }
    ) { page ->
        val question = questions[page]
        val answer = userAnswers[question.id]

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = MnemoraSpacing.Large,
                vertical = MnemoraSpacing.Large
            )
        ) {
            item(key = question.id) {
                Crossfade(
                    targetState = answer != null || isPreviewMode,
                    animationSpec = tween(250)
                ) { showAnswer ->
                    QuestionContent(
                        question = question,
                        selectedOption = answer?.selected,
                        showAnswer = showAnswer,
                        onOptionSelected = if (answer == null && !isPreviewMode) {
                            { onAnswerQuestion(it) }
                        } else null,
                        imageBasePath = imageBasePath
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PracticeBottomBar(
    showPrev: Boolean,
    showNext: Boolean,
    showAi: Boolean,
    isMarked: Boolean,
    showReset: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAi: () -> Unit,
    onToggleMark: () -> Unit,
    onLongPressBookmark: () -> Unit,
    onReset: () -> Unit
) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, enabled = showPrev) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
            }

            IconButton(onClick = onAi, enabled = showAi) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 24.dp),
                        onClick = onToggleMark,
                        onLongClick = onLongPressBookmark
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark / Add to collection",
                    tint = if (isMarked)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onReset, enabled = showReset) {
                Icon(Icons.Filled.Refresh, contentDescription = "Clear answer")
            }

            IconButton(onClick = onNext, enabled = showNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeDialogs(
    showResetDialog: Boolean,
    onDismissReset: () -> Unit,
    onConfirmReset: () -> Unit,
    showOverview: Boolean,
    onDismissOverview: () -> Unit,
    totalQuestions: Int,
    currentIndex: Int,
    onQuestionSelected: (Int) -> Unit,
    getQuestionStatus: (Int) -> QuestionStatus,
    showAiChat: Boolean,
    onDismissAiChat: () -> Unit,
    chat: AiChatUiState,
    onSwitchChatSession: (Int) -> Unit,
    onCreateChatSession: (String) -> Unit,
    onSendAiMessage: (String) -> Unit,
    onCancelAiMessage: () -> Unit,
    onDeleteChatSession: () -> Unit,
    onSaveChatScrollPosition: (Int, Int) -> Unit,
    aiModel: String,
    aiProvider: String,
    showNodeSelector: Boolean,
    onDismissNodeSelector: () -> Unit,
    nodes: List<Node>,
    currentPartitionId: String,
    onSelectNode: (String) -> Unit,
    showCollectionSheet: Boolean,
    onDismissCollection: () -> Unit,
    collections: List<Collection>,
    questionCollectionIds: Set<Int>,
    onToggleCollection: (Int) -> Unit,
    onCreateCollection: (String) -> Unit,
    onDeleteCollection: (Int) -> Unit
) {
    if (showResetDialog) {
        MnemoraAlertDialog(
            onDismissRequest = onDismissReset,
            title = "Clear Answer",
            message = "Reset your choices for this question?",
            confirmText = "Reset",
            onConfirm = onConfirmReset,
            dismissText = "Cancel",
            isDestructive = true
        )
    }

    if (showOverview) {
        OverviewSheet(
            totalQuestions = totalQuestions,
            currentIndex = currentIndex,
            getStatus = getQuestionStatus,
            onQuestionSelected = onQuestionSelected,
            onDismiss = onDismissOverview
        )
    }

    if (showAiChat) {
        AiChatSheet(
            sessions = chat.sessions,
            currentSessionId = chat.currentSessionId,
            history = chat.history,
            isLoading = chat.isLoading,
            streamingResponse = chat.streamingResponse,
            onSessionSelected = onSwitchChatSession,
            onCreateSession = { onCreateChatSession("New Chat") },
            onSendMessage = onSendAiMessage,
            onCancelMessage = onCancelAiMessage,
            onDismiss = onDismissAiChat,
            onDeleteSession = onDeleteChatSession,
            modelLabel = aiModel,
            providerLabel = aiProvider,
            initialScrollIndex = chat.scrollIndex,
            initialScrollOffset = chat.scrollOffset,
            onSaveScrollPosition = onSaveChatScrollPosition
        )
    }

    if (showNodeSelector) {
        NodeSheet(
            nodes = nodes,
            currentPartitionId = currentPartitionId,
            onNodeSelected = onSelectNode,
            onDismiss = onDismissNodeSelector
        )
    }

    if (showCollectionSheet) {
        CollectionSheet(
            collections = collections,
            questionCollectionIds = questionCollectionIds,
            onToggle = onToggleCollection,
            onCreate = onCreateCollection,
            onDelete = onDeleteCollection,
            onDismiss = onDismissCollection
        )
    }
}

// ── Previews ────────────────────────────────────────────────────────

private val mockBook = Book(id = 1, filename = "mock_book", name = "Mock Subject")

private val mockQuestions = listOf(
    Question(
        id = 1, bookId = 1, nodeId = "n1",
        content = "What is the capital of France?",
        choices = listOf(
            QuestionChoice("A", "Paris"), QuestionChoice("B", "London"),
            QuestionChoice("C", "Berlin"), QuestionChoice("D", "Madrid")
        ),
        answer = "A", questionType = QuestionType.MultipleChoice
    ),
    Question(
        id = 2, bookId = 1, nodeId = "n1",
        content = "What is 2 + 2?",
        choices = listOf(
            QuestionChoice("A", "3"), QuestionChoice("B", "4"),
            QuestionChoice("C", "5"), QuestionChoice("D", "6")
        ),
        answer = "B", questionType = QuestionType.MultipleChoice
    )
)

private val mockNodes = listOf(Node(id = "n1", bookId = 1, title = "Node 1", questionCount = 2))

private fun mockPracticeUiState(
    isLoading: Boolean = false,
    questions: List<Question> = mockQuestions,
    currentIndex: Int = 0,
    userAnswers: Map<Int, UserAnswer> = emptyMap(),
    markedQuestions: Set<Int> = emptySet(),
    availableCollections: List<Collection> = emptyList()
): PracticeUiState = PracticeUiState(
    book = mockBook,
    nodes = mockNodes,
    questions = questions,
    currentIndex = currentIndex,
    currentPartitionId = "all",
    userAnswers = userAnswers,
    markedQuestions = markedQuestions,
    isLoading = isLoading,
    availableCollections = availableCollections
)

@Preview(showBackground = true)
@Composable
private fun PracticeScreenContentPreview() {
    MnemoraTheme {
        PracticeScreenContent(
            uiState = mockPracticeUiState(),
            onBack = {},
            onToggleMark = {},
            onResetCurrentQuestion = {},
            onAnswerQuestion = {},
            onGoToQuestion = {},
            onSelectNode = {},
            onLoadChatHistory = {},
            onLoadCollectionData = {},
            onToggleQuestionInCollection = {},
            onCreateCollection = {},
            onDeleteCollection = {},
            onSwitchChatSession = {},
            onCreateChatSession = {},
            onDeleteChatSession = {},
            onSendAiMessage = {},
            onCancelAiMessage = {},
            onSaveChatScrollPosition = { _, _ -> },
            onConfettiFinished = {},
            getQuestionStatus = { QuestionStatus.Unanswered },
            imageBasePath = null
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PracticeScreenContentLoadingPreview() {
    MnemoraTheme {
        PracticeScreenContent(
            uiState = mockPracticeUiState(isLoading = true),
            onBack = {},
            onToggleMark = {},
            onResetCurrentQuestion = {},
            onAnswerQuestion = {},
            onGoToQuestion = {},
            onSelectNode = {},
            onLoadChatHistory = {},
            onLoadCollectionData = {},
            onToggleQuestionInCollection = {},
            onCreateCollection = {},
            onDeleteCollection = {},
            onSwitchChatSession = {},
            onCreateChatSession = {},
            onDeleteChatSession = {},
            onSendAiMessage = {},
            onCancelAiMessage = {},
            onSaveChatScrollPosition = { _, _ -> },
            onConfettiFinished = {},
            getQuestionStatus = { QuestionStatus.Unanswered },
            imageBasePath = null
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PracticeScreenContentAnsweredPreview() {
    MnemoraTheme {
        PracticeScreenContent(
            uiState = mockPracticeUiState(
                currentIndex = 0,
                userAnswers = mapOf(1 to UserAnswer(selected = "A", isCorrect = true))
            ),
            onBack = {},
            onToggleMark = {},
            onResetCurrentQuestion = {},
            onAnswerQuestion = {},
            onGoToQuestion = {},
            onSelectNode = {},
            onLoadChatHistory = {},
            onLoadCollectionData = {},
            onToggleQuestionInCollection = {},
            onCreateCollection = {},
            onDeleteCollection = {},
            onSwitchChatSession = {},
            onCreateChatSession = {},
            onDeleteChatSession = {},
            onSendAiMessage = {},
            onCancelAiMessage = {},
            onSaveChatScrollPosition = { _, _ -> },
            onConfettiFinished = {},
            getQuestionStatus = { QuestionStatus.Unanswered },
            imageBasePath = null
        )
    }
}
