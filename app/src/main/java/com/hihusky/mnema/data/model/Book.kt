package com.hihusky.mnema.data.model

data class Book(
    val id: Int = 0,
    val filename: String,
    val name: String = "",
    val description: String? = null,
    val totalQuestions: Int = 0,
    val totalNodes: Int = 0,
    val sortOrder: Int = 0,
    val icon: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val displayName: String get() = name

    companion object {
        fun fromMap(map: Map<String, Any?>): Book {
            return Book(
                id = (map["id"] as? Number)?.toInt() ?: 0,
                filename = (map["filename"] as? String) ?: "",
                name = (map["name"] as? String) ?: "",
                description = map["description"] as? String,
                totalQuestions = (map["total_questions"] as? Number)?.toInt() ?: 0,
                totalNodes = (map["total_nodes"] as? Number)?.toInt() ?: 0,
                sortOrder = (map["sort_order"] as? Number)?.toInt() ?: 0,
                icon = map["icon"] as? String,
                createdAt = (map["created_at"] as? Number)?.toLong() ?: 0L,
                updatedAt = (map["updated_at"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
