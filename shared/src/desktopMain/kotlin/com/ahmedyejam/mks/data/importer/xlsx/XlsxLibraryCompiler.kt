package com.ahmedyejam.mks.data.importer.xlsx




import com.ahmedyejam.mks.data.importer.dto.BookDto
import com.ahmedyejam.mks.data.importer.dto.LibraryBundleDto
import com.ahmedyejam.mks.data.importer.dto.OptionDto
import com.ahmedyejam.mks.data.importer.dto.QuestionDto
import com.ahmedyejam.mks.data.importer.dto.QuizDto
import com.ahmedyejam.mks.data.importer.parser.GenericImageExtractor
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetHeaderMapper
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetQuestionParser

import com.ahmedyejam.mks.data.importer.security.ImportLimits
import com.ahmedyejam.mks.util.copyToWithLimit
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.util.UUID
import java.util.zip.ZipFile

class XlsxLibraryCompiler {
    private val xlsxResolver = XlsxImageResolver()
    private val headerMapper = SpreadsheetHeaderMapper()
    private val imageExtractor = GenericImageExtractor()

    private fun prepareTempFile(filePath: String): java.io.File? {
        val sourceFile = java.io.File(filePath)
        val extension = sourceFile.extension.ifBlank { "xlsx" }
        val file = java.io.File(System.getProperty("java.io.tmpdir"), "import_lib_${System.currentTimeMillis()}.$extension")
        return try {
            sourceFile.inputStream().use { input ->
                file.outputStream().use { output ->
                    input.copyToWithLimit(output, ImportLimits.MAX_XLSX_IMPORT_BYTES)
                }
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    fun compile(filePath: String): LibraryBundleDto {
        val file = prepareTempFile(filePath) ?: throw IllegalStateException("Could not create temporary file")
        val questions = mutableListOf<QuestionDto>()
        val fileName = java.io.File(filePath).nameWithoutExtension.ifBlank { "Imported XLSX" }
        var zipFile: ZipFile? = null

        try {
            zipFile = ZipFile(file)
            val cellImagePathMap = xlsxResolver.getCellImagePathMap(zipFile)
            WorkbookFactory.create(file).use { workbook ->
                if (workbook.numberOfSheets > ImportLimits.MAX_XLSX_SHEETS) {
                    throw IllegalStateException("XLSX has too many sheets (max ${ImportLimits.MAX_XLSX_SHEETS}).")
                }
                val formatter = DataFormatter()

                for (sheetIndex in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheetAt(sheetIndex)
                    val sheetName = sheet.sheetName
                    if (sheet.lastRowNum < 0) continue
                    if ((sheet.lastRowNum + 1) > ImportLimits.MAX_XLSX_ROWS_PER_SHEET) {
                        throw IllegalStateException("Sheet '$sheetName' exceeds ${ImportLimits.MAX_XLSX_ROWS_PER_SHEET} row limit.")
                    }

                    var bestRowIdx = 0
                    var maxScore = -100
                    val scanLimit = minOf(sheet.lastRowNum, 10)
                    for (i in 0..scanLimit) {
                        val row = sheet.getRow(i) ?: continue
                        val cells =
                            extractRowCells(
                                row,
                                row.lastCellNum.toInt().coerceAtLeast(0).coerceAtMost(ImportLimits.MAX_XLSX_COLUMNS),
                                formatter,
                            )
                        val score = headerMapper.calculateRowScore(cells)
                        if (score > maxScore) {
                            maxScore = score
                            bestRowIdx = i
                        }
                        if (score >= 200) break
                    }

                    val headerColumnCount = detectStableColumnCount(sheet, bestRowIdx).coerceAtMost(ImportLimits.MAX_XLSX_COLUMNS)
                    if ((sheet.lastRowNum + 1).toLong() * headerColumnCount.toLong() > ImportLimits.MAX_XLSX_CELLS_PER_SHEET) {
                        throw IllegalStateException("Sheet '$sheetName' exceeds ${ImportLimits.MAX_XLSX_CELLS_PER_SHEET} cell limit.")
                    }
                    val headerCells = extractRowCells(sheet.getRow(bestRowIdx), headerColumnCount, formatter)
                    val mapping = headerMapper.mapHeaders(headerCells)
                    val optionCols = headerMapper.guessOptionColumns(headerCells, mapping)
                    val sheetImages = xlsxResolver.resolveSheetImages(zipFile, sheetName, cellImagePathMap)
                    val parser =
                        SpreadsheetQuestionParser(
                            mapping = mapping,
                            optionCols = optionCols,
                            sheetAddressImages = sheetImages.addressImages,
                            sheetRowImages = sheetImages.rowImages,
                            imageExtractor = imageExtractor,
                        )

                    for (i in (bestRowIdx + 1)..sheet.lastRowNum) {
                        val row = sheet.getRow(i) ?: continue
                        val cells = extractRowCells(row, headerColumnCount, formatter)
                        val parsedQuestion = parser.parseRow(cells, i + 1) ?: continue
                        if (parsedQuestion.stem.isBlank() && parsedQuestion.options.isEmpty()) continue

                        val deterministicId =
                            parsedQuestion.externalId
                                ?: generateDeterministicId(parsedQuestion.stem, parsedQuestion.options.map { it.text })

                        questions.add(
                            QuestionDto(
                                id = deterministicId,
                                stem = parsedQuestion.stem,
                                options = parsedQuestion.options.map { OptionDto(it.id, it.text) },
                                correct = parsedQuestion.correctAnswers,
                                explanation = parsedQuestion.explanation.orEmpty(),
                                hint = parsedQuestion.hint.orEmpty(),
                                reference = parsedQuestion.reference.orEmpty(),
                                imageDataUrl = parsedQuestion.imageDataUrl.orEmpty(),
                                imageSource = parsedQuestion.imageSource.orEmpty(),
                                categories = parsedQuestion.categories,
                                answerMode = if (parsedQuestion.correctAnswers.size > 1) "multiple" else "single",
                                additionalInfo = parsedQuestion.additionalInfo.orEmpty(),
                                sourceLine = parsedQuestion.sourceLine.takeIf { it > 0 },
                            ),
                        )
                    }
                }
            }
        } finally {
            zipFile?.close()
            file.delete()
        }

        val bookId = UUID.randomUUID().toString()
        val quizId = UUID.randomUUID().toString()

        return LibraryBundleDto(
            books = listOf(BookDto(id = bookId, title = fileName)),
            quizzes =
                listOf(
                    QuizDto(
                        id = quizId,
                        bookId = bookId,
                        title = fileName,
                        questions = questions,
                    ),
                ),
        )
    }

    private fun extractRowCells(
        row: Row?,
        columnCount: Int,
        formatter: DataFormatter,
    ): List<String> {
        if (row == null || columnCount <= 0) return emptyList()
        return (0 until columnCount).map { columnIndex ->
            row.getCell(columnIndex)?.let { cell ->
                formatCellSafely(cell, formatter).trim()
            }.orEmpty()
        }
    }

    private fun formatCellSafely(
        cell: Cell,
        formatter: DataFormatter,
    ): String {
        return try {
            if (cell.cellType == CellType.FORMULA) {
                fallbackFormulaText(cell, formatter)
            } else {
                formatter.formatCellValue(cell)
            }
        } catch (_: Exception) {
            fallbackFormulaText(cell, formatter)
        }
    }

    private fun fallbackFormulaText(
        cell: Cell,
        formatter: DataFormatter,
    ): String {
        if (cell.cellType != CellType.FORMULA) {
            return runCatching { formatter.formatCellValue(cell) }.getOrElse { cell.toString() }
        }

        return try {
            when (cell.cachedFormulaResultType) {
                CellType.STRING -> cell.richStringCellValue?.string.orEmpty()
                CellType.NUMERIC ->
                    formatter.formatRawCellContents(
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

    private fun detectStableColumnCount(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        headerRowIdx: Int,
    ): Int {
        var maxColumns = 0
        val scanEnd = minOf(sheet.lastRowNum, headerRowIdx + 40)
        for (rowIndex in 0..scanEnd) {
            val row = sheet.getRow(rowIndex) ?: continue
            maxColumns = maxOf(maxColumns, row.lastCellNum.toInt().coerceAtLeast(0))
        }
        return maxColumns
    }

    private fun generateDeterministicId(stem: String, options: List<String>): String {
        val data = stem + options.joinToString("|")
        return java.util.UUID.nameUUIDFromBytes(data.toByteArray()).toString()
    }
}
