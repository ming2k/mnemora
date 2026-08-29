package com.hihusky.mnemora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.data.model.ChatSession
import com.hihusky.mnemora.ui.theme.MnemoraAlpha
import com.hihusky.mnemora.ui.theme.MnemoraSize
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.MnemoraTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Slack (in px) when deciding the list is parked at the bottom, to absorb layout rounding. */
private const val AT_BOTTOM_TOLERANCE_PX = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatSheet(
    sessions: List<ChatSession>,
    currentSessionId: Int?,
    history: List<ChatMessage>,
    isLoading: Boolean,
    streamingResponse: String,
    onSessionSelected: (Int) -> Unit,
    onCreateSession: () -> Unit,
    onSendMessage: (String) -> Unit,
    onCancelMessage: () -> Unit = {},
    onDismiss: () -> Unit,
    onDeleteSession: () -> Unit = {},
    modelLabel: String = "",
    providerLabel: String = "",
    initialScrollIndex: Int = 0,
    initialScrollOffset: Int = 0,
    onSaveScrollPosition: (Int, Int) -> Unit = { _, _ -> },
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val coroutineScope = rememberCoroutineScope()
    val visibleMessages = history + listOfNotNull(
        streamingResponse.takeIf { it.isNotBlank() }?.let {
            // Use timestamp=0L so this item has a stable key across recompositions
            ChatMessage(text = it, isUser = false, timestamp = 0L)
        }
    )
    val initialTargetIndex = remember(currentSessionId) {
        if (initialScrollIndex > 0 || initialScrollOffset > 0) {
            initialScrollIndex.coerceIn(0, visibleMessages.lastIndex.coerceAtLeast(0))
        } else {
            visibleMessages.lastIndex.coerceAtLeast(0)
        }
    }
    val initialTargetOffset = remember(currentSessionId) {
        if (initialScrollIndex > 0 || initialScrollOffset > 0) initialScrollOffset else 0
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialTargetIndex,
        initialFirstVisibleItemScrollOffset = initialTargetOffset
    )
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Placed tracks whether initial scroll position for the current session has been applied.
    var placed by remember { mutableStateOf(false) }
    var lastHandledSessionId by remember { mutableStateOf<Int?>(null) }

    // Follow latch: determines whether incoming streaming tokens or new messages should keep the list pinned to bottom.
    var autoScroll by remember(currentSessionId) {
        mutableStateOf(initialScrollIndex == 0 && initialScrollOffset == 0)
    }

    // Derived state for bottom detection: layout-driven, robust against sub-pixel rounding.
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastItem = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            val isLastItem = lastItem.index >= info.totalItemsCount - 1
            val isBottomAligned = lastItem.offset + lastItem.size <= info.viewportEndOffset + AT_BOTTOM_TOLERANCE_PX
            isLastItem && isBottomAligned
        }
    }

    // Reset placement initialization when switching sessions
    LaunchedEffect(currentSessionId) {
        if (currentSessionId != lastHandledSessionId) {
            placed = false
            lastHandledSessionId = currentSessionId
        }
    }

    // Restore saved scroll position (or default to bottom for chat) as soon as messages are available
    LaunchedEffect(currentSessionId, visibleMessages.isEmpty()) {
        if (placed || visibleMessages.isEmpty()) return@LaunchedEffect
        if (initialScrollIndex > 0 || initialScrollOffset > 0) {
            val target = initialScrollIndex.coerceIn(0, visibleMessages.lastIndex)
            listState.scrollToItem(target, initialScrollOffset)
            autoScroll = isAtBottom
        } else {
            // Default for chat sessions: land directly at the latest message
            listState.scrollToItem(visibleMessages.lastIndex, Int.MAX_VALUE)
            autoScroll = true
        }
        placed = true
    }

    // Gesture interaction latch:
    // As soon as the user touches/drags the list, IMMEDIATELY disable autoScroll so streaming tokens NEVER fight their finger.
    // When drag/fling completes, re-evaluate if the user parked at the bottom.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start, is PressInteraction.Press -> {
                    autoScroll = false
                }
                is DragInteraction.Stop, is DragInteraction.Cancel, is PressInteraction.Release, is PressInteraction.Cancel -> {
                    snapshotFlow { listState.isScrollInProgress }.first { inProgress -> !inProgress }
                    autoScroll = isAtBottom
                }
            }
        }
    }

    // Sending a message explicitly demands following the new conversation flow
    val sendMessage: (String) -> Unit = { text ->
        autoScroll = true
        onSendMessage(text)
    }

    // Stream & new message auto-scroll:
    // Follows the bottom as tokens arrive ONLY when autoScroll is active and user is not actively dragging/scrolling.
    val lastMessageLength = visibleMessages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(visibleMessages.size, lastMessageLength) {
        if (placed && autoScroll && !listState.isScrollInProgress && visibleMessages.isNotEmpty()) {
            listState.scrollToItem(visibleMessages.lastIndex, Int.MAX_VALUE)
        }
    }

    // IME / Keyboard Inset adjustments:
    // When keyboard shows/hides, keep the viewport anchored to the latest message if autoScroll was engaged.
    val imeInsets = WindowInsets.ime
    LaunchedEffect(imeInsets) {
        if (placed && autoScroll && !listState.isScrollInProgress && visibleMessages.isNotEmpty()) {
            listState.scrollToItem(visibleMessages.lastIndex, Int.MAX_VALUE)
        }
    }

    val currentOnSaveScrollPosition by rememberUpdatedState(onSaveScrollPosition)
    DisposableEffect(Unit) {
        onDispose {
            currentOnSaveScrollPosition(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }
    }

    if (showDeleteConfirm) {
        MnemoraAlertDialog(
            title = "Delete Chat",
            message = "This conversation will be permanently deleted.",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onDismissRequest = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDeleteSession()
            }
        )
    }

    MnemoraBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        // The sheet wraps its content (the shared height cap lives in MnemoraBottomSheet):
        // short chats stay compact, long ones grow until the cap, then the message list
        // scrolls internally.
        Column {
            AiChatHeader(
                sessions = sessions,
                currentSessionId = currentSessionId,
                onSessionSelected = onSessionSelected,
                onCreateSession = onCreateSession,
                onDeleteSession = { showDeleteConfirm = true },
                modelLabel = modelLabel,
                providerLabel = providerLabel
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = MnemoraSize.ChatListMinHeight)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = MnemoraSpacing.Large,
                        top = MnemoraSpacing.Medium,
                        end = MnemoraSpacing.Large,
                        bottom = MnemoraSpacing.Large
                    ),
                    verticalArrangement = Arrangement.spacedBy(MnemoraSpacing.Medium)
                ) {
                    if (visibleMessages.isEmpty() && !isLoading) {
                        item(key = "empty_state") {
                            EmptyAssistantState(
                                enabled = !isLoading,
                                onSendMessage = sendMessage
                            )
                        }
                    }

                    itemsIndexed(
                        items = visibleMessages,
                        key = { index, message ->
                            if (message.timestamp == 0L) "streaming_$index" else "${message.timestamp}_${message.isUser}_$index"
                        }
                    ) { _, message ->
                        ChatMessageBlock(
                            message = message,
                            isStreaming = streamingResponse.isNotBlank() && message.timestamp == 0L
                        )
                    }

                    if (isLoading && streamingResponse.isBlank()) {
                        item(key = "thinking_indicator") {
                            ThinkingRow()
                        }
                    }
                }

                // Floating "Scroll to Bottom" button when user scrolls up
                androidx.compose.animation.AnimatedVisibility(
                    visible = placed && !isAtBottom && visibleMessages.isNotEmpty(),
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = MnemoraSpacing.Large, bottom = MnemoraSpacing.Small)
                ) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                autoScroll = true
                                listState.animateScrollToItem(visibleMessages.lastIndex, Int.MAX_VALUE)
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to bottom",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            if (streamingResponse.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 4.dp, end = 4.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }

            AiChatInputBar(
                isLoading = isLoading,
                onSendMessage = sendMessage,
                onCancelMessage = onCancelMessage
            )
        }
    }
}

