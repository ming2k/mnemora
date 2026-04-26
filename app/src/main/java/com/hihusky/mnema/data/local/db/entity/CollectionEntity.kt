package com.hihusky.mnema.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collections",
    indices = [
        Index(value = ["kind"])
    ]
)
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val kind: String,
    val behavior: String,
    val name: String,
    val description: String? = null,
    val config: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long? = null
)
