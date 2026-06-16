package com.ahmedyejam.mks.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "course_slides",
    foreignKeys = [
        ForeignKey(
            entity = SlideshowCourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("courseId"),
        Index("sourceQuestionId"),
        Index("deletedAt")
    ]
)
/**
 * Represents a single slide within a Study Slides (Slideshow) course.
 *
 * @property externalId A unique identifier for the slide.
 * @property courseId The ID of the parent [SlideshowCourseEntity].
 * @property title The title or header of the slide.
 * @property body The main content/body of the slide.
 * @property speakerNotes Optional notes intended for the presenter or student review.
 * @property imagePath Optional path to an associated image.
 * @property orderIndex The position of this slide within the course.
 * @property isCompleted Whether the student has marked this slide as studied/completed.
 * @property sourceQuestionId The ID of the question this slide was derived from, if any.
 */
data class CourseSlideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val externalId: String,
    val courseId: Long,
    val title: String,
    val body: String,
    val speakerNotes: String? = null,
    val imagePath: String? = null,
    val orderIndex: Int = 0,
    val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "'[]'") val tags: List<String> = emptyList(),
    // Spaced Repetition fields
    val difficulty: String? = null,
    @ColumnInfo(defaultValue = "0") val dueAt: Long = 0,
    @ColumnInfo(defaultValue = "0") val reviewCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val lastReviewedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sourceQuestionId: Long? = null,
    val syncConfig: Map<String, Boolean> = emptyMap(),
    val deletedAt: Long? = null
)
