package com.ahmedyejam.mks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object PromptOutputType {
    const val NOTE = "NOTE"
    const val BLUEPRINT = "BLUEPRINT"
    const val FLASHCARDS = "FLASHCARDS"
    const val QUIZ = "QUIZ"
    const val OTHER = "OTHER"
}

@Entity(
    tableName = "prompt_cards",
    foreignKeys = [
        ForeignKey(
            entity = PromptDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("deckId"),
        Index("outputType"),
        Index("deletedAt")
    ]
)
data class PromptCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val title: String,
    val promptText: String,
    val variablesJson: String? = null,
    val outputType: String = PromptOutputType.OTHER,
    @ColumnInfo(defaultValue = "'[]'") val tags: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "0") val usageCount: Int = 0,
    val lastUsedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
