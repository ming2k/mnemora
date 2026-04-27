package com.hihusky.mnemora.ui.screens.collections

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hihusky.mnemora.data.model.Collection
import com.hihusky.mnemora.data.model.CollectionBehavior
import com.hihusky.mnemora.data.model.CollectionKind
import com.hihusky.mnemora.data.model.CollectionSummary
import com.hihusky.mnemora.ui.components.MnemoraBottomSheet
import com.hihusky.mnemora.ui.components.MnemoraEmptyState
import com.hihusky.mnemora.ui.components.topappbar.MnemoraCollapsibleTopAppBar
import com.hihusky.mnemora.ui.theme.MnemoraSize
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.MnemoraTheme
import com.hihusky.mnemora.ui.theme.identityContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    onNavigateToCollection: (Int) -> Unit,
    viewModel: CollectionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectionsScreenContent(
        uiState = uiState,
        onNavigateToCollection = onNavigateToCollection,
        onCreateCollection = { name, desc -> viewModel.createCollection(name, desc) },
        onDeleteCollection = { id -> viewModel.deleteCollection(id) },
        onRetry = { viewModel.loadCollections() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollectionsScreenContent(
    uiState: CollectionsUiState,
    onNavigateToCollection: (Int) -> Unit,
    onCreateCollection: (String, String) -> Unit,
    onDeleteCollection: (Int) -> Unit,
    onRetry: () -> Unit
) {
    var showCreateSheet by remember { mutableStateOf(false) }
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

    val smartCollections = uiState.summaries.filter { it.collection.isSmart }
    val customCollections = uiState.summaries.filter { !it.collection.isSmart }

    Scaffold(
        topBar = {
            MnemoraCollapsibleTopAppBar(
                title = "Collections",
                scrollFraction = scrollFraction,
                actions = {
                    IconButton(onClick = { showCreateSheet = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New collection",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Error: ${uiState.error}",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(MnemoraSpacing.Small))
                            TextButton(onClick = onRetry) { Text("Retry") }
                        }
                    }
                }

                uiState.summaries.isEmpty() -> {
                    MnemoraEmptyState(
                        icon = Icons.Default.FolderOpen,
                        title = "No collections yet",
                        message = "Save questions you want to revisit, or group them by theme — then study them together.",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Medium)
                    ) {
                        if (smartCollections.isNotEmpty()) {
                            item {
                                SectionLabel("Smart")
                                Spacer(Modifier.height(MnemoraSpacing.Small))
                            }
                            items(smartCollections, key = { it.collection.id }) { summary ->
                                SwipeableCollectionCard(
                                    summary = summary,
                                    onClick = { onNavigateToCollection(summary.collection.id) },
                                    onDelete = { onDeleteCollection(summary.collection.id) }
                                )
                            }
                            if (customCollections.isNotEmpty()) {
                                item { Spacer(Modifier.height(MnemoraSpacing.Medium)) }
                            }
                        }

                        if (customCollections.isNotEmpty()) {
                            item {
                                SectionLabel("Custom")
                                Spacer(Modifier.height(MnemoraSpacing.Small))
                            }
                            items(customCollections, key = { it.collection.id }) { summary ->
                                SwipeableCollectionCard(
                                    summary = summary,
                                    onClick = { onNavigateToCollection(summary.collection.id) },
                                    onDelete = { onDeleteCollection(summary.collection.id) }
                                )
                            }
                        }

                        item { Spacer(Modifier.height(MnemoraSpacing.XLarge)) }
                    }
                }
            }
        }
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
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCollectionCard(
    summary: CollectionSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = MnemoraSpacing.Large)
                )
            }
        }
    ) {
        CollectionCard(summary = summary, onClick = onClick)
    }
}

@Composable
private fun CollectionCard(
    summary: CollectionSummary,
    onClick: () -> Unit
) {
    val collection = summary.collection
    val isSmart = collection.isSmart

    val accentColor = if (isSmart) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    val icon = if (isSmart) Icons.Default.AutoAwesome else Icons.Default.FolderOpen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MnemoraSpacing.Small, vertical = MnemoraSpacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = MaterialTheme.shapes.small,
                color = accentColor.identityContainer()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(MnemoraSize.IconSmall)
                    )
                }
            }

            Spacer(Modifier.width(MnemoraSpacing.Small))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    collection.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!collection.description.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        collection.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(MnemoraSpacing.Small))

            CountChip(count = summary.itemCount, color = accentColor)
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(start = 48.dp)
        )
    }
}

@Composable
private fun CountChip(count: Int, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = color.identityContainer()
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = MnemoraSpacing.Small, vertical = MnemoraSpacing.XSmall)
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

    MnemoraBottomSheet(onDismissRequest = onDismiss) {
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
                placeholder = { Text("e.g. Exam Week, Hard Questions") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Purpose (optional)") },
                placeholder = { Text("Why are you creating this?") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
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
private fun CollectionsScreenPreview() {
    MnemoraTheme {
        CollectionsScreenContent(
            uiState = CollectionsUiState(
                summaries = listOf(
                    CollectionSummary(
                        collection = Collection(
                            id = 1,
                            bookId = 1,
                            kind = CollectionKind.Custom,
                            behavior = CollectionBehavior.Manual,
                            name = "Exam Week",
                            description = "Questions for Thursday's test",
                            createdAt = 0L
                        ),
                        itemCount = 23
                    ),
                    CollectionSummary(
                        collection = Collection(
                            id = 2,
                            bookId = 1,
                            kind = CollectionKind.Smart,
                            behavior = CollectionBehavior.SmartFilter,
                            name = "Wrong Answers",
                            description = "Questions I keep getting wrong",
                            createdAt = 0L
                        ),
                        itemCount = 7
                    ),
                    CollectionSummary(
                        collection = Collection(
                            id = 3,
                            bookId = 2,
                            kind = CollectionKind.Custom,
                            behavior = CollectionBehavior.Manual,
                            name = "Chapter 5",
                            createdAt = 0L
                        ),
                        itemCount = 0
                    )
                )
            ),
            onNavigateToCollection = {},
            onCreateCollection = { _, _ -> },
            onDeleteCollection = {},
            onRetry = {}
        )
    }
}
