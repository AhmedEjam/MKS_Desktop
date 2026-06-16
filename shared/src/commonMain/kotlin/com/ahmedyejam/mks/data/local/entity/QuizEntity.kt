package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class QuizEntity(
    val id: Long = 0,
    val externalId: String,
    val bookId: Long,
    val title: String,
    val description: String = "",
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val iconName: String? = null,
    val coverImage: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val contentUpdatedAt: Long = 0L,
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = 0L,
    val isPinned: Boolean = false,
    val isSystem: Boolean = false,
    val questionCount: Int = 0,
    val answeredCount: Int = 0,
    val totalAttempts: Int = 0,
    val completionPercentage: Float = 0f,
    val accuracyPercentage: Float = 0f,
    val deletedAt: Long? = null
)
