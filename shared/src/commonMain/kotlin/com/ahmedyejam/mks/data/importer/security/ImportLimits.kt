package com.ahmedyejam.mks.data.importer.security

object ImportLimits {
    const val MAX_ZIP_COMPRESSED_BYTES: Long = 100L * 1024L * 1024L
    const val MAX_ZIP_ENTRIES: Int = 1_000
    const val MAX_ZIP_SINGLE_UNCOMPRESSED_BYTES: Long = 50L * 1024L * 1024L
    const val MAX_ZIP_TOTAL_UNCOMPRESSED_BYTES: Long = 200L * 1024L * 1024L

    const val MAX_TEXT_IMPORT_BYTES: Long = 10L * 1024L * 1024L
    const val MAX_CSV_IMPORT_BYTES: Long = 20L * 1024L * 1024L
    const val MAX_HTML_IMPORT_BYTES: Long = 10L * 1024L * 1024L
    const val MAX_XLSX_IMPORT_BYTES: Long = 50L * 1024L * 1024L

    const val MAX_XLSX_SHEETS: Int = 20
    const val MAX_XLSX_ROWS_PER_SHEET: Int = 20_000
    const val MAX_XLSX_CELLS_PER_SHEET: Int = 400_000
    const val MAX_XLSX_COLUMNS: Int = 120

    const val MAX_REMOTE_IMAGE_BYTES: Long = 12L * 1024L * 1024L
}
