package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class MistakeLogEntryEntity(
    val id: Long = 0,
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
    val isFixed: Boolean = false,
    val reviewAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
