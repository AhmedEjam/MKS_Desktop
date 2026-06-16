package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

enum class PromptOutputType {
    NOTE, BLUEPRINT, FLASHCARDS, QUIZ, OTHER
}

@Serializable
data class PromptCardEntity(
    val id: Long = 0,
    val deckId: Long,
    val title: String,
    val promptText: String,
    val variablesJson: String? = null,
    val outputType: String = "OTHER",
    val tags: List<String> = emptyList(),
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
