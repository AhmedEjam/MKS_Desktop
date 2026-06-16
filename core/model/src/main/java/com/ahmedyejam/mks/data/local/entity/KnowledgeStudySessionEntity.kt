package com.ahmedyejam.mks.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "knowledge_study_sessions",
    indices = [
        Index("deletedAt")
    ]
)
data class KnowledgeStudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetType: String, // "COURSE", "SLIDE", "NOTE", "PROMPT"
    val targetId: Long,
    val stateJson: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
