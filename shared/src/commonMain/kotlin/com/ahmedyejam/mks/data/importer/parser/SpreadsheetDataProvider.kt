package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.model.ParsedQuestion

/**
 * Platform abstraction for spreadsheet data access.
 *
 * Provides raw row data, sheet names, and per-cell image references
 * for interactive spreadsheet import workflows (XLSX column mapping,
 * header row selection, question toggling).
 *
 * Android: wraps Apache POI Workbook + XlsxImageResolver
 * Desktop: wraps Apache POI Workbook + XlsxImageResolver (same JVM impl)
 */
interface SpreadsheetDataProvider {

    /** All sheet names in the workbook (or ["CSV/TSV Content"] for CSV). */
    fun getSheetNames(): List<String>

    /** Raw rows for a given sheet. Each row is a list of cell string values. */
    fun getSheetRows(sheetName: String): List<List<String>>

    /** Per-cell image references (e.g. "A1" → image path or data URL). */
    fun getSheetAddressImages(sheetName: String): Map<String, String>

    /** Per-row image references (row index → image path or data URL). */
    fun getSheetRowImages(sheetName: String): Map<Int, String>

    /** Release workbook/ZIP resources. */
    fun close()
}

/**
 * Result of opening a spreadsheet file for interactive editing.
 */
data class SpreadsheetOpenResult(
    val provider: SpreadsheetDataProvider,
    val format: com.ahmedyejam.mks.data.importer.model.ImportFormat,
    val fileName: String,
)

/**
 * Platform-specific factory that opens a spreadsheet file and returns
 * a [SpreadsheetDataProvider] for interactive editing.
 *
 * Returns null if the format is not a spreadsheet (JSON, HTML, TEXT, ZIP)
 * or if the file cannot be opened.
 */
interface SpreadsheetDataProviderFactory {
    fun open(filePath: String, format: com.ahmedyejam.mks.data.importer.model.ImportFormat): SpreadsheetOpenResult?
}
