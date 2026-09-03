package com.hihusky.mnemora.ui.components

import com.hihusky.mnemora.data.model.ChatScrollPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AiChatScrollStateTest {
    @Test
    fun `bottom position remains an explicit follow state`() {
        val state =
            AiChatScrollState().stateFor(
                sessionId = 1,
                initialPosition = ChatScrollPosition(),
                messageCount = 8,
            )

        val snapshot = state.snapshot()

        assertTrue(state.followsOutput)
        assertTrue(snapshot.isAtBottom)
        assertEquals(0, snapshot.firstVisibleItemIndex)
        assertEquals(0, snapshot.firstVisibleItemScrollOffset)
    }

    @Test
    fun `top position is distinct from bottom position`() {
        val state =
            AiChatScrollState().stateFor(
                sessionId = 1,
                initialPosition =
                    ChatScrollPosition(
                        firstVisibleItemIndex = 0,
                        firstVisibleItemScrollOffset = 0,
                        isAtBottom = false,
                    ),
                messageCount = 8,
            )

        val snapshot = state.snapshot()

        assertFalse(state.followsOutput)
        assertFalse(snapshot.isAtBottom)
        assertEquals(0, snapshot.firstVisibleItemIndex)
        assertEquals(0, snapshot.firstVisibleItemScrollOffset)
    }

    @Test
    fun `same session retains its live lazy list state`() {
        val scrollState = AiChatScrollState()
        val first =
            scrollState.stateFor(
                sessionId = 7,
                initialPosition =
                    ChatScrollPosition(
                        firstVisibleItemIndex = 3,
                        firstVisibleItemScrollOffset = 24,
                        isAtBottom = false,
                    ),
                messageCount = 10,
            )
        val reopened =
            scrollState.stateFor(
                sessionId = 7,
                initialPosition = ChatScrollPosition(),
                messageCount = 11,
            )

        assertSame(first, reopened)
        assertEquals(3, reopened.listState.firstVisibleItemIndex)
        assertEquals(24, reopened.listState.firstVisibleItemScrollOffset)
    }
}
