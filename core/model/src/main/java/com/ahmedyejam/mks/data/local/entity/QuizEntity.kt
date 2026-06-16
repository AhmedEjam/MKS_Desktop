package com.ahmedyejam.mks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "quizzes",
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
        Index("deletedAt"),
        Index(value = ["externalId"], unique = true)
    ]
)
@JsonClass(generateAdapter = true)
data class QuizEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val externalId: String,
    val bookId: Long,
    val title: String,
    val description: String = "",
    val category: String? = null,
    @ColumnInfo(defaultValue = "'[]'") val tags: List<String> = emptyList(),
    val iconName: String? = null,
    val coverImage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val contentUpdatedAt: Long = System.currentTimeMillis(),
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isSystem: Boolean = false,
    // Cached stats
    val questionCount: Int = 0,
    val answeredCount: Int = 0,
    val totalAttempts: Int = 0,
    val completionPercentage: Float = 0f,
    val accuracyPercentage: Float = 0f,
    val deletedAt: Long? = null
)
