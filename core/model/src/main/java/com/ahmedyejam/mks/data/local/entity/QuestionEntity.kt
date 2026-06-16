package com.ahmedyejam.mks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = QuizEntity::class,
            parentColumns = ["id"],
            childColumns = ["quizId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("quizId"),
        Index("isMarked"),
        Index("isDropped"),
        Index("markReviewAt"),
        Index("deletedAt")
    ]
)
@JsonClass(generateAdapter = true)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val externalId: String,
    val quizId: Long,
    val text: String,
    val type: QuestionType,
    val options: List<String>,
    val correctAnswers: List<Int>, // Indices of correct options
    val explanation: String? = null,
    val hint: String? = null,
    val reference: String? = null,
    val weight: Int = 1,
    val imagePath: String? = null,
    val imageName: String? = null,
    val imageSource: String? = null,
    val attempts: Int = 0,
    val correctCount: Int = 0,
    val isDropped: Boolean = false,
    val droppedAt: Long? = null,
    val droppedReason: String? = null,
    val isMarked: Boolean = false,
    val markedAt: Long? = null,
    val markReason: String? = null,
    val markReviewAt: Long? = null,
    val notes: String? = null,
    val categories: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "'[]'") val tags: List<String> = emptyList(),
    // Spaced Repetition fields
    val difficulty: String? = null,
    @ColumnInfo(defaultValue = "0") val dueAt: Long = 0,
    @ColumnInfo(defaultValue = "0") val reviewCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val lastReviewedAt: Long = 0,
    val additionalInfo: String? = null,
    val sourceBookId: String? = null,
    val sourceQuizId: String? = null,
    val sourceQuestionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = System.currentTimeMillis(),
    val timeSpentMs: Long = 0,
    val lastAttemptResult: Boolean? = null,
    val consecutiveCorrect: Int = 0,
    val deletedAt: Long? = null
)

enum class QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    BOOLEAN
}
