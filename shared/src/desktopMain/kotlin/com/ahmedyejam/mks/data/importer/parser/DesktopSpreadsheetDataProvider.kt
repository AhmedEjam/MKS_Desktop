package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.model.ImportFormat
import com.ahmedyejam.mks.data.importer.xlsx.SheetImageResolution
import com.ahmedyejam.mks.data.importer.xlsx.XlsxImageResolver
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetDataProvider
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetDataProviderFactory
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetOpenResult
import com.ahmedyejam.mks.data.importer.security.ImportLimits
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.util.zip.ZipFile

/**
 * Desktop implementation of [SpreadsheetDataProvider] using Apache POI.
 */
class DesktopSpreadsheetDataProvider(
    private val workbook: org.apache.poi.ss.usermodel.Workbook,
    private val zipFile: ZipFile?,
    private val xlsxResolver: XlsxImageResolver,
    private val cellImagePathMap: Map<String, String>,
) : SpreadsheetDataProvider {

    private val formatter = DataFormatter()

    override fun getSheetNames(): List<String> {
        return (0 until workbook.numberOfSheets).map { workbook.getSheetAt(it).sheetName }
    }

    override fun getSheetRows(sheetName: String): List<List<String>> {
        val sheet = workbook.getSheet(sheetName) ?: return emptyList()
        if ((sheet.lastRowNum + 1) > ImportLimits.MAX_XLSX_ROWS_PER_SHEET) return emptyList()

        val columnCount = detectStableColumnCount(sheet).coerceAtMost(ImportLimits.MAX_XLSX_COLUMNS)
        if ((sheet.lastRowNum + 1).toLong() * columnCount.toLong() > ImportLimits.MAX_XLSX_CELLS_PER_SHEET) return emptyList()

        val rows = mutableListOf<List<String>>()
        for (i in 0..sheet.lastRowNum) {
            val row = sheet.getRow(i)
            val cells = row?.let { extractRowCells(it, columnCount, formatter) } ?: emptyList()
            rows.add(cells)
        }
        return rows
    }

    override fun getSheetAddressImages(sheetName: String): Map<String, String> {
        if (zipFile == null) return emptyMap()
        val resolution = xlsxResolver.resolveSheetImages(zipFile, sheetName, cellImagePathMap)
        return resolution.addressImages
    }

    override fun getSheetRowImages(sheetName: String): Map<Int, String> {
        if (zipFile == null) return emptyMap()
        val resolution = xlsxResolver.resolveSheetImages(zipFile, sheetName, cellImagePathMap)
        return resolution.rowImages
    }

    override fun close() {
        try { workbook.close() } catch (_: Exception) {}
        try { zipFile?.close() } catch (_: Exception) {}
    }

    private fun extractRowCells(row: org.apache.poi.ss.usermodel.Row, columnCount: Int, formatter: DataFormatter): List<String> {
        return (0 until columnCount).map { columnIndex ->
            row.getCell(columnIndex)?.let { cell ->
                formatCellSafely(cell, formatter).trim()
            }.orEmpty()
        }
    }

    private fun formatCellSafely(cell: Cell, formatter: DataFormatter): String {
        return try {
            if (cell.cellType == CellType.FORMULA) fallbackFormulaText(cell, formatter)
            else formatter.formatCellValue(cell)
        } catch (_: Exception) {
            fallbackFormulaText(cell, formatter)
        }
    }

    private fun fallbackFormulaText(cell: Cell, formatter: DataFormatter): String {
        if (cell.cellType != CellType.FORMULA) {
            return runCatching { formatter.formatCellValue(cell) }.getOrElse { cell.toString() }
        }
        return try {
            when (cell.cachedFormulaResultType) {
                CellType.STRING -> cell.richStringCellValue?.string.orEmpty()
                CellType.NUMERIC -> formatter.formatRawCellContents(
                    cell.numericCellValue,
                    cell.cellStyle.dataFormat.toInt(),
                    cell.cellStyle.dataFormatString,
                )
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.BLANK -> ""
                else -> "=" + cell.cellFormula.orEmpty()
            }
        } catch (_: Exception) {
            runCatching { "=" + cell.cellFormula.orEmpty() }.getOrElse { cell.toString() }
        }
    }

    private fun detectStableColumnCount(sheet: org.apache.poi.ss.usermodel.Sheet): Int {
        var maxColumns = 0
        val scanEnd = minOf(sheet.lastRowNum, 40)
        for (rowIndex in 0..scanEnd) {
            val row = sheet.getRow(rowIndex) ?: continue
            maxColumns = maxOf(maxColumns, row.lastCellNum.toInt().coerceAtLeast(0))
        }
        return maxColumns
    }
}

/**
 * Desktop implementation of [SpreadsheetDataProviderFactory].
 * Opens XLSX files using Apache POI and resolves images via [XlsxImageResolver].
 */
class DesktopSpreadsheetDataProviderFactory : SpreadsheetDataProviderFactory {

    private val xlsxResolver = XlsxImageResolver()

    override fun open(filePath: String, format: ImportFormat): SpreadsheetOpenResult? {
        if (format != ImportFormat.XLSX) return null

        val file = File(filePath)
        if (!file.exists()) return null

        return try {
            val workbook = WorkbookFactory.create(file)
            val zipFile = ZipFile(file)
            val cellImagePathMap = xlsxResolver.getCellImagePathMap(zipFile)

            val provider = DesktopSpreadsheetDataProvider(
                workbook = workbook,
                zipFile = zipFile,
                xlsxResolver = xlsxResolver,
                cellImagePathMap = cellImagePathMap,
            )

            SpreadsheetOpenResult(
                provider = provider,
                format = ImportFormat.XLSX,
                fileName = file.name,
            )
        } catch (_: Exception) {
            null
        }
    }
}
