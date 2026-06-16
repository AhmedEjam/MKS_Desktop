package com.ahmedyejam.mks.data.model

import com.squareup.moshi.JsonClass

/**
 * Represents the state of an active learning session for any knowledge target
 * (flashcard deck, slideshow course, note blueprint, or prompt).
 *
 * This state is serialized to JSON and stored in KnowledgeStudySessionEntity.stateJson
 * for persistence across app restarts.
 */
@JsonClass(generateAdapter = true)
data class LearningSessionState(
    val targetType: String,                              // "FLASHCARD", "COURSE", "NOTE", "PROMPT"
    val targetId: Long,                                  // ID of the target entity
    val deckId: Long? = null,                            // For flashcard sessions
    val courseId: Long? = null,                          // For slideshow sessions
    val reviewedCardIds: Set<Long> = emptySet(),         // Cards/slides already reviewed
    val cardScores: Map<Long, Float> = emptyMap(),       // Card ID → confidence (0-1)
    val currentCardIndex: Int = 0,                       // Current position in sequence
    val timersActive: Map<Long, Long> = emptyMap(),      // Card ID → elapsed milliseconds
    val startedAt: Long = System.currentTimeMillis(),    // Session start timestamp
    val completedAt: Long? = null,                       // Session completion timestamp (null if ongoing)
    val totalAttempts: Int = 0,                          // Total cards seen
    val correctAttempts: Int = 0,                        // Total correct responses
    val userNotes: String = "",                          // User's notes during session
    val flags: Map<String, Boolean> = emptyMap()         // Feature flags (e.g., "showHints" → true)
) {
    /**
     * Calculates session accuracy as a percentage (0-100)
     */
    val accuracy: Float
        get() = if (totalAttempts == 0) 0f else (correctAttempts.toFloat() / totalAttempts) * 100f

    /**
     * Calculates session progress (0-1)
     */
    val progress: Float
        get() = if (totalAttempts == 0) 0f else (reviewedCardIds.size.toFloat() / totalAttempts).coerceIn(0f, 1f)

    /**
     * Returns true if session is marked as completed
     */
    val isCompleted: Boolean
        get() = completedAt != null

    /**
     * Calculates duration in milliseconds
     */
    val durationMs: Long
        get() = (completedAt ?: System.currentTimeMillis()) - startedAt
}

