package com.ahmedyejam.mks.data.repository

import androidx.room.withTransaction
import com.ahmedyejam.mks.data.exchange.v7.*
import com.ahmedyejam.mks.data.importer.dto.*
import com.ahmedyejam.mks.data.importer.mapping.LibraryMapper
import com.ahmedyejam.mks.data.local.FileManager
import com.ahmedyejam.mks.data.local.MksDatabase
import com.ahmedyejam.mks.data.local.dao.*
import com.ahmedyejam.mks.data.local.entity.*
import com.ahmedyejam.mks.data.model.ExportResult
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream

class ExportManager(
    private val database: MksDatabase,
    private val bookDao: BookDao,
    private val quizDao: QuizDao,
    private val questionDao: QuestionDao,
    private val sessionDao: SessionDao,
    private val categoryMetadataDao: CategoryMetadataDao,
    private val fileManager: FileManager,
    private val mapper: LibraryMapper,
) {
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val okHttpClient by lazy { okhttp3.OkHttpClient() }

    companion object {
    }

    suspend fun exportQuizAsBundle(quizId: Long): LibraryBundleDto? {
        return database.withTransaction {
            val quiz = quizDao.getQuizById(quizId) ?: return@withTransaction null
            val book = bookDao.getBookById(quiz.bookId) ?: return@withTransaction null
            val questions = questionDao.getQuestionsByQuizId(quiz.id).first()
            val workspaceExternalId = database.workspaceDao().getWorkspaceById(book.workspaceId)?.externalId

            val exportBook = mapper.mapToBookDto(book, workspaceExternalId)
            val exportQuiz = mapper.mapToQuizDto(quiz, questions, book.externalId)

            val categoryNames =
                (questions.flatMap { it.categories } + quiz.category)
                    .distinct()
                    .filterNotNull()
                    .filter { it.isNotBlank() }

            val exportCategories =
                categoryNames.mapNotNull { catName ->
                    categoryMetadataDao.getMetadataForCategory(catName)?.let { mapper.mapToCategoryMetadataDto(it) }
                        ?: CategoryMetadataDto(name = catName)
                }

            LibraryBundleDto(
                kind = "quiz-bundle",
                books = listOf(exportBook),
                quizzes = listOf(exportQuiz),
                categories = exportCategories,
            )
        }
    }

    suspend fun exportQuizToZip(
        quizId: Long,
        outputStream: OutputStream,
    ): ExportResult {
        val bundle =
            exportQuizAsBundle(quizId)
                ?: return ExportResult(success = false, errorMessage = "Quiz not found.")
        val resolvedBundle = resolveBundleLocalPaths(bundle)
        return try {
            val supplemental = buildSupplementalData(quizId = quizId)
            MksExchangeV7Archive.writeLegacyBundleToSchema7Zip(resolvedBundle, outputStream, supplemental)
            ExportResult(success = true)
        } catch (e: java.lang.Exception) {
            ExportResult(success = false, errorMessage = e.message ?: "Export failed")
        }
    }

    suspend fun exportBookAsBundle(bookId: Long): LibraryBundleDto? {
        return database.withTransaction {
            val book = bookDao.getBookById(bookId) ?: return@withTransaction null
            val workspaceExternalId = database.workspaceDao().getWorkspaceById(book.workspaceId)?.externalId
            val exportBook = mapper.mapToBookDto(book, workspaceExternalId)
            val exportQuizzes = mutableListOf<QuizDto>()
            val exportSessions = mutableListOf<SessionDto>()

            val quizzes = quizDao.getQuizzesByBookId(book.id).first()
            val allQuestions = mutableListOf<QuestionEntity>()
            for (quiz in quizzes) {
                val questions = questionDao.getQuestionsByQuizId(quiz.id).first()
                allQuestions.addAll(questions)
                val sessions = sessionDao.getSessionsByQuizId(quiz.id).first()

                exportQuizzes.add(mapper.mapToQuizDto(quiz, questions, book.externalId))
                exportSessions.addAll(sessions.map { mapper.mapToSessionDto(it, quiz.externalId, questions) })
            }

            // Knowledge Bank Assets
            val flashcardDecks = database.flashcardDeckDao().getFlashcardDecksByBookIdNow(book.id)
            val exportFlashcardDecks =
                flashcardDecks.map { deck ->
                    val cards = database.flashcardDao().getFlashcardsByDeckIdNow(deck.id)
                    mapper.mapToFlashcardDeckDto(deck, cards, book.externalId)
                }

            val slideshowCourses = database.slideshowCourseDao().getSlideshowCoursesByBookIdNow(book.id)
            val exportSlideshowCourses =
                slideshowCourses.map { course ->
                    val slides = database.courseSlideDao().getSlidesByCourseIdNow(course.id)
                    mapper.mapToSlideshowCourseDto(course, slides, book.externalId)
                }

            val noteBlueprints = database.noteBlueprintDao().getNotesByBookIdNow(book.id)
            val exportNoteBlueprints = noteBlueprints.map { mapper.mapToNoteBlueprintDto(it, book.externalId) }

            val promptDecks = database.promptDeckDao().getDecksByBookIdNow(book.id)
            val exportPromptDecks =
                promptDecks.map { deck ->
                    val cards = database.promptCardDao().getCardsByDeckIdNow(deck.id)
                    mapper.mapToPromptDeckDto(deck, cards, book.externalId)
                }

            val studySessions = database.knowledgeStudySessionDao().getSessionsByBookIdNow(book.id)
            val exportStudySessions =
                studySessions.map { session ->
                    val contentExtId =
                        when (session.targetType) {
                            "FLASHCARD_DECK" -> flashcardDecks.find { it.id == session.targetId }?.externalId
                            "SLIDESHOW" -> slideshowCourses.find { it.id == session.targetId }?.externalId
                            "NOTE" -> noteBlueprints.find { it.id == session.targetId }?.externalId
                            "PROMPT" -> promptDecks.find { it.id == session.targetId }?.let { "prompt-deck-${it.id}" }
                            else -> session.targetId.toString()
                        } ?: session.targetId.toString()

                    KnowledgeStudySessionDto(
                        bookId = book.externalId,
                        contentId = contentExtId,
                        type = session.targetType,
                        progress = 0f,
                        isCompleted = session.isCompleted,
                        lastAccessedAt = session.updatedAt,
                    )
                }

            val categoryNames =
                (allQuestions.flatMap { it.categories } + exportQuizzes.mapNotNull { it.storageKey })
                    .distinct()
                    .filter { it.isNotBlank() }

            val exportCategories =
                categoryNames.mapNotNull { catName ->
                    categoryMetadataDao.getMetadataForCategory(catName)?.let { mapper.mapToCategoryMetadataDto(it) }
                        ?: CategoryMetadataDto(name = catName)
                }

            LibraryBundleDto(
                kind = "book-bundle",
                books = listOf(exportBook),
                quizzes = exportQuizzes,
                flashcardDecks = exportFlashcardDecks,
                slideshowCourses = exportSlideshowCourses,
                noteBlueprints = exportNoteBlueprints,
                promptDecks = exportPromptDecks,
                studySessions = exportStudySessions,
                sessions = exportSessions,
                categories = exportCategories,
            )
        }
    }

    suspend fun exportAllBooksAsBundle(): LibraryBundleDto {
        return database.withTransaction {
            val books = bookDao.getAllBooksFlow().first()
            val workspaceExternalIds =
                books
                    .map { it.workspaceId }
                    .distinct()
                    .associateWith { workspaceId -> database.workspaceDao().getWorkspaceById(workspaceId)?.externalId }
            val exportBooks = books.map { mapper.mapToBookDto(it, workspaceExternalIds[it.workspaceId]) }
            val exportQuizzes = mutableListOf<QuizDto>()
            val exportSessions = mutableListOf<SessionDto>()
            val allQuestionEntities = mutableListOf<QuestionEntity>()

            val exportFlashcardDecks = mutableListOf<FlashcardDeckDto>()
            val exportSlideshowCourses = mutableListOf<SlideshowCourseDto>()
            val exportNoteBlueprints = mutableListOf<NoteBlueprintDto>()
            val exportPromptDecks = mutableListOf<PromptDeckDto>()
            val exportStudySessions = mutableListOf<KnowledgeStudySessionDto>()

            for (book in books) {
                val quizzes = quizDao.getQuizzesByBookId(book.id).first()
                for (quiz in quizzes) {
                    val questions = questionDao.getQuestionsByQuizId(quiz.id).first()
                    allQuestionEntities.addAll(questions)
                    val sessions = sessionDao.getSessionsByQuizId(quiz.id).first()

                    exportQuizzes.add(mapper.mapToQuizDto(quiz, questions, book.externalId))
                    exportSessions.addAll(sessions.map { mapper.mapToSessionDto(it, quiz.externalId, questions) })
                }

                val flashcardDecks = database.flashcardDeckDao().getFlashcardDecksByBookIdNow(book.id)
                exportFlashcardDecks.addAll(
                    flashcardDecks.map { deck ->
                        val cards = database.flashcardDao().getFlashcardsByDeckIdNow(deck.id)
                        mapper.mapToFlashcardDeckDto(deck, cards, book.externalId)
                    },
                )

                val slideshowCourses = database.slideshowCourseDao().getSlideshowCoursesByBookIdNow(book.id)
                exportSlideshowCourses.addAll(
                    slideshowCourses.map { course ->
                        val slides = database.courseSlideDao().getSlidesByCourseIdNow(course.id)
                        mapper.mapToSlideshowCourseDto(course, slides, book.externalId)
                    },
                )

                val noteBlueprints = database.noteBlueprintDao().getNotesByBookIdNow(book.id)
                exportNoteBlueprints.addAll(noteBlueprints.map { mapper.mapToNoteBlueprintDto(it, book.externalId) })

                val promptDecks = database.promptDeckDao().getDecksByBookIdNow(book.id)
                exportPromptDecks.addAll(
                    promptDecks.map { deck ->
                        val cards = database.promptCardDao().getCardsByDeckIdNow(deck.id)
                        mapper.mapToPromptDeckDto(deck, cards, book.externalId)
                    },
                )

                val studySessions = database.knowledgeStudySessionDao().getSessionsByBookIdNow(book.id)
                exportStudySessions.addAll(
                    studySessions.map { session ->
                        val contentExtId =
                            when (session.targetType) {
                                "FLASHCARD_DECK" -> flashcardDecks.find { it.id == session.targetId }?.externalId
                                "SLIDESHOW" -> slideshowCourses.find { it.id == session.targetId }?.externalId
                                "NOTE" -> noteBlueprints.find { it.id == session.targetId }?.externalId
                                "PROMPT" -> promptDecks.find { it.id == session.targetId }?.let { "prompt-deck-${it.id}" }
                                else -> session.targetId.toString()
                            } ?: session.targetId.toString()

                        KnowledgeStudySessionDto(
                            bookId = book.externalId,
                            contentId = contentExtId,
                            type = session.targetType,
                            progress = 0f, // Need calculation if needed
                            isCompleted = session.isCompleted,
                            lastAccessedAt = session.updatedAt,
                        )
                    },
                )
            }

            val categoryNames =
                (allQuestionEntities.flatMap { it.categories } + exportQuizzes.mapNotNull { it.storageKey })
                    .distinct()
                    .filter { it.isNotBlank() }

            val exportCategories =
                categoryNames.mapNotNull { catName ->
                    categoryMetadataDao.getMetadataForCategory(catName)?.let { mapper.mapToCategoryMetadataDto(it) }
                        ?: CategoryMetadataDto(name = catName)
                }

            LibraryBundleDto(
                kind = "full-library",
                books = exportBooks,
                quizzes = exportQuizzes,
                flashcardDecks = exportFlashcardDecks,
                slideshowCourses = exportSlideshowCourses,
                noteBlueprints = exportNoteBlueprints,
                promptDecks = exportPromptDecks,
                studySessions = exportStudySessions,
                sessions = exportSessions,
                categories = exportCategories,
            )
        }
    }

    suspend fun exportBundleToZip(
        bookId: Long,
        outputStream: OutputStream,
    ): ExportResult {
        val bundle =
            exportBookAsBundle(bookId)
                ?: return ExportResult(success = false, errorMessage = "Book not found.")
        val resolvedBundle = resolveBundleLocalPaths(bundle)
        return try {
            val supplemental = buildSupplementalData(bookId)
            MksExchangeV7Archive.writeLegacyBundleToSchema7Zip(resolvedBundle, outputStream, supplemental)
            ExportResult(success = true)
        } catch (e: java.lang.Exception) {
            ExportResult(success = false, errorMessage = e.message ?: "Export failed")
        }
    }

    suspend fun exportAllToZip(outputStream: OutputStream): ExportResult {
        val bundle = exportAllBooksAsBundle()
        val resolvedBundle = resolveBundleLocalPaths(bundle)
        return try {
            val supplemental = buildSupplementalData(null)
            MksExchangeV7Archive.writeLegacyBundleToSchema7Zip(resolvedBundle, outputStream, supplemental)
            ExportResult(success = true)
        } catch (e: java.lang.Exception) {
            ExportResult(success = false, errorMessage = e.message ?: "Export failed")
        }
    }

    private fun resolveLocalPath(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("data:") || path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }
        val file =
            fileManager.getFile(path) ?: run {
                if (!path.contains("/") && !path.contains("\\")) {
                    fileManager.getFile(File(File(fileManager.getContext().filesDir, "images"), path).absolutePath)
                } else {
                    null
                }
            }
        return file?.absolutePath ?: path
    }

    private fun resolveBundleLocalPaths(bundle: LibraryBundleDto): LibraryBundleDto {
        return bundle.copy(
            books = bundle.books.map { it.copy(coverImage = resolveLocalPath(it.coverImage) ?: "") },
            quizzes =
                bundle.quizzes.map { quiz ->
                    quiz.copy(
                        coverImage = resolveLocalPath(quiz.coverImage) ?: "",
                        questions =
                            quiz.questions.map { q ->
                                q.copy(
                                    imageDataUrl = resolveLocalPath(q.imageDataUrl) ?: "",
                                    imageSource = resolveLocalPath(q.imageSource) ?: "",
                                )
                            },
                    )
                },
            flashcardDecks =
                bundle.flashcardDecks.map { deck ->
                    deck.copy(
                        coverImage = resolveLocalPath(deck.coverImage),
                        cards =
                            deck.cards.map { card ->
                                card.copy(imagePath = resolveLocalPath(card.imagePath))
                            },
                    )
                },
            slideshowCourses =
                bundle.slideshowCourses.map { course ->
                    course.copy(
                        coverImage = resolveLocalPath(course.coverImage),
                        slides =
                            course.slides.map { slide ->
                                slide.copy(imagePath = resolveLocalPath(slide.imagePath))
                            },
                    )
                },
        )
    }

    private suspend fun buildSupplementalData(
        bookId: Long? = null,
        quizId: Long? = null,
    ): MksExchangeV7SupplementalData {
        val allBooks = bookDao.getAllBooksFlow().first()
        val allQuizzes = quizDao.getAllQuizzesFlow().first()
        val allQuestions = questionDao.getAllQuestionsFlow().first()

        val bookMap = allBooks.associateBy { it.id }
        val quizMap = allQuizzes.associateBy { it.id }
        val questionMap = allQuestions.associateBy { it.id }

        val targetBookId = bookId ?: quizId?.let { qId -> quizMap[qId]?.bookId }

        // Fetch Room entities
        val sourceDocEntities =
            if (targetBookId != null) {
                database.sourceDocumentDao().getSourcesByBookIdIncludingDeleted(targetBookId)
            } else {
                database.sourceDocumentDao().getAllSourcesIncludingDeleted()
            }

        val questionAssetEntities =
            if (targetBookId != null) {
                database.questionAssetDao().getAssetsByBookIdIncludingDeleted(targetBookId).let { assets ->
                    if (quizId != null) assets.filter { it.quizId == quizId } else assets
                }
            } else {
                database.questionAssetDao().getAllAssetsIncludingDeleted()
            }

        val annotationEntities =
            if (targetBookId != null) {
                database.annotationDao().getAnnotationsByBookIdIncludingDeleted(targetBookId).let { annotations ->
                    if (quizId != null) {
                        annotations.filter { ann ->
                            val isQuiz = ann.ownerType.uppercase() == "QUIZ" && ann.ownerId == quizId
                            val isQuestion = ann.ownerType.uppercase() == "QUESTION" && questionMap[ann.ownerId]?.quizId == quizId
                            isQuiz || isQuestion
                        }
                    } else {
                        annotations
                    }
                }
            } else {
                database.annotationDao().getAllAnnotationsIncludingDeleted()
            }

        val assetReferenceEntities =
            database.assetReferenceDao().getAllReferencesIncludingDeleted().run {
                if (bookId != null) {
                    // Filter references in memory to only those belonging to the book or its sub-entities
                    val quizIds = allQuizzes.filter { it.bookId == bookId }.map { it.id }.toSet()
                    val questionIds = allQuestions.filter { it.quizId in quizIds }.map { it.id }.toSet()
                    val sourceDocIds = sourceDocEntities.map { it.id }.toSet()

                    val flashcardDeckIds = database.flashcardDeckDao().getFlashcardDecksByBookIdNow(bookId).map { it.id }.toSet()
                    val flashcardIds =
                        flashcardDeckIds.flatMap {
                            database.flashcardDao().getFlashcardsByDeckIdNow(
                                it,
                            )
                        }.map { it.id }.toSet()
                    val slideshowCourseIds = database.slideshowCourseDao().getSlideshowCoursesByBookIdNow(bookId).map { it.id }.toSet()
                    val courseSlideIds =
                        slideshowCourseIds.flatMap {
                            database.courseSlideDao().getSlidesByCourseIdNow(
                                it,
                            )
                        }.map { it.id }.toSet()
                    val noteBlueprintIds = database.noteBlueprintDao().getNotesByBookIdNow(bookId).map { it.id }.toSet()
                    val promptDeckIds = database.promptDeckDao().getDecksByBookIdNow(bookId).map { it.id }.toSet()
                    val promptCardIds = promptDeckIds.flatMap { database.promptCardDao().getCardsByDeckIdNow(it) }.map { it.id }.toSet()

                    filter { ref ->
                        when (ref.ownerType.uppercase()) {
                            "BOOK" -> ref.ownerId == bookId
                            "QUIZ" -> ref.ownerId in quizIds
                            "QUESTION" -> ref.ownerId in questionIds
                            "SOURCE_DOCUMENT" -> ref.ownerId in sourceDocIds
                            "FLASHCARD_DECK" -> ref.ownerId in flashcardDeckIds
                            "FLASHCARD" -> ref.ownerId in flashcardIds
                            "SLIDESHOW", "SLIDESHOW_COURSE" -> ref.ownerId in slideshowCourseIds
                            "COURSE_SLIDE" -> ref.ownerId in courseSlideIds
                            "NOTE_BLUEPRINT", "NOTE" -> ref.ownerId in noteBlueprintIds
                            "PROMPT_DECK" -> ref.ownerId in promptDeckIds
                            "PROMPT_CARD", "PROMPT" -> ref.ownerId in promptCardIds
                            else -> false
                        }
                    }
                } else if (quizId != null) {
                    val questionIds = allQuestions.filter { it.quizId == quizId }.map { it.id }.toSet()
                    filter { ref ->
                        when (ref.ownerType.uppercase()) {
                            "QUIZ" -> ref.ownerId == quizId
                            "QUESTION" -> ref.ownerId in questionIds
                            else -> false
                        }
                    }
                } else {
                    this
                }
            }

        // Map Room entities to Supplemental DTOs
        val sourceDocuments =
            sourceDocEntities.map { doc ->
                val bookExtId = doc.bookId?.let { bookMap[it]?.externalId }
                MksExchangeV7SupplementalSourceDocument(
                    id = doc.id,
                    bookId = doc.bookId,
                    bookExternalId = bookExtId,
                    title = doc.title,
                    sourceType = doc.sourceType,
                    author = doc.author,
                    edition = doc.edition,
                    year = doc.year,
                    publisher = doc.publisher,
                    localPath = resolveLocalPath(doc.localPath),
                    externalUrl = doc.externalUrl,
                    description = doc.description,
                    createdAt = doc.createdAt,
                    updatedAt = doc.updatedAt,
                    deletedAt = doc.deletedAt,
                )
            }

        val questionAssets =
            questionAssetEntities.map { asset ->
                val bookExtId = bookMap[asset.bookId]?.externalId
                val quizExtId = quizMap[asset.quizId]?.externalId
                val questionExtId = questionMap[asset.questionId]?.externalId
                MksExchangeV7SupplementalQuestionAsset(
                    id = asset.id,
                    bookId = asset.bookId,
                    quizId = asset.quizId,
                    questionId = asset.questionId,
                    bookExternalId = bookExtId,
                    quizExternalId = quizExtId,
                    questionExternalId = questionExtId,
                    assetType = asset.assetType,
                    title = asset.title,
                    description = asset.description,
                    localPath = resolveLocalPath(asset.localPath),
                    externalUrl = asset.externalUrl,
                    mimeType = asset.mimeType,
                    fileName = asset.fileName,
                    fileSizeBytes = asset.fileSizeBytes,
                    textContent = asset.textContent,
                    sourceDocumentId = asset.sourceDocumentId,
                    sourcePage = asset.sourcePage,
                    sourceQuote = asset.sourceQuote,
                    sortOrder = asset.sortOrder,
                    isPinned = asset.isPinned,
                    isPrimary = asset.isPrimary,
                    createdAt = asset.createdAt,
                    updatedAt = asset.updatedAt,
                    deletedAt = asset.deletedAt,
                )
            }

        val annotations =
            annotationEntities.map { ann ->
                val bookExtId = bookMap[ann.bookId]?.externalId
                val ownerExtId =
                    when (ann.ownerType.uppercase()) {
                        "BOOK" -> bookMap[ann.ownerId]?.externalId
                        "QUIZ" -> quizMap[ann.ownerId]?.externalId
                        "QUESTION" -> questionMap[ann.ownerId]?.externalId
                        else -> null
                    }
                MksExchangeV7SupplementalAnnotation(
                    id = ann.id,
                    workspaceId = ann.workspaceId,
                    bookId = ann.bookId,
                    bookExternalId = bookExtId,
                    ownerType = ann.ownerType,
                    ownerId = ann.ownerId,
                    ownerExternalId = ownerExtId,
                    selectedText = ann.selectedText,
                    noteBody = ann.noteBody,
                    colorLabel = ann.colorLabel,
                    positionDataJson = ann.positionDataJson,
                    createdAt = ann.createdAt,
                    updatedAt = ann.updatedAt,
                    deletedAt = ann.deletedAt,
                )
            }

        val assetReferences =
            assetReferenceEntities.map { ref ->
                val ownerExtId =
                    when (ref.ownerType.uppercase()) {
                        "BOOK" -> bookMap[ref.ownerId]?.externalId
                        "QUIZ" -> quizMap[ref.ownerId]?.externalId
                        "QUESTION" -> questionMap[ref.ownerId]?.externalId
                        else -> null
                    }
                MksExchangeV7SupplementalAssetReference(
                    id = ref.id,
                    path = resolveLocalPath(ref.path) ?: ref.path,
                    ownerType = ref.ownerType,
                    ownerId = ref.ownerId,
                    ownerExternalId = ownerExtId,
                    createdAt = ref.createdAt,
                    deletedAt = ref.deletedAt,
                )
            }

        return MksExchangeV7SupplementalData(
            assetReferences = assetReferences,
            questionAssets = questionAssets,
            sourceDocuments = sourceDocuments,
            annotations = annotations,
        )
    }
}
