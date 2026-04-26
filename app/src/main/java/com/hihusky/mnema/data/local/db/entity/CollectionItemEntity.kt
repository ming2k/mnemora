package com.hihusky.mnema.data.local.db.entity

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
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionPoolEntity::class,
            parentColumns = ["id"],
            childColumns = ["poolQuestionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["poolQuestionId"]),
        Index(value = ["collectionId", "poolQuestionId"], unique = true)
    ]
)
data class CollectionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val collectionId: Int,
    val poolQuestionId: Int,
    val sourceBookId: Int,
    val sourceQuestionId: Int,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
