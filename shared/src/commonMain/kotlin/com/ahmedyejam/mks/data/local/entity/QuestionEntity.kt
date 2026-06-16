package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

enum class QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    BOOLEAN
}

@Serializable
data class QuestionEntity(
    val id: Long = 0,
    val externalId: String,
    val quizId: Long,
    val text: String,
    val type: QuestionType = QuestionType.SINGLE_CHOICE,
    val options: List<String> = emptyList(),
    val correctAnswers: List<Int> = emptyList(),
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
    val tags: List<String> = emptyList(),
    val difficulty: String? = null,
    val dueAt: Long = 0,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long = 0,
    val additionalInfo: String? = null,
    val sourceBookId: String? = null,
    val sourceQuizId: String? = null,
    val sourceQuestionId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = 0L,
    val timeSpentMs: Long = 0,
    val lastAttemptResult: Boolean? = null,
    val consecutiveCorrect: Int = 0,
    val deletedAt: Long? = null
)
