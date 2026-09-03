package com.hihusky.mnemora.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collections",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["kind"]),
    ],
)
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bookId: Int,
    val kind: String,
    val behavior: String,
    val name: String,
    val description: String? = null,
    val config: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long? = null,
)
