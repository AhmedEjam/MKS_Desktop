package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
data class QuestionCategoryEntity(
    val questionId: Long,
    val category: String,
    val deletedAt: Long? = null
)
