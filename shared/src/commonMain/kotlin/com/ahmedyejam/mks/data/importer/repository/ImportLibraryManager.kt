package com.ahmedyejam.mks.data.importer.repository

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
import com.ahmedyejam.mks.data.importer.parser.BundleFileParser
import com.ahmedyejam.mks.data.importer.parser.CsvParser
import com.ahmedyejam.mks.data.importer.parser.GenericImageExtractor
import com.ahmedyejam.mks.data.importer.parser.HtmlQuestionParser
import com.ahmedyejam.mks.data.importer.parser.JsonQuestionParser
import com.ahmedyejam.mks.data.importer.parser.ParsedBundleResult
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetHeaderMapper
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetQuestionParser
import com.ahmedyejam.mks.data.importer.parser.TextQuestionParser
import com.ahmedyejam.mks.data.importer.security.ImportLimits
import com.ahmedyejam.mks.data.importer.validation.ImportValidator
import com.ahmedyejam.mks.data.local.WorkspaceDefaults
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.local.entity.WorkspaceEntity
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.db.MksDatabase
import com.ahmedyejam.mks.platform.FileManager
import com.ahmedyejam.mks.util.currentTimeMillis
import com.ahmedyejam.mks.util.readTextWithLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class ImportLibraryManager(
    private val db: MksDatabase,
    private val fileManager: FileManager,
    private val bundleFileParser: BundleFileParser,
) {

    private val formatDetector = ImportFormatDetector()
    private val csvParser = CsvParser()
    private val headerMapper = SpreadsheetHeaderMapper()
    private val imageExtractor = GenericImageExtractor()
    private val textParser = TextQuestionParser(imageExtractor)
    private val jsonQuestionParser = JsonQuestionParser(imageExtractor)
    private val htmlParser = HtmlQuestionParser(jsonQuestionParser)

    private val validator = ImportValidator()
    private val normalizer = BundleNormalizer()
    private val mapper = LibraryMapper()

    fun detectFormat(filePath: String): ImportFormat {
        return formatDetector.detectFormat(filePath)
    }

    suspend fun getImportPreview(filePath: String): ImportPreviewDto =
        withContext(Dispatchers.IO) {
            val format = formatDetector.detectFormat(filePath)
            if (format == ImportFormat.UNKNOWN) throw Exception("Unsupported file format")

            var parsedResult: ParsedBundleResult? = null
            try {
                val bundle = parseFileToBundle(filePath, format)
                parsedResult = bundle.parsedResult

                val validation = validator.validate(bundle.bundle)
                val sanitizedBundle = validation.sanitizedBundle ?: bundle.bundle
                val normalized = normalizer.normalize(sanitizedBundle)

                val booksToCreate = mutableListOf<String>()
                val booksToUpdate = mutableListOf<String>()
                normalized.books.forEach { book ->
                    if (getBookByExternalId(book.id) != null) {
                        booksToUpdate.add(book.title)
                    } else {
                        booksToCreate.add(book.title)
                    }
                }

                val quizzesToCreate = mutableListOf<String>()
                val quizzesToUpdate = mutableListOf<String>()
                normalized.quizzes.forEach { quiz ->
                    if (getQuizByExternalId(quiz.id) != null) {
                        quizzesToUpdate.add(quiz.title)
                    } else {
                        quizzesToCreate.add(quiz.title)
                    }
                }

                val questionsToCreate = mutableListOf<String>()
                val questionsToUpdate = mutableListOf<String>()
                normalized.quizzes.forEach { quizDto ->
                    val existingQuiz = getQuizByExternalId(quizDto.id)
                    if (existingQuiz == null) {
                        questionsToCreate.addAll(quizDto.questions.map { it.stem.take(50) })
                    } else {
                        quizDto.questions.forEach { questionDto ->
                            if (getQuestionByExternalId(existingQuiz.id, questionDto.id) != null) {
                                questionsToUpdate.add(questionDto.stem.take(50))
                            } else {
                                questionsToCreate.add(questionDto.stem.take(50))
                            }
                        }
                    }
                }

                val previewWarnings = validation.warnings.toMutableList()
                if (bundleContainsPlainHttpAssets(normalized)) {
                    previewWarnings.add(
                        ImportWarning(
                            "This import contains plain HTTP image URLs. They will not be downloaded unless you explicitly allow insecure image downloads.",
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
                    skippedRecordsCount = validation.skippedRecordsCount,
                    hasAssets = false,
                    warnings = previewWarnings,
                )
            } finally {
                parsedResult?.zipExtractDir?.let { dir ->
                    runCatching { File(dir).deleteRecursively() }
                }
            }
        }

    suspend fun importLibrary(
        filePath: String,
        strategy: MergeStrategy = MergeStrategy.SKIP_EXISTING,
        targetBookId: Long? = null,
        targetQuizId: Long? = null,
        allowInsecureRemoteImages: Boolean = false,
        activeWorkspaceId: Long? = null,
        onProgress: (Float, String) -> Unit = { _, _ -> },
    ): MksResult<ImportResult> =
        withContext(Dispatchers.IO) {
            val startTime = currentTimeMillis()
            onProgress(0.05f, "Detecting format...")
            val format = formatDetector.detectFormat(filePath)

            if (format == ImportFormat.UNKNOWN) {
                return@withContext MksResult.Error(message = "Unsupported file format")
            }

            var parsedResult: ParsedBundleResult? = null
            try {
                val bundle = parseFileToBundle(filePath, format)
                parsedResult = bundle.parsedResult

                val result = executeImportPipeline(
                    bundle = bundle.bundle,
                    format = format,
                    strategy = strategy,
                    targetBookId = targetBookId,
                    targetQuizId = targetQuizId,
                    startTime = startTime,
                    onProgress = onProgress,
                    allowInsecureRemoteImages = allowInsecureRemoteImages,
                    activeWorkspaceId = activeWorkspaceId,
                    assetsDir = parsedResult?.zipExtractDir,
                    manifest = parsedResult?.manifest,
                )
                MksResult.Success(result)
            } catch (e: Exception) {
                MksResult.Error(
                    message = "Import failed: ${e.message}",
                    exception = e,
                )
            } finally {
                parsedResult?.zipExtractDir?.let { dir ->
                    runCatching { File(dir).deleteRecursively() }
                }
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
            val startTime = currentTimeMillis()
            val bundle = wrapQuestionsToBundle(
                questions = questions,
                quizTitle = title,
                bookTitle = newBookTitle,
                includeBook = targetBookId == null && targetQuizId == null,
            )

            try {
                val result = executeImportPipeline(
                    bundle = bundle,
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
                    exception = e,
                )
            }
        }

    // ── Internal: File parsing ──────────────────────────────────────

    private data class FileParseOutput(
        val bundle: LibraryBundleDto,
        val parsedResult: ParsedBundleResult?,
    )

    private fun parseFileToBundle(filePath: String, format: ImportFormat): FileParseOutput {
        // Try platform-specific parser first (XLSX, ZIP, JSON library bundles)
        val platformResult = bundleFileParser.parseFile(filePath, format)
        if (platformResult != null) {
            return FileParseOutput(bundle = platformResult.bundle, parsedResult = platformResult)
        }

        // CommonMain handles text-based formats
        val file = File(filePath)
        val fileName = file.name

        return when (format) {
            ImportFormat.CSV_TSV -> {
                val content = file.readTextWithLimit(ImportLimits.MAX_CSV_IMPORT_BYTES)
                val questions = parseSpreadsheetSimple(content)
                FileParseOutput(
                    bundle = wrapQuestionsToBundle(questions, fileName),
                    parsedResult = null,
                )
            }
            ImportFormat.TEXT -> {
                val content = file.readTextWithLimit(ImportLimits.MAX_TEXT_IMPORT_BYTES)
                val questions = textParser.parse(content)
                FileParseOutput(
                    bundle = wrapQuestionsToBundle(questions, fileName),
                    parsedResult = null,
                )
            }
            ImportFormat.HTML -> {
                val content = file.readTextWithLimit(ImportLimits.MAX_HTML_IMPORT_BYTES)
                val questions = htmlParser.parse(content)
                FileParseOutput(
                    bundle = wrapQuestionsToBundle(questions, fileName),
                    parsedResult = null,
                )
            }
            else -> throw Exception("Format $format not handled in commonMain")
        }
    }

    private fun parseSpreadsheetSimple(content: String): List<ParsedQuestion> {
        if (content.isBlank()) return emptyList()
        val rows = csvParser.parse(content)
        if (rows.isEmpty()) return emptyList()

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

        val parser = SpreadsheetQuestionParser(
            mapping = mapping,
            optionCols = optionCols,
            sheetAddressImages = emptyMap(),
            sheetRowImages = emptyMap(),
            imageExtractor = imageExtractor,
        )

        val questions = mutableListOf<ParsedQuestion>()
        for (i in (bestRowIdx + 1) until rows.size) {
            parser.parseRow(rows[i], i + 1)?.let { questions.add(it) }
        }
        return questions
    }

    // ── Internal: Bundle wrapping ───────────────────────────────────

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

        val questionDtos = questions.map { pq ->
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
            quizzes = listOf(
                QuizDto(
                    id = quizId,
                    bookId = bookId,
                    title = normalizedQuizTitle,
                    questions = questionDtos,
                ),
            ),
        )
    }

    // ── Internal: Import pipeline ───────────────────────────────────

    private suspend fun executeImportPipeline(
        bundle: LibraryBundleDto,
        format: ImportFormat,
        strategy: MergeStrategy = MergeStrategy.SKIP_EXISTING,
        targetBookId: Long? = null,
        targetQuizId: Long? = null,
        startTime: Long,
        onProgress: (Float, String) -> Unit,
        allowInsecureRemoteImages: Boolean = false,
        activeWorkspaceId: Long? = null,
        assetsDir: String? = null,
        manifest: ManifestDto? = null,
    ): ImportResult = withContext(Dispatchers.IO) {
        // 1. Validate
        onProgress(0.2f, "Validating bundle...")
        val validation = validator.validate(
            bundle,
            allowUnboundQuizzes = targetBookId != null || targetQuizId != null,
        )
        if (!validation.isValid) {
            return@withContext ImportResult(
                success = false,
                detectedFormat = format,
                detectedSchemaVersion = bundle.schema,
                errors = listOf(ImportError(validation.criticalError ?: "Validation failed")),
                warnings = validation.warnings,
            )
        }

        // 2. Normalize
        onProgress(0.25f, "Normalizing data...")
        val sanitizedBundle = validation.sanitizedBundle ?: bundle
        val normalizedBundle = normalizer.normalize(sanitizedBundle)

        // 3. Persist to database
        val defaultWorkspaceId = getOrCreateDefaultWorkspaceId()
        var booksCount = 0
        var updatedBooksCount = 0
        var quizzesCount = 0
        var updatedQuizzesCount = 0
        var questionsCount = 0
        var updatedQuestionsCount = 0
        var sessionsCount = 0
        var imagesCount = 0
        var skippedRecordsCount = validation.skippedRecordsCount
        val warnings = validation.warnings.toMutableList()

        val bookIdMap = mutableMapOf<String, Long>()
        val quizIdMap = mutableMapOf<String, Long>()
        val questionIdMap = mutableMapOf<String, Long>()

        val affectedBookIds = mutableSetOf<Long>()
        val affectedQuizIds = mutableSetOf<Long>()

        db.bookQueriesQueries.transaction {
            // Categories
            normalizedBundle.categories.forEach { catDto ->
                try {
                    val entity = mapper.mapToCategoryMetadataEntity(catDto)
                    db.mistakeQueriesQueries.cm_upsert(
                        name = entity.name,
                        emoji = entity.emoji,
                        color = entity.color?.toLong(),
                        isPinned = if (entity.isPinned) 1L else 0L,
                    )
                } catch (e: Exception) {
                    warnings.add(ImportWarning("Failed to import category ${catDto.name}", e.message))
                }
            }
            onProgress(0.3f, "Importing categories...")

            // Books
            normalizedBundle.books.forEach { bookDto ->
                try {
                    val targetWorkspaceId = activeWorkspaceId?.takeIf { it > 0 }
                        ?: bookDto.workspaceExternalId?.let { getWorkspaceByExternalId(it)?.id }
                        ?: defaultWorkspaceId

                    val existingBook = getBookByExternalIdInWorkspace(bookDto.id, targetWorkspaceId)

                    if (existingBook != null && strategy == MergeStrategy.SKIP_EXISTING) {
                        bookIdMap[bookDto.id] = existingBook.id
                        return@forEach
                    }

                    val coverImage = resolveImagePath(bookDto.coverImage, assetsDir, manifest, allowInsecureRemoteImages)
                    coverImage.warning?.let { warning ->
                        warnings.add(ImportWarning("Book '${bookDto.title}' cover image: $warning", affectedId = bookDto.id))
                    }
                    val coverPath = coverImage.path
                    if (coverImage.importedLocally) imagesCount++

                    val bookEntity = mapper.mapToBookEntity(bookDto, targetWorkspaceId, coverPath)
                    val bookId = if (existingBook != null) {
                        val now = currentTimeMillis()
                        db.bookQueriesQueries.bk_update(
                            title = bookEntity.title,
                            description = bookEntity.description,
                            iconName = bookEntity.iconName,
                            coverImage = bookEntity.coverImage,
                            updatedAt = now,
                            contentUpdatedAt = now,
                            lastEditedAt = now,
                            isPinned = if (bookEntity.isPinned) 1L else 0L,
                            fields = "[]",
                            id = existingBook.id,
                        )
                        updatedBooksCount++
                        existingBook.id
                    } else {
                        val now = currentTimeMillis()
                        db.bookQueriesQueries.bk_insert(
                            workspaceId = bookEntity.workspaceId,
                            externalId = bookEntity.externalId,
                            title = bookEntity.title,
                            description = bookEntity.description,
                            iconName = bookEntity.iconName,
                            coverImage = bookEntity.coverImage,
                            createdAt = now,
                            updatedAt = now,
                            contentUpdatedAt = now,
                            lastStudiedAt = 0L,
                            lastEditedAt = now,
                            isPinned = if (bookEntity.isPinned) 1L else 0L,
                            isSystem = if (bookEntity.isSystem) 1L else 0L,
                            fields = "[]",
                        )
                        val inserted = db.bookQueriesQueries.bk_selectByExternalId(bookEntity.externalId).executeAsOne()
                        booksCount++
                        inserted.id
                    }
                    bookIdMap[bookDto.id] = bookId
                    replaceOwnerAssetReferences("book", bookId, listOf(coverPath))
                    affectedBookIds.add(bookId)
                } catch (e: Exception) {
                    warnings.add(ImportWarning("Failed to import book ${bookDto.title}", e.message, bookDto.id))
                    skippedRecordsCount++
                }
            }
            onProgress(0.4f, "Importing books...")

            // Quizzes & Questions
            normalizedBundle.quizzes.forEach { quizDto ->
                try {
                    var localBookId = targetQuizId?.let { getQuizById(it)?.bookId }
                        ?: targetBookId
                        ?: bookIdMap[quizDto.bookId]

                    if (localBookId == null && targetQuizId == null) {
                        val firstBookId = bookIdMap.values.firstOrNull()
                            ?: getBookByExternalIdInWorkspace("imported_default", defaultWorkspaceId)?.id
                        if (firstBookId != null) {
                            localBookId = firstBookId
                            warnings.add(ImportWarning(
                                "Quiz '${quizDto.title}' refers to unknown book ID '${quizDto.bookId}'. Linked to available book.",
                                affectedId = quizDto.id,
                            ))
                        } else {
                            val now = currentTimeMillis()
                            db.bookQueriesQueries.bk_insert(
                                workspaceId = defaultWorkspaceId,
                                externalId = "imported_default",
                                title = "Imported Books",
                                description = "Automatically created for imported quizzes",
                                iconName = "📚",
                                coverImage = null,
                                createdAt = now, updatedAt = now,
                                contentUpdatedAt = now, lastStudiedAt = 0L,
                                lastEditedAt = now, isPinned = 0L, isSystem = 0L, fields = "[]",
                            )
                            localBookId = db.bookQueriesQueries.bk_selectByExternalId("imported_default").executeAsOne().id
                            bookIdMap["imported_default"] = localBookId
                            booksCount++
                        }
                    }

                    val existingQuiz = getQuizByExternalId(quizDto.id)
                    var quizCoverPath: String? = null
                    val resolvedBookId = localBookId ?: throw IllegalStateException(
                        "Could not resolve destination book for quiz '${quizDto.title}'"
                    )

                    val quizId = targetQuizId ?: if (existingQuiz != null && strategy == MergeStrategy.SKIP_EXISTING) {
                        quizIdMap[quizDto.id] = existingQuiz.id
                        return@forEach
                    } else {
                        val coverImage = resolveImagePath(quizDto.coverImage, assetsDir, manifest, allowInsecureRemoteImages)
                        coverImage.warning?.let { warning ->
                            warnings.add(ImportWarning("Quiz '${quizDto.title}' cover image: $warning", affectedId = quizDto.id))
                        }
                        quizCoverPath = coverImage.path
                        if (coverImage.importedLocally) imagesCount++

                        val quizEntity = mapper.mapToQuizEntity(quizDto, resolvedBookId, quizCoverPath)

                        if (existingQuiz != null) {
                            val now = currentTimeMillis()
                            db.quizQueriesQueries.qz_update(
                                title = quizEntity.title,
                                description = quizEntity.description,
                                category = quizEntity.category,
                                tags = "[]",
                                iconName = quizEntity.iconName,
                                coverImage = quizEntity.coverImage,
                                updatedAt = now,
                                contentUpdatedAt = now,
                                lastEditedAt = now,
                                isPinned = if (quizEntity.isPinned) 1L else 0L,
                                id = existingQuiz.id,
                            )
                            updatedQuizzesCount++
                            existingQuiz.id
                        } else {
                            val now = currentTimeMillis()
                            val extId = quizEntity.externalId.ifBlank { "quiz_$now" }
                            db.quizQueriesQueries.qz_insert(
                                externalId = extId,
                                bookId = quizEntity.bookId,
                                title = quizEntity.title,
                                description = quizEntity.description,
                                category = quizEntity.category,
                                tags = "[]",
                                iconName = quizEntity.iconName,
                                coverImage = quizEntity.coverImage,
                                createdAt = now, updatedAt = now,
                                contentUpdatedAt = now, lastStudiedAt = 0L,
                                lastEditedAt = now, isPinned = 0L, isSystem = 0L,
                            )
                            val inserted = db.quizQueriesQueries.qz_selectByExternalId(extId).executeAsOne()
                            quizzesCount++
                            inserted.id
                        }
                    }

                    quizIdMap[quizDto.id] = quizId
                    if (quizCoverPath != null) {
                        replaceOwnerAssetReferences("quiz", quizId, listOf(quizCoverPath))
                    }
                    affectedQuizIds.add(quizId)
                    affectedBookIds.add(resolvedBookId)

                    // Questions within this quiz
                    quizDto.questions.forEach { qDto ->
                        try {
                            val primaryImage = resolveImagePath(qDto.imageDataUrl, assetsDir, manifest, allowInsecureRemoteImages)
                            val secondaryImage = if (primaryImage.path == null) {
                                resolveImagePath(qDto.imageSource, assetsDir, manifest, allowInsecureRemoteImages)
                            } else {
                                ResolvedImageResult()
                            }
                            val chosenImage = if (primaryImage.path != null) primaryImage else secondaryImage
                            val imagePath = chosenImage.path
                            if (primaryImage.importedLocally || secondaryImage.importedLocally) imagesCount++
                            chosenImage.warning?.let { warning ->
                                warnings.add(ImportWarning(
                                    "Question image in '${quizDto.title}': $warning",
                                    affectedId = qDto.id,
                                ))
                            }

                            val qEntity = mapper.mapToQuestionEntity(qDto, quizId, imagePath)
                            val existingQuestion = getQuestionByExternalId(quizId, qEntity.externalId)

                            val localId = if (existingQuestion != null) {
                                val now = currentTimeMillis()
                                db.questionQueriesQueries.qu_update(
                                    text = qEntity.text,
                                    type = qEntity.type.name,
                                    options = "[]",
                                    correctAnswers = "[]",
                                    explanation = qEntity.explanation,
                                    hint = qEntity.hint,
                                    reference = qEntity.reference,
                                    weight = qEntity.weight.toLong(),
                                    imagePath = qEntity.imagePath,
                                    imageName = qEntity.imageName,
                                    imageSource = qEntity.imageSource,
                                    notes = qEntity.notes,
                                    categories = "[]",
                                    tags = "[]",
                                    updatedAt = now,
                                    lastEditedAt = now,
                                    id = existingQuestion.id,
                                )
                                updatedQuestionsCount++
                                existingQuestion.id
                            } else {
                                val now = currentTimeMillis()
                                val qExtId = qEntity.externalId.ifBlank { "q_$now" }
                                db.questionQueriesQueries.qu_insert(
                                    externalId = qExtId,
                                    quizId = qEntity.quizId,
                                    text = qEntity.text,
                                    type = qEntity.type.name,
                                    options = "[]",
                                    correctAnswers = "[]",
                                    explanation = qEntity.explanation,
                                    hint = qEntity.hint,
                                    reference = qEntity.reference,
                                    weight = qEntity.weight.toLong(),
                                    imagePath = qEntity.imagePath,
                                    imageName = qEntity.imageName,
                                    imageSource = qEntity.imageSource,
                                    attempts = 0L, correctCount = 0L,
                                    isDropped = 0L, droppedAt = null, droppedReason = null,
                                    isMarked = 0L, markedAt = null, markReason = null, markReviewAt = null,
                                    notes = qEntity.notes, categories = "[]", tags = "[]",
                                    difficulty = null, dueAt = 0L, reviewCount = 0L, lastReviewedAt = 0L,
                                    additionalInfo = qEntity.additionalInfo,
                                    sourceBookId = null, sourceQuizId = null, sourceQuestionId = null,
                                    createdAt = now, updatedAt = now, lastStudiedAt = 0L, lastEditedAt = now,
                                    timeSpentMs = 0L, lastAttemptResult = null, consecutiveCorrect = 0L,
                                )
                                val inserted = db.questionQueriesQueries.qu_selectByExternalId(qExtId).executeAsOne()
                                questionsCount++
                                inserted.id
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
            }
            onProgress(0.7f, "Importing quizzes...")

            // Sessions
            normalizedBundle.sessions?.forEach { sessionDto ->
                try {
                    val localQuizId = quizIdMap[sessionDto.quizId]
                    if (localQuizId != null) {
                        val sessionEntity = mapper.mapToSessionEntity(sessionDto, localQuizId, questionIdMap)
                        val now = currentTimeMillis()
                        db.sessionQueriesQueries.se_insert(
                            quizId = sessionEntity.quizId,
                            label = sessionEntity.label,
                            currentQuestionIndex = sessionEntity.currentQuestionIndex.toLong(),
                            score = sessionEntity.score.toLong(),
                            incorrectCount = sessionEntity.incorrectCount.toLong(),
                            answers = "{}",
                            answersByIndex = "{}",
                            isCompleted = if (sessionEntity.isCompleted) 1L else 0L,
                            createdAt = now, updatedAt = now, lastModifiedAt = now,
                            lastStudiedAt = 0L, lastEditedAt = now,
                            questionIds = "[]", originalQuestionCount = 0L,
                            shuffleQuestions = if (sessionEntity.shuffleQuestions) 1L else 0L,
                            shuffleOptions = if (sessionEntity.shuffleOptions) 1L else 0L,
                            rapidMode = if (sessionEntity.rapidMode) 1L else 0L,
                            repeatWrong = if (sessionEntity.repeatWrong) 1L else 0L,
                            quizTimerSeconds = sessionEntity.quizTimerSeconds.toLong(),
                            questionTimerSeconds = sessionEntity.questionTimerSeconds.toLong(),
                            rangeFrom = sessionEntity.rangeFrom.toLong(),
                            rangeTo = sessionEntity.rangeTo.toLong(),
                            includeFilters = "[]",
                            droppedOptions = "{}",
                            droppedOptionsByIndex = "{}",
                            visibleOptionsCount = "{}",
                            visibleOptionsCountByIndex = "{}",
                            currentStreak = 0L, maxStreak = 0L, resultTaxonomy = "{}",
                        )
                        sessionsCount++
                    } else {
                        warnings.add(ImportWarning("Skipped session for unknown quiz ID ${sessionDto.quizId}"))
                        skippedRecordsCount++
                    }
                } catch (e: Exception) {
                    warnings.add(ImportWarning("Failed to import session ${sessionDto.id}", e.message, sessionDto.id))
                    skippedRecordsCount++
                }
            }
            onProgress(0.9f, "Importing sessions...")
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
            importedImagesCount = imagesCount,
            skippedRecordsCount = skippedRecordsCount,
            affectedBookIds = affectedBookIds.toList(),
            affectedQuizIds = affectedQuizIds.toList(),
            warnings = warnings,
            durationMillis = currentTimeMillis() - startTime,
            partiallyImported = skippedRecordsCount > 0,
        )
    }

    // ── Internal: Database lookups ──────────────────────────────────

    private fun getBookByExternalId(externalId: String): BookEntity? {
        return db.bookQueriesQueries.bk_selectByExternalId(externalId)
            .executeAsOneOrNull()?.toBookEntity()
    }

    private fun getBookByExternalIdInWorkspace(externalId: String, workspaceId: Long): BookEntity? {
        return db.bookQueriesQueries.bk_selectByExternalIdInWorkspace(externalId, workspaceId)
            .executeAsOneOrNull()?.toBookEntity()
    }

    private fun getQuizByExternalId(externalId: String): QuizEntity? {
        return db.quizQueriesQueries.qz_selectByExternalId(externalId)
            .executeAsOneOrNull()?.toQuizEntity()
    }

    private fun getQuizById(id: Long): QuizEntity? {
        return db.quizQueriesQueries.qz_selectById(id)
            .executeAsOneOrNull()?.toQuizEntity()
    }

    private fun getQuestionByExternalId(quizId: Long, externalId: String): QuestionEntity? {
        return db.questionQueriesQueries.qu_selectByExternalIdInQuiz(quizId, externalId)
            .executeAsOneOrNull()?.toQuestionEntity()
    }

    private fun getWorkspaceByExternalId(externalId: String): WorkspaceEntity? {
        return db.workspaceQueriesQueries.ws_selectByExternalId(externalId)
            .executeAsOneOrNull()?.toWorkspaceEntity()
    }

    private suspend fun getOrCreateDefaultWorkspaceId(): Long = withContext(Dispatchers.IO) {
        getWorkspaceByExternalId(WorkspaceDefaults.DEFAULT_EXTERNAL_ID)?.let { return@withContext it.id }

        db.workspaceQueriesQueries.ws_selectDefault().executeAsOneOrNull()?.let { ws ->
            // Fix the externalId if the default workspace doesn't have it yet
            val currentEntity = ws.toWorkspaceEntity()
            if (currentEntity.externalId != WorkspaceDefaults.DEFAULT_EXTERNAL_ID) {
                db.workspaceQueriesQueries.ws_updateExternalId(
                    externalId = WorkspaceDefaults.DEFAULT_EXTERNAL_ID,
                    updatedAt = currentTimeMillis(),
                    id = ws.id,
                )
            }
            db.workspaceQueriesQueries.ws_update(
                name = WorkspaceDefaults.DEFAULT_NAME,
                description = WorkspaceDefaults.DEFAULT_DESCRIPTION,
                updatedAt = currentTimeMillis(),
                id = ws.id,
            )
            // Check settings
            db.workspaceQueriesQueries.ws_settingsByWorkspaceId(ws.id).executeAsOneOrNull()
                ?: db.workspaceQueriesQueries.ws_insertSettings(
                    workspaceId = ws.id, language = null, theme = null,
                    defaultSort = null, quizDefaultsJson = null, importDefaultsJson = null,
                    createdAt = currentTimeMillis(), updatedAt = currentTimeMillis(),
                )
            return@withContext ws.id
        }

        // Create new default workspace
        val now = currentTimeMillis()
        db.workspaceQueriesQueries.ws_insert(
            externalId = WorkspaceDefaults.DEFAULT_EXTERNAL_ID,
            name = WorkspaceDefaults.DEFAULT_NAME,
            description = WorkspaceDefaults.DEFAULT_DESCRIPTION,
            isDefault = 1L,
            createdAt = now,
            updatedAt = now,
        )
        val ws = db.workspaceQueriesQueries.ws_selectByExternalId(WorkspaceDefaults.DEFAULT_EXTERNAL_ID)
            .executeAsOne()
        db.workspaceQueriesQueries.ws_insertSettings(
            workspaceId = ws.id, language = null, theme = null,
            defaultSort = null, quizDefaultsJson = null, importDefaultsJson = null,
            createdAt = now, updatedAt = now,
        )
        ws.id
    }

    // ── Internal: Asset reference management ────────────────────────

    private fun replaceOwnerAssetReferences(ownerType: String, ownerId: Long, paths: List<String?>) {
        val cleaned = paths.mapNotNull { it?.trim()?.takeIf { v -> isTrackableLocalAsset(v) } }.distinct()

        // Delete existing references for this owner
        db.assetQueriesQueries.as_deleteRefsByOwner(ownerType, ownerId)

        // Insert new references
        val now = currentTimeMillis()
        cleaned.forEach { path ->
            db.assetQueriesQueries.as_insertRef(path, ownerType, ownerId, now)
        }

        // Clean up orphaned images
        cleaned.forEach { path ->
            // Check if this path is still referenced by anyone
            val count = db.assetQueriesQueries.as_countRefsForPath(path).executeAsOne().toInt()
            if (count == 0) {
                fileManager.deleteFile(path)
            }
        }
    }

    private fun syncQuestionCategories(questionId: Long, categories: List<String>) {
        db.mistakeQueriesQueries.qc_deleteByQuestion(questionId)
        categories.forEach { category ->
            db.mistakeQueriesQueries.qc_insert(questionId, category)
        }
    }

    private fun isTrackableLocalAsset(path: String?): Boolean {
        val value = path?.trim().orEmpty()
        if (value.isBlank()) return false
        return !value.startsWith("http://", ignoreCase = true) &&
            !value.startsWith("https://", ignoreCase = true) &&
            !value.startsWith("data:", ignoreCase = true) &&
            !value.startsWith("assets/", ignoreCase = true)
    }

    // ── Internal: Image resolution ──────────────────────────────────

    private data class ResolvedImageResult(
        val path: String? = null,
        val warning: String? = null,
        val importedLocally: Boolean = false,
    )

    private fun resolveImagePath(
        assetRef: String?,
        assetsDir: String?,
        manifest: ManifestDto?,
        allowInsecureRemoteImages: Boolean = false,
    ): ResolvedImageResult {
        if (assetRef.isNullOrBlank()) return ResolvedImageResult()

        // Data URL → save directly
        if (assetRef.startsWith("data:")) {
            val saved = fileManager.saveBase64AsImageDetailed(assetRef)
            return ResolvedImageResult(
                path = saved.getOrNull(),
                warning = saved.exceptionOrNull()?.message,
                importedLocally = saved.isSuccess,
            )
        }

        // Search ZIP extraction dir for relative assets
        if (assetsDir != null) {
            val searchDir = File(assetsDir)
            if (searchDir.exists()) {
                val normalizedRef = normalizeAssetRef(assetRef)
                if (normalizedRef.isNotBlank() && !normalizedRef.split('/').any { it == ".." }) {
                    val candidate = File(searchDir, normalizedRef)
                    if (candidate.exists() && candidate.isFile) {
                        val saved = fileManager.saveImageDetailed(candidate.inputStream())
                        return ResolvedImageResult(
                            path = saved.getOrNull(),
                            warning = saved.exceptionOrNull()?.message,
                            importedLocally = saved.isSuccess,
                        )
                    }
                }

                // Manifest-based lookup
                if (manifest != null && manifest.assets.isNotEmpty()) {
                    val manifestPath = manifest.assets[assetRef]
                        ?: manifest.assets.entries.find {
                            normalizeAssetRef(it.key) == normalizedRef
                        }?.value
                    if (manifestPath != null) {
                        val manifestFile = File(searchDir, normalizeAssetRef(manifestPath))
                        if (manifestFile.exists() && manifestFile.isFile) {
                            val saved = fileManager.saveImageDetailed(manifestFile.inputStream())
                            return ResolvedImageResult(
                                path = saved.getOrNull(),
                                warning = saved.exceptionOrNull()?.message,
                                importedLocally = saved.isSuccess,
                            )
                        }
                    }
                }

                // Fallback: search by filename
                val fileName = assetRef.substringAfterLast('/')
                val found = searchDir.walkTopDown()
                    .filter { it.isFile && it.name == fileName }
                    .firstOrNull()
                if (found != null) {
                    val saved = fileManager.saveImageDetailed(found.inputStream())
                    return ResolvedImageResult(
                        path = saved.getOrNull(),
                        warning = saved.exceptionOrNull()?.message,
                        importedLocally = saved.isSuccess,
                    )
                }
            }
        }

        // Remote URL
        val remoteScheme = runCatching { assetRef.substringBefore("://").lowercase() }.getOrNull()
        if (remoteScheme == "http" || remoteScheme == "https") {
            if (remoteScheme == "http" && !allowInsecureRemoteImages) {
                return ResolvedImageResult(
                    warning = "Plain HTTP image URL skipped. Enable insecure downloads to import it.",
                    importedLocally = false,
                )
            }
            // Suspend not possible in transaction; delegate to fileManager (non-suspend fallback)
            val localPath = runCatching { fileManager.downloadAndSaveImage(assetRef) }.getOrNull()
            if (localPath != null) {
                return ResolvedImageResult(path = localPath, importedLocally = true)
            }
            return ResolvedImageResult(
                path = assetRef,
                warning = "Kept remote URL — download failed.",
                importedLocally = false,
            )
        }

        // Local path that might already exist
        if (File(assetRef).exists()) {
            val saved = fileManager.saveImageDetailed(File(assetRef).inputStream())
            return ResolvedImageResult(
                path = saved.getOrNull(),
                warning = saved.exceptionOrNull()?.message,
                importedLocally = saved.isSuccess,
            )
        }

        return ResolvedImageResult(warning = "Image reference could not be resolved: $assetRef")
    }

    private fun normalizeAssetRef(assetRef: String): String {
        return assetRef.replace('\\', '/').removePrefix("./").removePrefix("/")
    }

    private fun bundleContainsPlainHttpAssets(bundle: LibraryBundleDto): Boolean {
        fun isPlainHttp(value: String?): Boolean = value?.startsWith("http://", ignoreCase = true) == true
        if (bundle.books.any { isPlainHttp(it.coverImage) }) return true
        return bundle.quizzes.any { quiz ->
            isPlainHttp(quiz.coverImage) || quiz.questions.any { q ->
                isPlainHttp(q.imageDataUrl) || isPlainHttp(q.imageSource) || isPlainHttp(q.imageName)
            }
        }
    }

    // ── Utility ─────────────────────────────────────────────────────

    companion object {
        fun generateDeterministicId(stem: String, options: List<String>): String {
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

// ── SQLDelight row → entity mappers (co-located for ImportLibraryManager) ──

private fun com.ahmedyejam.mks.db.Books.toBookEntity() = BookEntity(
    id = id, workspaceId = workspaceId, externalId = externalId,
    title = title, description = description, iconName = iconName, coverImage = coverImage,
    createdAt = createdAt, updatedAt = updatedAt, contentUpdatedAt = contentUpdatedAt,
    lastStudiedAt = lastStudiedAt, lastEditedAt = lastEditedAt,
    isPinned = isPinned != 0L, isSystem = isSystem != 0L,
    fields = emptyList(),
    questionCount = questionCount.toInt(), answeredCount = answeredCount.toInt(),
    totalAttempts = totalAttempts.toInt(),
    completionPercentage = completionPercentage.toFloat(),
    accuracyPercentage = accuracyPercentage.toFloat(),
    deletedAt = deletedAt,
)

private fun com.ahmedyejam.mks.db.Quizzes.toQuizEntity() = com.ahmedyejam.mks.data.local.entity.QuizEntity(
    id = id, externalId = externalId, bookId = bookId,
    title = title, description = description, category = category,
    tags = emptyList(), iconName = iconName, coverImage = coverImage,
    createdAt = createdAt, updatedAt = updatedAt, contentUpdatedAt = contentUpdatedAt,
    lastStudiedAt = lastStudiedAt, lastEditedAt = lastEditedAt,
    isPinned = isPinned != 0L, isSystem = isSystem != 0L,
    questionCount = questionCount.toInt(), answeredCount = answeredCount.toInt(),
    totalAttempts = totalAttempts.toInt(),
    completionPercentage = completionPercentage.toFloat(),
    accuracyPercentage = accuracyPercentage.toFloat(),
    deletedAt = deletedAt,
)

private fun com.ahmedyejam.mks.db.Questions.toQuestionEntity() = com.ahmedyejam.mks.data.local.entity.QuestionEntity(
    id = id, externalId = externalId, quizId = quizId, text = text,
    type = try { com.ahmedyejam.mks.data.local.entity.QuestionType.valueOf(type) }
        catch (_: Exception) { com.ahmedyejam.mks.data.local.entity.QuestionType.SINGLE_CHOICE },
    options = emptyList(), correctAnswers = emptyList(),
    explanation = explanation, hint = hint, reference = reference, weight = weight.toInt(),
    imagePath = imagePath, imageName = imageName, imageSource = imageSource,
    attempts = attempts.toInt(), correctCount = correctCount.toInt(),
    isDropped = isDropped != 0L, droppedAt = droppedAt, droppedReason = droppedReason,
    isMarked = isMarked != 0L, markedAt = markedAt, markReason = markReason, markReviewAt = markReviewAt,
    notes = notes, categories = emptyList(), tags = emptyList(),
    difficulty = difficulty, dueAt = dueAt, reviewCount = reviewCount.toInt(),
    lastReviewedAt = lastReviewedAt, additionalInfo = additionalInfo,
    sourceBookId = sourceBookId, sourceQuizId = sourceQuizId, sourceQuestionId = sourceQuestionId,
    createdAt = createdAt, updatedAt = updatedAt, lastStudiedAt = lastStudiedAt,
    lastEditedAt = lastEditedAt, timeSpentMs = timeSpentMs, lastAttemptResult = lastAttemptResult != 0L,
    consecutiveCorrect = consecutiveCorrect.toInt(), deletedAt = deletedAt,
)

private fun com.ahmedyejam.mks.db.Workspaces.toWorkspaceEntity() = WorkspaceEntity(
    id = id, externalId = externalId, name = name, description = description,
    isDefault = isDefault != 0L, createdAt = createdAt, updatedAt = updatedAt,
    deletedAt = deletedAt,
)
