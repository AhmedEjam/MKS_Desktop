package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class BookEntity(
    val id: Long = 0,
    val workspaceId: Long = 0L,
    val externalId: String,
    val title: String,
    val description: String = "",
    val iconName: String? = null,
    val coverImage: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val contentUpdatedAt: Long = 0L,
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = 0L,
    val isPinned: Boolean = false,
    val isSystem: Boolean = false,
    val fields: List<String> = emptyList(),
    val questionCount: Int = 0,
    val answeredCount: Int = 0,
    val totalAttempts: Int = 0,
    val completionPercentage: Float = 0f,
    val accuracyPercentage: Float = 0f,
    val deletedAt: Long? = null
)
