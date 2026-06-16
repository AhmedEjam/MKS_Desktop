package com.ahmedyejam.mks.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "books",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workspaceId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index("workspaceId"),
        androidx.room.Index("deletedAt"),
        androidx.room.Index(value = ["externalId"], unique = true)
    ]
)
@JsonClass(generateAdapter = true)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workspaceId: Long = 0L,
    val externalId: String,
    val title: String,
    val description: String = "",
    val iconName: String? = null,
    val coverImage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val contentUpdatedAt: Long = System.currentTimeMillis(),
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isSystem: Boolean = false,
    val fields: List<String> = emptyList(),
    // Cached stats
    val questionCount: Int = 0,
    val answeredCount: Int = 0,
    val totalAttempts: Int = 0,
    val completionPercentage: Float = 0f,
    val accuracyPercentage: Float = 0f,
    val deletedAt: Long? = null
)
