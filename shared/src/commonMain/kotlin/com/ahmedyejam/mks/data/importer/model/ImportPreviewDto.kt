package com.ahmedyejam.mks.data.importer.model

import com.ahmedyejam.mks.data.importer.dto.LibraryBundleDto

data class ImportPreviewDto(
    val bundle: LibraryBundleDto,
    val booksToCreate: List<String>,
    val booksToUpdate: List<String>,
    val quizzesToCreate: List<String>,
    val quizzesToUpdate: List<String>,
    val questionsToCreate: List<String>,
    val questionsToUpdate: List<String>,
    val totalQuestions: Int,
    val totalSessions: Int,
    val totalCategories: Int,
    val totalImages: Int = 0,
    val skippedRecordsCount: Int = 0,
    val hasAssets: Boolean,
    val warnings: List<ImportWarning> = emptyList(),
    val errors: List<ImportError> = emptyList(),
)
