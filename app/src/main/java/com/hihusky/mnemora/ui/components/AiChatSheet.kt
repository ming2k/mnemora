package com.hihusky.mnemora.ui.components

import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hihusky.mnemora.data.model.ChatMessage
import com.hihusky.mnemora.data.model.ChatScrollPosition
import com.hihusky.mnemora.data.model.ChatSession
import com.hihusky.mnemora.ui.theme.MnemoraAlpha
import com.hihusky.mnemora.ui.theme.MnemoraSize
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.MnemoraTheme
import kotlinx.coroutines.launch

private const val SCROLL_THRESHOLD_OFFSET = 30

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
    onContinueMessage: (ChatMessage) -> Unit = {},
    onRegenerateMessage: (ChatMessage) -> Unit = {},
    onDismiss: () -> Unit,
    onDeleteSession: () -> Unit = {},
    modelLabel: String = "",
    providerLabel: String = "",
    initialScrollPosition: ChatScrollPosition = ChatScrollPosition(),
    scrollState: AiChatScrollState = rememberAiChatScrollState(),
    onSaveScrollPosition: (Int, ChatScrollPosition) -> Unit = { _, _ -> },
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val coroutineScope = rememberCoroutineScope()
    val sessionScrollState =
        scrollState.stateFor(
            sessionId = currentSessionId,
            initialPosition = initialScrollPosition,
            messageCount = history.size + if (streamingResponse.isNotBlank()) 1 else 0,
        )
    val listState = sessionScrollState.listState
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isScrolledUp by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > SCROLL_THRESHOLD_OFFSET
        }
    }

    val currentOnSaveScrollPosition by rememberUpdatedState(onSaveScrollPosition)

    fun saveScrollPosition(sessionId: Int) {
        currentOnSaveScrollPosition(sessionId, sessionScrollState.snapshot())
    }

    DisposableEffect(currentSessionId, sessionScrollState) {
        onDispose {
            currentSessionId?.let { sessionId ->
                saveScrollPosition(sessionId)
            }
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
            },
        )
    }

    MnemoraBottomSheet(
        onDismissRequest = {
            currentSessionId?.let { sessionId ->
                saveScrollPosition(sessionId)
            }
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column {
            AiChatHeader(
                sessions = sessions,
                currentSessionId = currentSessionId,
                onSessionSelected = onSessionSelected,
                onCreateSession = onCreateSession,
                onDeleteSession = { showDeleteConfirm = true },
                modelLabel = modelLabel,
                providerLabel = providerLabel,
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = MnemoraSize.ChatListMinHeight)
                        .fillMaxWidth(),
            ) {
                AiChatMessageList(
                    history = history,
                    isLoading = isLoading,
                    streamingResponse = streamingResponse,
                    listState = listState,
                    onSendMessage = onSendMessage,
                    onContinueMessage = onContinueMessage,
                    onRegenerateMessage = onRegenerateMessage,
                )

                AiChatScrollToBottomFab(
                    visible = isScrolledUp,
                    hasActiveStreaming = isLoading || streamingResponse.isNotBlank(),
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }

            AiChatInputBar(
                isLoading = isLoading,
                onSendMessage = onSendMessage,
                onCancelMessage = onCancelMessage,
            )
        }
    }
}

@Composable
private fun AiChatMessageList(
    history: List<ChatMessage>,
    isLoading: Boolean,
    streamingResponse: String,
    listState: LazyListState,
    onSendMessage: (String) -> Unit,
    onContinueMessage: (ChatMessage) -> Unit,
    onRegenerateMessage: (ChatMessage) -> Unit,
) {
    val showEmptyState = history.isEmpty() && !isLoading && streamingResponse.isBlank()
    val showThinkingIndicator = isLoading && streamingResponse.isBlank()

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(
                start = MnemoraSpacing.Large,
                top = MnemoraSpacing.Medium,
                end = MnemoraSpacing.Large,
                bottom = MnemoraSpacing.Large,
            ),
        verticalArrangement = Arrangement.spacedBy(MnemoraSpacing.Medium),
    ) {
        if (showThinkingIndicator) {
            item(key = "thinking_indicator") {
                ThinkingRow()
            }
        }

        if (streamingResponse.isNotBlank()) {
            item(key = "streaming_response") {
                ChatMessageBlock(
                    message =
                        ChatMessage(
                            text = streamingResponse,
                            isUser = false,
                            timestamp = 0L,
                        ),
                    isStreaming = true,
                )
            }
        }

        items(
            items = history.asReversed(),
            key = { message ->
                if (message.id > 0) "msg_${message.id}" else "msg_ts_${message.timestamp}_${message.isUser}"
            },
        ) { message ->
            ChatMessageBlock(
                message = message,
                isStreaming = false,
                isLoading = isLoading,
                onContinue = { onContinueMessage(message) },
                onRegenerate = { onRegenerateMessage(message) },
            )
        }

        if (showEmptyState) {
            item(key = "empty_state") {
                EmptyAssistantState(
                    enabled = !isLoading,
                    onSendMessage = onSendMessage,
                )
            }
        }
    }
}

