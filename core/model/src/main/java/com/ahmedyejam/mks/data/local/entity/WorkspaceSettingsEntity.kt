package com.ahmedyejam.mks.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "workspace_settings",
    foreignKeys = [
        ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workspaceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workspaceId")]
)
@JsonClass(generateAdapter = true)
data class WorkspaceSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workspaceId: Long,
    val language: String? = null,
    val theme: String? = null,
    val defaultSort: String? = null,
    val quizDefaultsJson: String? = null,
    val importDefaultsJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