@Composable
private fun AiChatHeader(
    sessions: List<ChatSession>,
    currentSessionId: Int?,
    onSessionSelected: (Int) -> Unit,
    onCreateSession: () -> Unit,
    onDeleteSession: () -> Unit,
    modelLabel: String,
    providerLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MnemoraSpacing.Large,
                end = MnemoraSpacing.Medium,
                bottom = MnemoraSpacing.Small
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = MnemoraAlpha.StatusContainer)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(MnemoraSpacing.Medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Tutor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val subtitle = buildString {
                    if (modelLabel.isNotBlank()) append(modelLabel)
                    if (providerLabel.isNotBlank()) {
                        if (isNotBlank()) append(" · ")
                        append(providerLabel)
                    }
                }
                Text(
                    text = subtitle.ifBlank { "Question-focused explanation" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (sessions.isNotEmpty() && currentSessionId != null) {
                IconButton(onClick = onDeleteSession) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onCreateSession) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New chat"
                )
            }
        }

        if (sessions.size > 1) {
            Spacer(modifier = Modifier.height(MnemoraSpacing.Small))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Small),
                contentPadding = PaddingValues(end = MnemoraSpacing.Small)
            ) {
                items(sessions) { session ->
                    SessionPill(
                        title = session.title,
                        selected = session.id == currentSessionId,
                        onClick = { onSessionSelected(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = MnemoraAlpha.StatusContainer)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            0.5.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = title.ifBlank { "Chat" },
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = MnemoraSpacing.Medium, vertical = MnemoraSpacing.Small)
        )
    }
}

