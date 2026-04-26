package com.hihusky.mnemora.data.model

enum class CollectionKind {
    Custom, Smart;

    companion object {
        fun fromName(value: String?): CollectionKind {
            if (value.isNullOrEmpty()) return Custom
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Custom
        }
    }
}

enum class CollectionBehavior {
    Manual, SmartFilter;

    companion object {
        fun fromName(value: String?): CollectionBehavior {
            if (value.isNullOrEmpty()) return Manual
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Manual
        }
    }
}

data class Collection(
    val id: Int = 0,
    val bookId: Int,
    val kind: CollectionKind,
    val behavior: CollectionBehavior,
    val name: String,
    val description: String? = null,
    val config: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long? = null
) {
    val isSmart: Boolean get() = kind == CollectionKind.Smart
}

data class CollectionSummary(
    val collection: Collection,
    val itemCount: Int
)

data class CollectionItem(
    val id: Int = 0,
    val collectionId: Int,
    val questionId: Int,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
