package com.ahmedyejam.mks.data.importer.model

enum class ImportFormat {
    JSON,
    ZIP,
    XLSX,
    CSV_TSV,
    TEXT,
    HTML,
    UNKNOWN,
}

enum class MergeStrategy {
    SKIP_EXISTING, // Only add new, ignore if exists by externalId
    OVERWRITE_EXISTING, // Add new and update existing by externalId
    DUPLICATE, // Always add as new, ignoring externalId checks
}

data class ImportResult(
    val success: Boolean,
    val detectedFormat: ImportFormat = ImportFormat.UNKNOWN,
    val detectedSchemaVersion: Int? = null,
    val importedBooksCount: Int = 0,
    val updatedBooksCount: Int = 0,
    val importedQuizzesCount: Int = 0,
    val updatedQuizzesCount: Int = 0,
    val importedQuestionsCount: Int = 0,
    val updatedQuestionsCount: Int = 0,
    val importedSessionsCount: Int = 0,
    val updatedSessionsCount: Int = 0,
    val importedImagesCount: Int = 0,
    val skippedRecordsCount: Int = 0,
    val affectedBookIds: List<Long> = emptyList(),
    val affectedQuizIds: List<Long> = emptyList(),
    val warnings: List<ImportWarning> = emptyList(),
    val errors: List<ImportError> = emptyList(),
    val durationMillis: Long = 0,
    val partiallyImported: Boolean = false,
)

data class ImportWarning(
    val message: String,
    val details: String? = null,
    val affectedId: String? = null,
)

data class ImportError(
    val message: String,
    val exception: Throwable? = null,
    val details: String? = null,
)
