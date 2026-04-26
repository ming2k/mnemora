package com.hihusky.mnemora.data.model

data class Node(
    val id: String,
    val bookId: Int,
    val parentId: String? = null,
    val title: String = "",
    val questionCount: Int = 0,
    val sortOrder: Int = 0,
    val depth: Int = 0,
    val children: List<Node> = emptyList()
) {
    val displayTitle: String get() = title
}
