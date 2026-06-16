package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

enum class AnnotationOwnerType {
    QUESTION, SLIDE, NOTE, SOURCE, ASSET
}

enum class AnnotationColorLabel {
    YELLOW, GREEN, BLUE, PINK, ORANGE, RED
}

@Serializable
data class AnnotationEntity(
    val id: Long = 0,
    val workspaceId: Long,
    val bookId: Long,
    val ownerType: String,
    val ownerId: Long,
    val selectedText: String? = null,
    val noteBody: String? = null,
    val colorLabel: String = "YELLOW",
    val positionDataJson: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
