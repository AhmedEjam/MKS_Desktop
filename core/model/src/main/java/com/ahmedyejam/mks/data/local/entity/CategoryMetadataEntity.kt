package com.ahmedyejam.mks.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_metadata",
    indices = [
        Index("deletedAt")
    ]
)
data class CategoryMetadataEntity(
    @PrimaryKey val name: String,
    val emoji: String? = null,
    val color: Int? = null,
    val isPinned: Boolean = false,
    val deletedAt: Long? = null
)
