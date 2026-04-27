package com.hihusky.mnemora.ui.screens.practice

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.GridView
import com.hihusky.mnemora.ui.components.MnemoraAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.hihusky.mnemora.data.model.Book
import com.hihusky.mnemora.data.model.Collection
import com.hihusky.mnemora.data.model.Node
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.QuestionChoice
import com.hihusky.mnemora.data.model.QuestionStatus
import com.hihusky.mnemora.data.model.QuestionType
import com.hihusky.mnemora.data.model.UserAnswer
import com.hihusky.mnemora.ui.components.AiChatPanel
import com.hihusky.mnemora.ui.components.CollectionSheet
import com.hihusky.mnemora.ui.components.ConfettiOverlay
import com.hihusky.mnemora.ui.components.DopamineProgressBar
import com.hihusky.mnemora.ui.components.NodeSelector
import com.hihusky.mnemora.ui.components.OverviewSheet
import com.hihusky.mnemora.ui.components.QuestionContent
import com.hihusky.mnemora.ui.components.topappbar.MnemoraTopAppBar
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.MnemoraTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PracticeScreen(
    onBack: () -> Unit,
    viewModel: PracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    PracticeScreenContent(
        uiState = uiState,
        onBack = onBack,
        onPreviousQuestion = viewModel::previousQuestion,
        onNextQuestion = viewModel::nextQuestion,
        onToggleMark = viewModel::toggleMark,
        onResetCurrentQuestion = viewModel::resetCurrentQuestion,
        onAnswerQuestion = viewModel::answerQuestion,
        onGoToQuestion = viewModel::goToQuestion,
        onSelectNode = viewModel::selectNode,
        onLoadCollectionData = viewModel::loadCollectionData,
        onToggleQuestionInCollection = viewModel::toggleQuestionInCollection,
        onCreateCollection = viewModel::createCollection,
        onDeleteCollection = viewModel::deleteCollection,
        onSwitchChatSession = viewModel::switchChatSession,
        onCreateChatSession = viewModel::createChatSession,
        onDeleteChatSession = viewModel::deleteChatSession,
        onSendAiMessage = viewModel::sendAiMessage,
        onCancelAiMessage = viewModel::cancelAiChat,
        onSaveChatScrollPosition = viewModel::saveChatScrollPosition,
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
    onPreviousQuestion: () -> Unit,
    onNextQuestion: () -> Unit,
    onToggleMark: () -> Unit,
    onResetCurrentQuestion: () -> Unit,
    onAnswerQuestion: (String) -> Unit,
    onGoToQuestion: (Int) -> Unit,
    onSelectNode: (String) -> Unit,
    onLoadCollectionData: () -> Unit,
    onToggleQuestionInCollection: (Int) -> Unit,
    onCreateCollection: (String) -> Unit,
    onDeleteCollection: (Int) -> Unit,
    onSwitchChatSession: (Int) -> Unit,
    onCreateChatSession: () -> Unit,
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
    val haptic = LocalHapticFeedback.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showOverview by remember { mutableStateOf(false) }
    var showAiChat by remember { mutableStateOf(false) }
    var showNodeSelector by remember { mutableStateOf(false) }
    var showCollectionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentIndex) {
        if (pagerState.currentPage != uiState.currentIndex && uiState.questions.isNotEmpty()) {
            pagerState.scrollToPage(uiState.currentIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.currentIndex) {
            onGoToQuestion(pagerState.currentPage)
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
                    IconButton(
                        onClick = onPreviousQuestion,
                        enabled = uiState.currentIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    }

                    IconButton(
                        onClick = { showAiChat = true },
                        enabled = uiState.currentQuestion != null
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant")
                    }

                    // Bookmark: tap = toggle mark, long-press = collection sheet
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 24.dp),
                                onClick = onToggleMark,
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onLoadCollectionData()
                                    showCollectionSheet = true
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isCurrentMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark / Add to collection",
                            tint = if (uiState.isCurrentMarked)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { showResetDialog = true },
                        enabled = uiState.currentUserAnswer != null && !uiState.isPreviewMode
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Reset answer")
                    }

                    IconButton(
                        onClick = onNextQuestion,
                        enabled = uiState.currentIndex < uiState.totalQuestions - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .onKeyEvent { event ->
                    when (event.key) {
                        Key.DirectionLeft -> { onPreviousQuestion(); true }
                        Key.DirectionRight -> { onNextQuestion(); true }
                        Key.A -> { showAiChat = true; true }
                        Key.M -> { onToggleMark(); true }
                        Key.R -> {
                            if (uiState.currentUserAnswer != null) onResetCurrentQuestion()
                            true
                        }
                        Key.One, Key.NumPad1 -> {
                            uiState.currentQuestion?.choices?.getOrNull(0)?.key?.let { onAnswerQuestion(it) }
                            true
                        }
                        Key.Two, Key.NumPad2 -> {
                            uiState.currentQuestion?.choices?.getOrNull(1)?.key?.let { onAnswerQuestion(it) }
                            true
                        }
                        Key.Three, Key.NumPad3 -> {
                            uiState.currentQuestion?.choices?.getOrNull(2)?.key?.let { onAnswerQuestion(it) }
                            true
                        }
                        Key.Four, Key.NumPad4 -> {
                            uiState.currentQuestion?.choices?.getOrNull(3)?.key?.let { onAnswerQuestion(it) }
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) { page ->
                    val question = uiState.questions[page]
                    val isCurrent = page == uiState.currentIndex
                    val answer = if (isCurrent) uiState.currentUserAnswer else null

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = MnemoraSpacing.Large,
                            vertical = MnemoraSpacing.Large
                        )
                    ) {
                        item {
                            QuestionContent(
                                question = question,
                                selectedOption = if (isCurrent) answer?.selected else null,
                                showAnswer = isCurrent && (answer != null || uiState.isPreviewMode),
                                onOptionSelected = if (isCurrent && answer == null && !uiState.isPreviewMode) {
                                    { onAnswerQuestion(it) }
                                } else null,
                                imageBasePath = imageBasePath
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.confettiId > 0L) {
        androidx.compose.runtime.key(uiState.confettiId) {
            ConfettiOverlay(onFinished = onConfettiFinished)
        }
    }

    } // end Box

    if (showResetDialog) {
        MnemoraAlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = "Reset Progress",
            message = "Clear your answer for this question? This cannot be undone.",
            confirmText = "Reset",
            onConfirm = {
                onResetCurrentQuestion()
                showResetDialog = false
            },
            dismissText = "Cancel",
            isDestructive = true
        )
    }

    if (showOverview) {
        OverviewSheet(
            totalQuestions = uiState.totalQuestions,
            currentIndex = uiState.currentIndex,
            getStatus = getQuestionStatus,
            onQuestionSelected = { onGoToQuestion(it); showOverview = false },
            onDismiss = { showOverview = false }
        )
    }

    if (showAiChat) {
        AiChatPanel(
            sessions = uiState.chatSessions,
            currentSessionId = uiState.currentChatSessionId,
            history = uiState.chatHistory,
            isLoading = uiState.isCurrentSessionLoading,
            streamingResponse = uiState.currentStreamingResponse,
            onSessionSelected = onSwitchChatSession,
            onCreateSession = onCreateChatSession,
            onSendMessage = onSendAiMessage,
            onCancelMessage = onCancelAiMessage,
            onDismiss = { showAiChat = false },
            onDeleteSession = onDeleteChatSession,
            modelLabel = uiState.aiModel,
            providerLabel = uiState.aiProvider,
            initialScrollIndex = uiState.chatScrollIndex,
            initialScrollOffset = uiState.chatScrollOffset,
            onSaveScrollPosition = onSaveChatScrollPosition
        )
    }

    if (showNodeSelector) {
        NodeSelector(
            nodes = uiState.nodes,
            currentPartitionId = uiState.currentPartitionId,
            onNodeSelected = { onSelectNode(it); showNodeSelector = false },
            onDismiss = { showNodeSelector = false }
        )
    }

    if (showCollectionSheet) {
        CollectionSheet(
            collections = uiState.availableCollections,
            questionCollectionIds = uiState.questionCollectionIds,
            onToggle = onToggleQuestionInCollection,
            onCreate = onCreateCollection,
            onDelete = onDeleteCollection,
            onDismiss = { showCollectionSheet = false }
        )
    }
}

//region Preview helpers

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

//endregion

@Preview(showBackground = true)
@Composable
private fun PracticeScreenContentPreview() {
    MnemoraTheme {
        PracticeScreenContent(
            uiState = mockPracticeUiState(),
            onBack = {},
            onPreviousQuestion = {},
            onNextQuestion = {},
            onToggleMark = {},
            onResetCurrentQuestion = {},
            onAnswerQuestion = {},
            onGoToQuestion = {},
            onSelectNode = {},
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
            onPreviousQuestion = {},
            onNextQuestion = {},
            onToggleMark = {},
            onResetCurrentQuestion = {},
            onAnswerQuestion = {},
            onGoToQuestion = {},
            onSelectNode = {},
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
            onPreviousQuestion = {},
            onNextQuestion = {},
            onToggleMark = {},
            onResetCurrentQuestion = {},
            onAnswerQuestion = {},
            onGoToQuestion = {},
            onSelectNode = {},
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
