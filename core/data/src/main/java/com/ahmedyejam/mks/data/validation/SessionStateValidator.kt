package com.ahmedyejam.mks.data.validation

import com.ahmedyejam.mks.data.local.entity.SessionEntity

/**
 * Result of a session validation check.
 */
data class SessionValidationResult(
    val isValid: Boolean,
    val canBeRepaired: Boolean = false,
    val message: String? = null,
)

/**
 * Validates session state to prevent crashes during study.
 */
object SessionStateValidator {
    fun validate(session: SessionEntity): SessionValidationResult {
        if (session.questionIds.isEmpty()) {
            return SessionValidationResult(
                isValid = false,
                canBeRepaired = true,
                message = "Session question sequence has not been initialized.",
            )
        }

        if (session.currentQuestionIndex !in session.questionIds.indices) {
            return SessionValidationResult(
                isValid = false,
                canBeRepaired = true,
                message = "Current question index is out of bounds.",
            )
        }

        // Additional checks for timer and other settings can go here

        return SessionValidationResult(isValid = true)
    }
}
