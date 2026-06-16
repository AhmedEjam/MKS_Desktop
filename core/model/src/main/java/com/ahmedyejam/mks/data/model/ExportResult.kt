package com.ahmedyejam.mks.data.model

data class ExportResult(
    val success: Boolean,
    val exportedAssetCount: Int = 0,
    val failedAssetCount: Int = 0,
    val warnings: List<ExportWarning> = emptyList(),
    val errorMessage: String? = null
)

data class ExportWarning(
    val source: String,
    val message: String
)
