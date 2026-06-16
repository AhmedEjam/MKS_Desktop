package com.ahmedyejam.mks.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = QuizEntity::class,
            parentColumns = ["id"],
            childColumns = ["quizId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("quizId"),
        Index("deletedAt")
    ]
)
@JsonClass(generateAdapter = true)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quizId: Long,
    val label: String,
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val incorrectCount: Int = 0,
    val answers: Map<Long, List<Int>> = emptyMap(), // questionId to list of option indices (Legacy/Unique)
    val answersByIndex: Map<Int, List<Int>> = emptyMap(), // index to list of option indices (For repeats)
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val lastStudiedAt: Long = 0,
    val lastEditedAt: Long = System.currentTimeMillis(),
    
    // Question management
    val questionIds: List<Long> = emptyList(), // Store the sequence of question IDs (including repeats)
    val originalQuestionCount: Int = 0, // Number of unique questions in the original set
    
    // Config fields (added in v5)
    val shuffleQuestions: Boolean = true,
    val shuffleOptions: Boolean = true,
    val rapidMode: Boolean = false,
    val repeatWrong: Boolean = true,
    val quizTimerSeconds: Int = 0, // 0 means no timer
    val questionTimerSeconds: Int = 0, // 0 means no timer
    val rangeFrom: Int = 0, // 0-based index
    val rangeTo: Int = -1, // -1 means until the end
    val includeFilters: List<String> = emptyList(), // "unanswered", "missed", "marked", "categorized", "uncategorized"
    
    // Restoration fields (v8)
    val droppedOptions: Map<Long, List<Int>> = emptyMap(), // Legacy
    val droppedOptionsByIndex: Map<Int, List<Int>> = emptyMap(),
    val visibleOptionsCount: Map<Long, Int> = emptyMap(), // Legacy
    val visibleOptionsCountByIndex: Map<Int, Int> = emptyMap(),
    
    // Performance fields (v10)
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val deletedAt: Long? = null,

    // Result taxonomy (v30)
    // Key: original index, Value: result status (CORRECT_FIRST_TRY, WRONG, CORRECTED_AFTER_REPEAT, UNANSWERED, DROPPED)
    val resultTaxonomy: Map<Int, String> = emptyMap()
)
