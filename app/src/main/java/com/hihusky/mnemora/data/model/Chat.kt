package com.hihusky.mnemora.data.model

data class ChatMessage(
    val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isInterrupted: Boolean = false,
)

data class ChatScrollPosition(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
    val isAtBottom: Boolean = true,
)

data class ChatSession(
    val id: Int = 0,
    val questionId: Int,
    val title: String,
    val createdAt: Long,
    val lastScrollPosition: ChatScrollPosition = ChatScrollPosition(),
)
