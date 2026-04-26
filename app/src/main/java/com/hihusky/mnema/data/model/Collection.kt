package com.hihusky.mnema.data.model

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

data class CollectionItem(
    val id: Int = 0,
    val collectionId: Int,
    val poolQuestionId: Int,
    val sourceBookId: Int,
    val sourceQuestionId: Int,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
