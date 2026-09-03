package com.hihusky.mnemora.data.model

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
    val updatedAt: Long = 0L,
) {
    val displayName: String get() = name
}
