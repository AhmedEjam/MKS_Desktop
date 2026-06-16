package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class CategoryMetadataEntity(
    val name: String,
    val emoji: String? = null,
    val color: Int? = null,
    val isPinned: Boolean = false,
    val deletedAt: Long? = null
)
