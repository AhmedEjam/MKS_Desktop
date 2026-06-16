package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class CourseSlideEntity(
    val id: Long = 0,
    val externalId: String,
    val courseId: Long,
    val title: String,
    val body: String,
    val speakerNotes: String? = null,
    val imagePath: String? = null,
    val orderIndex: Int = 0,
    val isCompleted: Boolean = false,
    val tags: List<String> = emptyList(),
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
