package com.ahmedyejam.mks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

object QuestionAssetType {
    const val IMAGE = "image"
    const val PDF = "pdf"
    const val TEXT_NOTE = "text_note"
    const val WEB_LINK = "web_link"
    const val SOURCE_REFERENCE = "source_reference"
    const val BLUEPRINT_LINK = "blueprint_link"
    const val FLASHCARD_LINK = "flashcard_link"
    const val PROMPT_OUTPUT = "prompt_output"
    const val OTHER = "other"

    val firstPassTypes = listOf(IMAGE, PDF, TEXT_NOTE, WEB_LINK)
}

@Entity(
    tableName = "question_assets",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("bookId"),
        Index("quizId"),
        Index("questionId"),
        Index("assetType"),
        Index("createdAt"),
        Index("sourceDocumentId"),
        Index("deletedAt")
    ]
)
@JsonClass(generateAdapter = true)
data class QuestionAssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    @ColumnInfo(defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "0") val isPinned: Boolean = false,
    @ColumnInfo(defaultValue = "0") val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
