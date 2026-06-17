package com.ahmedyejam.mks.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SlideGenerationConfig(
    val includeExplanation: Boolean = true,
    val includeOptions: Boolean = true,
    val includeCorrectAnswer: Boolean = true,
    val includeHint: Boolean = false,
    val templateId: String? = null
)
