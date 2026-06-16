package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class AssetReferenceEntity(
    val id: Long = 0,
    val path: String,
    val ownerType: String,
    val ownerId: Long,
    val createdAt: Long = 0L,
    val deletedAt: Long? = null
)
