package com.ahmedyejam.mks.data.importer.validation

import com.ahmedyejam.mks.data.importer.dto.LibraryBundleDto
import com.ahmedyejam.mks.data.importer.dto.QuestionDto
import com.ahmedyejam.mks.data.importer.model.ImportWarning

data class SkippedImportRecord(
    val id: String?,
    val quizId: String?,
    val line: Int?,
    val reason: String,
)

data class ValidationResult(
    val isValid: Boolean,
    val warnings: List<ImportWarning> = emptyList(),
    val criticalError: String? = null,
    val sanitizedBundle: LibraryBundleDto? = null,
    val skippedRecords: List<SkippedImportRecord> = emptyList(),
) {
    val skippedRecordsCount: Int get() = skippedRecords.size
}

class ImportValidator {
    fun validate(
        bundle: LibraryBundleDto,
        allowUnboundQuizzes: Boolean = false,
    ): ValidationResult {
        val warnings = mutableListOf<ImportWarning>()
        val skipped = mutableListOf<SkippedImportRecord>()

        if (bundle.schema !in listOf(4, 5, 6, 7)) {
            warnings.add(ImportWarning("Schema version ${bundle.schema} is not officially supported, but import will proceed."))
        }

        val validKinds = listOf("book-bundle", "library-bundle", "quiz-bundle", "full-library")
        if (bundle.kind !in validKinds) {
            warnings.add(ImportWarning("Unexpected bundle kind: ${bundle.kind}"))
        }

        val bookIds = bundle.books.map { it.id }.toSet()
        if (bookIds.size < bundle.books.size) {
            warnings.add(ImportWarning("Duplicate book IDs found in bundle"))
        }

        val quizIds = bundle.quizzes.map { it.id }.toSet()
        if (quizIds.size < bundle.quizzes.size) {
            warnings.add(ImportWarning("Duplicate quiz IDs found in bundle"))
        }

        val sanitizedQuizzes =
            bundle.quizzes.map { quiz ->
                if (!allowUnboundQuizzes && quiz.bookId !in bookIds) {
                    warnings.add(ImportWarning("Quiz '${quiz.title}' refers to unknown book ID '${quiz.bookId}'", affectedId = quiz.id))
                }

                val seenQuestionIds = mutableSetOf<String>()
                val validQuestions =
                    quiz.questions.mapIndexedNotNull { index, question ->
                        val problems = questionProblems(question, seenQuestionIds)
                        if (problems.isEmpty()) {
                            seenQuestionIds.add(question.id)
                            question
                        } else {
                            val location = question.sourceLine?.takeIf { it > 0 }
                            val displayLine = location?.let { "line/row $it" } ?: "line/row unknown"
                            val reason = problems.joinToString("; ")
                            skipped.add(SkippedImportRecord(question.id, quiz.id, location, reason))
                            warnings.add(
                                ImportWarning(
                                    message = "Skipped question '${question.id.ifBlank { "#${index + 1}" }}' in quiz '${quiz.title}' at $displayLine: $reason",
                                    details = "quizId=${quiz.id}; questionId=${question.id}; line=${location ?: "unknown"}",
                                    affectedId = question.id,
                                ),
                            )
                            null
                        }
                    }
                quiz.copy(questions = validQuestions)
            }

        val sanitizedBundle = bundle.copy(quizzes = sanitizedQuizzes)
        return ValidationResult(
            isValid = true,
            warnings = warnings,
            sanitizedBundle = sanitizedBundle,
            skippedRecords = skipped,
        )
    }

    private fun questionProblems(
        question: QuestionDto,
        seenQuestionIds: Set<String>,
    ): List<String> {
        val problems = mutableListOf<String>()
        if (question.id.isBlank()) problems.add("blank question ID")
        if (question.id.isNotBlank() && question.id in seenQuestionIds) problems.add("duplicate question ID in same quiz")
        if (question.stem.isBlank()) problems.add("blank question stem")
        if (question.options.isEmpty()) problems.add("no answer options")

        val optionIds = question.options.map { it.id }
        val distinctOptionIds = optionIds.toSet()
        if (distinctOptionIds.size < optionIds.size) problems.add("duplicate option IDs")
        question.options.forEachIndexed { optionIndex, option ->
            if (option.id.isBlank()) problems.add("blank option ID at option ${optionIndex + 1}")
            if (option.text.isBlank()) problems.add("blank option text at option ${optionIndex + 1}")
        }

        if (question.correct.isEmpty()) problems.add("no correct answer IDs")
        question.correct.forEach { correctId ->
            if (correctId !in distinctOptionIds) {
                problems.add("unknown correct option ID '$correctId'")
            }
        }

        val answerMode = question.answerMode?.lowercase()
        if (answerMode != null && answerMode !in listOf("single", "multiple")) {
            problems.add("unsupported answer mode '${question.answerMode}'")
        }
        if (answerMode == "single" && question.correct.size > 1) {
            problems.add("single-answer question has multiple correct answers")
        }
        return problems.distinct()
    }
}
