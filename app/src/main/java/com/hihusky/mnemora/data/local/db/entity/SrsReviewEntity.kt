package com.hihusky.mnemora.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "srs_reviews",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["dueDate"]),
    ],
)
data class SrsReviewEntity(
    @PrimaryKey
    val questionId: Int,
    val bookId: Int,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val dueDate: Long? = null,
    val lastReviewed: Long? = null,
    val reviewState: Int = 0,
)
