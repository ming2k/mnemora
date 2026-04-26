package com.hihusky.mnema.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "question_pool",
    indices = [
        Index(value = ["sourceBookId", "sourceQuestionId"], unique = true)
    ]
)
data class QuestionPoolEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sourceBookId: Int,
    val sourceQuestionId: Int,
    val content: String? = null,
    val choices: String? = null,
    val answer: String? = null,
    val explanation: String? = null,
    val questionType: String = "multiple_choice",
    val frontTemplate: String? = null,
    val backTemplate: String? = null,
    val createdAt: Long
)
