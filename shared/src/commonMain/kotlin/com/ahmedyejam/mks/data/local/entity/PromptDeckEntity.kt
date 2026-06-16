package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class PromptDeckEntity(
    val id: Long = 0,
    val bookId: Long,
    val title: String,
    val description: String? = null,
    val iconName: String? = null,
    val coverImage: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