@Composable
private fun AiChatScrollToBottomFab(
    visible: Boolean,
    hasActiveStreaming: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier =
            modifier.padding(
                end = MnemoraSpacing.Large,
                bottom = MnemoraSpacing.Small,
            ),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 2.dp,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                if (hasActiveStreaming) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
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
    providerLabel: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = MnemoraSpacing.Large,
                    end = MnemoraSpacing.Medium,
                    bottom = MnemoraSpacing.Small,
                ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = MnemoraAlpha.StatusContainer)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(MnemoraSpacing.Medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Tutor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val subtitle =
                    buildString {
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
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (sessions.isNotEmpty() && currentSessionId != null) {
                IconButton(onClick = onDeleteSession) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onCreateSession) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New chat",
                )
            }
        }

        if (sessions.size > 1) {
            Spacer(modifier = Modifier.height(MnemoraSpacing.Small))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Small),
                contentPadding = PaddingValues(end = MnemoraSpacing.Small),
            ) {
                items(sessions) { session ->
                    SessionPill(
                        title = session.title,
                        selected = session.id == currentSessionId,
                        onClick = { onSessionSelected(session.id) },
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
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = MnemoraAlpha.StatusContainer)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        border =
            BorderStroke(
                0.5.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = title.ifBlank { "Chat" },
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = MnemoraSpacing.Medium, vertical = MnemoraSpacing.Small),
        )
    }
}

@Composable
private fun EmptyAssistantState(
    enabled: Boolean,
    onSendMessage: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = MnemoraSpacing.Large),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Ask for the missing step, not just the answer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(MnemoraSpacing.Medium))
        val prompts =
            listOf(
                "Explain why the answer is correct",
                "Compare the confusing options",
                "Summarize the concept",
            )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Small)) {
            items(prompts) { prompt ->
                SuggestionPill(
                    text = prompt,
                    enabled = enabled,
                    onClick = { onSendMessage(prompt) },
                )
            }
        }
    }
}

@Composable
private fun SuggestionPill(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MnemoraSpacing.Medium, vertical = MnemoraSpacing.Small),
        )
    }
}

@Composable
private fun ChatMessageBlock(
    message: ChatMessage,
    isStreaming: Boolean = false,
    isLoading: Boolean = false,
    onContinue: () -> Unit = {},
    onRegenerate: () -> Unit = {},
) {
    if (message.isUser) {
        UserMessage(message.text)
    } else {
        AssistantMessage(
            text = message.text,
            isStreaming = isStreaming,
            isInterrupted = message.isInterrupted,
            isLoading = isLoading,
            onContinue = onContinue,
            onRegenerate = onRegenerate,
        )
    }
}

@Composable
private fun UserMessage(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            shape =
                RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 6.dp,
                ),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier =
                Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentWidth(Alignment.End),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.padding(
                        horizontal = MnemoraSpacing.Medium,
                        vertical = MnemoraSpacing.Small,
                    ),
            )
        }
    }
}

@Composable
private fun AssistantMessage(
    text: String,
    isStreaming: Boolean = false,
    isInterrupted: Boolean = false,
    isLoading: Boolean = false,
    onContinue: () -> Unit = {},
    onRegenerate: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier =
                Modifier
                    .width(3.dp)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isInterrupted) {
                            MaterialTheme.colorScheme.error.copy(alpha = MnemoraAlpha.Muted)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = MnemoraAlpha.Muted)
                        },
                    ),
        )
        Spacer(modifier = Modifier.width(MnemoraSpacing.Small))
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = MnemoraSpacing.Medium,
                            vertical = MnemoraSpacing.Small,
                        ),
                ) {
                    MarkdownText(
                        content = text,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    )

                    if (isInterrupted) {
                        Spacer(modifier = Modifier.height(MnemoraSpacing.Small))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Text(
                                text = "Interrupted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            if (!isStreaming && text.isNotBlank()) {
                AssistantMessageActions(
                    text = text,
                    isInterrupted = isInterrupted,
                    isLoading = isLoading,
                    onContinue = onContinue,
                    onRegenerate = onRegenerate,
                )
            }
        }
    }
}

@Composable
private fun AssistantMessageActions(
    text: String,
    isInterrupted: Boolean,
    isLoading: Boolean,
    onContinue: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isInterrupted) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier =
                    Modifier
                        .clickable(enabled = !isLoading, onClick = onContinue)
                        .padding(end = MnemoraSpacing.Small),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Continue",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier =
                    Modifier
                        .clickable(enabled = !isLoading, onClick = onRegenerate)
                        .padding(end = MnemoraSpacing.Small),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Retry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(text))
                copied = true
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(28.dp),
        ) {
            val iconTint =
                if (copied) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            Icon(
                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = "Copy message",
                tint = iconTint,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ThinkingRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(MnemoraSpacing.Small))
        Text(
            text = "Thinking...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AiChatInputBar(
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onCancelMessage: () -> Unit,
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
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime)
                    .padding(
                        start = MnemoraSpacing.Large,
                        top = MnemoraSpacing.Small,
                        end = MnemoraSpacing.Large,
                        bottom = MnemoraSpacing.Small,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Small),
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
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )
            FilledIconButton(
                onClick = if (isLoading) onCancelMessage else ::submit,
                enabled = isLoading || input.isNotBlank(),
                modifier = Modifier.size(44.dp),
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
            sessions =
                listOf(
                    ChatSession(id = 1, questionId = 1, title = "Main", createdAt = 0L),
                    ChatSession(id = 2, questionId = 1, title = "Why B?", createdAt = 0L),
                ),
            currentSessionId = 1,
            history =
                listOf(
                    ChatMessage(text = "Why is B correct?", isUser = true),
                    ChatMessage(
                        text =
                            "B is correct because it matches the definition in the stem. " +
                                "A looks close, but it reverses the condition.",
                        isUser = false,
                    ),
                ),
            isLoading = false,
            streamingResponse = "",
            onSessionSelected = {},
            onCreateSession = {},
            onSendMessage = {},
            onDismiss = {},
        )
    }
}
