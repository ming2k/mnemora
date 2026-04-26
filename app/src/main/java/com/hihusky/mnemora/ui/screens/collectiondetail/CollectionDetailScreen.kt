package com.hihusky.mnemora.ui.screens.collectiondetail

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.hihusky.mnemora.ui.components.topappbar.MnemoraTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hihusky.mnemora.data.model.Collection
import com.hihusky.mnemora.data.model.CollectionKind
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.QuestionChoice
import com.hihusky.mnemora.data.model.QuestionType
import com.hihusky.mnemora.ui.components.MnemoraCard
import com.hihusky.mnemora.ui.components.QuestionContent
import com.hihusky.mnemora.ui.theme.MnemoraSize
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.MnemoraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    onBack: () -> Unit,
    onNavigateToPractice: (Int, Int) -> Unit,
    viewModel: CollectionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.deleted) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }
    }

    CollectionDetailScreenContent(
        uiState = uiState,
        onBack = onBack,
        onNavigateToPractice = onNavigateToPractice,
        onDeleteCollection = { viewModel.deleteCollection() },
        onRetry = { viewModel.loadCollection() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollectionDetailScreenContent(
    uiState: CollectionDetailUiState,
    onBack: () -> Unit,
    onNavigateToPractice: (Int, Int) -> Unit,
    onDeleteCollection: () -> Unit,
    onRetry: () -> Unit
) {
    val collection = uiState.collection
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MnemoraTopAppBar(
                title = {
                    Text(
                        collection?.name ?: "Collection",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (collection != null && uiState.questions.isNotEmpty()) {
                        IconButton(onClick = {
                            val bookId = uiState.representativeBookId ?: -1
                            onNavigateToPractice(bookId, collection.id)
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Practice")
                        }
                    }
                    if (collection != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(MnemoraSize.AvatarLarge),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Error: ${uiState.error}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            IconButton(onClick = onRetry) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry")
                            }
                        }
                    }
                }

                collection != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            MnemoraCard(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                    if (!collection.description.isNullOrBlank()) {
                                        Text(
                                            collection.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(MnemoraSpacing.Small))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${uiState.questions.size} questions",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(collection.kind.name) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                labelColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        items(uiState.questions, key = { it.id }) { question ->
                            QuestionContent(
                                question = question,
                                selectedOption = null,
                                showAnswer = false,
                                onOptionSelected = null,
                                imageBasePath = uiState.imageBasePath
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    "Delete Collection?",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this collection? The questions will not be removed.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteCollection()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ────────────────────────────────────────────────────────────
// Previews
// ────────────────────────────────────────────────────────────

private val previewCollectionQuestion = Question(
    id = 1,
    bookId = 1,
    content = "What does this buoy indicate?",
    choices = listOf(
        QuestionChoice("A", "Safe water"),
        QuestionChoice("B", "Danger"),
        QuestionChoice("C", "Channel edge")
    ),
    answer = "A",
    explanation = "Safe water marks indicate safe water all around.",
    questionType = QuestionType.MultipleChoice
)

@Preview(showBackground = true)
@Composable
private fun CollectionDetailScreenLoadingPreview() {
    MnemoraTheme {
        CollectionDetailScreenContent(
            uiState = CollectionDetailUiState(isLoading = true),
            onBack = {},
            onNavigateToPractice = { _, _ -> },
            onDeleteCollection = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionDetailScreenLoadedPreview() {
    MnemoraTheme {
        CollectionDetailScreenContent(
            uiState = CollectionDetailUiState(
                collection = Collection(
                    id = 1,
                    bookId = 1,
                    kind = CollectionKind.Custom,
                    behavior = com.hihusky.mnemora.data.model.CollectionBehavior.Manual,
                    name = "Weak Cards",
                    description = "Questions I got wrong before",
                    createdAt = 0L
                ),
                questions = List(5) { i ->
                    previewCollectionQuestion.copy(id = i)
                }
            ),
            onBack = {},
            onNavigateToPractice = { _, _ -> },
            onDeleteCollection = {},
            onRetry = {}
        )
    }
}
