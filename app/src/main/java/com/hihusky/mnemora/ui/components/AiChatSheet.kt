package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
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
    val listState = rememberLazyListState(initialScrollIndex, initialScrollOffset)
    val visibleMessages = history + listOfNotNull(
        streamingResponse.takeIf { it.isNotBlank() }?.let {
            // Use timestamp=0L so this item has a stable key across recompositions
            ChatMessage(text = it, isUser = false, timestamp = 0L)
        }
    )
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // Follow the latest message only for a fresh conversation. When a saved scroll
    // position is restored we leave the user where they were until they act.
    var autoScroll by remember {
        mutableStateOf(initialScrollIndex == 0 && initialScrollOffset == 0)
    }

    // Whether the list is parked at the end. Derived purely from the list layout
    // (never from a captured message list) so it stays correct as the streaming reply
    // grows. A few-px tolerance keeps sub-pixel layout rounding from reading as
    // "not at bottom".
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastItem = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            lastItem.index >= info.totalItemsCount - 1 &&
                lastItem.offset + lastItem.size <= info.viewportEndOffset + AT_BOTTOM_TOLERANCE_PX
        }
    }

    // The follow latch is driven only by genuine user drags. Once the finger lifts and
    // any fling settles we adopt wherever the user landed. Programmatic scroll-to-bottom
    // carries no DragInteraction, so it can never flip the latch — that decoupling is
    // what removes the streaming/auto-scroll fight (the "bounce") at the bottom.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Stop || interaction is DragInteraction.Cancel) {
                snapshotFlow { listState.isScrollInProgress }.first { inProgress -> !inProgress }
                autoScroll = isAtBottom
            }
        }
    }

    // Sending a message (or a reply starting to stream) should bring the user back
    // to the bottom even if they had a restored position higher up.
    LaunchedEffect(isLoading) {
        if (isLoading) autoScroll = true
    }

    // Pin to the bottom as content arrives. Keyed on both the message count and the
    // length of the last message so streaming (which only grows one item) still
    // follows. A large scroll offset lands on the *end* of the last item, avoiding
    // the backward jump that animating to the item's top would cause.
    val lastMessageLength = visibleMessages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(visibleMessages.size, lastMessageLength) {
        if (visibleMessages.isNotEmpty() && autoScroll) {
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

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = MnemoraSize.ChatListMinHeight)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = MnemoraSpacing.Large,
                    top = MnemoraSpacing.Medium,
                    end = MnemoraSpacing.Large,
                    bottom = MnemoraSpacing.Large
                ),
                verticalArrangement = Arrangement.spacedBy(MnemoraSpacing.Medium)
            ) {
                if (visibleMessages.isEmpty() && !isLoading) {
                    item {
                        EmptyAssistantState(
                            enabled = !isLoading,
                            onSendMessage = onSendMessage
                        )
                    }
                }

                items(visibleMessages, key = { it.timestamp }) { message ->
                    ChatMessageBlock(
                        message = message,
                        isStreaming = streamingResponse.isNotBlank() && message.timestamp == 0L
                    )
                }

                if (isLoading && streamingResponse.isBlank()) {
                    item {
                        ThinkingRow()
                    }
                }
            }

            AiChatInputBar(
                isLoading = isLoading,
                onSendMessage = onSendMessage,
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
