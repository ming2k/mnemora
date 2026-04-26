package com.hihusky.mnema.data.model

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

    companion object {
        fun fromMap(map: Map<String, Any?>): Node {
            return Node(
                id = (map["id"] as? String) ?: "",
                bookId = (map["book_id"] as? Number)?.toInt() ?: 0,
                parentId = map["parent_id"] as? String,
                title = (map["title"] as? String) ?: "",
                questionCount = (map["question_count"] as? Number)?.toInt() ?: 0,
                sortOrder = (map["sort_order"] as? Number)?.toInt() ?: 0,
                depth = (map["depth"] as? Number)?.toInt() ?: 0
            )
        }
    }
}
