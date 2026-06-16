package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceEntity(
    val id: Long = 0,
    val externalId: String,
    val name: String,
    val description: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
