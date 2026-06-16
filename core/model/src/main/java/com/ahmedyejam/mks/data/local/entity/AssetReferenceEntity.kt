package com.ahmedyejam.mks.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "asset_references",
    indices = [
        Index("path"),
        Index(value = ["ownerType", "ownerId"]),
        Index(value = ["ownerType", "ownerId", "path"], unique = true),
        Index("deletedAt")
    ]
)
data class AssetReferenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val ownerType: String,
    val ownerId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
