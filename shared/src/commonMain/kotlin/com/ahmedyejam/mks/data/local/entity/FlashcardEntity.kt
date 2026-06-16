package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class FlashcardEntity(
    val id: Long = 0,
    val externalId: String,
    val deckId: Long,
    val frontText: String,
    val backText: String,
    val hint: String? = null,
    val imagePath: String? = null,
    val tags: List<String> = emptyList(),
    val orderIndex: Int = 0,
    val attempts: Int = 0,
    val correctCount: Int = 0,
    val difficulty: String? = null,
    val dueAt: Long = 0,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val sourceQuestionId: Long? = null,
    val syncConfig: Map<String, Boolean> = emptyMap(),
    val deletedAt: Long? = null
)
