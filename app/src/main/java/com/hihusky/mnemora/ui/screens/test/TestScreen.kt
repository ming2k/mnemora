package com.hihusky.mnemora.ui.screens.test

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.QuestionChoice
import com.hihusky.mnemora.data.model.QuestionType
import com.hihusky.mnemora.data.model.UserAnswer
import com.hihusky.mnemora.ui.components.DopamineProgressBar
import com.hihusky.mnemora.ui.components.MnemoraAlertDialog
import com.hihusky.mnemora.ui.components.MnemoraCard
import com.hihusky.mnemora.ui.components.QuestionContent
import com.hihusky.mnemora.ui.components.topappbar.MnemoraTopAppBar
import com.hihusky.mnemora.ui.theme.MnemoraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    onBack: () -> Unit,
    viewModel: TestViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    TestScreenContent(
        onBack = onBack,
        uiState = uiState,
        onAnswer = { viewModel.answerQuestion(it) },
        onPrevious = { viewModel.previousQuestion() },
        onNext = { viewModel.nextQuestion() },
        onFinish = { viewModel.finishTest() },
        onRetake = { viewModel.resetTest() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TestScreenContent(
    onBack: () -> Unit,
    uiState: TestUiState,
    onAnswer: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFinishDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MnemoraTopAppBar(
                title = {
                    Text("Test", style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = { showFinishDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Finish",
                        )
                    }
                },
                actions = {
                    Text(
                        uiState.formattedTime,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            androidx.compose.ui.Modifier
                                .padding(end = 8.dp),
                    )
                    TextButton(onClick = { showFinishDialog = true }) {
                        Text("Finish")
                    }
                },
            )
        },
        bottomBar = {
            if (uiState.isRunning) {
                androidx.compose.material3.BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            enabled = uiState.currentIndex > 0,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous",
                            )
                        }
                        Text(
                            "${uiState.currentIndex + 1} / ${uiState.totalQuestions}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        IconButton(
                            onClick = onNext,
                            enabled = uiState.currentIndex < uiState.totalQuestions - 1,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next",
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = modifier.padding(padding)) {
            DopamineProgressBar(progress = uiState.progress)

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.showResults -> {
                    TestResults(
                        uiState = uiState,
                        onBack = onBack,
                        onRetake = onRetake,
                    )
                }

                uiState.currentQuestion != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        item {
                            QuestionContent(
                                question = uiState.currentQuestion!!,
                                selectedOption = uiState.userAnswers[uiState.currentQuestion!!.id]?.selected,
                                showAnswer = false,
                                onOptionSelected = { onAnswer(it) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFinishDialog) {
        MnemoraAlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = "Finish Test",
            message = "Are you sure you want to finish the test? Unanswered questions will be marked as wrong.",
            confirmText = "Finish",
            onConfirm = {
                onFinish()
                showFinishDialog = false
            },
            dismissText = "Cancel",
            isDestructive = true,
        )
    }
}

@Composable
private fun TestResults(
    uiState: TestUiState,
    onBack: () -> Unit,
    onRetake: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Test Result",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(24.dp))

        MnemoraCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(20.dp),
        ) {
            ResultRow("Total", uiState.totalQuestions.toString())
            HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            ResultRow(
                "Correct",
                uiState.correctCount.toString(),
                MaterialTheme.colorScheme.primary,
            )
            ResultRow(
                "Wrong",
                uiState.wrongCount.toString(),
                MaterialTheme.colorScheme.error,
            )
            ResultRow(
                "Unanswered",
                uiState.unansweredCount.toString(),
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            ResultRow("Time", uiState.formattedTime)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onRetake,
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retake")
            }
            Button(
                onClick = onBack,
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Home")
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TestScreenContentActivePreview() {
    val mockQuestion =
        Question(
            id = 1,
            bookId = 1,
            content = "What is the capital of France?",
            choices =
                listOf(
                    QuestionChoice("A", "London"),
                    QuestionChoice("B", "Paris"),
                    QuestionChoice("C", "Berlin"),
                    QuestionChoice("D", "Madrid"),
                ),
            answer = "B",
            questionType = QuestionType.MultipleChoice,
        )
    MnemoraTheme {
        TestScreenContent(
            onBack = {},
            uiState =
                TestUiState(
                    questions = listOf(mockQuestion),
                    currentIndex = 0,
                    isLoading = false,
                    isRunning = true,
                    userAnswers = emptyMap(),
                ),
            onAnswer = {},
            onPrevious = {},
            onNext = {},
            onFinish = {},
            onRetake = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TestScreenContentResultsPreview() {
    val q1 =
        Question(
            id = 1,
            bookId = 1,
            content = "What is the capital of France?",
            choices =
                listOf(
                    QuestionChoice("A", "London"),
                    QuestionChoice("B", "Paris"),
                    QuestionChoice("C", "Berlin"),
                    QuestionChoice("D", "Madrid"),
                ),
            answer = "B",
            questionType = QuestionType.MultipleChoice,
        )
    val q2 =
        Question(
            id = 2,
            bookId = 1,
            content = "Which planet is known as the Red Planet?",
            choices =
                listOf(
                    QuestionChoice("A", "Venus"),
                    QuestionChoice("B", "Mars"),
                    QuestionChoice("C", "Jupiter"),
                    QuestionChoice("D", "Saturn"),
                ),
            answer = "B",
            questionType = QuestionType.MultipleChoice,
        )
    MnemoraTheme {
        TestScreenContent(
            onBack = {},
            uiState =
                TestUiState(
                    questions = listOf(q1, q2),
                    currentIndex = 1,
                    isLoading = false,
                    isRunning = false,
                    showResults = true,
                    userAnswers =
                        mapOf(
                            1 to UserAnswer(selected = "B", isCorrect = true),
                            2 to UserAnswer(selected = "A", isCorrect = false),
                        ),
                    elapsedSeconds = 125,
                ),
            onAnswer = {},
            onPrevious = {},
            onNext = {},
            onFinish = {},
            onRetake = {},
        )
    }
}
