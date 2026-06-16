package com.ahmedyejam.mks.data.model

import com.ahmedyejam.mks.data.local.entity.CategoryMetadataEntity

data class CategoryWithMetadata(
    val name: String,
    val questionCount: Int,
    val answeredCount: Int = 0,
    val accuracyPercentage: Float = 0f,
    val lastEditedAt: Long = 0L,
    val metadata: CategoryMetadataEntity? = null
) {
    val isPinned: Boolean get() = metadata?.isPinned ?: false
    val emoji: String? get() = metadata?.emoji
    val color: Int? get() = metadata?.color
}
