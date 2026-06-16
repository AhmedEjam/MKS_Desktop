package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

enum class QuestionAssetType {
    image, pdf, text_note, web_link, source_reference,
    blueprint_link, flashcard_link, prompt_output, other
}

@Serializable
data class QuestionAssetEntity(
    val id: Long = 0,
    val bookId: Long,
    val quizId: Long,
    val questionId: Long,
    val assetType: String,
    val title: String,
    val description: String? = null,
    val localPath: String? = null,
    val externalUrl: String? = null,
    val mimeType: String? = null,
    val fileName: String? = null,
    val fileSizeBytes: Long? = null,
    val textContent: String? = null,
    val sourceDocumentId: Long? = null,
    val sourcePage: String? = null,
    val sourceQuote: String? = null,
    val sortOrder: Int = 0,
    val isPinned: Boolean = false,
    val isPrimary: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
