package com.ahmedyejam.mks.data.local.entity

import kotlinx.serialization.Serializable

enum class SourceDocumentTypes {
    BOOK, PDF, LECTURE, GUIDELINE, ARTICLE, WEBSITE, OTHER
}

@Serializable
data class SourceDocumentEntity(
    val id: Long = 0,
    val bookId: Long? = null,
    val title: String,
    val sourceType: String = "OTHER",
    val author: String? = null,
    val edition: String? = null,
    val year: String? = null,
    val publisher: String? = null,
    val localPath: String? = null,
    val externalUrl: String? = null,
    val description: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
