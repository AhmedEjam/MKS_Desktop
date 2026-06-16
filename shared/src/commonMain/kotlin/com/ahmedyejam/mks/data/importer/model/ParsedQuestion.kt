package com.ahmedyejam.mks.data.importer.model

import com.ahmedyejam.mks.data.local.entity.QuestionType

enum class ImportMode {
    SPREADSHEET,
    TEXT,
    JSON,
    HTML,
    AUTO,
}

data class ResolvedImage(
    val imageDataUrl: String? = null,
    val imageSource: String? = null,
    val via: String? = null,
)

data class ImageDebug(
    val cellImagesFound: Int = 0,
    val dispImgMapped: Int = 0,
    val rowsResolved: Int = 0,
    val anchorCandidates: Int = 0,
)

data class ParsedOption(
    val id: String,
    val text: String,
    val marked: Boolean = false,
)

data class ParseIssue(
    val message: String,
    val severity: IssueSeverity = IssueSeverity.WARNING,
)

enum class IssueSeverity {
    INFO,
    WARNING,
    ERROR,
}

data class ParseStats(
    val totalRowsProcessed: Int = 0,
    val successfullyParsed: Int = 0,
    val skippedEmptyStem: Int = 0,
    val errors: Int = 0,
    val questionsWithImages: Int = 0,
    val questionsWithIssues: Int = 0,
)

data class ParsedQuestion(
    val stem: String,
    val externalId: String? = null,
    val options: List<ParsedOption> = emptyList(),
    val correctAnswers: List<String> = emptyList(),
    val explanation: String? = null,
    val hint: String? = null,
    val reference: String? = null,
    val additionalInfo: String? = null,
    val categories: List<String> = emptyList(),
    val imageDataUrl: String? = null,
    val imageSource: String? = null,
    val derivedFlags: List<String> = emptyList(),
    val sourceLine: Int = 0,
    val issues: List<String> = emptyList(),
    val parseIssues: List<ParseIssue> = emptyList(),
    val answerMode: String? = null,
    val type: QuestionType = QuestionType.SINGLE_CHOICE,
    val isIncluded: Boolean = true,
)

data class ParseResult(
    val questions: List<ParsedQuestion>,
    val mode: ImportMode,
    val debugInfo: String? = null,
    val stats: ParseStats = ParseStats(),
)
