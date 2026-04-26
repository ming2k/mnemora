package com.hihusky.mnema.ui.screens.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import com.hihusky.mnema.ui.components.topappbar.MnemaCenterTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hihusky.mnema.data.model.Question
import com.hihusky.mnema.data.model.QuestionChoice
import com.hihusky.mnema.data.model.QuestionType
import com.hihusky.mnema.data.model.SrsRating
import com.hihusky.mnema.ui.components.QuestionContent
import com.hihusky.mnema.ui.theme.MnemaAlpha
import com.hihusky.mnema.ui.theme.MnemaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ReviewScreenContent(
        onBack = onBack,
        uiState = uiState,
        onRate = { viewModel.rateCurrent(it) },
        getIntervalLabel = { viewModel.getIntervalLabel(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewScreenContent(
    onBack: () -> Unit,
    uiState: ReviewUiState,
    onRate: (SrsRating) -> Unit,
    getIntervalLabel: (SrsRating) -> String,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            MnemaCenterTopAppBar(
                title = {
                    Text(
                        text = "Review",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .onKeyEvent { event ->
                    when (event.key) {
                        Key.One, Key.NumPad1 -> {
                            onRate(SrsRating.Again)
                            true
                        }
                        Key.Two, Key.NumPad2 -> {
                            onRate(SrsRating.Hard)
                            true
                        }
                        Key.Three, Key.NumPad3 -> {
                            onRate(SrsRating.Good)
                            true
                        }
                        Key.Four, Key.NumPad4 -> {
                            onRate(SrsRating.Easy)
                            true
                        }
                        else -> false
                    }
                }
        ) {
            // Progress indicator at top
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.isComplete -> {
                    CompleteState(onBack = onBack)
                }

                uiState.currentQuestion != null -> {
                    ReviewContent(
                        uiState = uiState,
                        onRate = onRate,
                        getIntervalLabel = getIntervalLabel
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewContent(
    uiState: ReviewUiState,
    onRate: (SrsRating) -> Unit,
    getIntervalLabel: (SrsRating) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        QuestionContent(
            question = uiState.currentQuestion!!,
            selectedOption = null,
            showAnswer = false,
            onOptionSelected = null
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "How well did you recall?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // M3 button hierarchy: Filled for primary, Tonal for secondary, Outlined for tertiary.
        // Rating actions are primary workflow steps → Filled buttons with semantic color containers.
        val ratings = listOf(
            SrsRating.Again to MaterialTheme.colorScheme.errorContainer,
            SrsRating.Hard to MaterialTheme.colorScheme.tertiaryContainer,
            SrsRating.Good to MaterialTheme.colorScheme.secondaryContainer,
            SrsRating.Easy to MaterialTheme.colorScheme.primaryContainer
        )

        ratings.forEach { (rating, containerColor) ->
            val contentColor = when (rating) {
                SrsRating.Again -> MaterialTheme.colorScheme.onErrorContainer
                SrsRating.Hard -> MaterialTheme.colorScheme.onTertiaryContainer
                SrsRating.Good -> MaterialTheme.colorScheme.onSecondaryContainer
                SrsRating.Easy -> MaterialTheme.colorScheme.onPrimaryContainer
            }
            val label = getIntervalLabel(rating)

            FilledTonalButton(
                onClick = { onRate(rating) },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        rating.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = MnemaAlpha.Strong)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Press 1–4 on keyboard",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = MnemaAlpha.Deemphasized),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun CompleteState(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Review Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Great job keeping up with your studies.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onBack,
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Back to Home")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewScreenContentActivePreview() {
    val mockQuestion = Question(
        id = 1,
        bookId = 1,
        content = "What is the capital of France?",
        choices = listOf(
            QuestionChoice("A", "London"),
            QuestionChoice("B", "Paris"),
            QuestionChoice("C", "Berlin"),
            QuestionChoice("D", "Madrid")
        ),
        answer = "B",
        questionType = QuestionType.MultipleChoice
    )
    MnemaTheme {
        ReviewScreenContent(
            onBack = {},
            uiState = ReviewUiState(
                questions = listOf(mockQuestion),
                currentIndex = 0,
                isLoading = false,
                isComplete = false
            ),
            onRate = {},
            getIntervalLabel = { rating ->
                when (rating) {
                    SrsRating.Again -> "< 1m"
                    SrsRating.Hard -> "1d"
                    SrsRating.Good -> "3d"
                    SrsRating.Easy -> "7d"
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewScreenContentCompletePreview() {
    val mockQuestion = Question(
        id = 1,
        bookId = 1,
        content = "What is the capital of France?",
        choices = listOf(
            QuestionChoice("A", "London"),
            QuestionChoice("B", "Paris"),
            QuestionChoice("C", "Berlin"),
            QuestionChoice("D", "Madrid")
        ),
        answer = "B",
        questionType = QuestionType.MultipleChoice
    )
    MnemaTheme {
        ReviewScreenContent(
            onBack = {},
            uiState = ReviewUiState(
                questions = listOf(mockQuestion),
                currentIndex = 1,
                isLoading = false,
                isComplete = true
            ),
            onRate = {},
            getIntervalLabel = { "" }
        )
    }
}
