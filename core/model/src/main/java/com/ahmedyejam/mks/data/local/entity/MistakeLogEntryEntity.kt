package com.ahmedyejam.mks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mistake_log_entries",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("bookId"),
        Index("quizId"),
        Index("questionId"),
        Index("sessionId"),
        Index("reviewAt"),
        Index("isFixed"),
        Index("createdAt"),
        Index("deletedAt")
    ]
)
data class MistakeLogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val quizId: Long,
    val questionId: Long,
    val sessionId: Long? = null,
    val selectedAnswer: String? = null,
    val correctAnswer: String? = null,
    val userReason: String? = null,
    val correctConcept: String? = null,
    val preventionNote: String? = null,
    val linkedFlashcardId: Long? = null,
    val linkedBlueprintId: Long? = null,
    val linkedAssetId: Long? = null,
    @ColumnInfo(defaultValue = "0") val isFixed: Boolean = false,
    val reviewAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
