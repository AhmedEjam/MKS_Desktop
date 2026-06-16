package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class SlideshowCourseEntity(
    val id: Long = 0,
    val externalId: String,
    val bookId: Long,
    val title: String,
    val description: String? = null,
    val iconName: String? = null,
    val coverImage: String? = null,
    val tags: List<String> = emptyList(),
    val slideCount: Int = 0,
    val studiedSlideCount: Int = 0,
    val progress: Float = 0f,
    val isSystem: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = 0L,
    val isDerived: Boolean = false,
    val sourceQuizId: Long? = null,
    val deletedAt: Long? = null
)
