package com.hihusky.mnema.data.model

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: Int = 0,
    val questionId: Int,
    val title: String,
    val createdAt: Long
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): ChatSession {
            return ChatSession(
                id = (map["id"] as? Number)?.toInt() ?: 0,
                questionId = (map["question_id"] as? Number)?.toInt() ?: 0,
                title = (map["title"] as? String) ?: "",
                createdAt = (map["created_at"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}

data class AiStreamState(
    val questionId: Int,
    val sessionId: Int,
    var streamingResponse: String = "",
    var isLoading: Boolean = true,
    var error: String? = null,
    private var onCancel: (() -> Unit)? = null
) {
    fun setCancelAction(action: () -> Unit) {
        onCancel = action
    }

    fun cancel() {
        onCancel?.invoke()
        onCancel = null
    }
}
