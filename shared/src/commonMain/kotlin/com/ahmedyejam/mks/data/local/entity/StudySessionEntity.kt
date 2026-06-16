package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class StudySessionEntity(
    val id: Long = 0,
    val targetType: String,
    val targetId: Long,
    val label: String? = null,
    val stateJson: String,
    val timeSpentMs: Long = 0,
    val completionPercentage: Float = 0f,
    val isCompleted: Boolean = false,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
