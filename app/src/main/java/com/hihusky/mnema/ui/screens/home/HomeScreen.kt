package com.hihusky.mnema.ui.screens.home

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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.res.painterResource
import com.hihusky.mnema.R
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import com.hihusky.mnema.ui.components.topappbar.MnemaCollapsibleTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.collectAsState
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
import com.hihusky.mnema.data.local.db.entity.StudySessionEntity
import com.hihusky.mnema.data.model.Book
import com.hihusky.mnema.ui.components.MnemaBookAvatar
import com.hihusky.mnema.ui.components.MnemaCard
import com.hihusky.mnema.ui.components.MnemaEmptyState
import com.hihusky.mnema.ui.theme.MnemaTheme
import com.hihusky.mnema.ui.theme.MnemaSize
import com.hihusky.mnema.ui.theme.MnemaSpacing
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
        onNavigateToPractice = onNavigateToPractice,
        onNavigateToReview = onNavigateToReview,
        onNavigateToTest = onNavigateToTest,
        onNavigateToPreview = onNavigateToPreview,
        onNavigateToBookDetail = onNavigateToBookDetail,

        onImport = { importLauncher.launch("*/*") },
        onRetry = { viewModel.loadBooks() },
        onDeleteBook = { viewModel.deleteBook(it) },
        onSearchQueryChange = viewModel::onSearchQueryChange
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun HomeScreenContent(
    uiState: HomeUiState,
    onNavigateToPractice: (Int, String?) -> Unit,
    onNavigateToReview: (Int) -> Unit,
    onNavigateToTest: (Int, Long?) -> Unit,
    onNavigateToPreview: (Int) -> Unit,
    onNavigateToBookDetail: (Int) -> Unit,

    onImport: () -> Unit,
    onRetry: () -> Unit,
    onDeleteBook: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
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
                MnemaCollapsibleTopAppBar(
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
                                onDetail = { onNavigateToBookDetail(book.id) },
                                onDelete = { bookToDelete = book }
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
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Importing", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
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
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {}
        )
    }

    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete book?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "\"${bookToDelete!!.displayName}\" and all its progress will be permanently removed.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBook(bookToDelete!!.id)
                        bookToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
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
    val topBarHeight = lerp(MnemaSize.TopBarExpanded, MnemaSize.TopBarCollapsed, fraction)
    val searchFieldHeight = lerp(MnemaSize.SearchFieldExpanded, MnemaSize.SearchFieldCollapsed, fraction)
    val fieldShape = MaterialTheme.shapes.medium

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBarHeight)
                .padding(horizontal = MnemaSpacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MnemaSpacing.XSmall)
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
                    .padding(start = MnemaSpacing.Medium, end = if (query.isEmpty()) MnemaSpacing.Medium else 2.dp),
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
                            modifier = Modifier.size(MnemaSize.IconSmall),
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
    MnemaEmptyState(
        icon = Icons.Default.Search,
        title = "No results for \"$query\"",
        message = "Try a different search term",
        modifier = Modifier
            .fillMaxSize()
    )
}

@Composable
private fun EmptyState(onImport: () -> Unit) {
    MnemaEmptyState(
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
    MnemaEmptyState(
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
    onDetail: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showModeSheet by remember { mutableStateOf(false) }
    var showRecordsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val recordsSheetState = rememberModalBottomSheetState()

    MnemaCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MnemaSpacing.Large, vertical = MnemaSpacing.Small)
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MnemaBookAvatar(
                    bookId = book.id,
                    displayName = book.displayName,
                    iconName = book.icon
                )
                Spacer(modifier = Modifier.width(MnemaSpacing.Large))
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
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Records") },
                            onClick = {
                                expanded = false
                                showRecordsSheet = true
                            },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Outlined.MenuBook, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Details") },
                            onClick = {
                                expanded = false
                                onDetail()
                            },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Outlined.Article, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                expanded = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MnemaSpacing.Medium))

            if (activeSession != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MnemaSpacing.Small)
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
                        modifier = Modifier.size(MnemaSize.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(MnemaSpacing.Small))
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

    if (showRecordsSheet) {
        val viewModel: HomeViewModel = hiltViewModel()
        val sessions by viewModel.getSessionsByBook(book.id).collectAsState(initial = emptyList())
        ModalBottomSheet(
            onDismissRequest = { showRecordsSheet = false },
            sheetState = recordsSheetState
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Records",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "History sessions for this book",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No records yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    sessions.forEach { session ->
                        val dateText = java.text.SimpleDateFormat(
                            "MMM dd, HH:mm",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(session.startTime))
                        val statusText = when {
                            session.isActive -> "In progress"
                            session.isCompleted -> "Completed"
                            else -> "Abandoned"
                        }
                        ListItem(
                            headlineContent = { Text("${session.mode} · $dateText") },
                            supportingContent = {
                                Text("$statusText · ${session.currentIndex}/${session.totalQuestions}")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRecordsSheet = false
                                    when (session.mode) {
                                        "Practice" -> onPractice()
                                        "Review" -> onReview()
                                        "Test" -> onTest(session.id)
                                        "Preview" -> onPreview()
                                    }
                                }
                        )
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
    MnemaTheme {
        HomeScreenContent(
            uiState = HomeUiState(isLoading = true),
            onNavigateToPractice = { _, _ -> },
            onNavigateToReview = {},
            onNavigateToTest = { _, _ -> },
            onNavigateToPreview = {},
            onNavigateToBookDetail = {},

            onImport = {},
            onRetry = {},
            onDeleteBook = {},
            onSearchQueryChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    MnemaTheme {
        HomeScreenContent(
            uiState = HomeUiState(),
            onNavigateToPractice = { _, _ -> },
            onNavigateToReview = {},
            onNavigateToTest = { _, _ -> },
            onNavigateToPreview = {},
            onNavigateToBookDetail = {},

            onImport = {},
            onRetry = {},
            onDeleteBook = {},
            onSearchQueryChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenWithBooksPreview() {
    MnemaTheme {
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
            onDeleteBook = {},
            onSearchQueryChange = {}
        )
    }
}
