package com.hihusky.mnema.ui.screens.bookdetail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.hihusky.mnema.ui.components.topappbar.MnemaTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hihusky.mnema.data.model.Book
import com.hihusky.mnema.data.model.Collection
import com.hihusky.mnema.data.model.CollectionKind
import com.hihusky.mnema.data.model.Node
import com.hihusky.mnema.data.model.SrsStats
import com.hihusky.mnema.ui.components.MnemaBookAvatar
import com.hihusky.mnema.ui.components.MnemaCard
import com.hihusky.mnema.ui.theme.MnemaSpacing
import com.hihusky.mnema.ui.theme.MnemaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onBack: () -> Unit,
    onNavigateToPractice: (Int, String?, String?) -> Unit,
    onNavigateToCollection: (Int) -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BookDetailScreenContent(
        uiState = uiState,
        onBack = onBack,
        onNavigateToPractice = onNavigateToPractice,
        onNavigateToCollection = onNavigateToCollection,
        onCreateCollection = { name, desc ->
            viewModel.createCollection(name, desc)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookDetailScreenContent(
    uiState: BookDetailUiState,
    onBack: () -> Unit,
    onNavigateToPractice: (Int, String?, String?) -> Unit,
    onNavigateToCollection: (Int) -> Unit,
    onCreateCollection: (String, String) -> Unit,
) {
    val book = uiState.book
    var showCreateDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            MnemaTopAppBar(
                title = {
                    Text(
                        text = book?.displayName ?: "Book Detail",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
        floatingActionButton = {
            if (book != null) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Collection") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large
                )
            }
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

                book != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        item {
                            BookInfoCard(uiState)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Filter chips for book-bound filters
                        item {
                            FilterChips(
                                bookId = book.id,
                                markedCount = uiState.markedCount,
                                wrongCount = uiState.wrongCount,
                                unansweredCount = uiState.unansweredCount,
                                dueCount = uiState.dueCount,
                                onNavigateToPractice = onNavigateToPractice
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Collections section
                        item {
                            SectionHeader(
                                title = "Collections",
                                icon = Icons.Outlined.FolderOpen
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (uiState.collections.isEmpty()) {
                            item {
                                Text(
                                    "No collections yet. Tap + to create one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        items(uiState.collections) { collection ->
                            CollectionListItem(
                                collection = collection,
                                onClick = { onNavigateToCollection(collection.id) }
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }

                        // Nodes section
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            SectionHeader(
                                title = "Nodes",
                                icon = Icons.AutoMirrored.Filled.MenuBook
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        uiState.nodes.forEach { node ->
                            item {
                                NodeListItem(
                                    node = node,
                                    onClick = { onNavigateToPractice(book.id, node.id, null) }
                                )
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCollectionDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc ->
                onCreateCollection(name, desc)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun FilterChips(
    bookId: Int,
    markedCount: Int,
    wrongCount: Int,
    unansweredCount: Int,
    dueCount: Int,
    onNavigateToPractice: (Int, String?, String?) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterChip("All", null, true) {
            onNavigateToPractice(bookId, null, null)
        }
        if (markedCount > 0) {
            FilterChip("Bookmarked $markedCount", "marked", false) {
                onNavigateToPractice(bookId, null, "marked")
            }
        }
        if (wrongCount > 0) {
            FilterChip("Wrong $wrongCount", "wrong", false) {
                onNavigateToPractice(bookId, null, "wrong")
            }
        }
        if (unansweredCount > 0) {
            FilterChip("Unanswered $unansweredCount", "unanswered", false) {
                onNavigateToPractice(bookId, null, "unanswered")
            }
        }
        if (dueCount > 0) {
            FilterChip("Due $dueCount", "srs_due", false) {
                onNavigateToPractice(bookId, null, "srs_due")
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    filter: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = if (selected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.primary
        ),
        modifier = if (selected) {
            Modifier.background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                MaterialTheme.shapes.small
            )
        } else Modifier
    )
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BookInfoCard(uiState: BookDetailUiState) {
    val book = uiState.book ?: return

    MnemaCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentPadding = PaddingValues(MnemaSpacing.XLarge)
    ) {
            // Title row with color accent
            Row(verticalAlignment = Alignment.CenterVertically) {
                MnemaBookAvatar(
                    bookId = book.id,
                    displayName = book.displayName,
                    iconName = book.icon
                )
                Spacer(modifier = Modifier.width(MnemaSpacing.Large))
                Column {
                    Text(
                        book.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(MnemaSpacing.Large))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Questions", book.totalQuestions.toString())
                StatItem("Answered", uiState.answeredCount.toString())
                StatItem("Due", uiState.srsStats.dueToday.toString())
            }

            // SRS chips
            if (uiState.srsStats.total > 0) {
                Spacer(modifier = Modifier.height(MnemaSpacing.Medium))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MnemaSpacing.Small)
                ) {
                    if (uiState.srsStats.newCards > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("New ${uiState.srsStats.newCards}") },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    if (uiState.srsStats.learning > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Learning ${uiState.srsStats.learning}") },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                    if (uiState.srsStats.review > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Due ${uiState.srsStats.review}") },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
            }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CollectionListItem(
    collection: com.hihusky.mnema.data.model.Collection,
    onClick: () -> Unit
) {
    val typeLabel = when (collection.kind) {
        CollectionKind.Smart -> "Smart"
        CollectionKind.Custom -> "Custom"
    }
    val icon = when (collection.kind) {
        CollectionKind.Smart -> Icons.Default.SmartToy
        CollectionKind.Custom -> Icons.Outlined.FolderOpen
    }

    ListItem(
        headlineContent = {
            Text(
                collection.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun NodeListItem(
    node: com.hihusky.mnema.data.model.Node,
    onClick: () -> Unit = {}
) {
    ListItem(
        headlineContent = {
            Text(
                node.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = if (node.children.isNotEmpty()) {
            {
                Text(
                    "${node.children.size} sub-node(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        trailingContent = {
            Text(
                "${node.questionCount}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun CreateCollectionDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create Collection",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), description.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// FlowRow helper (Compose Foundation doesn't have FlowRow in all versions)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        androidx.compose.ui.layout.Layout(
            content = content,
            measurePolicy = androidx.compose.ui.layout.MeasurePolicy { measurables, constraints ->
                val hGapPx = horizontalArrangement.spacing.roundToPx()
                val vGapPx = verticalArrangement.spacing.roundToPx()
                val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
                val rowWidths = mutableListOf<Int>()
                val rowHeights = mutableListOf<Int>()

                var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
                var currentRowWidth = 0
                var currentRowHeight = 0

                measurables.forEach { measurable ->
                    val placeable = measurable.measure(constraints)
                    if (currentRow.isNotEmpty() && currentRowWidth + hGapPx + placeable.width > constraints.maxWidth) {
                        rows.add(currentRow)
                        rowWidths.add(currentRowWidth)
                        rowHeights.add(currentRowHeight)
                        currentRow = mutableListOf()
                        currentRowWidth = 0
                        currentRowHeight = 0
                    }
                    currentRow.add(placeable)
                    currentRowWidth += if (currentRow.size == 1) placeable.width else hGapPx + placeable.width
                    currentRowHeight = maxOf(currentRowHeight, placeable.height)
                }
                if (currentRow.isNotEmpty()) {
                    rows.add(currentRow)
                    rowWidths.add(currentRowWidth)
                    rowHeights.add(currentRowHeight)
                }

                val totalHeight = rowHeights.sum() + (rows.size - 1).coerceAtLeast(0) * vGapPx
                layout(constraints.maxWidth, totalHeight) {
                    var y = 0
                    rows.forEachIndexed { rowIndex, row ->
                        var x = 0
                        row.forEach { placeable ->
                            placeable.placeRelative(x, y)
                            x += placeable.width + hGapPx
                        }
                        y += rowHeights[rowIndex] + vGapPx
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookDetailScreenPreviewLoading() {
    MnemaTheme {
        BookDetailScreenContent(
            uiState = BookDetailUiState(isLoading = true),
            onBack = {},
            onNavigateToPractice = { _, _, _ -> },
            onNavigateToCollection = {},
            onCreateCollection = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BookDetailScreenPreviewLoaded() {
    MnemaTheme {
        BookDetailScreenContent(
            uiState = BookDetailUiState(
                book = Book(
                    id = 1,
                    filename = "test.json",
                    name = "Sample Book",
                    description = "A sample book for preview",
                    totalQuestions = 120,
                    totalNodes = 12,
                    sortOrder = 0
                ),
                nodes = listOf(
                    Node(
                        id = "n1",
                        bookId = 1,
                        title = "Node 1",
                        questionCount = 10,
                        children = listOf(
                            Node(id = "n1_0", bookId = 1, title = "Sub-node 1.1", questionCount = 5, depth = 1)
                        )
                    ),
                    Node(
                        id = "n2",
                        bookId = 1,
                        title = "Node 2",
                        questionCount = 15,
                        depth = 0
                    )
                ),
                collections = listOf(
                    Collection(
                        id = 1,
                        kind = CollectionKind.Smart,
                        behavior = com.hihusky.mnema.data.model.CollectionBehavior.SmartFilter,
                        name = "Smart Review",
                        createdAt = 0L
                    ),
                    Collection(
                        id = 2,
                        kind = CollectionKind.Custom,
                        behavior = com.hihusky.mnema.data.model.CollectionBehavior.Manual,
                        name = "My Practice Set",
                        createdAt = 0L
                    )
                ),
                srsStats = SrsStats(
                    total = 50,
                    newCards = 10,
                    learning = 15,
                    review = 20,
                    dueToday = 25
                ),
                answeredCount = 45,
                markedCount = 12,
                wrongCount = 8,
                unansweredCount = 67,
                dueCount = 25,
                isLoading = false
            ),
            onBack = {},
            onNavigateToPractice = { _, _, _ -> },
            onNavigateToCollection = {},
            onCreateCollection = { _, _ -> }
        )
    }
}
