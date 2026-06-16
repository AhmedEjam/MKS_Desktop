package com.ahmedyejam.mks.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

object SourceDocumentTypes {
    const val BOOK = "BOOK"
    const val PDF = "PDF"
    const val LECTURE = "LECTURE"
    const val GUIDELINE = "GUIDELINE"
    const val ARTICLE = "ARTICLE"
    const val WEBSITE = "WEBSITE"
    const val OTHER = "OTHER"

    val all = listOf(BOOK, PDF, LECTURE, GUIDELINE, ARTICLE, WEBSITE, OTHER)
}

@Entity(
    tableName = "source_documents",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("bookId"),
        Index("title"),
        Index("sourceType"),
        Index("deletedAt")
    ]
)
@JsonClass(generateAdapter = true)
data class SourceDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long? = null,
    val title: String,
    val sourceType: String = SourceDocumentTypes.OTHER,
    val author: String? = null,
    val edition: String? = null,
    val year: String? = null,
    val publisher: String? = null,
    val localPath: String? = null,
    val externalUrl: String? = null,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
