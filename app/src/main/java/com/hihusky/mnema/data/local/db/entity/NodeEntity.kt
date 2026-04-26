package com.hihusky.mnema.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nodes",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["parentId"])
    ]
)
data class NodeEntity(
    @PrimaryKey
    val id: String,
    val bookId: Int,
    val parentId: String? = null,
    val title: String? = null,
    val questionCount: Int = 0,
    val sortOrder: Int = 0,
    val depth: Int = 0
)
