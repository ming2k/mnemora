package com.hihusky.mnemora.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookId", "mode", "isActive"]),
        Index(value = ["bookId", "startTime"]),
    ],
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int,
    val mode: String, // Practice / Review / Preview / Test
    val startTime: Long,
    val lastActiveTime: Long,
    val currentIndex: Int = 0,
    val totalQuestions: Int = 0,
    val isCompleted: Boolean = false,
    val isActive: Boolean = true,
    val answersJson: String? = null,
    val collectionId: Int? = null,
    val nodeId: String? = null,
)
