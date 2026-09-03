package com.hihusky.mnemora.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collection_items",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["questionId"]),
        Index(value = ["collectionId", "questionId"], unique = true),
    ],
)
data class CollectionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val collectionId: Int,
    val questionId: Int,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
)
