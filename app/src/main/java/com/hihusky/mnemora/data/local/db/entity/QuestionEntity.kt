package com.hihusky.mnemora.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["nodeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["nodeId"]),
    ],
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bookId: Int,
    val nodeId: String? = null,
    val parentId: Int? = null,
    val content: String? = null,
    val choices: String? = null,
    val answer: String? = null,
    val explanation: String? = null,
    val questionType: String = "multiple_choice",
    val frontTemplate: String? = null,
    val backTemplate: String? = null,
    val format: String = "markdown",
)
