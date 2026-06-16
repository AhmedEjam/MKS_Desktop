package com.ahmedyejam.mks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "note_collections",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("bookId"),
        Index("deletedAt")
    ]
)
@JsonClass(generateAdapter = true)
data class NoteCollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val bookId: Long,
    val title: String,
    val description: String? = null,
    val iconName: String? = null,
    val coverImage: String? = null,
    @ColumnInfo(defaultValue = "'[]'") val tags: List<String> = emptyList(),
    
    // Stats
    val noteCount: Int = 0,
    
    // Metadata
    val isSystem: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
