package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class PromptEntity(
    val id: Long = 0,
    val externalId: String,
    val bookId: Long,
    val title: String,
    val stem: String,
    val conversationLinks: List<String> = emptyList(),
    val usageCount: Int = 0,
    val lastUsedAt: Long = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
