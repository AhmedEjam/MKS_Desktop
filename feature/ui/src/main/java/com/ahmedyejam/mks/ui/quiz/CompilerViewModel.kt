package com.ahmedyejam.mks.ui.quiz


import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.importer.detector.ImportFormatDetector
import com.ahmedyejam.mks.data.importer.model.ImportFormat
import com.ahmedyejam.mks.data.importer.model.ImportMode
import com.ahmedyejam.mks.data.importer.model.ParseStats
import com.ahmedyejam.mks.data.importer.model.ParsedQuestion
import com.ahmedyejam.mks.data.importer.parser.GenericImageExtractor
import com.ahmedyejam.mks.data.importer.parser.HtmlQuestionParser
import com.ahmedyejam.mks.data.importer.parser.JsonQuestionParser
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetHeaderMapper
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetQuestionParser
import com.ahmedyejam.mks.data.importer.parser.TextQuestionParser
import com.ahmedyejam.mks.data.importer.security.ImportLimits
import com.ahmedyejam.mks.data.importer.xlsx.SheetImageResolution
import com.ahmedyejam.mks.data.importer.xlsx.XlsxImageResolver
import com.ahmedyejam.mks.data.local.entity.FlashcardEntity
import com.ahmedyejam.mks.data.repository.KnowledgeRepository
import com.ahmedyejam.mks.data.repository.QuizRepository
import com.ahmedyejam.mks.util.copyToWithLimit
import com.ahmedyejam.mks.util.readTextWithLimit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.util.zip.ZipFile
import javax.inject.Inject

data class CompilerUiState(
    val questions: List<ParsedQuestion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val mode: ImportMode = ImportMode.AUTO,
    val detectedMode: ImportMode? = null,
    val sheetNames: List<String> = emptyList(),
    val selectedSheet: String? = null,
    val headerRow: Int = 0,
    val mapping: Map<String, Int> = emptyMap(),
    val optionColumns: List<Int> = emptyList(),
    val availableColumns: List<String> = emptyList(),
    val stats: ParseStats = ParseStats(),
    val targetQuizId: Long? = null,
    val targetDeckId: Long? = null,
)

