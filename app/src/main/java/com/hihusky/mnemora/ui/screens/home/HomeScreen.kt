package com.hihusky.mnemora.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.res.painterResource
import com.hihusky.mnemora.R
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import com.hihusky.mnemora.ui.components.topappbar.MnemoraCollapsibleTopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hihusky.mnemora.data.local.db.entity.StudySessionEntity
import com.hihusky.mnemora.data.model.Book
import com.hihusky.mnemora.ui.components.MnemoraBookAvatar
import com.hihusky.mnemora.ui.components.MnemoraCard
import com.hihusky.mnemora.ui.components.MnemoraEmptyState
import com.hihusky.mnemora.ui.theme.MnemoraTheme
import com.hihusky.mnemora.ui.theme.MnemoraSize
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPractice: (Int, String?) -> Unit,
    onNavigateToReview: (Int) -> Unit,
    onNavigateToTest: (Int, Long?) -> Unit,
    onNavigateToPreview: (Int) -> Unit,
    onNavigateToBookDetail: (Int) -> Unit,

    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.importPackage(uri)
    }

    LaunchedEffect(uiState.importSuccess) {
        uiState.importSuccess?.let { name ->
            snackbarHostState.showSnackbar(
                message = "Imported \"$name\"",
                duration = SnackbarDuration.Short
            )
            viewModel.dismissImportSuccess()
        }
    }
    LaunchedEffect(uiState.importError) {
        uiState.importError?.let { error ->
            snackbarHostState.showSnackbar(
                message = "Import failed: $error",
                duration = SnackbarDuration.Long
            )
            viewModel.dismissImportError()
        }
    }

    HomeScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateToPractice = onNavigateToPractice,
        onNavigateToReview = onNavigateToReview,
        onNavigateToTest = onNavigateToTest,
        onNavigateToPreview = onNavigateToPreview,
        onNavigateToBookDetail = onNavigateToBookDetail,

        onImport = { importLauncher.launch("*/*") },
        onRetry = { viewModel.loadBooks() },
        onSearchQueryChange = viewModel::onSearchQueryChange
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun HomeScreenContent(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateToPractice: (Int, String?) -> Unit,
    onNavigateToReview: (Int) -> Unit,
    onNavigateToTest: (Int, Long?) -> Unit,
    onNavigateToPreview: (Int) -> Unit,
    onNavigateToBookDetail: (Int) -> Unit,

    onImport: () -> Unit,
    onRetry: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollFraction by remember {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset
            when {
                firstIndex > 0 -> 1f
                else -> (firstOffset / 120f).coerceIn(0f, 1f)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSearchActive) {
                LibrarySearchTopBar(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClose = {
                        isSearchActive = false
                        onSearchQueryChange("")
                    },
                    scrollFraction = scrollFraction
                )
            } else {
                MnemoraCollapsibleTopAppBar(
                    title = "Library",
                    scrollFraction = scrollFraction,
                    actions = {
                        // Search icon — always visible
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = onImport) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "Import package"
                            )
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading && uiState.books.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && uiState.books.isEmpty() -> {
                    EmptyErrorState(
                        message = uiState.error,
                        onRetry = onRetry
                    )
                }

                uiState.books.isEmpty() && uiState.searchQuery.isBlank() -> {
                    EmptyState(onImport = onImport)
                }

                uiState.books.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    SearchEmptyState(query = uiState.searchQuery)
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.books,
                            key = { it.id }
                        ) { book ->
                            BookCard(
                                book = book,
                                activeSession = uiState.activeSessions[book.id],
                                onPractice = { onNavigateToPractice(book.id, null) },
                                onReview = { onNavigateToReview(book.id) },
                                onTest = { sessionId -> onNavigateToTest(book.id, sessionId) },
                                onPreview = { onNavigateToPreview(book.id) },
                                onDetail = { onNavigateToBookDetail(book.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.importStatus != null) {
        val importStatus = uiState.importStatus
        val importProgress = uiState.importProgress
        Dialog(onDismissRequest = { }) {
            androidx.compose.material3.Surface(
                modifier = Modifier.width(270.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Importing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (importProgress != null) {
                        LinearProgressIndicator(
                            progress = { importProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        importStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

}

@Composable
private fun LibrarySearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    scrollFraction: Float,
    modifier: Modifier = Modifier
) {
    val fraction = scrollFraction.coerceIn(0f, 1f)
    val topBarHeight = lerp(MnemoraSize.TopBarExpanded, MnemoraSize.TopBarCollapsed, fraction)
    val searchFieldHeight = lerp(MnemoraSize.SearchFieldExpanded, MnemoraSize.SearchFieldCollapsed, fraction)
    val fieldShape = MaterialTheme.shapes.medium

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight)
                .padding(horizontal = MnemoraSpacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.XSmall)
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close search"
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(searchFieldHeight)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, fieldShape)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, fieldShape)
                    .padding(start = MnemoraSpacing.Medium, end = if (query.isEmpty()) MnemoraSpacing.Medium else 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search books...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(MnemoraSize.IconSmall),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SearchEmptyState(query: String) {
    MnemoraEmptyState(
        icon = Icons.Default.Search,
        title = "No results for \"$query\"",
        message = "Try a different search term",
        modifier = Modifier
            .fillMaxSize()
    )
}

@Composable
private fun EmptyState(onImport: () -> Unit) {
    MnemoraEmptyState(
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        title = "No books yet",
        message = "Import a study package to get started",
        modifier = Modifier
            .fillMaxSize()
    ) {
        androidx.compose.material3.FilledTonalButton(
            onClick = onImport,
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Package")
        }
    }
}

@Composable
private fun EmptyErrorState(message: String, onRetry: () -> Unit) {
    MnemoraEmptyState(
        icon = Icons.AutoMirrored.Outlined.HelpOutline,
        title = message,
        message = null,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        isError = true
    ) {
        androidx.compose.material3.FilledTonalButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BookCard(
    book: Book,
    activeSession: StudySessionEntity?,
    onPractice: () -> Unit,
    onReview: () -> Unit,
    onTest: (Long?) -> Unit,
    onPreview: () -> Unit,
    onDetail: () -> Unit
) {
    var showModeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    MnemoraCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Small)
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MnemoraBookAvatar(
                    bookId = book.id,
                    displayName = book.displayName,
                    iconName = book.icon
                )
                Spacer(modifier = Modifier.width(MnemoraSpacing.Large))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${book.totalQuestions} questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDetail) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open package",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(MnemoraSpacing.Medium))

            if (activeSession != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Small)
                ) {
                    val resumeAction = {
                        when (activeSession.mode) {
                            "Practice" -> onPractice()
                            "Review" -> onReview()
                            "Test" -> onTest(activeSession.id)
                            "Preview" -> onPreview()
                            else -> onPractice()
                        }
                    }
                    androidx.compose.material3.FilledTonalButton(
                        onClick = resumeAction,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Resume ${activeSession.mode}")
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showModeSheet = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("New")
                    }
                }
            } else {
                androidx.compose.material3.FilledTonalButton(
                    onClick = { showModeSheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_practice),
                        contentDescription = null,
                        modifier = Modifier.size(MnemoraSize.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(MnemoraSpacing.Small))
                    Text("Start")
                }
            }
    }

    if (showModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModeSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Choose a mode",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Select how you want to study this book",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val modes = listOf(
                    Triple(
                        "Practice",
                        "Practice with instant feedback and explanations",
                        R.drawable.ic_practice to onPractice
                    ),
                    Triple(
                        "Review",
                        "Review due questions based on your memory curve",
                        R.drawable.ic_review to onReview
                    ),
                    Triple(
                        "Test",
                        "Simulate exam conditions with a timer",
                        R.drawable.ic_test to { onTest(null) }
                    ),
                    Triple(
                        "Preview",
                        "Quickly scan all questions without scoring",
                        R.drawable.ic_preview to onPreview
                    )
                )

                modes.forEach { (title, desc, pair) ->
                    val (iconRes, onClick) = pair
                    androidx.compose.material3.Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                showModeSheet = false
                                onClick()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}



// ────────────────────────────────────────────────────────────
// Previews
// ────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    MnemoraTheme {
        HomeScreenContent(
            uiState = HomeUiState(isLoading = true),
            onNavigateToPractice = { _, _ -> },
            onNavigateToReview = {},
            onNavigateToTest = { _, _ -> },
            onNavigateToPreview = {},
            onNavigateToBookDetail = {},

            onImport = {},
            onRetry = {},
            onSearchQueryChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    MnemoraTheme {
        HomeScreenContent(
            uiState = HomeUiState(),
            onNavigateToPractice = { _, _ -> },
            onNavigateToReview = {},
            onNavigateToTest = { _, _ -> },
            onNavigateToPreview = {},
            onNavigateToBookDetail = {},

            onImport = {},
            onRetry = {},
            onSearchQueryChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenWithBooksPreview() {
    MnemoraTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                books = listOf(
                    Book(
                        id = 1,
                        filename = "sample.zip",
                        name = "Sample Book",
                        totalQuestions = 120,
                        totalNodes = 12
                    ),
                    Book(
                        id = 2,
                        filename = "test.zip",
                        name = "Another Subject",
                        totalQuestions = 85,
                        totalNodes = 8
                    )
                )
            ),
            onNavigateToPractice = { _, _ -> },
            onNavigateToReview = {},
            onNavigateToTest = { _, _ -> },
            onNavigateToPreview = {},
            onNavigateToBookDetail = {},

            onImport = {},
            onRetry = {},
            onSearchQueryChange = {}
        )
    }
}
