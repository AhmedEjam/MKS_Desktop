package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class LearningSessionEntity(
    val id: Long = 0,
    val deckId: Long,
    val label: String? = null,
    val stateJson: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
