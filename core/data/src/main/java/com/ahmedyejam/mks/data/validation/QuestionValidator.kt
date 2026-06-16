package com.ahmedyejam.mks.data.validation

import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuestionType

/**
 * Result of a question validation check.
 */
data class QuestionValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

/**
 * Validates question data integrity.
 */
object QuestionValidator {
    fun validate(question: QuestionEntity): QuestionValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (question.text.isBlank()) {
            errors.add("Question stem cannot be blank.")
        }

        if (question.type == QuestionType.SINGLE_CHOICE || question.type == QuestionType.MULTIPLE_CHOICE) {
            if (question.options.size < 2) {
                errors.add("Multiple choice questions must have at least 2 options.")
            }
            if (question.options.any { it.isBlank() }) {
                errors.add("Question options cannot be blank.")
            }
        }

        if (question.correctAnswers.isEmpty()) {
            errors.add("At least one correct answer must be selected.")
        }

        if (question.correctAnswers.any { it !in question.options.indices }) {
            errors.add("One or more correct answer indices are out of range.")
        }

        if (question.type == QuestionType.SINGLE_CHOICE && question.correctAnswers.size > 1) {
            errors.add("Single choice questions can only have one correct answer.")
        }

        if (question.explanation.isNullOrBlank()) {
            warnings.add("Adding an explanation is recommended for better learning.")
        }

        return QuestionValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
        )
    }
}