@HiltViewModel
class CompilerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val knowledgeRepository: KnowledgeRepository,
    private val quizRepository: QuizRepository,
    private val dataStoreManager: com.ahmedyejam.mks.data.preferences.DataStoreManager
) : ViewModel() {

    private val applicationContext = context.applicationContext
    private val _uiState = MutableStateFlow(CompilerUiState())
    val uiState = _uiState.asStateFlow()

    private val importFormatDetector = ImportFormatDetector(applicationContext)
    private val csvParser = com.ahmedyejam.mks.data.importer.parser.CsvParser()
    private val imageExtractor = GenericImageExtractor()
    private val xlsxResolver = XlsxImageResolver()
    private val jsonParser = JsonQuestionParser(imageExtractor)
    private val htmlParser = HtmlQuestionParser(jsonParser)
    private val textParser = TextQuestionParser(imageExtractor)
    private val headerMapper = SpreadsheetHeaderMapper()

    private var cellImagePathMap: Map<String, String> = emptyMap()
    private var currentUri: Uri? = null
    private var tempFile: java.io.File? = null
    private var currentFormat: ImportFormat = ImportFormat.UNKNOWN
    private var activeWorkspaceIdOverride: Long? = null

    // Cache for raw spreadsheet/CSV data
    private var cachedRows: List<List<String>>? = null
    private var cachedSheetImages: SheetImageResolution? = null

    // Persistent objects for performance
    private var currentWorkbook: org.apache.poi.ss.usermodel.Workbook? = null
    private var currentZipFile: ZipFile? = null

    override fun onCleared() {
        super.onCleared()
        closeCurrentResources()
        tempFile?.delete()
    }

    private fun closeCurrentResources() {
        try {
            currentWorkbook?.close()
            currentZipFile?.close()
        } catch (_: Exception) {}
        currentWorkbook = null
        currentZipFile = null
    }

    private fun prepareTempFile(uri: Uri, format: ImportFormat): java.io.File? {
        tempFile?.delete()
        val extension = getFileName(uri)
            ?.substringAfterLast('.', "xlsx")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: "xlsx"
        val file = java.io.File(applicationContext.cacheDir, "import_${System.currentTimeMillis()}.$extension")
        val maxBytes = when (format) {
            ImportFormat.CSV_TSV -> ImportLimits.MAX_CSV_IMPORT_BYTES
            else -> ImportLimits.MAX_XLSX_IMPORT_BYTES
        }
        return try {
            applicationContext.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyToWithLimit(output, maxBytes)
                }
            }
            tempFile = file
            file
        } catch (_: Exception) {
            null
        }
    }

    fun onFileSelected(
        uri: Uri,
        targetQuizId: Long? = null,
        targetDeckId: Long? = null,
        activeWorkspaceId: Long? = null
    ) {
        currentUri = uri
        activeWorkspaceIdOverride = activeWorkspaceId
        closeCurrentResources()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, questions = emptyList(), targetQuizId = targetQuizId, targetDeckId = targetDeckId) }
            try {
                val format = withContext(Dispatchers.IO) {
                    importFormatDetector.detectFormat(uri)
                }
                currentFormat = format

                val detectedMode = when (format) {
                    ImportFormat.XLSX, ImportFormat.CSV_TSV -> ImportMode.SPREADSHEET
                    ImportFormat.JSON -> ImportMode.JSON
                    ImportFormat.HTML -> ImportMode.HTML
                    else -> ImportMode.TEXT
                }

                _uiState.update { it.copy(detectedMode = detectedMode) }

                if (detectedMode == ImportMode.SPREADSHEET) {
                    loadSpreadsheet(uri, format)
                } else {
                    val content = withContext(Dispatchers.IO) {
                        val limit = when (format) {
                            ImportFormat.HTML -> ImportLimits.MAX_HTML_IMPORT_BYTES
                            else -> ImportLimits.MAX_TEXT_IMPORT_BYTES
                        }
                        applicationContext.contentResolver.openInputStream(uri)?.use { it.readTextWithLimit(limit) } ?: ""
                    }
                    parseContent(content, detectedMode)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private suspend fun loadSpreadsheet(uri: Uri, format: ImportFormat) = withContext(Dispatchers.IO) {
        try {
            closeCurrentResources()
            val file = prepareTempFile(uri, format) ?: throw IllegalStateException("Could not create temporary file")
            
            cellImagePathMap = if (format == ImportFormat.XLSX) {
                try {
                    val zipFile = ZipFile(file)
                    currentZipFile = zipFile
                    xlsxResolver.getCellImagePathMap(zipFile)
                } catch (_: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            if (format == ImportFormat.CSV_TSV) {
                val sheetName = "CSV/TSV Content"
                _uiState.update {
                    it.copy(
                        sheetNames = listOf(sheetName),
                        selectedSheet = sheetName,
                        isLoading = false
                    )
                }
                onSheetSelected(sheetName)
            } else {
                val workbook = withContext(Dispatchers.IO) {
                    WorkbookFactory.create(file)
                }
                if (workbook.numberOfSheets > ImportLimits.MAX_XLSX_SHEETS) {
                    throw IllegalStateException("XLSX has too many sheets (max ${ImportLimits.MAX_XLSX_SHEETS}).")
                }
                currentWorkbook = workbook
                
                val sheets = (0 until workbook.numberOfSheets).map { workbook.getSheetAt(it).sheetName }
                val firstSheet = sheets.firstOrNull() ?: return@withContext

                _uiState.update {
                    it.copy(
                        sheetNames = sheets,
                        selectedSheet = firstSheet,
                        isLoading = false
                    )
                }

                onSheetSelected(firstSheet)
            }
        } catch (e: Exception) {
            closeCurrentResources()
            _uiState.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    fun onSheetSelected(sheetName: String?) {
        val file = tempFile ?: return
        if (sheetName == null) return
        
        // Reset cache for new sheet
        cachedRows = null
        cachedSheetImages = null
        
        _uiState.update { it.copy(isLoading = true, selectedSheet = sheetName) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (currentFormat == ImportFormat.CSV_TSV) {
                    val rows = parseCsvFile(file)
                    cachedRows = rows
                    
                    var bestRowIdx = 0
                    var maxScore = -100
                    val scanLimit = minOf(rows.size - 1, 10)
                    
                    for (i in 0..scanLimit) {
                        val cells = rows[i]
                        val score = headerMapper.calculateRowScore(cells)
                        if (score > maxScore) {
                            maxScore = score
                            bestRowIdx = i
                        }
                        if (score >= 200) break
                    }
                    
                    processSpreadsheetRows(rows, rows.getOrNull(bestRowIdx), bestRowIdx)
                } else {
                    val workbook = currentWorkbook ?: WorkbookFactory.create(file).also { currentWorkbook = it }
                    val sheet = workbook.getSheet(sheetName) ?: return@launch
                    if ((sheet.lastRowNum + 1) > ImportLimits.MAX_XLSX_ROWS_PER_SHEET) {
                        throw IllegalStateException("Sheet '$sheetName' exceeds ${ImportLimits.MAX_XLSX_ROWS_PER_SHEET} row limit.")
                    }
                    val formatter = DataFormatter()

                    var bestRowIdx = 0
                    var maxScore = -100
                    val scanLimit = minOf(sheet.lastRowNum, 10)
                    
                    val columnCount = detectStableColumnCount(sheet).coerceAtMost(ImportLimits.MAX_XLSX_COLUMNS)
                    if ((sheet.lastRowNum + 1).toLong() * columnCount.toLong() > ImportLimits.MAX_XLSX_CELLS_PER_SHEET) {
                        throw IllegalStateException("Sheet '$sheetName' exceeds ${ImportLimits.MAX_XLSX_CELLS_PER_SHEET} cell limit.")
                    }
                    
                    // Pre-load rows for header detection and caching
                    val allRows = mutableListOf<List<String>>()
                    for (i in 0..sheet.lastRowNum) {
                        val row = sheet.getRow(i)
                        val cells = row?.let { extractRowCells(it, columnCount, formatter) } ?: emptyList()
                        allRows.add(cells)
                        
                        if ((i <= scanLimit) && cells.isNotEmpty()) {
                            val score = headerMapper.calculateRowScore(cells)
                            if (score > maxScore) {
                                maxScore = score
                                bestRowIdx = i
                            }
                            // Early exit if high confidence header is found (Question + Answer + Options)
                            if (score >= 200) {
                                // Continue loading remaining rows without re-calculating score
                                for (j in (i + 1)..sheet.lastRowNum) {
                                    val nextRow = sheet.getRow(j)
                                    allRows.add(nextRow?.let { extractRowCells(it, columnCount, formatter) } ?: emptyList())
                                }
                                break
                            }
                        }
                    }
                    cachedRows = allRows
                    
                    // Resolve images once for the whole sheet
                    val zipFile = currentZipFile ?: ZipFile(file).also { currentZipFile = it }
                    cachedSheetImages = xlsxResolver.resolveSheetImages(zipFile, sheetName, cellImagePathMap)

                    val headerRowIdx = bestRowIdx
                    val headerRow = allRows.getOrNull(headerRowIdx) ?: emptyList()
                    processSpreadsheetRows(null, headerRow, headerRowIdx)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun processSpreadsheetRows(
        csvRows: List<List<String>>?,
        headerRow: List<String>? = null,
        headerIdx: Int = 0
    ) {
        val finalHeaderRow = headerRow ?: csvRows?.getOrNull(headerIdx) ?: emptyList()
        val mapping = headerMapper.mapHeaders(finalHeaderRow)
        val options = headerMapper.guessOptionColumns(finalHeaderRow, mapping)

        _uiState.update {
            it.copy(
                headerRow = headerIdx,
                mapping = mapping,
                optionColumns = options,
                availableColumns = finalHeaderRow
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            performCachedParse(mapping, options, headerIdx)
        }
    }

    private suspend fun performCachedParse(
        mapping: Map<String, Int>,
        optionCols: List<Int>,
        headerIdx: Int
    ) = withContext(Dispatchers.Default) {
        val rows = cachedRows ?: return@withContext
        val images = cachedSheetImages
        
        try {
            val parser = SpreadsheetQuestionParser(
                mapping = mapping,
                optionCols = optionCols,
                sheetAddressImages = images?.addressImages ?: emptyMap(),
                sheetRowImages = images?.rowImages ?: emptyMap(),
                imageExtractor = imageExtractor
            )

            val questions = mutableListOf<ParsedQuestion>()
            var skippedEmptyStem = 0
            var errors = 0
            
            for (i in (headerIdx + 1) until rows.size) {
                try {
                    val cells = rows[i]
                    val parsedQuestion = parser.parseRow(cells, i + 1)
                    if (parsedQuestion == null) {
                        skippedEmptyStem++
                    } else {
                        questions.add(parsedQuestion)
                    }
                } catch (e: Exception) {
                    errors++
                }
            }

            val stats = ParseStats(
                totalRowsProcessed = rows.size - headerIdx - 1,
                successfullyParsed = questions.size,
                skippedEmptyStem = skippedEmptyStem,
                errors = errors,
                questionsWithImages = questions.count { it.imageDataUrl != null || it.imageSource != null },
                questionsWithIssues = questions.count { it.issues.isNotEmpty() }
            )

            _uiState.update { it.copy(questions = questions, stats = stats, isLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    fun updateMapping(mapping: Map<String, Int>, optionCols: List<Int>) {
        val headerIdx = _uiState.value.headerRow

        _uiState.update {
            it.copy(
                mapping = mapping,
                optionColumns = optionCols,
                isLoading = true
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            performCachedParse(mapping, optionCols, headerIdx)
        }
    }

    fun updateHeaderRow(index: Int) {
        val rows = cachedRows ?: return
        val headerRow = rows.getOrNull(index) ?: emptyList()
        
        _uiState.update { it.copy(headerRow = index, isLoading = true) }
        processSpreadsheetRows(rows, headerRow, index)
    }

    fun toggleQuestionInclusion(index: Int) {
        _uiState.update { state ->
            val currentQuestions = state.questions.toMutableList()
            if (index in currentQuestions.indices) {
                val q = currentQuestions[index]
                currentQuestions[index] = q.copy(isIncluded = !q.isIncluded)
                state.copy(questions = currentQuestions)
            } else state
        }
    }

    fun toggleQuestionsRange(from: Int, to: Int, include: Boolean) {
        _uiState.update { state ->
            val currentQuestions = state.questions.toMutableList()
            val start = (from - 1).coerceIn(currentQuestions.indices)
            val end = (to - 1).coerceIn(currentQuestions.indices)
            val range = if (start <= end) start..end else end..start

            for (i in range) {
                currentQuestions[i] = currentQuestions[i].copy(isIncluded = include)
            }
            state.copy(questions = currentQuestions)
        }
    }

    fun updateQuestionCorrectAnswer(index: Int, answerId: String) {
        _uiState.update { state ->
            val currentQuestions = state.questions.toMutableList()
            if (index in currentQuestions.indices) {
                val q = currentQuestions[index]
                val newAnswers = if (q.correctAnswers.contains(answerId)) {
                    q.correctAnswers.filter { it != answerId }
                } else {
                    q.correctAnswers + answerId
                }
                currentQuestions[index] = q.copy(correctAnswers = newAnswers.sorted())
                state.copy(questions = currentQuestions)
            } else {
                state
            }
        }
    }

    fun saveParsedQuestions(
        title: String,
        bookId: Long?,
        targetQuizId: Long? = null,
        targetDeckId: Long? = null,
        newBookTitle: String? = null
    ) {
        val questions = _uiState.value.questions.filter { it.isIncluded }
        if (questions.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentWorkspaceId =
                    activeWorkspaceIdOverride ?: dataStoreManager.currentWorkspaceId.firstOrNull()
                val currentTargetDeckId = targetDeckId ?: _uiState.value.targetDeckId
                if (currentTargetDeckId != null) {
                    val flashcards = questions.mapIndexed { index, q ->
                        val optionsText = if (q.options.isNotEmpty()) {
                            q.options.joinToString("\n") { opt -> "${opt.id.removePrefix("opt_")}: ${opt.text}" }
                        } else ""
                        val answersText = if (q.correctAnswers.isNotEmpty()) {
                            q.options.filter { q.correctAnswers.contains(it.id) }.joinToString(", ") { it.text }
                                .takeIf { it.isNotBlank() } ?: q.correctAnswers.joinToString(", ") { it.removePrefix("opt_") }
                        } else ""
                        FlashcardEntity(
                            externalId = java.util.UUID.randomUUID().toString(),
                            deckId = currentTargetDeckId,
                            frontText = buildString {
                                append(q.stem)
                                if (optionsText.isNotBlank()) append("\n\n$optionsText")
                            },
                            backText = buildString {
                                append(answersText)
                                if (!q.explanation.isNullOrBlank()) append("\n\n${q.explanation}")
                                if (!q.reference.isNullOrBlank()) append("\n\nReference: ${q.reference}")
                            },
                            hint = q.hint,
                            tags = q.categories,
                            orderIndex = index
                        )
                    }
                    val currentOrder = knowledgeRepository.getFlashcardsByDeckId(currentTargetDeckId).firstOrNull()?.size ?: 0
                    val finalFlashcards = flashcards.mapIndexed { i, f -> f.copy(orderIndex = currentOrder + i) }
                    knowledgeRepository.insertFlashcards(finalFlashcards)
                } else {
                    quizRepository.importCompiledQuestions(
                        title = title,
                        targetBookId = bookId,
                        targetQuizId = targetQuizId,
                        newBookTitle = newBookTitle,
                        questions = questions,
                        activeWorkspaceId = currentWorkspaceId
                    )
                }
                activeWorkspaceIdOverride = null
                _uiState.update { it.copy(isLoading = false, questions = emptyList()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun parseCsvFile(file: java.io.File): List<List<String>> {
        val content = file.readTextWithLimit(ImportLimits.MAX_CSV_IMPORT_BYTES)
        return csvParser.parse(content)
    }

    private suspend fun parseContent(content: String, mode: ImportMode) = withContext(Dispatchers.Default) {
        val questions = when (mode) {
            ImportMode.JSON -> jsonParser.parse(content)
            ImportMode.HTML -> htmlParser.parse(content)
            else -> textParser.parse(content)
        }
        _uiState.update { it.copy(questions = questions, isLoading = false) }
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
                    cell.cellStyle.dataFormatString
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

    private fun getFileName(uri: Uri): String? {
        return try {
            applicationContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) cursor.getString(index) else null
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }
}
