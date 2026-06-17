package com.ahmedyejam.mks.ui.importer

import com.ahmedyejam.mks.data.importer.detector.ImportFormatDetector
import com.ahmedyejam.mks.data.importer.model.ImportFormat
import com.ahmedyejam.mks.data.importer.model.ImportMode
import com.ahmedyejam.mks.data.importer.model.ImportResult
import com.ahmedyejam.mks.data.importer.model.MergeStrategy
import com.ahmedyejam.mks.data.importer.model.ParseStats
import com.ahmedyejam.mks.data.importer.model.ParsedQuestion
import com.ahmedyejam.mks.data.importer.parser.BundleFileParser
import com.ahmedyejam.mks.data.importer.parser.CsvParser
import com.ahmedyejam.mks.data.importer.parser.GenericImageExtractor
import com.ahmedyejam.mks.data.importer.parser.HtmlQuestionParser
import com.ahmedyejam.mks.data.importer.parser.JsonQuestionParser
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetDataProvider
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetDataProviderFactory
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetHeaderMapper
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetQuestionParser
import com.ahmedyejam.mks.data.importer.parser.TextQuestionParser
import com.ahmedyejam.mks.data.importer.repository.ImportLibraryManager
import com.ahmedyejam.mks.data.importer.security.ImportLimits
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.util.readTextWithLimit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    val filePath: String? = null,
    val fileName: String? = null,
)

/**
 * KMP CompilerViewModel — interactive file-to-questions compilation engine.
 *
 * Supports:
 * - Spreadsheet (XLSX/CSV/TSV): multi-step interactive with sheet selection,
 *   header row detection, column mapping, question toggling
 * - Text/JSON/HTML: direct parse with question toggling
 *
 * Uses [SpreadsheetDataProviderFactory] for platform-specific spreadsheet access
 * (Apache POI on JVM) and commonMain parsers for text formats.
 */
