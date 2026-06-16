package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class NoteCollectionEntity(
    val id: Long = 0,
    val externalId: String,
    val bookId: Long,
    val title: String,
    val description: String? = null,
    val iconName: String? = null,
    val coverImage: String? = null,
    val tags: List<String> = emptyList(),
    val noteCount: Int = 0,
    val isSystem: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = 0L,
    val deletedAt: Long? = null
)
