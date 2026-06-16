package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class KnowledgeStudySessionEntity(
    val id: Long = 0,
    val targetType: String,
    val targetId: Long,
    val stateJson: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
