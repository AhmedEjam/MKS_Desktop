package com.ahmedyejam.mks.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "study_sessions",
    indices = [
        Index("targetId", "targetType"),
        Index("deletedAt")
    ]
)
@JsonClass(generateAdapter = true)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetType: String, // "QUIZ", "DECK", "COURSE", "NOTE_COLLECTION", "BLUEPRINT"
    val targetId: Long,
    val label: String? = null,
    
    // Core Session Data
    val stateJson: String,
    val timeSpentMs: Long = 0,
    val completionPercentage: Float = 0f,
    val isCompleted: Boolean = false,
    
    // Performance metrics
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
