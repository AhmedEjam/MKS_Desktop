package com.ahmedyejam.mks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "flashcard_decks",
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
data class FlashcardDeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val bookId: Long,
    val title: String,
    val description: String? = null,
    val iconName: String? = null,
    val coverImage: String? = null, // local path or http URL
    @ColumnInfo(defaultValue = "'[]'") val tags: List<String> = emptyList(),
    // Stats / UI
    val cardCount: Int = 0,
    val studiedCount: Int = 0,
    val masteryPercentage: Float = 0f,
    // Metadata
    val isSystem: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

