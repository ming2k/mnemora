package com.hihusky.mnemora.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val filename: String,
    val name: String? = null,
    val description: String? = null,
    val totalQuestions: Int = 0,
    val totalNodes: Int = 0,
    val sortOrder: Int = 0,
    val icon: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
