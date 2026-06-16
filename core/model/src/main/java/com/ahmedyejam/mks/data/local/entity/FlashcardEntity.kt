package com.ahmedyejam.mks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = FlashcardDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("deckId"),
        Index("sourceQuestionId"),
        Index("dueAt"),
        Index("deletedAt")
    ]
)
@JsonClass(generateAdapter = true)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val deckId: Long,
    val frontText: String,
    val backText: String,
    val hint: String? = null,
    val imagePath: String? = null,
    val tags: List<String> = emptyList(),  // Converters.kt already supports List<String>
    val orderIndex: Int = 0,
    // Performance metrics
    val attempts: Int = 0,
    val correctCount: Int = 0,
    val difficulty: String? = null,
    @ColumnInfo(defaultValue = "0") val dueAt: Long = 0,
    @ColumnInfo(defaultValue = "0") val reviewCount: Int = 0,
    val lastReviewedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceQuestionId: Long? = null,
    val syncConfig: Map<String, Boolean> = emptyMap(),
    val deletedAt: Long? = null
)