@Composable
private fun EmptyAssistantState(
    enabled: Boolean,
    onSendMessage: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MnemoraSpacing.Large),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Ask for the missing step, not just the answer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(MnemoraSpacing.Medium))
        val prompts = listOf(
            "Explain why the answer is correct",
            "Compare the confusing options",
            "Summarize the concept"
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Small)) {
            items(prompts) { prompt ->
                SuggestionPill(
                    text = prompt,
                    enabled = enabled,
                    onClick = { onSendMessage(prompt) }
                )
            }
        }
    }
}

@Composable
private fun SuggestionPill(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MnemoraSpacing.Medium, vertical = MnemoraSpacing.Small)
        )
    }
}

@Composable
private fun ChatMessageBlock(message: ChatMessage, isStreaming: Boolean = false) {
    if (message.isUser) {
        UserMessage(message.text)
    } else {
        AssistantMessage(text = message.text, isStreaming = isStreaming)
    }
}

@Composable
private fun UserMessage(text: String) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val maxBubbleWidth = maxWidth * 0.82f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 6.dp
                ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.widthIn(max = maxBubbleWidth)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        horizontal = MnemoraSpacing.Medium,
                        vertical = MnemoraSpacing.Small
                    )
                )
            }
        }
    }
}

@Composable
private fun AssistantMessage(text: String, isStreaming: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .heightIn(min = 36.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = MnemoraAlpha.Muted))
        )
        Spacer(modifier = Modifier.width(MnemoraSpacing.Small))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.fillMaxWidth()
        ) {
            MarkdownText(
                content = text,
                textStyle = MaterialTheme.typography.bodyMedium,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    horizontal = MnemoraSpacing.Medium,
                    vertical = MnemoraSpacing.Small
                )
            )
        }
    }
}

@Composable
private fun ThinkingRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(MnemoraSpacing.Small))
        Text(
            text = "Reading the question...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AiChatInputBar(
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onCancelMessage: () -> Unit
) {
    var input by remember { mutableStateOf("") }

    fun submit() {
        val message = input.trim()
        if (message.isNotEmpty() && !isLoading) {
            onSendMessage(message)
            input = ""
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
                .padding(
                    start = MnemoraSpacing.Large,
                    top = MnemoraSpacing.Small,
                    end = MnemoraSpacing.Large,
                    bottom = MnemoraSpacing.Small
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Small)
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask anything...") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
                minLines = 1,
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            FilledIconButton(
                onClick = if (isLoading) onCancelMessage else ::submit,
                enabled = isLoading || input.isNotBlank(),
                modifier = Modifier.size(44.dp)
            ) {
                if (isLoading) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun AiChatSheetPreview() {
    MnemoraTheme {
        AiChatSheet(
            sessions = listOf(
                ChatSession(id = 1, questionId = 1, title = "Main", createdAt = 0L),
                ChatSession(id = 2, questionId = 1, title = "Why B?", createdAt = 0L)
            ),
            currentSessionId = 1,
            history = listOf(
                ChatMessage(text = "Why is B correct?", isUser = true),
                ChatMessage(
                    text = "B is correct because it matches the definition in the stem. A looks close, but it reverses the condition.",
                    isUser = false
                )
            ),
            isLoading = false,
            streamingResponse = "",
            onSessionSelected = {},
            onCreateSession = {},
            onSendMessage = {},
            onDismiss = {}
        )
    }
}
