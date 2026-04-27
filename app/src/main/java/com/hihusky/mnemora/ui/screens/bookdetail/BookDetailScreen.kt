package com.hihusky.mnemora.ui.screens.bookdetail

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FolderOpen
import com.hihusky.mnemora.ui.components.MnemoraAlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hihusky.mnemora.data.local.db.entity.StudySessionEntity
import com.hihusky.mnemora.data.model.Book
import com.hihusky.mnemora.data.model.Collection
import com.hihusky.mnemora.data.model.CollectionKind
import com.hihusky.mnemora.data.model.Node
import com.hihusky.mnemora.data.model.SrsStats
import com.hihusky.mnemora.ui.components.MnemoraBookAvatar
import com.hihusky.mnemora.ui.components.MnemoraCard
import com.hihusky.mnemora.ui.components.MnemoraSettingsDivider
import com.hihusky.mnemora.ui.components.MnemoraSettingsGroup
import com.hihusky.mnemora.ui.components.MnemoraSettingsSectionHeader
import com.hihusky.mnemora.ui.components.topappbar.MnemoraTopAppBar
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.MnemoraTheme
import com.hihusky.mnemora.ui.theme.identityContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onBack: () -> Unit,
    onNavigateToPractice: (Int, String?, String?) -> Unit,
    onNavigateToCollection: (Int) -> Unit,
    onResumeSession: (Int, String, Long?) -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BookDetailScreenContent(
        uiState = uiState,
        onBack = onBack,
        onNavigateToPractice = onNavigateToPractice,
        onNavigateToCollection = onNavigateToCollection,
        onResumeSession = onResumeSession,
        onCreateCollection = { name, desc -> viewModel.createCollection(name, desc) },
        onDeleteBook = { viewModel.deleteBook(onBack) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookDetailScreenContent(
    uiState: BookDetailUiState,
    onBack: () -> Unit,
    onNavigateToPractice: (Int, String?, String?) -> Unit,
    onNavigateToCollection: (Int) -> Unit,
    onResumeSession: (Int, String, Long?) -> Unit,
    onCreateCollection: (String, String) -> Unit,
    onDeleteBook: () -> Unit,
) {
    val book = uiState.book
    var showCreateSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MnemoraTopAppBar(
                title = {
                    Text(
                        text = book?.displayName ?: "Book",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (book != null) {
                        IconButton(onClick = { showCreateSheet = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "New collection",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete package",
                                tint = MaterialTheme.colorScheme.error
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                book != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = MnemoraSpacing.XLarge)
                    ) {
                        item {
                            BookSummaryCard(uiState, modifier = Modifier.padding(MnemoraSpacing.Large))
                        }

                        if (uiState.collections.isNotEmpty()) {
                            item {
                                MnemoraSettingsSectionHeader(title = "Collections")
                                MnemoraSettingsGroup {
                                    uiState.collections.forEachIndexed { index, collection ->
                                        if (index > 0) MnemoraSettingsDivider()
                                        CollectionRow(
                                            collection = collection,
                                            onClick = { onNavigateToCollection(collection.id) }
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.sessions.isNotEmpty()) {
                            item {
                                MnemoraSettingsSectionHeader(title = "Records")
                                MnemoraSettingsGroup {
                                    uiState.sessions.forEachIndexed { index, session ->
                                        if (index > 0) MnemoraSettingsDivider()
                                        SessionRow(
                                            session = session,
                                            onClick = {
                                                onResumeSession(
                                                    session.bookId,
                                                    session.mode,
                                                    if (session.isActive || session.isCompleted) session.id else null
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.nodes.isNotEmpty()) {
                            item {
                                MnemoraSettingsSectionHeader(title = "Nodes")
                                MnemoraSettingsGroup {
                                    uiState.nodes.forEachIndexed { index, node ->
                                        if (index > 0) MnemoraSettingsDivider()
                                        NodeRow(
                                            node = node,
                                            onClick = { onNavigateToPractice(book.id, node.id, null) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && book != null) {
        MnemoraAlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = "Delete package?",
            message = "\"${book.displayName}\" and all of its questions, collections, records, progress, and AI chats will be permanently removed.",
            confirmText = "Delete",
            onConfirm = {
                showDeleteDialog = false
                onDeleteBook()
            },
            dismissText = "Cancel",
            isDestructive = true
        )
    }

    if (showCreateSheet) {
        CreateCollectionSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { name, desc ->
                onCreateCollection(name, desc)
                showCreateSheet = false
            }
        )
    }
}

@Composable
private fun SessionRow(session: StudySessionEntity, onClick: () -> Unit) {
    val progress = if (session.totalQuestions > 0) {
        "${session.currentIndex.coerceAtMost(session.totalQuestions)}/${session.totalQuestions}"
    } else {
        "0/0"
    }
    val status = when {
        session.isActive -> "In progress"
        session.isCompleted -> "Completed"
        else -> "Closed"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Medium)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary.identityContainer()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.mode,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${formatSessionTime(session.lastActiveTime)} · $status",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            progress,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatSessionTime(timestamp: Long): String {
    return SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))
}

@Composable
private fun BookSummaryCard(uiState: BookDetailUiState, modifier: Modifier = Modifier) {
    val book = uiState.book ?: return

    MnemoraCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(MnemoraSpacing.XLarge)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MnemoraBookAvatar(
                bookId = book.id,
                displayName = book.displayName,
                iconName = book.icon
            )
            Spacer(Modifier.width(MnemoraSpacing.Large))
            Column {
                Text(
                    book.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!book.description.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        book.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(MnemoraSpacing.XLarge))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCell("Total", book.totalQuestions.toString())
            StatCell("Answered", uiState.answeredCount.toString())
            StatCell("Due", uiState.srsStats.dueToday.toString())
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CollectionRow(collection: Collection, onClick: () -> Unit) {
    val isSmart = collection.isSmart
    val accentColor = if (isSmart) MaterialTheme.colorScheme.tertiary
    else MaterialTheme.colorScheme.secondary
    val icon = if (isSmart) Icons.Default.AutoAwesome else Icons.Outlined.FolderOpen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Medium)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = MaterialTheme.shapes.small,
            color = accentColor.identityContainer()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            collection.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!collection.description.isNullOrBlank()) {
            Text(
                collection.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NodeRow(node: Node, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Medium)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                node.displayTitle,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (node.children.isNotEmpty()) {
                Text(
                    "${node.children.size} sub-nodes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            node.questionCount.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCollectionSheet(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = MnemoraSpacing.XLarge)
                .padding(bottom = MnemoraSpacing.XXLarge),
            verticalArrangement = Arrangement.spacedBy(MnemoraSpacing.Medium)
        ) {
            Text(
                "New Collection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(MnemoraSpacing.XSmall))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Purpose (optional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(Modifier.height(MnemoraSpacing.Small))

            Button(
                onClick = { if (name.isNotBlank()) onCreate(name.trim(), description.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookDetailScreenPreviewLoading() {
    MnemoraTheme {
        BookDetailScreenContent(
            uiState = BookDetailUiState(isLoading = true),
            onBack = {},
            onNavigateToPractice = { _, _, _ -> },
            onNavigateToCollection = {},
            onResumeSession = { _, _, _ -> },
            onCreateCollection = { _, _ -> },
            onDeleteBook = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookDetailScreenPreviewLoaded() {
    MnemoraTheme {
        BookDetailScreenContent(
            uiState = BookDetailUiState(
                book = Book(
                    id = 1, filename = "test.json", name = "Navigation Rules",
                    description = "International and Inland Rules of the Road",
                    totalQuestions = 120, totalNodes = 12, sortOrder = 0
                ),
                nodes = listOf(
                    Node(id = "n1", bookId = 1, title = "Part A — General", questionCount = 10,
                        children = listOf(Node(id = "n1_0", bookId = 1, title = "Rule 1", questionCount = 5, depth = 1))),
                    Node(id = "n2", bookId = 1, title = "Part B — Steering", questionCount = 15)
                ),
                collections = listOf(
                    Collection(id = 1, bookId = 1, kind = CollectionKind.Custom,
                        behavior = com.hihusky.mnemora.data.model.CollectionBehavior.Manual,
                        name = "Exam Prep", description = "For Thursday", createdAt = 0L),
                    Collection(id = 2, bookId = 1, kind = CollectionKind.Smart,
                        behavior = com.hihusky.mnemora.data.model.CollectionBehavior.SmartFilter,
                        name = "Wrong Answers", createdAt = 0L)
                ),
                srsStats = SrsStats(total = 50, dueToday = 8, newCards = 5, learning = 12, review = 3),
                answeredCount = 86
            ),
            onBack = {},
            onNavigateToPractice = { _, _, _ -> },
            onNavigateToCollection = {},
            onResumeSession = { _, _, _ -> },
            onCreateCollection = { _, _ -> },
            onDeleteBook = {}
        )
    }
}
