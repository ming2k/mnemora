package com.hihusky.mnemora.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_chat_sessions",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["questionId"])],
)
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val questionId: Int,
    val title: String? = null,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0")
    val lastScrollIndex: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val lastScrollOffset: Int = 0,
    @ColumnInfo(defaultValue = "1")
    val lastScrollAtBottom: Boolean = true,
)
