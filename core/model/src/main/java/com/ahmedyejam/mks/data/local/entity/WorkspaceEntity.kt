package com.ahmedyejam.mks.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "workspaces",
    indices = [Index(value = ["externalId"], unique = true)]
)
@JsonClass(generateAdapter = true)
data class WorkspaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val externalId: String,
    val name: String,
    val description: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