class CompilerViewModel(
    private val importManager: ImportLibraryManager,
    private val spreadsheetFactory: SpreadsheetDataProviderFactory,
    private val bundleFileParser: BundleFileParser,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) {
    private val _uiState = MutableStateFlow(CompilerUiState())
    val uiState = _uiState.asStateFlow()

    private val formatDetector = ImportFormatDetector()
    private val csvParser = CsvParser()
    private val headerMapper = SpreadsheetHeaderMapper()
    private val imageExtractor = GenericImageExtractor()
    private val jsonQuestionParser = JsonQuestionParser(imageExtractor)
    private val htmlParser = HtmlQuestionParser(jsonQuestionParser)
    private val textParser = TextQuestionParser(imageExtractor)

    private var currentProvider: SpreadsheetDataProvider? = null
    private var cachedRows: List<List<String>>? = null
    private var currentFormat: ImportFormat = ImportFormat.UNKNOWN
    private var activeWorkspaceIdOverride: Long? = null

    fun onFileSelected(
        filePath: String,
        targetQuizId: Long? = null,
        targetDeckId: Long? = null,
        activeWorkspaceId: Long? = null,
    ) {
        activeWorkspaceIdOverride = activeWorkspaceId
        closeCurrentResources()

        coroutineScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true, error = null, questions = emptyList(),
                    targetQuizId = targetQuizId, targetDeckId = targetDeckId,
                    filePath = filePath, fileName = File(filePath).name,
                )
            }

            try {
                val format = withContext(Dispatchers.IO) { formatDetector.detectFormat(filePath) }
                currentFormat = format

                val detectedMode = when (format) {
                    ImportFormat.XLSX, ImportFormat.CSV_TSV -> ImportMode.SPREADSHEET
                    ImportFormat.JSON -> ImportMode.JSON
                    ImportFormat.HTML -> ImportMode.HTML
                    ImportFormat.ZIP -> ImportMode.JSON // ZIP bundles parsed as JSON inside
                    else -> ImportMode.TEXT
                }

                _uiState.update { it.copy(detectedMode = detectedMode) }

                if (detectedMode == ImportMode.SPREADSHEET) {
                    loadSpreadsheet(filePath, format)
                } else {
                    val content = withContext(Dispatchers.IO) {
                        val limit = when (format) {
                            ImportFormat.HTML -> ImportLimits.MAX_HTML_IMPORT_BYTES
                            else -> ImportLimits.MAX_TEXT_IMPORT_BYTES
                        }
                        File(filePath).readTextWithLimit(limit)
                    }
                    parseContent(content, detectedMode, filePath, format)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private suspend fun loadSpreadsheet(filePath: String, format: ImportFormat) =
        withContext(Dispatchers.IO) {
            try {
                closeCurrentResources()

                // Try platform-specific spreadsheet provider (XLSX via Apache POI)
                val openResult = spreadsheetFactory.open(filePath, format)
                if (openResult != null) {
                    currentProvider = openResult.provider
                    val sheetNames = openResult.provider.getSheetNames()
                    val firstSheet = sheetNames.firstOrNull()

                    _uiState.update {
                        it.copy(
                            sheetNames = sheetNames,
                            selectedSheet = firstSheet,
                            fileName = openResult.fileName,
                            isLoading = false,
                        )
                    }

                    if (firstSheet != null) {
                        onSheetSelected(firstSheet)
                    }
                    return@withContext
                }

                // Fallback for CSV/TSV (no Apache POI needed)
                if (format == ImportFormat.CSV_TSV) {
                    val content = File(filePath).readTextWithLimit(ImportLimits.MAX_CSV_IMPORT_BYTES)
                    val rows = csvParser.parse(content)
                    cachedRows = rows

                    val sheetName = "CSV/TSV Content"
                    _uiState.update {
                        it.copy(
                            sheetNames = listOf(sheetName),
                            selectedSheet = sheetName,
                            isLoading = false,
                        )
                    }
                    onSheetSelected(sheetName)
                } else {
                    throw IllegalStateException("Spreadsheet format not supported: $format")
                }
            } catch (e: Exception) {
                closeCurrentResources()
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }

    fun onSheetSelected(sheetName: String?) {
        if (sheetName == null) return

        _uiState.update { it.copy(isLoading = true, selectedSheet = sheetName) }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                if (currentFormat == ImportFormat.CSV_TSV && currentProvider == null) {
                    // CSV handled directly in commonMain
                    val rows = cachedRows ?: return@launch

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
                    // XLSX via SpreadsheetDataProvider
                    val provider = currentProvider ?: return@launch
                    val rows = provider.getSheetRows(sheetName)
                    cachedRows = rows

                    val addressImages = provider.getSheetAddressImages(sheetName)
                    val rowImages = provider.getSheetRowImages(sheetName)

                    var bestRowIdx = 0
                    var maxScore = -100
                    val scanLimit = minOf(rows.size - 1, 10)

                    for (i in 0..scanLimit) {
                        val cells = rows.getOrNull(i) ?: emptyList()
                        val score = headerMapper.calculateRowScore(cells)
                        if (score > maxScore) {
                            maxScore = score
                            bestRowIdx = i
                        }
                        if (score >= 200) break
                    }

                    processSpreadsheetRows(rows, rows.getOrNull(bestRowIdx), bestRowIdx, addressImages, rowImages)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun processSpreadsheetRows(
        rows: List<List<String>>,
        headerRow: List<String>? = null,
        headerIdx: Int = 0,
        addressImages: Map<String, String> = emptyMap(),
        rowImages: Map<Int, String> = emptyMap(),
    ) {
        val finalHeaderRow = headerRow ?: rows.getOrNull(headerIdx) ?: emptyList()
        val mapping = headerMapper.mapHeaders(finalHeaderRow)
        val options = headerMapper.guessOptionColumns(finalHeaderRow, mapping)

        _uiState.update {
            it.copy(
                headerRow = headerIdx,
                mapping = mapping,
                optionColumns = options,
                availableColumns = finalHeaderRow,
            )
        }

        coroutineScope.launch(Dispatchers.Default) {
            performCachedParse(mapping, options, headerIdx, addressImages, rowImages)
        }
    }

    private suspend fun performCachedParse(
        mapping: Map<String, Int>,
        optionCols: List<Int>,
        headerIdx: Int,
        addressImages: Map<String, String> = emptyMap(),
        rowImages: Map<Int, String> = emptyMap(),
    ) = withContext(Dispatchers.Default) {
        val rows = cachedRows ?: return@withContext

        try {
            val parser = SpreadsheetQuestionParser(
                mapping = mapping,
                optionCols = optionCols,
                sheetAddressImages = addressImages,
                sheetRowImages = rowImages,
                imageExtractor = imageExtractor,
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
                } catch (_: Exception) {
                    errors++
                }
            }

            val stats = ParseStats(
                totalRowsProcessed = rows.size - headerIdx - 1,
                successfullyParsed = questions.size,
                skippedEmptyStem = skippedEmptyStem,
                errors = errors,
                questionsWithImages = questions.count { it.imageDataUrl != null || it.imageSource != null },
                questionsWithIssues = questions.count { it.issues.isNotEmpty() },
            )

            _uiState.update { it.copy(questions = questions, stats = stats, isLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isLoading = false) }
        }
    }

    fun updateMapping(mapping: Map<String, Int>, optionCols: List<Int>) {
        val headerIdx = _uiState.value.headerRow

        _uiState.update { it.copy(mapping = mapping, optionColumns = optionCols, isLoading = true) }

        val provider = currentProvider
        val sheetName = _uiState.value.selectedSheet
        val addressImages = provider?.getSheetAddressImages(sheetName ?: "") ?: emptyMap()
        val rowImages = provider?.getSheetRowImages(sheetName ?: "") ?: emptyMap()

        coroutineScope.launch(Dispatchers.Default) {
            performCachedParse(mapping, optionCols, headerIdx, addressImages, rowImages)
        }
    }

    fun updateHeaderRow(index: Int) {
        val rows = cachedRows ?: return
        val headerRow = rows.getOrNull(index) ?: emptyList()

        _uiState.update { it.copy(headerRow = index, isLoading = true) }
        processSpreadsheetRows(rows, headerRow, index)
    }

    fun updateOptionColumns(optionCols: List<Int>) {
        updateMapping(_uiState.value.mapping, optionCols)
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

    /**
     * Save the included parsed questions as a quiz or flashcard deck.
     *
     * For quiz: delegates to [ImportLibraryManager.importQuestions].
     * For flashcards: converts ParsedQuestions → FlashcardEntities and inserts via KnowledgeRepository.
     */
    fun saveParsedQuestions(
        title: String,
        bookId: Long?,
        targetQuizId: Long? = null,
        targetDeckId: Long? = null,
        newBookTitle: String? = null,
    ) {
        val questions = _uiState.value.questions.filter { it.isIncluded }
        if (questions.isEmpty()) return

        coroutineScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = importManager.importQuestions(
                    title = title,
                    questions = questions,
                    targetBookId = bookId,
                    targetQuizId = targetQuizId ?: _uiState.value.targetQuizId,
                    newBookTitle = newBookTitle,
                    activeWorkspaceId = activeWorkspaceIdOverride,
                )

                when (result) {
                    is MksResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, questions = emptyList()) }
                    }
                    is MksResult.Error -> {
                        _uiState.update { it.copy(error = result.message, isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private suspend fun parseContent(
        content: String,
        mode: ImportMode,
        filePath: String,
        format: ImportFormat,
    ) = withContext(Dispatchers.Default) {
        // For JSON library bundles, ZIP, and formats handled by BundleFileParser,
        // try the platform-specific parser first
        if (format == ImportFormat.JSON || format == ImportFormat.ZIP) {
            val parsed = withContext(Dispatchers.IO) { bundleFileParser.parseFile(filePath, format) }
            if (parsed != null) {
                // Flatten all quizzes' questions into a single list for preview
                val allQuestions = parsed.bundle.quizzes.flatMap { it.questions }
                    .map { dto ->
                        ParsedQuestion(
                            stem = dto.stem,
                            options = dto.options.map { com.ahmedyejam.mks.data.importer.model.ParsedOption(it.id, it.text) },
                            correctAnswers = dto.correct,
                            explanation = dto.explanation,
                            hint = dto.hint,
                            reference = dto.reference,
                            imageDataUrl = dto.imageDataUrl,
                            imageSource = dto.imageSource,
                            categories = dto.categories,
                            sourceLine = dto.sourceLine ?: 0,
                            isIncluded = true,
                        )
                    }
                _uiState.update { it.copy(questions = allQuestions, isLoading = false) }
                return@withContext
            }
        }

        val questions = when (mode) {
            ImportMode.JSON -> jsonQuestionParser.parse(content)
            ImportMode.HTML -> htmlParser.parse(content)
            else -> textParser.parse(content)
        }
        _uiState.update { it.copy(questions = questions, isLoading = false) }
    }

    fun closeCurrentResources() {
        try {
            currentProvider?.close()
        } catch (_: Exception) {}
        currentProvider = null
        cachedRows = null
    }
}
