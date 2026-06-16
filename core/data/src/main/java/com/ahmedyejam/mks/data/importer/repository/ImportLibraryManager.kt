package com.ahmedyejam.mks.data.importer.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.ahmedyejam.mks.data.importer.detector.ImportFormatDetector
import com.ahmedyejam.mks.data.importer.dto.BookDto
import com.ahmedyejam.mks.data.importer.dto.LibraryBundleDto
import com.ahmedyejam.mks.data.importer.dto.ManifestDto
import com.ahmedyejam.mks.data.importer.dto.OptionDto
import com.ahmedyejam.mks.data.importer.dto.QuestionDto
import com.ahmedyejam.mks.data.importer.dto.QuizDto
import com.ahmedyejam.mks.data.importer.mapping.LibraryMapper
import com.ahmedyejam.mks.data.importer.model.ImportError
import com.ahmedyejam.mks.data.importer.model.ImportFormat
import com.ahmedyejam.mks.data.importer.model.ImportPreviewDto
import com.ahmedyejam.mks.data.importer.model.ImportResult
import com.ahmedyejam.mks.data.importer.model.ImportWarning
import com.ahmedyejam.mks.data.importer.model.MergeStrategy
import com.ahmedyejam.mks.data.importer.model.ParsedQuestion
import com.ahmedyejam.mks.data.importer.normalization.BundleNormalizer
import com.ahmedyejam.mks.data.importer.parser.CsvParser
import com.ahmedyejam.mks.data.importer.parser.GenericImageExtractor
import com.ahmedyejam.mks.data.importer.parser.HtmlQuestionParser
import com.ahmedyejam.mks.data.importer.parser.JsonLibraryParser
import com.ahmedyejam.mks.data.importer.parser.JsonQuestionParser
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetHeaderMapper
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetQuestionParser
import com.ahmedyejam.mks.data.importer.parser.TextQuestionParser
import com.ahmedyejam.mks.data.importer.parser.ZipLibraryParser
import com.ahmedyejam.mks.data.importer.security.ImportLimits
import com.ahmedyejam.mks.data.importer.validation.ImportValidator
import com.ahmedyejam.mks.data.importer.xlsx.XlsxLibraryCompiler
import com.ahmedyejam.mks.data.local.FileManager
import com.ahmedyejam.mks.data.local.MksDatabase
import com.ahmedyejam.mks.data.local.WorkspaceDefaults
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.WorkspaceEntity
import com.ahmedyejam.mks.data.local.entity.WorkspaceSettingsEntity
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.data.network.RemoteAssetPolicy
import com.ahmedyejam.mks.util.readTextWithLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class ImportLibraryManager(
    private val context: Context,
    private val database: MksDatabase,
    private val fileManager: FileManager,
) {
    private val bookDao = database.bookDao()
    private val quizDao = database.quizDao()
    private val questionDao = database.questionDao()
    private val sessionDao = database.sessionDao()
    private val categoryMetadataDao = database.categoryMetadataDao()
    private val questionCategoryDao = database.questionCategoryDao()
    private val assetReferenceDao = database.assetReferenceDao()
    private val workspaceDao = database.workspaceDao()

    private val formatDetector = ImportFormatDetector(context)
    private val jsonParser = JsonLibraryParser()
    private val zipParser = ZipLibraryParser(context, jsonParser)

    private val csvParser = CsvParser()
    private val headerMapper = SpreadsheetHeaderMapper()
    private val imageExtractor = GenericImageExtractor()
    private val textParser = TextQuestionParser(imageExtractor)
    private val jsonQuestionParser = JsonQuestionParser(imageExtractor)
    private val htmlParser = HtmlQuestionParser(jsonParser = jsonQuestionParser)
    private val xlsxCompiler = XlsxLibraryCompiler(context)

    private val validator = ImportValidator()
    private val normalizer = BundleNormalizer()
    private val mapper = LibraryMapper()

    private suspend fun getOrCreateDefaultWorkspaceId(): Long {
        workspaceDao.getWorkspaceByExternalId(WorkspaceDefaults.DEFAULT_EXTERNAL_ID)?.let { return it.id }
        workspaceDao.getDefaultWorkspace()?.let { workspace ->
            workspaceDao.updateWorkspace(
                workspace.copy(
                    externalId = WorkspaceDefaults.DEFAULT_EXTERNAL_ID,
                    name = WorkspaceDefaults.DEFAULT_NAME,
                    description = WorkspaceDefaults.DEFAULT_DESCRIPTION,
                    isDefault = true,
                    deletedAt = null,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            if (workspaceDao.getSettingsByWorkspaceId(workspace.id) == null) {
                workspaceDao.insertSettings(WorkspaceSettingsEntity(workspaceId = workspace.id))
            }
            return workspace.id
        }

        val workspaceId =
            workspaceDao.insertWorkspace(
                WorkspaceEntity(
                    externalId = WorkspaceDefaults.DEFAULT_EXTERNAL_ID,
                    name = WorkspaceDefaults.DEFAULT_NAME,
                    description = WorkspaceDefaults.DEFAULT_DESCRIPTION,
                    isDefault = true,
                ),
            )
        workspaceDao.insertSettings(WorkspaceSettingsEntity(workspaceId = workspaceId))
        return workspaceId
    }

    fun cleanupStaleImportCache(maxAgeMillis: Long = 24L * 60L * 60L * 1000L) {
        val cutoff = System.currentTimeMillis() - maxAgeMillis
        context.cacheDir.listFiles()
            ?.filter { file -> file.name.startsWith("import_") && file.lastModified() < cutoff }
            ?.forEach { file -> runCatching { file.deleteRecursively() } }
    }

    private data class ResolvedImageResult(
        val path: String? = null,
        val warning: String? = null,
        val importedLocally: Boolean = false,
    )

    private fun isTrackableLocalAsset(path: String?): Boolean {
        val value = path?.trim().orEmpty()
        if (value.isBlank()) return false
        return !value.startsWith("http://", ignoreCase = true) &&
            !value.startsWith("https://", ignoreCase = true) &&
            !value.startsWith("content://", ignoreCase = true) &&
            !value.startsWith("data:", ignoreCase = true) &&
            !value.startsWith("assets/", ignoreCase = true)
    }

    private suspend fun replaceOwnerAssetReferences(
        ownerType: String,
        ownerId: Long,
        paths: List<String?>,
    ) {
        val cleaned =
            paths
                .mapNotNull { it?.trim()?.takeIf { value -> isTrackableLocalAsset(value) } }
                .distinct()
        val previousPaths =
            assetReferenceDao.getReferencesForOwner(ownerType, ownerId)
                .map { it.path }
                .distinct()
        assetReferenceDao.replaceOwnerReferences(ownerType, ownerId, cleaned)
        previousPaths
            .filterNot { it in cleaned }
            .forEach { path ->
                if (assetReferenceDao.countReferencesForPath(path) == 0) {
                    fileManager.deleteImage(path)
                }
            }
    }

    private suspend fun syncQuestionCategories(
        questionId: Long,
        categories: List<String>,
    ) {
        questionCategoryDao.replaceCategories(questionId, categories)
    }

    fun detectFormat(uri: Uri): ImportFormat {
        return formatDetector.detectFormat(uri)
    }

    suspend fun getImportPreview(uri: Uri): ImportPreviewDto =
        withContext(Dispatchers.IO) {
            val format = formatDetector.detectFormat(uri)
            if (format == ImportFormat.UNKNOWN) throw Exception("Unsupported file format")

            var zipResult: ZipLibraryParser.ZipResult? = null
            try {
                val bundle =
                    when (format) {
                        ImportFormat.JSON -> {
                            val stream = context.contentResolver.openInputStream(uri) ?: throw Exception("Could not open stream")
                            stream.use { jsonParser.parse(it) }
                        }
                        ImportFormat.ZIP -> {
                            val stream = context.contentResolver.openInputStream(uri) ?: throw Exception("Could not open stream")
                            val parsedZip = stream.use { zipParser.parse(it) }
                            zipResult = parsedZip
                            parsedZip.bundle
                        }
                        ImportFormat.XLSX -> {
                            xlsxCompiler.compile(uri)
                        }
                        ImportFormat.CSV_TSV, ImportFormat.TEXT, ImportFormat.HTML -> {
                            val content = readImportText(uri, format)
                            val questions =
                                when (format) {
                                    ImportFormat.HTML -> htmlParser.parse(content)
                                    ImportFormat.TEXT -> textParser.parse(content)
                                    else -> parseSpreadsheetSimple(uri)
                                }
                            wrapQuestionsToBundle(questions, uri.lastPathSegment ?: "Imported File")
                        }
                        else -> throw Exception("Unexpected format: $format")
                    }

                val validation = validator.validate(bundle)
                val sanitizedBundle = validation.sanitizedBundle ?: bundle
                val normalized = normalizer.normalize(sanitizedBundle)

                val booksToCreate = mutableListOf<String>()
                val booksToUpdate = mutableListOf<String>()
                normalized.books.forEach { book ->
                    if (bookDao.getBookByExternalId(book.id) != null) {
                        booksToUpdate.add(book.title)
                    } else {
                        booksToCreate.add(book.title)
                    }
                }

                val quizzesToCreate = mutableListOf<String>()
                val quizzesToUpdate = mutableListOf<String>()
                normalized.quizzes.forEach { quiz ->
                    if (quizDao.getQuizByExternalId(quiz.id) != null) {
                        quizzesToUpdate.add(quiz.title)
                    } else {
                        quizzesToCreate.add(quiz.title)
                    }
                }

                val questionsToCreate = mutableListOf<String>()
                val questionsToUpdate = mutableListOf<String>()
                normalized.quizzes.forEach { quizDto ->
                    val existingQuiz = quizDao.getQuizByExternalId(quizDto.id)
                    if (existingQuiz == null) {
                        questionsToCreate.addAll(quizDto.questions.map { it.stem.take(50) })
                    } else {
                        quizDto.questions.forEach { questionDto ->
                            if (questionDao.getQuestionByExternalId(existingQuiz.id, questionDto.id) != null) {
                                questionsToUpdate.add(questionDto.stem.take(50))
                            } else {
                                questionsToCreate.add(questionDto.stem.take(50))
                            }
                        }
                    }
                }

                val searchDirs = mutableListOf<File>()
                zipResult?.libraryDir?.let { searchDirs.add(it) }
                zipResult?.rootDir?.let { if (it != zipResult.libraryDir) searchDirs.add(it) }

                val imageFiles =
                    searchDirs.flatMap { dir ->
                        dir.walkTopDown().filter {
                            it.isFile && it.extension.matches(Regex("png|jpe?g|webp|gif|svg|bmp", RegexOption.IGNORE_CASE))
                        }.toList()
                    }
                val totalImages = imageFiles.size
                val hasAssets = totalImages > 0
                val previewWarnings = validation.warnings.toMutableList()
                if (bundleContainsPlainHttpAssets(normalized)) {
                    previewWarnings.add(
                        ImportWarning(
                            "This import contains plain HTTP image URLs. They will not be downloaded unless you explicitly allow insecure image downloads in the review dialog.",
                            details = "Plain HTTP images are visible to the network and can be modified in transit.",
                        ),
                    )
                }

                ImportPreviewDto(
                    bundle = normalized,
                    booksToCreate = booksToCreate,
                    booksToUpdate = booksToUpdate,
                    quizzesToCreate = quizzesToCreate,
                    quizzesToUpdate = quizzesToUpdate,
                    questionsToCreate = questionsToCreate,
                    questionsToUpdate = questionsToUpdate,
                    totalQuestions = normalized.quizzes.sumOf { it.questions.size },
                    totalSessions = normalized.sessions?.size ?: 0,
                    totalCategories = normalized.categories.size,
                    totalImages = totalImages,
                    skippedRecordsCount = validation.skippedRecordsCount,
                    hasAssets = hasAssets,
                    warnings = previewWarnings,
                )
            } finally {
                zipResult?.rootDir?.deleteRecursively()
            }
        }

    suspend fun importLibrary(
        uri: Uri,
        strategy: MergeStrategy = MergeStrategy.SKIP_EXISTING,
        targetBookId: Long? = null,
        targetQuizId: Long? = null,
        allowInsecureRemoteImages: Boolean = false,
        activeWorkspaceId: Long? = null,
        onProgress: (Float, String) -> Unit = { _, _ -> },
    ): MksResult<ImportResult> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            onProgress(0.05f, "Detecting format...")
            val format = formatDetector.detectFormat(uri)

            if (format == ImportFormat.UNKNOWN) {
                return@withContext MksResult.Error(message = "Unsupported file format")
            }

            var zipResult: ZipLibraryParser.ZipResult? = null
            try {
                val bundle =
                    when (format) {
                        ImportFormat.JSON -> {
                            onProgress(0.1f, "Parsing JSON...")
                            val stream =
                                context.contentResolver.openInputStream(uri)
                                    ?: throw Exception("Could not open stream")
                            stream.use { jsonParser.parse(it) }
                        }
                        ImportFormat.ZIP -> {
                            onProgress(0.1f, "Extracting ZIP...")
                            val stream =
                                context.contentResolver.openInputStream(uri)
                                    ?: throw Exception("Could not open stream")
                            val parsedZip = stream.use { zipParser.parse(it) }
                            zipResult = parsedZip
                            parsedZip.bundle
                        }
                        ImportFormat.XLSX -> {
                            onProgress(0.1f, "Compiling XLSX...")
                            xlsxCompiler.compile(uri)
                        }
                        ImportFormat.CSV_TSV, ImportFormat.TEXT, ImportFormat.HTML -> {
                            onProgress(0.1f, "Parsing file...")
                            val content = readImportText(uri, format)
                            val questions =
                                when (format) {
                                    ImportFormat.HTML -> htmlParser.parse(content)
                                    ImportFormat.TEXT -> textParser.parse(content)
                                    else -> parseSpreadsheetSimple(uri)
                                }
                            wrapQuestionsToBundle(questions, uri.lastPathSegment ?: "Imported File")
                        }
                        else -> throw Exception("Unexpected format: $format")
                    }

                val result =
                    executeImportPipeline(
                        bundle = bundle,
                        assetsDir = zipResult?.libraryDir,
                        format = format,
                        strategy = strategy,
                        targetBookId = targetBookId,
                        targetQuizId = targetQuizId,
                        startTime = startTime,
                        onProgress = onProgress,
                        allowInsecureRemoteImages = allowInsecureRemoteImages,
                        rootDir = zipResult?.rootDir,
                        manifest = zipResult?.manifest,
                        activeWorkspaceId = activeWorkspaceId,
                    )

                return@withContext MksResult.Success(result)
            } catch (e: Exception) {
                return@withContext MksResult.Error(
                    message = "Import failed: ${e.message}",
                    exception = e
                )
            } finally {
                zipResult?.rootDir?.deleteRecursively()
            }
        }

    suspend fun importQuestions(
        title: String,
        questions: List<ParsedQuestion>,
        targetBookId: Long? = null,
        targetQuizId: Long? = null,
        newBookTitle: String? = null,
        activeWorkspaceId: Long? = null,
        onProgress: (Float, String) -> Unit = { _, _ -> },
    ): MksResult<ImportResult> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val bundle =
                wrapQuestionsToBundle(
                    questions = questions,
                    quizTitle = title,
                    bookTitle = newBookTitle,
                    includeBook = targetBookId == null && targetQuizId == null,
                )

            try {
                val result = executeImportPipeline(
                    bundle = bundle,
                    assetsDir = null,
                    format = ImportFormat.TEXT,
                    strategy = MergeStrategy.SKIP_EXISTING,
                    targetBookId = targetBookId,
                    targetQuizId = targetQuizId,
                    startTime = startTime,
                    onProgress = onProgress,
                    activeWorkspaceId = activeWorkspaceId,
                )
                MksResult.Success(result)
            } catch (e: Exception) {
                MksResult.Error(
                    message = "Questions import failed: ${e.message}",
                    exception = e
                )
            }
        }

    private fun wrapQuestionsToBundle(
        questions: List<ParsedQuestion>,
        quizTitle: String,
        bookTitle: String? = null,
        includeBook: Boolean = true,
    ): LibraryBundleDto {
        val normalizedQuizTitle = quizTitle.ifBlank { "Imported Quiz" }
        val normalizedBookTitle = bookTitle?.ifBlank { normalizedQuizTitle } ?: normalizedQuizTitle
        val quizId = UUID.nameUUIDFromBytes(normalizedQuizTitle.toByteArray()).toString()
        val bookId = UUID.nameUUIDFromBytes("book_$normalizedBookTitle".toByteArray()).toString()

        val questionDtos =
            questions.map { pq ->
                val deterministicId = pq.externalId ?: generateDeterministicId(pq.stem, pq.options.map { it.text })
                QuestionDto(
                    id = deterministicId,
                    stem = pq.stem,
                    options = pq.options.map { OptionDto(it.id, it.text) },
                    correct = pq.correctAnswers,
                    explanation = pq.explanation ?: "",
                    hint = pq.hint ?: "",
                    reference = pq.reference ?: "",
                    imageDataUrl = pq.imageDataUrl ?: "",
                    imageSource = pq.imageSource ?: "",
                    categories = pq.categories,
                    additionalInfo = pq.additionalInfo ?: "",
                    sourceLine = pq.sourceLine.takeIf { it > 0 },
                )
            }

        return LibraryBundleDto(
            books = if (includeBook) listOf(BookDto(id = bookId, title = normalizedBookTitle)) else emptyList(),
            quizzes =
                listOf(
                    QuizDto(
                        id = quizId,
                        bookId = bookId,
                        title = normalizedQuizTitle,
                        questions = questionDtos,
                    ),
                ),
        )
    }

    private fun parseSpreadsheetSimple(uri: Uri): List<ParsedQuestion> {
        val content = runCatching { readImportText(uri, ImportFormat.CSV_TSV) }.getOrDefault("")
        if (content.isBlank()) return emptyList()

        val rows = csvParser.parse(content)
        if (rows.isEmpty()) return emptyList()

        // 1. Detect header row
        var bestRowIdx = 0
        var maxScore = -1
        val scanLimit = minOf(rows.size - 1, 20)

        for (i in 0..scanLimit) {
            val cells = rows[i]
            val mapping = headerMapper.mapHeaders(cells)
            if (mapping.size > maxScore) {
                maxScore = mapping.size
                bestRowIdx = i
            }
        }

        val headerRow = rows[bestRowIdx]
        val mapping = headerMapper.mapHeaders(headerRow)
        val optionCols = headerMapper.guessOptionColumns(headerRow, mapping)

        val parser =
            SpreadsheetQuestionParser(
                mapping = mapping,
                optionCols = optionCols,
                sheetAddressImages = emptyMap(),
                sheetRowImages = emptyMap(),
                imageExtractor = imageExtractor,
            )

        val questions = mutableListOf<ParsedQuestion>()
        for (i in (bestRowIdx + 1) until rows.size) {
            val cells = rows[i]
            parser.parseRow(cells, i + 1)?.let { questions.add(it) }
        }

        return questions
    }

    private suspend fun executeImportPipeline(
        bundle: LibraryBundleDto,
        assetsDir: File?,
        format: ImportFormat,
        strategy: MergeStrategy = MergeStrategy.SKIP_EXISTING,
        targetBookId: Long? = null,
        targetQuizId: Long? = null,
        startTime: Long,
        onProgress: (Float, String) -> Unit,
        allowInsecureRemoteImages: Boolean = false,
        rootDir: File? = null,
        manifest: ManifestDto? = null,
        activeWorkspaceId: Long? = null,
    ): ImportResult {
        // 1. Validate
        onProgress(0.2f, "Validating bundle...")
        val validation =
            validator.validate(
                bundle,
                allowUnboundQuizzes = targetBookId != null || targetQuizId != null,
            )
        if (!validation.isValid) {
            return ImportResult(
                success = false,
                detectedFormat = format,
                detectedSchemaVersion = bundle.schema,
                errors = listOf(ImportError(validation.criticalError ?: "Validation failed")),
                warnings = validation.warnings,
            )
        }

        // 2. Normalize only the sanitized, importable subset.
        onProgress(0.25f, "Normalizing data...")
        val sanitizedBundle = validation.sanitizedBundle ?: bundle
        val normalizedBundle = normalizer.normalize(sanitizedBundle)

        // 3. Save to Database (Atomically)
        return database.withTransaction {
            val defaultWorkspaceId = getOrCreateDefaultWorkspaceId()
            var booksCount = 0
            var updatedBooksCount = 0
            var quizzesCount = 0
            var updatedQuizzesCount = 0
            var questionsCount = 0
            var updatedQuestionsCount = 0
            var sessionsCount = 0
            val updatedSessionsCount = 0
            var imagesCount = 0
            var skippedRecordsCount = validation.skippedRecordsCount
            val warnings = validation.warnings.toMutableList()

            val bookIdMap = mutableMapOf<String, Long>()
            val quizIdMap = mutableMapOf<String, Long>()
            val questionIdMap = mutableMapOf<String, Long>()
            val targetQuiz =
                targetQuizId?.let { quizId ->
                    quizDao.getQuizById(quizId)
                        ?: throw IllegalArgumentException("Target quiz not found: $quizId")
                }

            val affectedBookIds = mutableSetOf<Long>()
            val affectedQuizIds = mutableSetOf<Long>()

            // Total steps for progress calculation
            val totalSteps =
                normalizedBundle.categories.size +
                    normalizedBundle.books.size +
                    normalizedBundle.quizzes.size +
                    (normalizedBundle.sessions?.size ?: 0)
            var currentStep = 0

            fun updateProgress(label: String) {
                currentStep++
                val progress = 0.3f + (currentStep.toFloat() / totalSteps.coerceAtLeast(1) * 0.65f)
                onProgress(progress, label)
            }

            // Categories
            normalizedBundle.categories.forEach { catDto ->
                try {
                    categoryMetadataDao.insertMetadata(mapper.mapToCategoryMetadataEntity(catDto))
                } catch (e: Exception) {
                    warnings.add(ImportWarning("Failed to import category ${catDto.name}", e.message))
                }
                updateProgress("Importing category: ${catDto.name}")
            }

            // Books
            normalizedBundle.books.forEach { bookDto ->
                try {
                    val targetWorkspaceId =
                        activeWorkspaceId?.takeIf { it > 0 } ?: bookDto.workspaceExternalId
                            ?.let { workspaceDao.getWorkspaceByExternalId(it)?.id }
                            ?: defaultWorkspaceId
                    val existingBook = bookDao.getBookByExternalIdInWorkspace(bookDto.id, targetWorkspaceId)

                    if (existingBook != null && strategy == MergeStrategy.SKIP_EXISTING) {
                        bookIdMap[bookDto.id] = existingBook.id
                        updateProgress("Skipping existing book: ${bookDto.title}")
                        return@forEach
                    }

                    val coverImage = resolveImagePath(bookDto.coverImage, assetsDir, rootDir, manifest, allowInsecureRemoteImages)
                    coverImage.warning?.let { warning ->
                        warnings.add(ImportWarning("Book '${bookDto.title}' cover image import warning: $warning", affectedId = bookDto.id))
                    }
                    val coverPath = coverImage.path
                    if (coverImage.importedLocally) imagesCount++

                    val bookEntity = mapper.mapToBookEntity(bookDto, targetWorkspaceId, coverPath)
                    val bookId =
                        if (existingBook != null) {
                            bookDao.updateBook(
                                bookEntity.copy(
                                    id = existingBook.id,
                                    workspaceId = existingBook.workspaceId,
                                    createdAt = existingBook.createdAt,
                                    lastStudiedAt = existingBook.lastStudiedAt,
                                ),
                            )
                            updatedBooksCount++
                            existingBook.id
                        } else {
                            val newId = bookDao.insertBook(bookEntity)
                            booksCount++
                            newId
                        }
                    bookIdMap[bookDto.id] = bookId
                    replaceOwnerAssetReferences("book", bookId, listOf(coverPath))
                    affectedBookIds.add(bookId)
                } catch (e: Exception) {
                    warnings.add(ImportWarning("Failed to import book ${bookDto.title}", e.message, bookDto.id))
                    skippedRecordsCount++
                }
                updateProgress("Importing book: ${bookDto.title}")
            }

            // Quizzes & Questions
            normalizedBundle.quizzes.forEach { quizDto ->
                try {
                    // Handle target IDs
                    var localBookId = targetQuiz?.bookId ?: targetBookId ?: bookIdMap[quizDto.bookId]

                    if (targetQuiz != null && targetBookId != null && targetBookId != targetQuiz.bookId) {
                        warnings.add(
                            ImportWarning(
                                "Ignoring targetBookId=$targetBookId because targetQuizId=$targetQuizId belongs to bookId=${targetQuiz.bookId}.",
                                affectedId = quizDto.id,
                            ),
                        )
                    }

                    if (localBookId == null && targetQuizId == null) {
                        // If it's a single quiz import without targetBookId, find first book or create default
                        val firstBookId =
                            bookIdMap.values.firstOrNull()
                                ?: bookDao.getBookByExternalIdInWorkspace("imported_default", defaultWorkspaceId)?.id
                        if (firstBookId != null) {
                            localBookId = firstBookId
                            warnings.add(
                                ImportWarning(
                                    "Quiz '${quizDto.title}' refers to unknown book ID '${quizDto.bookId}'. Linked to an available book.",
                                    affectedId = quizDto.id,
                                ),
                            )
                        } else {
                            // Create a default book if none exists
                            val defaultBook =
                                BookEntity(
                                    workspaceId = defaultWorkspaceId,
                                    title = "Imported Books",
                                    externalId = "imported_default",
                                    description = "Automatically created for imported quizzes",
                                )
                            localBookId = bookDao.insertBook(defaultBook)
                            bookIdMap["imported_default"] = localBookId
                            booksCount++
                        }
                    }

                    val existingQuiz = quizDao.getQuizByExternalId(quizDto.id)
                    var quizCoverPath: String? = null

                    val quizId =
                        targetQuizId ?: if (existingQuiz != null && strategy == MergeStrategy.SKIP_EXISTING) {
                            quizIdMap[quizDto.id] = existingQuiz.id
                            updateProgress("Skipping existing quiz: ${quizDto.title}")
                            return@forEach
                        } else if (existingQuiz != null) {
                            val resolvedBookId =
                                localBookId
                                    ?: throw IllegalStateException("Could not resolve a destination book for quiz '${quizDto.title}'")
                            val coverImage = resolveImagePath(quizDto.coverImage, assetsDir, rootDir, manifest, allowInsecureRemoteImages)
                            coverImage.warning?.let { warning ->
                                warnings.add(ImportWarning("Quiz '${quizDto.title}' cover image import warning: $warning", affectedId = quizDto.id))
                            }
                            val coverPath = coverImage.path
                            quizCoverPath = coverPath
                            if (coverImage.importedLocally) imagesCount++
                            val quizEntity = mapper.mapToQuizEntity(quizDto, resolvedBookId, coverPath)
                            quizDao.updateQuiz(
                                quizEntity.copy(
                                    id = existingQuiz.id,
                                    createdAt = existingQuiz.createdAt,
                                    lastStudiedAt = existingQuiz.lastStudiedAt,
                                ),
                            )
                            updatedQuizzesCount++
                            existingQuiz.id
                        } else {
                            val resolvedBookId =
                                localBookId
                                    ?: throw IllegalStateException("Could not resolve a destination book for quiz '${quizDto.title}'")
                            val coverImage = resolveImagePath(quizDto.coverImage, assetsDir, rootDir, manifest, allowInsecureRemoteImages)
                            coverImage.warning?.let { warning ->
                                warnings.add(ImportWarning("Quiz '${quizDto.title}' cover image import warning: $warning", affectedId = quizDto.id))
                            }
                            val coverPath = coverImage.path
                            quizCoverPath = coverPath
                            if (coverImage.importedLocally) imagesCount++
                            val quizEntity = mapper.mapToQuizEntity(quizDto, resolvedBookId, coverPath)
                            val newId = quizDao.insertQuiz(quizEntity)
                            quizzesCount++
                            newId
                        }
                    quizIdMap[quizDto.id] = quizId
                    if (quizCoverPath != null) {
                        replaceOwnerAssetReferences("quiz", quizId, listOf(quizCoverPath))
                    }
                    affectedQuizIds.add(quizId)
                    affectedBookIds.add(
                        localBookId ?: targetQuiz?.bookId
                            ?: throw IllegalStateException("Could not resolve a destination book for quiz '${quizDto.title}'"),
                    )

                    quizDto.questions.forEach { qDto ->
                        try {
                            val primaryImage = resolveImagePath(qDto.imageDataUrl, assetsDir, rootDir, manifest, allowInsecureRemoteImages)
                            val secondaryImage =
                                if (primaryImage.path == null) {
                                    resolveImagePath(qDto.imageSource, assetsDir, rootDir, manifest, allowInsecureRemoteImages)
                                } else {
                                    ResolvedImageResult()
                                }
                            val chosenImage = if (primaryImage.path != null) primaryImage else secondaryImage
                            val imagePath = chosenImage.path
                            if (primaryImage.importedLocally || secondaryImage.importedLocally) {
                                imagesCount++
                            }
                            chosenImage.warning?.let { warning ->
                                warnings.add(
                                    ImportWarning(
                                        "Question image in quiz '${quizDto.title}' import warning: $warning",
                                        affectedId = qDto.id,
                                    ),
                                )
                            }

                            val qEntity = mapper.mapToQuestionEntity(qDto, quizId, imagePath)

                            val existingQuestion = questionDao.getQuestionByExternalId(quizId, qEntity.externalId)

                            val localId =
                                if (existingQuestion != null) {
                                    questionDao.updateQuestion(
                                        qEntity.copy(
                                            id = existingQuestion.id,
                                            attempts = existingQuestion.attempts,
                                            correctCount = existingQuestion.correctCount,
                                            isDropped = existingQuestion.isDropped,
                                            droppedAt = existingQuestion.droppedAt,
                                            droppedReason = existingQuestion.droppedReason,
                                            isMarked = existingQuestion.isMarked,
                                            markedAt = existingQuestion.markedAt,
                                            markReason = existingQuestion.markReason,
                                            markReviewAt = existingQuestion.markReviewAt,
                                            notes = existingQuestion.notes ?: qEntity.notes,
                                            createdAt = existingQuestion.createdAt,
                                            lastStudiedAt = existingQuestion.lastStudiedAt,
                                            timeSpentMs = existingQuestion.timeSpentMs,
                                            lastAttemptResult = existingQuestion.lastAttemptResult,
                                            consecutiveCorrect = existingQuestion.consecutiveCorrect,
                                        ),
                                    )
                                    updatedQuestionsCount++
                                    existingQuestion.id
                                } else {
                                    val newId = questionDao.insertQuestion(qEntity)
                                    questionsCount++
                                    newId
                                }

                            questionIdMap[qDto.id] = localId
                            syncQuestionCategories(localId, qEntity.categories)
                            replaceOwnerAssetReferences("question", localId, listOf(qEntity.imagePath))
                        } catch (e: Exception) {
                            warnings.add(ImportWarning("Failed to import question in ${quizDto.title}", e.message, qDto.id))
                            skippedRecordsCount++
                        }
                    }
                } catch (e: Exception) {
                    warnings.add(ImportWarning("Failed to import quiz ${quizDto.title}", e.message, quizDto.id))
                    skippedRecordsCount++
                }
                updateProgress("Importing quiz: ${quizDto.title}")
            }

            // Sessions
            normalizedBundle.sessions?.forEach { sessionDto ->
                try {
                    val localQuizId = quizIdMap[sessionDto.quizId]
                    if (localQuizId != null) {
                        sessionDao.insertSession(mapper.mapToSessionEntity(sessionDto, localQuizId, questionIdMap))
                        sessionsCount++
                    } else {
                        warnings.add(ImportWarning("Skipped session for unknown quiz ID ${sessionDto.quizId}"))
                        skippedRecordsCount++
                    }
                } catch (e: Exception) {
                    warnings.add(ImportWarning("Failed to import session ${sessionDto.id}", e.message, sessionDto.id))
                    skippedRecordsCount++
                }
                updateProgress("Importing sessions...")
            }

            onProgress(1.0f, "Import complete")

            ImportResult(
                success = true,
                detectedFormat = format,
                detectedSchemaVersion = bundle.schema,
                importedBooksCount = booksCount,
                updatedBooksCount = updatedBooksCount,
                importedQuizzesCount = quizzesCount,
                updatedQuizzesCount = updatedQuizzesCount,
                importedQuestionsCount = questionsCount,
                updatedQuestionsCount = updatedQuestionsCount,
                importedSessionsCount = sessionsCount,
                updatedSessionsCount = updatedSessionsCount,
                importedImagesCount = imagesCount,
                skippedRecordsCount = skippedRecordsCount,
                affectedBookIds = affectedBookIds.toList(),
                affectedQuizIds = affectedQuizIds.toList(),
                warnings = warnings,
                durationMillis = System.currentTimeMillis() - startTime,
                partiallyImported = skippedRecordsCount > 0,
            )
        }
    }

    private suspend fun resolveImagePath(
        assetRef: String?,
        assetsDir: File?,
        rootDir: File? = null,
        manifest: ManifestDto? = null,
        allowInsecureRemoteImages: Boolean = false,
    ): ResolvedImageResult {
        if (assetRef.isNullOrBlank()) return ResolvedImageResult()

        // If it's a data URL, save it directly via the new sanitization helper
        if (assetRef.startsWith("data:")) {
            val saved = fileManager.saveBase64AsImageDetailed(assetRef)
            return ResolvedImageResult(
                path = saved.getOrNull(),
                warning = saved.exceptionOrNull()?.message,
                importedLocally = saved.isSuccess,
            )
        }

        // Potential directories to search for assets
        val searchDirs = mutableListOf<File>()
        if (assetsDir != null) searchDirs.add(assetsDir)
        if (rootDir != null && rootDir != assetsDir) searchDirs.add(rootDir)

        if (searchDirs.isNotEmpty()) {
            // 1. Try the exact zip-relative path first.
            resolveRelativeAssetPath(searchDirs, assetRef)?.let { matchedFile ->
                matchedFile.inputStream().use { input ->
                    val saved = fileManager.saveImageDetailed(input, matchedFile.name)
                    return ResolvedImageResult(
                        path = saved.getOrNull(),
                        warning = saved.exceptionOrNull()?.message,
                        importedLocally = saved.isSuccess,
                    )
                }
            }

            // 2. Try manifest mappings for exported bundles and legacy manifests.
            if (manifest != null && manifest.assets.isNotEmpty()) {
                val manifestPath =
                    manifest.assets[assetRef]
                        ?: manifest.assets.entries.find { normalizeAssetRef(it.key) == normalizeAssetRef(assetRef) }?.value
                        ?: manifest.assets.entries.find { normalizeAssetRef(it.value) == normalizeAssetRef(assetRef) }?.key
                manifestPath?.let { path ->
                    resolveRelativeAssetPath(searchDirs, path)?.let { manifestFile ->
                        manifestFile.inputStream().use { input ->
                            val saved = fileManager.saveImageDetailed(input, manifestFile.name)
                            return ResolvedImageResult(
                                path = saved.getOrNull(),
                                warning = saved.exceptionOrNull()?.message,
                                importedLocally = saved.isSuccess,
                            )
                        }
                    }
                }
            }

            // 3. Fallback to a constrained recursive search for legacy bundles.
            val fileName = assetRef.substringAfterLast('/')
            val cleanRef = normalizeAssetRef(assetRef)

            val suffixMatches =
                searchDirs.flatMap { dir ->
                    dir.walkTopDown()
                        .filter { file -> file.isFile }
                        .filter { file ->
                            val relativePath = file.relativeTo(dir).invariantSeparatorsPath
                            relativePath == cleanRef || relativePath.endsWith("/$cleanRef")
                        }
                        .toList()
                }.distinctBy { it.canonicalPath }

            val matchedFile =
                when {
                    suffixMatches.size == 1 -> suffixMatches.first()
                    suffixMatches.isEmpty() -> {
                        val nameMatches =
                            searchDirs.flatMap { dir ->
                                dir.walkTopDown()
                                    .filter { file -> file.isFile && file.name == fileName }
                                    .toList()
                            }.distinctBy { it.canonicalPath }
                        nameMatches.singleOrNull()
                    }
                    else -> null
                }

            if (matchedFile != null) {
                matchedFile.inputStream().use { input ->
                    val saved = fileManager.saveImageDetailed(input, matchedFile.name)
                    return ResolvedImageResult(
                        path = saved.getOrNull(),
                        warning = saved.exceptionOrNull()?.message,
                        importedLocally = saved.isSuccess,
                    )
                }
            }
        }

        // If it's a remote URL, download it through the central policy gate.
        val remoteScheme = runCatching { assetRef.toUri().scheme?.lowercase() }.getOrNull()
        if (remoteScheme == "http" || remoteScheme == "https") {
            val localImage =
                fileManager.downloadAndSaveImageDetailed(
                    assetRef,
                    if (allowInsecureRemoteImages) RemoteAssetPolicy.UserAllowedPlainHttp else RemoteAssetPolicy.Default,
                )
            if (localImage is MksResult.Success) {
                return ResolvedImageResult(
                    path = localImage.data,
                    warning = null,
                    importedLocally = true
                )
            }
            if (remoteScheme == "http" && !allowInsecureRemoteImages) {
                return ResolvedImageResult(
                    warning = localImage.exceptionOrNull()?.message
                        ?: "Plain HTTP image URL was skipped until the user allows insecure image downloads.",
                    importedLocally = false,
                )
            }
            // HTTPS failures and user-approved HTTP failures are preserved as remote references.
            return ResolvedImageResult(
                path = assetRef,
                warning =
                    localImage.exceptionOrNull()?.message?.let { "Kept remote URL instead of importing locally: $it" }
                        ?: "Kept remote URL instead of importing locally.",
                importedLocally = false,
            )
        }

        // Final attempt as base64 or other string-based image if not already tried
        // SECURITY: FileManager.saveImage handles absolute path validation
        val saved = fileManager.saveImageDetailed(assetRef)
        if (saved is MksResult.Success) {
            return ResolvedImageResult(path = saved.data, importedLocally = true)
        }

        return if (assetRef.contains(",") || assetRef.length < 255) {
            ResolvedImageResult(
                warning = saved.exceptionOrNull()?.message ?: "Unsupported inline image data."
            )
        } else {
            ResolvedImageResult(
                path = assetRef,
                warning = saved.exceptionOrNull()?.message,
                importedLocally = false
            )
        }
    }

    private fun resolveRelativeAssetPath(
        searchDirs: List<File>,
        assetRef: String,
    ): File? {
        val normalizedRef = normalizeAssetRef(assetRef)
        if (normalizedRef.isBlank() || normalizedRef.split('/').any { it == ".." }) return null

        return searchDirs
            .asSequence()
            .mapNotNull { dir ->
                val baseDir = runCatching { dir.canonicalFile }.getOrNull() ?: return@mapNotNull null
                val candidate = runCatching { File(baseDir, normalizedRef).canonicalFile }.getOrNull() ?: return@mapNotNull null
                val isInsideBase = candidate == baseDir || candidate.toPath().startsWith(baseDir.toPath())
                if (isInsideBase && candidate.exists() && candidate.isFile) candidate else null
            }
            .firstOrNull()
    }

    private fun normalizeAssetRef(assetRef: String): String {
        return assetRef
            .replace('\\', '/')
            .removePrefix("./")
            .removePrefix("/")
    }

    private fun bundleContainsPlainHttpAssets(bundle: LibraryBundleDto): Boolean {
        fun isPlainHttp(value: String?): Boolean = value?.startsWith("http://", ignoreCase = true) == true
        if (bundle.books.any { isPlainHttp(it.coverImage) }) return true
        return bundle.quizzes.any { quiz ->
            isPlainHttp(quiz.coverImage) ||
                quiz.questions.any { question ->
                    isPlainHttp(question.imageDataUrl) || isPlainHttp(question.imageSource) || isPlainHttp(question.imageName)
                }
        }
    }

    private fun readImportText(
        uri: Uri,
        format: ImportFormat,
    ): String {
        val limit =
            when (format) {
                ImportFormat.HTML -> ImportLimits.MAX_HTML_IMPORT_BYTES
                ImportFormat.CSV_TSV -> ImportLimits.MAX_CSV_IMPORT_BYTES
                else -> ImportLimits.MAX_TEXT_IMPORT_BYTES
            }
        return context.contentResolver.openInputStream(uri)?.use { input -> input.readTextWithLimit(limit) }
            ?: throw Exception("Could not read file content")
    }

    companion object {
        fun generateDeterministicId(
            stem: String,
            options: List<String>,
        ): String {
            val input = stem + options.sorted().joinToString("|")
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(input.toByteArray())
                hash.joinToString("") { "%02x".format(it) }.take(32)
            } catch (_: Exception) {
                UUID.nameUUIDFromBytes(input.toByteArray()).toString()
            }
        }
    }
}
