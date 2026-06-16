package com.ahmedyejam.mks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

object AnnotationOwnerType {
    const val QUESTION = "QUESTION"
    const val SLIDE = "SLIDE"
    const val NOTE = "NOTE"
    const val SOURCE = "SOURCE"
    const val ASSET = "ASSET"

    val all = setOf(QUESTION, SLIDE, NOTE, SOURCE, ASSET)
}

object AnnotationColorLabel {
    const val YELLOW = "YELLOW"
    const val GREEN = "GREEN"
    const val BLUE = "BLUE"
    const val PINK = "PINK"
    const val ORANGE = "ORANGE"
    const val RED = "RED"
}

@Entity(
    tableName = "annotations",
    foreignKeys = [
        ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workspaceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("workspaceId"),
        Index("bookId"),
        Index(value = ["ownerType", "ownerId"]),
        Index("colorLabel"),
        Index("deletedAt"),
        Index("updatedAt")
    ]
)
@JsonClass(generateAdapter = true)
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workspaceId: Long,
    val bookId: Long,
    val ownerType: String,
    val ownerId: Long,
    val selectedText: String? = null,
    val noteBody: String? = null,
    @ColumnInfo(defaultValue = "'YELLOW'") val colorLabel: String = AnnotationColorLabel.YELLOW,
    val positionDataJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
