package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hihusky.mnemora.data.model.ChatScrollPosition

private const val DRAFT_SESSION_ID = Int.MIN_VALUE

@Stable
class AiChatScrollState internal constructor() {
    private val sessionStates = mutableMapOf<Int, AiChatSessionScrollState>()

    internal fun stateFor(
        sessionId: Int?,
        initialPosition: ChatScrollPosition,
        messageCount: Int,
    ): AiChatSessionScrollState {
        val key = sessionId ?: DRAFT_SESSION_ID
        return sessionStates.getOrPut(key) {
            val initialIndex =
                if (initialPosition.isAtBottom) {
                    0
                } else {
                    initialPosition.firstVisibleItemIndex.coerceIn(
                        minimumValue = 0,
                        maximumValue = (messageCount - 1).coerceAtLeast(0),
                    )
                }
            val initialOffset =
                if (initialPosition.isAtBottom) {
                    0
                } else {
                    initialPosition.firstVisibleItemScrollOffset.coerceAtLeast(0)
                }
            AiChatSessionScrollState(
                listState = LazyListState(initialIndex, initialOffset),
                followsOutput = initialPosition.isAtBottom,
            )
        }
    }
}

@Stable
internal class AiChatSessionScrollState(
    val listState: LazyListState,
    followsOutput: Boolean,
) {
    var followsOutput by mutableStateOf(followsOutput)

    val isAtBottom: Boolean
        get() = followsOutput || (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0)

    fun snapshot(): ChatScrollPosition {
        if (followsOutput) return ChatScrollPosition(isAtBottom = true)
        return ChatScrollPosition(
            firstVisibleItemIndex = listState.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
            isAtBottom = false,
        )
    }
}

@Composable
fun rememberAiChatScrollState(): AiChatScrollState = remember { AiChatScrollState() }
