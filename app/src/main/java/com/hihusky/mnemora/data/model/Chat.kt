package com.hihusky.mnemora.data.model

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: Int = 0,
    val questionId: Int,
    val title: String,
    val createdAt: Long,
    val lastScrollIndex: Int = 0,
    val lastScrollOffset: Int = 0
)
