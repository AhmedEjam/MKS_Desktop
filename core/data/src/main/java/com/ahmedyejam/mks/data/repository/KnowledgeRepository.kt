package com.ahmedyejam.mks.data.repository

import com.ahmedyejam.mks.data.importer.repository.ImportLibraryManager
import com.ahmedyejam.mks.data.local.FileManager
import com.ahmedyejam.mks.data.local.dao.AnnotationDao
import com.ahmedyejam.mks.data.local.dao.AssetReferenceDao
import com.ahmedyejam.mks.data.local.dao.BookDao
import com.ahmedyejam.mks.data.local.dao.CategoryMetadataDao
import com.ahmedyejam.mks.data.local.dao.CourseSlideDao
import com.ahmedyejam.mks.data.local.dao.FlashcardDao
import com.ahmedyejam.mks.data.local.dao.FlashcardDeckDao
import com.ahmedyejam.mks.data.local.dao.KnowledgeStudySessionDao
import com.ahmedyejam.mks.data.local.dao.LearningSessionDao
import com.ahmedyejam.mks.data.local.dao.MistakeLogDao
import com.ahmedyejam.mks.data.local.dao.NoteBlueprintDao
import com.ahmedyejam.mks.data.local.dao.PromptCardDao
import com.ahmedyejam.mks.data.local.dao.PromptDao
import com.ahmedyejam.mks.data.local.dao.PromptDeckDao
import com.ahmedyejam.mks.data.local.dao.PromptRunDao
import com.ahmedyejam.mks.data.local.dao.QuestionAssetDao
import com.ahmedyejam.mks.data.local.dao.QuestionCategoryDao
import com.ahmedyejam.mks.data.local.dao.QuestionDao
import com.ahmedyejam.mks.data.local.dao.QuizDao
import com.ahmedyejam.mks.data.local.dao.SessionDao
import com.ahmedyejam.mks.data.local.dao.SlideshowCourseDao
import com.ahmedyejam.mks.data.local.dao.SourceDocumentDao
import com.ahmedyejam.mks.data.local.dao.WorkspaceDao
import com.ahmedyejam.mks.data.local.entity.AnnotationOwnerType
import com.ahmedyejam.mks.data.local.entity.BlueprintMode
import com.ahmedyejam.mks.data.local.entity.BlueprintReviewStatus
import com.ahmedyejam.mks.data.local.entity.CourseSlideEntity
import com.ahmedyejam.mks.data.local.entity.FlashcardDeckEntity
import com.ahmedyejam.mks.data.local.entity.FlashcardEntity
import com.ahmedyejam.mks.data.local.entity.NoteBlueprintEntity
import com.ahmedyejam.mks.data.local.entity.PromptCardEntity
import com.ahmedyejam.mks.data.local.entity.PromptDeckEntity
import com.ahmedyejam.mks.data.local.entity.PromptEntity
import com.ahmedyejam.mks.data.local.entity.PromptOutputType
import com.ahmedyejam.mks.data.local.entity.PromptRunEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.local.entity.SlideshowCourseEntity
import com.ahmedyejam.mks.data.model.ArticleGenerationConfig
import com.ahmedyejam.mks.data.model.FlashcardGenerationConfig
import com.ahmedyejam.mks.data.model.SlideGenerationConfig
import com.ahmedyejam.mks.data.preview.CategoryMergePreviewService
import com.ahmedyejam.mks.data.preview.ClearMarksPreviewService
import com.ahmedyejam.mks.data.preview.DeletePreviewService
import com.ahmedyejam.mks.data.repair.AssetReferenceAuditService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgeRepository
    @Inject
    constructor(
        private val workspaceDao: WorkspaceDao,
        private val bookDao: BookDao,
        private val quizDao: QuizDao,
        private val questionDao: QuestionDao,
        private val sessionDao: SessionDao,
        private val categoryMetadataDao: CategoryMetadataDao,
        private val fileManager: FileManager,
        private val exportManager: ExportManager? = null,
        private val importManager: ImportLibraryManager? = null,
        private val flashcardDeckDao: FlashcardDeckDao,
        private val flashcardDao: FlashcardDao,
        private val learningSessionDao: LearningSessionDao,
        private val slideshowCourseDao: SlideshowCourseDao,
        private val courseSlideDao: CourseSlideDao,
        private val noteCollectionDao: com.ahmedyejam.mks.data.local.dao.NoteCollectionDao,
        private val noteBlueprintDao: NoteBlueprintDao,
        private val promptDao: PromptDao,
        private val studySessionDao: com.ahmedyejam.mks.data.local.dao.StudySessionDao,
        private val knowledgeStudySessionDao: KnowledgeStudySessionDao,
        private val questionCategoryDao: QuestionCategoryDao,
        private val assetReferenceDao: AssetReferenceDao,
        private val questionAssetDao: QuestionAssetDao,
        private val sourceDocumentDao: SourceDocumentDao,
        private val promptDeckDao: PromptDeckDao,
        private val promptCardDao: PromptCardDao,
        private val promptRunDao: PromptRunDao,
        private val mistakeLogDao: MistakeLogDao,
        private val quizRepositoryProvider: javax.inject.Provider<QuizRepository>,
        private val bookRepositoryProvider: javax.inject.Provider<BookRepository>,
        private val assetRepositoryProvider: javax.inject.Provider<AssetRepository>,
        private val annotationDao: AnnotationDao,
        private val deletePreviewService: DeletePreviewService? = null,
        private val categoryMergePreviewService: CategoryMergePreviewService? = null,
        private val clearMarksPreviewService: ClearMarksPreviewService? = null,
        private val assetReferenceAuditService: AssetReferenceAuditService? = null,
    ) {
        fun getFlashcardDecksByBookId(bookId: Long): Flow<List<FlashcardDeckEntity>> = flashcardDeckDao.getFlashcardDecksByBookId(bookId)

        suspend fun getFlashcardDeckById(id: Long) = flashcardDeckDao.getFlashcardDeckById(id)

        suspend fun insertFlashcardDeck(deck: FlashcardDeckEntity): Long {
            var finalDeck = deck
            if (deck.coverImage?.startsWith("http", ignoreCase = true) == true) {
                val localPath = fileManager.downloadAndSaveImage(deck.coverImage!!)
                if (localPath != null) finalDeck = deck.copy(coverImage = localPath)
            }
            val id = flashcardDeckDao.insertFlashcardDeck(finalDeck)
            assetRepositoryProvider.get().replaceOwnerAssetReferences("flashcard_deck", id, listOf(finalDeck.coverImage))
            refreshFlashcardDeckStats(id)
            bookRepositoryProvider.get().refreshBookStats(finalDeck.bookId)
            return id
        }

        suspend fun updateFlashcardDeck(deck: FlashcardDeckEntity) {
            val updated = deck.copy(updatedAt = System.currentTimeMillis(), lastEditedAt = System.currentTimeMillis())
            flashcardDeckDao.updateFlashcardDeck(updated)
            assetRepositoryProvider.get().replaceOwnerAssetReferences("flashcard_deck", updated.id, listOf(updated.coverImage))
            val refreshedDeck = refreshFlashcardDeckStats(deck.id) ?: updated
            bookRepositoryProvider.get().refreshBookStats(refreshedDeck.bookId)
        }

        suspend fun deleteFlashcardDeck(deck: FlashcardDeckEntity) {
            val now = System.currentTimeMillis()
            flashcardDao.softDeleteAllCardsInDeck(deck.id, now)
            flashcardDeckDao.softDeleteFlashcardDeckById(deck.id, now)
            bookRepositoryProvider.get().refreshBookStats(deck.bookId)
        }

        suspend fun restoreFlashcardDeck(deckId: Long) {
            val deck = flashcardDeckDao.getFlashcardDeckByIdIncludingDeleted(deckId) ?: return
            val book = bookDao.getBookByIdIncludingDeleted(deck.bookId)
            if (book != null && book.deletedAt != null) {
                bookRepositoryProvider.get().restoreBook(book.id)
            }
            val now = System.currentTimeMillis()
            val deletedAtFilter = deck.deletedAt ?: now
            flashcardDeckDao.restoreFlashcardDeckById(deckId, now)
            flashcardDao.restoreAllCardsInDeck(deckId, now, deletedAtFilter)
            bookRepositoryProvider.get().refreshBookStats(deck.bookId)
        }

        suspend fun permanentlyDeleteFlashcardDeck(deck: FlashcardDeckEntity) {
            val cards = flashcardDao.getFlashcardsByDeckId(deck.id).first()
            cards.forEach { assetRepositoryProvider.get().releaseOwnerAssets("flashcard", it.id) }
            assetRepositoryProvider.get().releaseOwnerAssets("flashcard_deck", deck.id)
            flashcardDeckDao.hardDeleteFlashcardDeck(deck)
            bookRepositoryProvider.get().refreshBookStats(deck.bookId)
        }

        fun getFlashcardsByDeckId(deckId: Long): Flow<List<FlashcardEntity>> = flashcardDao.getFlashcardsByDeckId(deckId)

        suspend fun insertFlashcard(card: FlashcardEntity): Long {
            var finalCard = card
            if (card.imagePath?.startsWith("http", ignoreCase = true) == true) {
                val localPath = fileManager.downloadAndSaveImage(card.imagePath!!)
                if (localPath != null) finalCard = card.copy(imagePath = localPath)
            }
            val id = flashcardDao.insertFlashcard(finalCard)
            assetRepositoryProvider.get().replaceOwnerAssetReferences("flashcard", id, listOf(finalCard.imagePath))
            val deck = refreshFlashcardDeckStats(finalCard.deckId)
            deck?.let { bookRepositoryProvider.get().refreshBookStats(it.bookId) }
            return id
        }

        suspend fun updateFlashcard(card: FlashcardEntity) {
            val updated = card.copy(updatedAt = System.currentTimeMillis())
            flashcardDao.updateFlashcard(updated)
            assetRepositoryProvider.get().replaceOwnerAssetReferences("flashcard", updated.id, listOf(updated.imagePath))
            val deck = refreshFlashcardDeckStats(card.deckId)
            deck?.let { bookRepositoryProvider.get().refreshBookStats(it.bookId) }
        }

        suspend fun deleteFlashcard(card: FlashcardEntity) {
            flashcardDao.softDeleteFlashcardById(card.id, System.currentTimeMillis())
            val deck = refreshFlashcardDeckStats(card.deckId)
            deck?.let { bookRepositoryProvider.get().refreshBookStats(it.bookId) }
        }

        suspend fun restoreFlashcard(cardId: Long) {
            flashcardDao.restoreFlashcardById(cardId, System.currentTimeMillis())
        }

        suspend fun permanentlyDeleteFlashcard(card: FlashcardEntity) {
            assetRepositoryProvider.get().releaseOwnerAssets("flashcard", card.id)
            flashcardDao.hardDeleteFlashcard(card)
            val deck = refreshFlashcardDeckStats(card.deckId)
            deck?.let { bookRepositoryProvider.get().refreshBookStats(it.bookId) }
        }

        fun getSlidesByCourseId(courseId: Long): Flow<List<CourseSlideEntity>> = courseSlideDao.getSlidesByCourseId(courseId)

        fun getPromptsByBookId(bookId: Long): Flow<List<PromptEntity>> = promptDao.getPromptsByBookId(bookId)

        suspend fun getPromptById(id: Long) = promptDao.getPromptById(id)

        suspend fun insertPrompt(prompt: PromptEntity): Long {
            return promptDao.insertPrompt(prompt.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        }

        suspend fun updatePrompt(prompt: PromptEntity) {
            promptDao.updatePrompt(prompt.copy(updatedAt = System.currentTimeMillis()))
        }

        suspend fun deletePrompt(prompt: PromptEntity) = promptDao.softDeletePromptById(prompt.id, System.currentTimeMillis())

        suspend fun refreshFlashcardDeckStats(deckId: Long): FlashcardDeckEntity? {
            val deck = flashcardDeckDao.getFlashcardDeckById(deckId) ?: return null
            val cards = flashcardDao.getFlashcardsByDeckId(deckId).first()
            val totalCards = cards.size
            val studiedCards = cards.count { it.attempts > 0 }
            val totalAttempts = cards.sumOf { it.attempts }
            val totalCorrect = cards.sumOf { it.correctCount }
            val mastery = if (totalAttempts == 0) 0f else totalCorrect.toFloat() / totalAttempts
            val now = System.currentTimeMillis()
            val refreshed =
                deck.copy(
                    cardCount = totalCards,
                    studiedCount = studiedCards,
                    masteryPercentage = mastery.coerceIn(0f, 1f),
                    updatedAt = now,
                )
            flashcardDeckDao.updateFlashcardDeck(refreshed)
            return refreshed
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        suspend fun addArticlesFromQuestions(
            bookId: Long,
            questionIds: List<Long>,
            config: ArticleGenerationConfig,
        ): List<Long> {
            val questions = questionDao.getQuestionsByIds(questionIds).sortedBy { questionIds.indexOf(it.id) }
            val ids = mutableListOf<Long>()
            val now = System.currentTimeMillis()

            for (q in questions) {
                val title = if (config.includeStemAsTitle) q.text.take(80) else "Generated Article"
                val bodyBuilder = StringBuilder()
                if (config.includeExplanationInBody && !q.explanation.isNullOrBlank()) {
                    bodyBuilder.append(q.explanation).append("\n\n")
                }
                if (config.includeHintInBody && !q.hint.isNullOrBlank()) {
                    bodyBuilder.append("Hint: ${q.hint}\n\n")
                }
                if (config.includeReferenceInBody && !q.reference.isNullOrBlank()) {
                    bodyBuilder.append("Reference: ${q.reference}\n\n")
                }
                val body = bodyBuilder.toString().trim()

                val bulletPoints = if (config.includeOptionsAsBulletPoints) q.options else emptyList()
                val tags = if (config.includeTags) q.categories else emptyList()

                val id =
                    insertNoteBlueprint(
                        NoteBlueprintEntity(
                            externalId = java.util.UUID.randomUUID().toString(),
                            collectionId = getOrCreateDefaultNoteCollection(bookId),
                            title = title,
                            summary = q.text.take(180),
                            body = body,
                            bulletPoints = bulletPoints,
                            tags = tags,
                            blueprintMode = config.articleMode,
                            linkedQuestionsJson = encodeLongList(listOf(q.id)),
                            linkedAssetsJson = "[]",
                            reviewStatus = BlueprintReviewStatus.NEW,
                            createdAt = now,
                            updatedAt = now,
                            sourceQuestionId = q.id,
                        ),
                    )
                ids.add(id)
            }
            return ids
        }

        suspend fun addCourseSlidesFromQuestionsToCourse(
            courseId: Long,
            questionIds: List<Long>,
            config: SlideGenerationConfig = SlideGenerationConfig.DEFAULT,
        ): Int {
            val course = slideshowCourseDao.getCourseById(courseId) ?: return 0
            val questions = questionDao.getQuestionsByIds(questionIds).sortedBy { questionIds.indexOf(it.id) }
            val existingSlides = courseSlideDao.getSlidesByCourseIdNow(courseId)
            val existingCount = existingSlides.size

            val slides =
                questions.mapIndexed { index, question ->
                    questionToSlide(courseId, question, existingCount + index, config)
                }

            if (slides.isNotEmpty()) {
                insertCourseSlides(slides)
            }
            return slides.size
        }

        suspend fun addFlashcardsFromQuestionsToDeck(
            deckId: Long,
            questionIds: List<Long>,
            config: FlashcardGenerationConfig = FlashcardGenerationConfig.DEFAULT,
        ): Int {
            val deck = flashcardDeckDao.getFlashcardDeckById(deckId) ?: return 0
            val questions = questionDao.getQuestionsByIds(questionIds).sortedBy { questionIds.indexOf(it.id) }
            val existingCount = flashcardDao.countCardsInDeck(deckId)
            val cards =
                questions.mapIndexed { index, question ->
                    questionToFlashcard(deckId, question, existingCount + index, config).copy(
                        sourceQuestionId = question.id,
                    )
                }
            if (cards.isNotEmpty()) {
                insertFlashcards(cards)
            }
            return cards.size
        }

        private fun blueprintBodyFromQuestion(
            question: QuestionEntity,
            mode: String,
        ): String {
            val correctAnswer = question.correctAnswers.mapNotNull(question.options::getOrNull).joinToString("; ")
            val heading =
                when (mode) {
                    BlueprintMode.DISEASE_TEMPLATE -> "Disease / condition blueprint"
                    BlueprintMode.DRUG_TEMPLATE -> "Drug blueprint"
                    BlueprintMode.MISTAKE_REVIEW -> "Mistake-review blueprint"
                    else -> "Concept blueprint"
                }
            return buildString {
                append(heading)
                append("\n\nQuestion\n")
                append(question.text)
                append("\n\nCorrect answer\n")
                append(correctAnswer.ifBlank { "Add the correct answer or concept here." })
                question.explanation?.takeIf { it.isNotBlank() }?.let {
                    append("\n\nExplanation\n")
                    append(it)
                }
                question.hint?.takeIf { it.isNotBlank() }?.let {
                    append("\n\nHint\n")
                    append(it)
                }
                question.reference?.takeIf { it.isNotBlank() }?.let {
                    append("\n\nReference\n")
                    append(it)
                }
            }
        }

        private fun buildFlashcardsFromBlueprint(
            deckId: Long,
            note: NoteBlueprintEntity,
        ): List<FlashcardEntity> {
            val bullets = note.bulletPoints.filter { it.isNotBlank() }
            val cards = mutableListOf<FlashcardEntity>()
            cards +=
                FlashcardEntity(
                    externalId = java.util.UUID.randomUUID().toString(),
                    deckId = deckId,
                    frontText = note.title,
                    backText = note.body,
                    hint = note.summary,
                    tags = note.tags + note.blueprintMode,
                    orderIndex = 0,
                    sourceQuestionId = note.sourceQuestionId,
                )
            bullets.forEachIndexed { index, point ->
                cards +=
                    FlashcardEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        deckId = deckId,
                        frontText = "${note.title}: key point ${index + 1}",
                        backText = point,
                        tags = note.tags + note.blueprintMode,
                        orderIndex = index + 1,
                        sourceQuestionId = note.sourceQuestionId,
                    )
            }
            return cards
        }

        suspend fun convertPromptOutputToBlueprint(
            bookId: Long,
            title: String,
            outputText: String,
            promptCardId: Long? = null,
        ): Long {
            val noteId =
                insertNoteBlueprint(
                    NoteBlueprintEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        collectionId = getOrCreateDefaultNoteCollection(bookId),
                        title = title.ifBlank { "Prompt blueprint" },
                        summary = "Structured blueprint saved from prompt output.",
                        body = outputText.ifBlank { "No output text was provided." },
                        bulletPoints = outputText.lines().map { it.trim().trimStart('-', '*') }.filter { it.isNotBlank() }.take(12),
                        tags = listOf("prompt-output", "blueprint"),
                        blueprintMode = BlueprintMode.CONCEPT_TEMPLATE,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            promptCardId?.let { recordPromptRun(it, "{}", "", outputText, "BLUEPRINT", noteId) }
            return noteId
        }

        suspend fun convertPromptOutputToFlashcards(
            bookId: Long,
            title: String,
            outputText: String,
            promptCardId: Long? = null,
        ): Long {
            val deckId =
                insertFlashcardDeck(
                    FlashcardDeckEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        bookId = bookId,
                        title = title.ifBlank { "Prompt flashcards" },
                        description = "Generated from prompt output.",
                    ),
                )
            val cards = parsePromptOutputFlashcards(deckId, outputText)
            if (cards.isNotEmpty()) {
                insertFlashcards(cards)
            } else {
                insertFlashcard(
                    FlashcardEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        deckId = deckId,
                        frontText = title.ifBlank { "Prompt output" },
                        backText = outputText.ifBlank { "No output text was provided." },
                        tags = listOf("prompt-output"),
                    ),
                )
            }
            promptCardId?.let { recordPromptRun(it, "{}", "", outputText, "FLASHCARDS", deckId) }
            return deckId
        }

        suspend fun convertPromptOutputToNote(
            bookId: Long,
            title: String,
            outputText: String,
            promptCardId: Long? = null,
        ): Long {
            val noteId =
                insertNoteBlueprint(
                    NoteBlueprintEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        collectionId = getOrCreateDefaultNoteCollection(bookId),
                        title = title.ifBlank { "Prompt output note" },
                        summary = "Saved from prompt output.",
                        body = outputText.ifBlank { "No output text was provided." },
                        bulletPoints = emptyList(),
                        tags = listOf("prompt-output"),
                        blueprintMode = BlueprintMode.SIMPLE_NOTE,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            promptCardId?.let { recordPromptRun(it, "{}", "", outputText, "NOTE", noteId) }
            return noteId
        }

        suspend fun convertPromptOutputToQuiz(
            bookId: Long,
            title: String,
            outputText: String,
            promptCardId: Long? = null,
        ): Long {
            val quizId =
                insertQuiz(
                    QuizEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        bookId = bookId,
                        title = title.ifBlank { "Prompt quiz" },
                        description = "Generated from prompt output.",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            val questions = parsePromptOutputQuestions(quizId, outputText)
            if (questions.isNotEmpty()) {
                insertQuestions(questions)
            } else {
                // Fallback: single descriptive question if no structure detected
                insertQuestion(
                    QuestionEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        quizId = quizId,
                        text = title.ifBlank { "Prompt output" },
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("Review output"),
                        correctAnswers = listOf(0),
                        explanation = outputText,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
            promptCardId?.let { recordPromptRun(it, "{}", "", outputText, "QUIZ", quizId) }
            return quizId
        }

        suspend fun copyCourseSlides(
            slideIds: List<Long>,
            targetCourseId: Long,
        ) {
            val existingCount = courseSlideDao.countSlidesInCourse(targetCourseId)
            val now = System.currentTimeMillis()

            val copiedSlides =
                slideIds.chunked(999).flatMap { chunk ->
                    courseSlideDao.getSlidesByIds(chunk).mapIndexed { index, slide ->
                        slide.copy(
                            id = 0,
                            externalId = java.util.UUID.randomUUID().toString(),
                            courseId = targetCourseId,
                            orderIndex = existingCount + index,
                            isCompleted = false,
                            createdAt = now,
                            updatedAt = now,
                            deletedAt = null,
                        )
                    }
                }

            if (copiedSlides.isNotEmpty()) {
                insertCourseSlides(copiedSlides)
            }
        }

        suspend fun copyFlashcards(
            cardIds: List<Long>,
            targetDeckId: Long,
        ) {
            val existingCount = flashcardDao.countCardsInDeck(targetDeckId)
            val now = System.currentTimeMillis()

            val copiedCards =
                cardIds.chunked(999).flatMap { chunk ->
                    flashcardDao.getFlashcardsByIds(chunk).mapIndexed { index, card ->
                        card.copy(
                            id = 0,
                            externalId = java.util.UUID.randomUUID().toString(),
                            deckId = targetDeckId,
                            orderIndex = existingCount + index,
                            attempts = 0,
                            correctCount = 0,
                            difficulty = null,
                            dueAt = 0,
                            reviewCount = 0,
                            lastReviewedAt = 0,
                            createdAt = now,
                            updatedAt = now,
                            deletedAt = null,
                        )
                    }
                }

            if (copiedCards.isNotEmpty()) {
                insertFlashcards(copiedCards)
            }
        }

        suspend fun createBlueprintFromMarkedQuestions(
            bookId: Long,
            title: String,
        ): Long {
            val questions = bookRepositoryProvider.get().getBookStudyBundle(bookId)?.questions.orEmpty().filter { it.isMarked }
            return createBlueprintFromQuestions(bookId, title, questions.map { it.id }, BlueprintMode.MISTAKE_REVIEW)
        }

        suspend fun createBlueprintFromMissedQuestions(
            bookId: Long,
            title: String,
        ): Long {
            val questions =
                bookRepositoryProvider.get().getBookStudyBundle(bookId)?.questions.orEmpty().filter { question ->
                    question.attempts > 0 && (question.correctCount < question.attempts || question.lastAttemptResult == false)
                }
            return createBlueprintFromQuestions(bookId, title, questions.map { it.id }, BlueprintMode.MISTAKE_REVIEW)
        }

        suspend fun createBlueprintFromQuestion(
            bookId: Long,
            questionId: Long,
            mode: String = BlueprintMode.CONCEPT_TEMPLATE,
        ): Long {
            val question =
                questionDao.getQuestionById(questionId)
                    ?: throw IllegalArgumentException("Question not found for blueprint creation.")
            val body = blueprintBodyFromQuestion(question, mode)
            return insertNoteBlueprint(
                NoteBlueprintEntity(
                    externalId = java.util.UUID.randomUUID().toString(),
                    collectionId = getOrCreateDefaultNoteCollection(bookId),
                    title = question.text.take(80).ifBlank { "Question blueprint" },
                    summary = question.explanation?.take(180),
                    body = body,
                    bulletPoints = question.options,
                    tags = question.categories,
                    blueprintMode = mode,
                    linkedQuestionsJson = encodeLongList(listOf(question.id)),
                    linkedAssetsJson = "[]",
                    reviewStatus = BlueprintReviewStatus.NEW,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    sourceQuestionId = question.id,
                ),
            )
        }

        suspend fun createBlueprintFromQuestions(
            bookId: Long,
            title: String,
            questionIds: List<Long>,
            mode: String = BlueprintMode.MISTAKE_REVIEW,
        ): Long {
            val questions = questionDao.getQuestionsByIds(questionIds).sortedBy { questionIds.indexOf(it.id) }
            if (questions.isEmpty()) throw IllegalArgumentException("No questions selected for blueprint creation.")
            val body = questions.joinToString(separator = "\n\n---\n\n") { question -> blueprintBodyFromQuestion(question, mode) }
            return insertNoteBlueprint(
                NoteBlueprintEntity(
                    externalId = java.util.UUID.randomUUID().toString(),
                    collectionId = getOrCreateDefaultNoteCollection(bookId),
                    title = title.ifBlank { "Question group blueprint" },
                    summary = "Generated from ${questions.size} question(s).",
                    body = body,
                    bulletPoints = questions.map { it.text.take(160) },
                    tags = questions.flatMap { it.categories }.distinct(),
                    blueprintMode = mode,
                    linkedQuestionsJson = encodeLongList(questions.map { it.id }),
                    linkedAssetsJson = "[]",
                    reviewStatus = BlueprintReviewStatus.NEW,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    sourceQuestionId = questions.firstOrNull()?.id,
                ),
            )
        }

        suspend fun createDefaultPromptDeck(
            bookId: Long,
            title: String,
            description: String? = null,
            seedDefaultCards: Boolean = false,
        ): Long {
            return insertPromptDeck(
                PromptDeckEntity(
                    bookId = bookId,
                    title = title.ifBlank { "Study prompt deck" },
                    description = description,
                ),
                seedDefaultCards = seedDefaultCards,
            )
        }

        suspend fun createExplainPromptDeck(
            bookId: Long,
            title: String,
            description: String? = null,
            seedDefaultCards: Boolean = false,
        ): Long {
            val deckId =
                insertPromptDeck(
                    PromptDeckEntity(
                        bookId = bookId,
                        title = title.ifBlank { "Explain & Teach" },
                        description = description,
                    ),
                    seedDefaultCards = false,
                )
            if (seedDefaultCards) {
                seedExplainPromptCard(deckId)
            }
            return deckId
        }

        suspend fun createFlashcardDeckFromBlueprint(noteId: Long): Long {
            val note = noteBlueprintDao.getNoteById(noteId) ?: throw IllegalArgumentException("Blueprint not found.")
            val deckId =
                insertFlashcardDeck(
                    FlashcardDeckEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        bookId = noteCollectionDao.getCollectionById(note.collectionId)?.bookId ?: 0L,
                        title = "${note.title} Flashcards",
                        description = "Generated from blueprint: ${note.title}",
                    ),
                )
            val cards = buildFlashcardsFromBlueprint(deckId, note)
            if (cards.isNotEmpty()) insertFlashcards(cards)
            return deckId
        }

        suspend fun createFlashcardDeckFromBook(
            bookId: Long,
            title: String,
            description: String? = null,
            coverImage: String? = null,
            config: FlashcardGenerationConfig = FlashcardGenerationConfig.DEFAULT,
        ): Long {
            val bundle =
                bookRepositoryProvider.get().getBookStudyBundle(bookId)
                    ?: throw IllegalArgumentException("Book $bookId not found")
            val now = System.currentTimeMillis()
            val deckId =
                insertFlashcardDeck(
                    FlashcardDeckEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        bookId = bookId,
                        title = title,
                        description = description,
                        coverImage = coverImage ?: bundle.book.coverImage,
                        createdAt = now,
                        updatedAt = now,
                        lastEditedAt = now,
                    ),
                )
            val cards =
                bundle.quizzes.flatMap { quiz ->
                    bundle.questionsByQuiz[quiz.id].orEmpty().mapIndexed { index, question ->
                        questionToFlashcard(deckId, question, index, config).copy(
                            tags = (question.categories + listOfNotNull(quiz.category)).distinct(),
                            createdAt = now,
                            updatedAt = now,
                            frontText =
                                if (!config.includeStemInFront || question.text.isBlank()) {
                                    "${quiz.title} ${index + 1}"
                                } else {
                                    questionToFlashcard(
                                        deckId,
                                        question,
                                        index,
                                        config,
                                    ).frontText
                                },
                        )
                    }
                }
            if (cards.isNotEmpty()) {
                insertFlashcards(cards)
            }
            return deckId
        }

        suspend fun createFlashcardDeckFromMarkedQuestions(
            bookId: Long,
            title: String,
            description: String? = null,
            clearMarksAfter: Boolean = false,
            config: FlashcardGenerationConfig = FlashcardGenerationConfig.DEFAULT,
        ): Long {
            val questions = bookRepositoryProvider.get().getBookStudyBundle(bookId)?.questions.orEmpty().filter { it.isMarked }
            return createFlashcardDeckFromQuestions(bookId, title, description, questions.map { it.id }, clearMarksAfter, config)
        }

        suspend fun createFlashcardDeckFromMissedQuestions(
            bookId: Long,
            title: String,
            description: String? = null,
            config: FlashcardGenerationConfig = FlashcardGenerationConfig.DEFAULT,
        ): Long {
            val questions =
                bookRepositoryProvider.get().getBookStudyBundle(bookId)?.questions.orEmpty().filter { question ->
                    question.attempts > 0 && (question.correctCount < question.attempts || question.lastAttemptResult == false)
                }
            return createFlashcardDeckFromQuestions(bookId, title, description, questions.map { it.id }, clearMarksAfter = false, config)
        }

        suspend fun createFlashcardDeckFromQuestions(
            bookId: Long,
            title: String,
            description: String? = null,
            questionIds: List<Long>,
            clearMarksAfter: Boolean = false,
            config: FlashcardGenerationConfig = FlashcardGenerationConfig.DEFAULT,
        ): Long {
            val questions = questionDao.getQuestionsByIds(questionIds).sortedBy { questionIds.indexOf(it.id) }
            if (questions.isEmpty()) throw IllegalArgumentException("No questions selected for flashcard generation.")
            val now = System.currentTimeMillis()
            val deckId =
                insertFlashcardDeck(
                    FlashcardDeckEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        bookId = bookId,
                        title = title.ifBlank { "Generated flashcards" },
                        description = description,
                        createdAt = now,
                        updatedAt = now,
                        lastEditedAt = now,
                    ),
                )
            addFlashcardsFromQuestionsToDeck(deckId, questions.map { it.id }, config)
            if (clearMarksAfter) {
                questions.filter { it.isMarked }.forEach { question ->
                    questionDao.updateQuestion(question.copy(isMarked = false, updatedAt = now, lastEditedAt = now))
                }
            }
            return deckId
        }

        suspend fun createSlideshowCourseFromQuestions(
            bookId: Long,
            title: String,
            description: String?,
            questionIds: List<Long>,
            clearMarksAfter: Boolean = false,
        ): Long {
            val now = System.currentTimeMillis()
            val courseId =
                insertSlideshowCourse(
                    SlideshowCourseEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        bookId = bookId,
                        title = title,
                        description = description,
                        createdAt = now,
                        updatedAt = now,
                        lastEditedAt = now,
                    ),
                )

            addCourseSlidesFromQuestionsToCourse(courseId, questionIds)

            if (clearMarksAfter && questionIds.isNotEmpty()) {
                val questions = questionDao.getQuestionsByIds(questionIds).map { it.copy(isMarked = false) }
                questionDao.updateQuestions(questions)
            }
            return courseId
        }

        suspend fun deleteCourseSlide(slide: CourseSlideEntity) {
            val now = System.currentTimeMillis()
            assetRepositoryProvider.get().softDeleteOwnerAnnotations(AnnotationOwnerType.SLIDE, slide.id, now)
            courseSlideDao.softDeleteSlideById(slide.id, now)
            refreshCourseStats(slide.courseId)
        }

        suspend fun deleteNoteBlueprint(note: NoteBlueprintEntity) {
            val now = System.currentTimeMillis()
            assetRepositoryProvider.get().softDeleteOwnerAnnotations(AnnotationOwnerType.NOTE, note.id, now)
            noteBlueprintDao.softDeleteNoteById(note.id, now)
        }

        suspend fun deletePromptCard(card: PromptCardEntity) = promptCardDao.softDeleteCardById(card.id, System.currentTimeMillis())

        suspend fun deletePromptDeck(deck: PromptDeckEntity) = promptDeckDao.softDeleteDeckById(deck.id, System.currentTimeMillis())

        suspend fun deleteSlideshowCourse(course: SlideshowCourseEntity) {
            val now = System.currentTimeMillis()
            assetRepositoryProvider.get().softDeleteSlideshowAnnotationTree(course.id, now)
            slideshowCourseDao.softDeleteCourseById(course.id, now)
            bookRepositoryProvider.get().refreshBookStats(course.bookId)
        }

        fun getDeletedFlashcardDecks(workspaceId: Long): Flow<List<FlashcardDeckEntity>> =
            flashcardDeckDao.getDeletedDecksByWorkspaceFlow(
                workspaceId,
            )

        fun getDeletedNoteBlueprints(workspaceId: Long): Flow<List<NoteBlueprintEntity>> =
            noteBlueprintDao.getDeletedNotesByWorkspaceFlow(
                workspaceId,
            )

        fun getDeletedPromptDecks(workspaceId: Long): Flow<List<PromptDeckEntity>> =
            promptDeckDao.getDeletedDecksByWorkspaceFlow(
                workspaceId,
            )

        fun getDeletedSlideshowCourses(workspaceId: Long): Flow<List<SlideshowCourseEntity>> =
            slideshowCourseDao.getDeletedCoursesByWorkspaceFlow(
                workspaceId,
            )

        fun getLinkedBlueprintsForQuestion(
            bookId: Long,
            questionId: Long,
        ): Flow<List<NoteBlueprintEntity>> = noteBlueprintDao.getNotesByLinkedQuestion(bookId, questionId)

        suspend fun getNoteBlueprintById(id: Long) = noteBlueprintDao.getNoteById(id)

        fun getNoteBlueprintsByBookId(bookId: Long): Flow<List<NoteBlueprintEntity>> = noteBlueprintDao.getNotesByBookId(bookId)

        fun getPromptCardsByDeckId(deckId: Long): Flow<List<PromptCardEntity>> = promptCardDao.getCardsByDeckId(deckId)

        suspend fun getPromptCardsByDeckIdNow(deckId: Long): List<PromptCardEntity> = promptCardDao.getCardsByDeckIdNow(deckId)

        suspend fun getPromptDeckById(id: Long): PromptDeckEntity? = promptDeckDao.getDeckById(id)

        fun getPromptDecksByBookId(bookId: Long): Flow<List<PromptDeckEntity>> = promptDeckDao.getDecksByBookId(bookId)

        fun getPromptRunsByDeckId(deckId: Long): Flow<List<PromptRunEntity>> = promptRunDao.getRunsByDeckId(deckId)

        suspend fun getPromptRunsByDeckIdNow(deckId: Long): List<PromptRunEntity> = promptRunDao.getRunsByDeckIdNow(deckId)

        suspend fun getSlideshowCourseById(id: Long) = slideshowCourseDao.getCourseById(id)

        fun getSlideshowCoursesByBookId(bookId: Long): Flow<List<SlideshowCourseEntity>> = slideshowCourseDao.getCoursesByBookId(bookId)

        suspend fun insertCourseSlide(slide: CourseSlideEntity): Long {
            var finalSlide = slide
            // Download image if it's an HTTP URL
            if (slide.imagePath?.startsWith("http", ignoreCase = true) == true) {
                val localPath = fileManager.downloadAndSaveImage(slide.imagePath!!)
                if (localPath != null) finalSlide = slide.copy(imagePath = localPath)
            }
            val saved = finalSlide.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
            val id = courseSlideDao.insertSlide(saved)
            assetRepositoryProvider.get().replaceOwnerAssetReferences("course_slide", id, listOf(saved.imagePath))
            refreshCourseStats(slide.courseId)
            return id
        }

        suspend fun insertCourseSlides(slides: List<CourseSlideEntity>): List<Long> {
            val now = System.currentTimeMillis()
            val updated =
                slides.map { slide ->
                    val finalSlide =
                        if (slide.imagePath?.startsWith("http", ignoreCase = true) == true) {
                            val localPath = fileManager.downloadAndSaveImage(slide.imagePath!!)
                            if (localPath != null) slide.copy(imagePath = localPath) else slide
                        } else {
                            slide
                        }
                    finalSlide.copy(createdAt = now, updatedAt = now)
                }

            // We do a manual insert slide-by-slide to return IDs for asset reference replacing,
            // since Dao insertSlides does not return a list of IDs. Or we can just insert one by one.
            val ids = mutableListOf<Long>()
            for (slide in updated) {
                val id = courseSlideDao.insertSlide(slide)
                assetRepositoryProvider.get().replaceOwnerAssetReferences("course_slide", id, listOf(slide.imagePath))
                ids.add(id)
            }

            updated.map { it.courseId }.distinct().forEach { refreshCourseStats(it) }
            return ids
        }

        suspend fun insertFlashcards(cards: List<FlashcardEntity>): List<Long> {
            val now = System.currentTimeMillis()
            val updated =
                cards.map { card ->
                    val finalCard =
                        if (card.imagePath?.startsWith("http", ignoreCase = true) == true) {
                            val localPath = fileManager.downloadAndSaveImage(card.imagePath!!)
                            if (localPath != null) card.copy(imagePath = localPath) else card
                        } else {
                            card
                        }
                    finalCard.copy(createdAt = now, updatedAt = now)
                }
            val ids = flashcardDao.insertFlashcards(updated)
            ids.zip(updated).forEach { (id, card) ->
                assetRepositoryProvider.get().replaceOwnerAssetReferences("flashcard", id, listOf(card.imagePath))
            }
            updated.map { it.deckId }.distinct().forEach { deckId ->
                val deck = refreshFlashcardDeckStats(deckId)
                deck?.let { bookRepositoryProvider.get().refreshBookStats(it.bookId) }
            }
            return ids
        }

        suspend fun insertNoteBlueprint(note: NoteBlueprintEntity): Long {
            return noteBlueprintDao.insertNote(note.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        }

        suspend fun insertPromptCard(card: PromptCardEntity): Long {
            val now = System.currentTimeMillis()
            val deck = promptDeckDao.getDeckById(card.deckId)
            val id = promptCardDao.insertCard(card.copy(createdAt = now, updatedAt = now))
            deck?.let { promptDeckDao.updateDeck(it.copy(updatedAt = now)) }
            return id
        }

        suspend fun insertPromptDeck(
            deck: PromptDeckEntity,
            seedDefaultCards: Boolean = false,
        ): Long {
            val now = System.currentTimeMillis()
            val id = promptDeckDao.insertDeck(deck.copy(createdAt = now, updatedAt = now))
            if (seedDefaultCards) seedDefaultPromptCards(id)
            return id
        }

        suspend fun insertSlideshowCourse(course: SlideshowCourseEntity): Long {
            var finalCourse = course
            // Download cover image if it's an HTTP URL
            if (course.coverImage?.startsWith("http", ignoreCase = true) == true) {
                val localPath = fileManager.downloadAndSaveImage(course.coverImage!!)
                if (localPath != null) finalCourse = course.copy(coverImage = localPath)
            }
            val saved =
                finalCourse.copy(
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    lastEditedAt = System.currentTimeMillis(),
                )
            val id = slideshowCourseDao.insertCourse(saved)
            assetRepositoryProvider.get().replaceOwnerAssetReferences("slideshow_course", id, listOf(saved.coverImage))
            bookRepositoryProvider.get().refreshBookStats(course.bookId)
            return id
        }

        suspend fun moveCourseSlides(
            slideIds: List<Long>,
            targetCourseId: Long,
        ) {
            val now = System.currentTimeMillis()
            val originalCourses = mutableSetOf<Long>()

            slideIds.chunked(999).forEach { chunk ->
                val slides = courseSlideDao.getSlidesByIds(chunk)
                slides.forEach { originalCourses.add(it.courseId) }
                courseSlideDao.moveSlidesToCourse(chunk, targetCourseId, now)
            }

            originalCourses.forEach { refreshCourseStats(it) }
            refreshCourseStats(targetCourseId)
        }

        suspend fun moveFlashcards(
            cardIds: List<Long>,
            targetDeckId: Long,
        ) {
            val now = System.currentTimeMillis()
            val originalDecks = mutableSetOf<Long>()

            cardIds.chunked(999).forEach { chunk ->
                val cards = flashcardDao.getFlashcardsByIds(chunk)
                cards.forEach { originalDecks.add(it.deckId) }
                flashcardDao.moveCardsToDeck(chunk, targetDeckId, now)
            }

            originalDecks.forEach { refreshFlashcardDeckStats(it) }
            refreshFlashcardDeckStats(targetDeckId)
        }

        fun observeFlashcardDeckById(id: Long): Flow<FlashcardDeckEntity?> = flashcardDeckDao.observeFlashcardDeckById(id)

        private fun parsePromptOutputFlashcards(
            deckId: Long,
            outputText: String,
        ): List<FlashcardEntity> {
            val cards = mutableListOf<FlashcardEntity>()

            // Try JSON parsing first
            try {
                val jsonStartIndex = outputText.indexOf('[')
                val jsonEndIndex = outputText.lastIndexOf(']')
                if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonStartIndex < jsonEndIndex) {
                    val jsonString = outputText.substring(jsonStartIndex, jsonEndIndex + 1)
                    val jsonArray = org.json.JSONArray(jsonString)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val front = obj.optString("front", obj.optString("frontText", obj.optString("question", "")))
                        val back = obj.optString("back", obj.optString("backText", obj.optString("answer", "")))

                        if (front.isNotBlank() && back.isNotBlank()) {
                            cards.add(
                                FlashcardEntity(
                                    externalId = java.util.UUID.randomUUID().toString(),
                                    deckId = deckId,
                                    frontText = front,
                                    backText = back,
                                    tags = listOf("prompt-output"),
                                    orderIndex = i,
                                ),
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (cards.isNotEmpty()) return cards

            val blocks = outputText.split(Regex("\\n\\s*\\n"))
            return blocks.mapIndexedNotNull { index, raw ->
                val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
                val front =
                    lines.firstOrNull { it.startsWith("Front", ignoreCase = true) }
                        ?.substringAfter(':', "")?.trim()
                val back =
                    lines.firstOrNull { it.startsWith("Back", ignoreCase = true) }
                        ?.substringAfter(':', "")?.trim()
                if (front.isNullOrBlank() || back.isNullOrBlank()) {
                    null
                } else {
                    FlashcardEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        deckId = deckId,
                        frontText = front,
                        backText = back,
                        tags = listOf("prompt-output"),
                        orderIndex = index,
                    )
                }
            }
        }

        private fun parsePromptOutputQuestions(
            quizId: Long,
            outputText: String,
        ): List<QuestionEntity> {
            val questions = mutableListOf<QuestionEntity>()

            // Try JSON parsing first
            try {
                val jsonStartIndex = outputText.indexOf('[')
                val jsonEndIndex = outputText.lastIndexOf(']')
                if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonStartIndex < jsonEndIndex) {
                    val jsonString = outputText.substring(jsonStartIndex, jsonEndIndex + 1)
                    val jsonArray = org.json.JSONArray(jsonString)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val qText = obj.optString("question", obj.optString("text", ""))
                        if (qText.isBlank()) continue

                        val optionsJson = obj.optJSONArray("options") ?: org.json.JSONArray()
                        val options = mutableListOf<String>()
                        for (j in 0 until optionsJson.length()) {
                            options.add(optionsJson.getString(j))
                        }

                        val answersJson = obj.optJSONArray("answers") ?: obj.optJSONArray("correctAnswers") ?: org.json.JSONArray()
                        val correctIndices = mutableListOf<Int>()
                        for (j in 0 until answersJson.length()) {
                            val ans = answersJson.get(j)
                            if (ans is Int) {
                                correctIndices.add(ans)
                            } else if (ans is String) {
                                val idx = options.indexOfFirst { it.equals(ans, ignoreCase = true) }
                                if (idx != -1) correctIndices.add(idx)
                            }
                        }
                        if (correctIndices.isEmpty() && obj.has("answer")) {
                            val ans = obj.optString("answer")
                            val idx = options.indexOfFirst { it.equals(ans, ignoreCase = true) }
                            if (idx != -1) correctIndices.add(idx)
                        }

                        val explanation = obj.optString("explanation", obj.optString("rationale", ""))

                        questions.add(
                            QuestionEntity(
                                externalId = java.util.UUID.randomUUID().toString(),
                                quizId = quizId,
                                text = qText,
                                type = if (correctIndices.size > 1) QuestionType.MULTIPLE_CHOICE else QuestionType.SINGLE_CHOICE,
                                options = options.ifEmpty { listOf("Option A", "Option B") },
                                correctAnswers = correctIndices.ifEmpty { listOf(0) },
                                explanation = explanation.ifBlank { null },
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (questions.isNotEmpty()) return questions

            // Simple parser looking for "Question:", "Options:", "Answer:" pattern blocks
            val blocks = outputText.split(Regex("\\n\\s*\\n"))
            return blocks.mapIndexedNotNull { index, raw ->
                val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
                val qText =
                    lines.firstOrNull { it.startsWith("Question", ignoreCase = true) }
                        ?.substringAfter(':', "")?.trim() ?: return@mapIndexedNotNull null

                val options =
                    lines.filter { it.startsWith("Option", ignoreCase = true) || it.matches(Regex("^[A-D][).].*")) }
                        .map { line ->
                            if (line.startsWith("Option", ignoreCase = true)) {
                                line.substringAfter(':').trim()
                            } else {
                                line.substring(2).trim()
                            }
                        }

                val ansText =
                    lines.firstOrNull { it.startsWith("Answer", ignoreCase = true) }
                        ?.substringAfter(':', "")?.trim()

                val correctIndices = mutableListOf<Int>()
                if (!ansText.isNullOrBlank()) {
                    // Try letter match
                    val letterMatch = Regex("([A-D])").findAll(ansText.uppercase())
                    letterMatch.forEach { match ->
                        val idx = match.value[0] - 'A'
                        if (idx in options.indices) correctIndices.add(idx)
                    }
                    // Try text match if no letters matched
                    if (correctIndices.isEmpty()) {
                        val matchIdx = options.indexOfFirst { it.equals(ansText, ignoreCase = true) }
                        if (matchIdx != -1) correctIndices.add(matchIdx)
                    }
                }

                val explanation =
                    lines.firstOrNull { it.startsWith("Explanation", ignoreCase = true) }
                        ?.substringAfter(':', "")?.trim()

                QuestionEntity(
                    externalId = java.util.UUID.randomUUID().toString(),
                    quizId = quizId,
                    text = qText,
                    type = if (correctIndices.size > 1) QuestionType.MULTIPLE_CHOICE else QuestionType.SINGLE_CHOICE,
                    options = options.ifEmpty { listOf("Option A", "Option B") },
                    correctAnswers = correctIndices.ifEmpty { listOf(0) },
                    explanation = explanation,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }

        suspend fun permanentlyDeleteCourseSlide(slide: CourseSlideEntity) {
            assetRepositoryProvider.get().permanentlyDeleteOwnerAnnotations(AnnotationOwnerType.SLIDE, slide.id)
            assetRepositoryProvider.get().releaseOwnerAssets("course_slide", slide.id)
            courseSlideDao.hardDeleteSlide(slide)
            refreshCourseStats(slide.courseId)
        }

        suspend fun permanentlyDeleteNoteBlueprint(note: NoteBlueprintEntity) {
            assetRepositoryProvider.get().permanentlyDeleteOwnerAnnotations(AnnotationOwnerType.NOTE, note.id)
            noteBlueprintDao.hardDeleteNote(note)
        }

        // Prompts

        suspend fun permanentlyDeletePrompt(prompt: PromptEntity) = promptDao.hardDeletePrompt(prompt)

        // True Prompt Decks

        suspend fun permanentlyDeletePromptCard(card: PromptCardEntity) = promptCardDao.hardDeleteCard(card)

        suspend fun permanentlyDeletePromptDeck(deck: PromptDeckEntity) = promptDeckDao.hardDeleteDeck(deck)

        suspend fun permanentlyDeleteSlideshowCourse(course: SlideshowCourseEntity) {
            assetRepositoryProvider.get().permanentlyDeleteSlideshowAnnotationTree(course.id)
            val slides = courseSlideDao.getSlidesByCourseIdIncludingDeleted(course.id)
            slides.forEach { assetRepositoryProvider.get().releaseOwnerAssets("course_slide", it.id) }
            assetRepositoryProvider.get().releaseOwnerAssets("slideshow_course", course.id)
            slideshowCourseDao.hardDeleteCourse(course)
            bookRepositoryProvider.get().refreshBookStats(course.bookId)
        }

        // Course Slides

        private fun questionToFlashcard(
            deckId: Long,
            question: QuestionEntity,
            orderIndex: Int,
            config: FlashcardGenerationConfig,
        ): FlashcardEntity {
            val frontText =
                buildString {
                    if (config.includeStemInFront) {
                        append(question.text)
                    }
                    if (config.includeOptionsInFront && question.options.isNotEmpty()) {
                        if (isNotEmpty()) append("\n\n")
                        question.options.forEachIndexed { i, opt -> append("${(i + 65).toChar()}) $opt\n") }
                    }
                }.trim()

            val backText =
                buildString {
                    if (config.includeAnswerInBack) {
                        val answerText =
                            question.correctAnswers
                                .mapNotNull(question.options::getOrNull)
                                .joinToString(separator = "\n")
                                .ifBlank { "Review the source material for the expected answer." }
                        append(answerText)
                    }

                    if (config.includeExplanationInBack && !question.explanation.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append("Explanation\n${question.explanation}")
                    }

                    if (config.includeHintInBack && !question.hint.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append("Hint\n${question.hint}")
                    }

                    if (config.includeReferenceInBack && !question.reference.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append("Reference\n${question.reference}")
                    }

                    if (config.includeAdditionalInfoInBack && !question.additionalInfo.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append("Additional Info\n${question.additionalInfo}")
                    }
                }.trim()

            return FlashcardEntity(
                externalId = java.util.UUID.randomUUID().toString(),
                deckId = deckId,
                frontText = frontText.ifBlank { "Empty Front" },
                backText = backText.ifBlank { "Empty Back" },
                hint = question.hint.takeIf { !config.includeHintInBack }, // Only use standalone hint if not in back
                imagePath = if (config.includeImageInFront || config.includeImageInBack) question.imagePath else null,
                tags = question.categories,
                orderIndex = orderIndex,
                sourceQuestionId = question.id,
                syncConfig = emptyMap(),
            )
        }

        private fun questionToSlide(
            courseId: Long,
            question: QuestionEntity,
            orderIndex: Int,
            config: SlideGenerationConfig,
        ): CourseSlideEntity {
            val title = if (config.includeStemInTitle && question.text.isNotBlank()) question.text.trim() else "Slide ${orderIndex + 1}"

            val body =
                buildString {
                    if (config.includeOptionsInBody) {
                        val optionsText = question.options.joinToString(separator = "\n") { "- $it" }
                        if (optionsText.isNotBlank()) append(optionsText)
                    }
                    if (config.includeAnswerInBody) {
                        val answerText =
                            question.correctAnswers
                                .mapNotNull(question.options::getOrNull)
                                .joinToString(separator = "\n")
                        if (answerText.isNotBlank()) {
                            if (isNotEmpty()) append("\n\n")
                            append("Answer:\n$answerText")
                        }
                    }
                    if (config.includeExplanationInBody && !question.explanation.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append("Explanation:\n${question.explanation}")
                    }
                }.trim().ifBlank { "No content" }

            val notes =
                buildString {
                    if (config.includeHintInSpeakerNotes && !question.hint.isNullOrBlank()) {
                        append("Hint: ${question.hint}\n")
                    }
                    if (config.includeReferenceInSpeakerNotes && !question.reference.isNullOrBlank()) {
                        append("Reference: ${question.reference}\n")
                    }
                }.trim()

            val now = System.currentTimeMillis()
            return CourseSlideEntity(
                externalId = java.util.UUID.randomUUID().toString(),
                courseId = courseId,
                title = title,
                body = body,
                speakerNotes = notes.ifBlank { null },
                imagePath = if (config.includeImage) question.imagePath else null,
                orderIndex = orderIndex,
                sourceQuestionId = question.id,
                syncConfig = emptyMap(),
                createdAt = now,
                updatedAt = now,
            )
        }

        suspend fun rateFlashcard(
            card: FlashcardEntity,
            rating: String,
        ) {
            val now = System.currentTimeMillis()
            val normalized = rating.lowercase().trim().ifBlank { "good" }
            val correctIncrement =
                when (normalized) {
                    "easy", "good" -> 1
                    else -> 0
                }
            val day = 24L * 60L * 60L * 1000L
            val nextDueAt =
                when (normalized) {
                    "again" -> now + 10L * 60L * 1000L
                    "easy" -> now + maxOf(3L, (card.reviewCount + 1L) * 2L) * day
                    "good" -> now + maxOf(1L, card.reviewCount + 1L) * day
                    else -> now + day
                }
            val updated =
                card.copy(
                    attempts = card.attempts + 1,
                    correctCount = card.correctCount + correctIncrement,
                    difficulty = normalized,
                    lastReviewedAt = now,
                    updatedAt = now,
                )
            updateFlashcard(updated)
            flashcardDao.markReviewed(card.id, now, nextDueAt)
            val deck = flashcardDeckDao.getFlashcardDeckById(card.deckId)
            deck?.let { flashcardDeckDao.updateFlashcardDeck(it.copy(lastStudiedAt = now, updatedAt = now)) }
        }

        suspend fun recordPromptRun(
            cardId: Long,
            inputValuesJson: String,
            renderedPrompt: String,
            outputText: String? = null,
            linkedAssetType: String? = null,
            linkedAssetId: Long? = null,
        ): Long {
            val now = System.currentTimeMillis()
            val id =
                promptRunDao.insertRun(
                    PromptRunEntity(
                        promptCardId = cardId,
                        inputValuesJson = inputValuesJson,
                        renderedPrompt = renderedPrompt,
                        outputText = outputText?.takeIf { it.isNotBlank() },
                        linkedAssetType = linkedAssetType,
                        linkedAssetId = linkedAssetId,
                        createdAt = now,
                    ),
                )
            promptCardDao.recordUse(cardId, now, now)
            promptCardDao.getCardById(cardId)?.let { card ->
                promptDeckDao.getDeckById(card.deckId)?.let { deck -> promptDeckDao.updateDeck(deck.copy(updatedAt = now)) }
            }
            return id
        }

        private suspend fun refreshCourseStats(courseId: Long) {
            val course = slideshowCourseDao.getCourseById(courseId) ?: return
            val slides = courseSlideDao.getSlidesByCourseId(courseId).first()
            val total = slides.size
            val studied = slides.count { it.isCompleted }
            val progress = if (total == 0) 0f else studied.toFloat() / total
            slideshowCourseDao.updateCourse(
                course.copy(slideCount = total, studiedSlideCount = studied, progress = progress, updatedAt = System.currentTimeMillis()),
            )
        }

        // Note Blueprints

        fun renderPromptTemplate(
            promptText: String,
            values: Map<String, String>,
        ): String {
            var rendered = promptText
            values.forEach { (key, value) ->
                rendered = rendered.replace("{$key}", value)
            }
            return rendered
        }

        suspend fun reorderCourseSlides(slides: List<CourseSlideEntity>) {
            val now = System.currentTimeMillis()
            val updated =
                slides.mapIndexed { index, slide ->
                    slide.copy(orderIndex = index, updatedAt = now)
                }
            courseSlideDao.updateSlides(updated)
            slides.firstOrNull()?.courseId?.let { refreshCourseStats(it) }
        }

        suspend fun reorderFlashcards(cards: List<FlashcardEntity>) {
            val now = System.currentTimeMillis()
            cards.forEachIndexed { index, card ->
                flashcardDao.updateCardOrder(card.id, index, now)
            }
            cards.firstOrNull()?.deckId?.let { refreshFlashcardDeckStats(it) }
        }

        suspend fun restoreCourseSlide(slideId: Long) {
            val now = System.currentTimeMillis()
            courseSlideDao.restoreSlideById(slideId, now)
            assetRepositoryProvider.get().restoreOwnerAnnotations(AnnotationOwnerType.SLIDE, slideId, now)
        }

        suspend fun restoreNoteBlueprint(noteId: Long) {
            val note = noteBlueprintDao.getNoteByIdIncludingDeleted(noteId) ?: return
            val bookId = noteCollectionDao.getCollectionById(note.collectionId)?.bookId ?: 0L
            val book = bookDao.getBookByIdIncludingDeleted(bookId)
            if (book != null && book.deletedAt != null) {
                bookRepositoryProvider.get().restoreBook(book.id)
            }
            val now = System.currentTimeMillis()
            noteBlueprintDao.restoreNoteById(noteId, now)
            assetRepositoryProvider.get().restoreOwnerAnnotations(AnnotationOwnerType.NOTE, noteId, now)
        }

        suspend fun restorePrompt(promptId: Long) = promptDao.restorePromptById(promptId, System.currentTimeMillis())

        suspend fun restorePromptCard(cardId: Long) = promptCardDao.restoreCardById(cardId, System.currentTimeMillis())

        suspend fun restorePromptDeck(deckId: Long) {
            val deck = promptDeckDao.getDeckByIdIncludingDeleted(deckId) ?: return
            val book = bookDao.getBookByIdIncludingDeleted(deck.bookId)
            if (book != null && book.deletedAt != null) {
                bookRepositoryProvider.get().restoreBook(book.id)
            }
            promptDeckDao.restoreDeckById(deckId, System.currentTimeMillis())
        }

        suspend fun restoreSlideshowCourse(courseId: Long) {
            val course = slideshowCourseDao.getCourseByIdIncludingDeleted(courseId) ?: return
            val book = bookDao.getBookByIdIncludingDeleted(course.bookId)
            if (book != null && book.deletedAt != null) {
                bookRepositoryProvider.get().restoreBook(book.id)
            }
            val now = System.currentTimeMillis()
            val deletedAtFilter = course.deletedAt ?: now
            slideshowCourseDao.restoreCourseById(courseId, now)
            courseSlideDao.restoreSlidesByCourseId(courseId, now, deletedAtFilter)
            assetRepositoryProvider.get().restoreSlideshowAnnotationTree(courseId, now)
            bookRepositoryProvider.get().refreshBookStats(course.bookId)
        }

        private suspend fun seedDefaultPromptCards(deckId: Long) {
            val now = System.currentTimeMillis()
            val cards =
                listOf(
                    getExplainPromptCardEntity(deckId, now, 0),
                    PromptCardEntity(
                        deckId = deckId,
                        title = "Quiz generator",
                        promptText = "<system_role>You are an expert test creator and medical educator.</system_role>\n<instructions>Analyze the provided material and generate 3 to 5 high-quality, challenging multiple-choice questions.\nEnsure that:\n1. The stem is clinically relevant or focuses on key conceptual understanding.\n2. There are 4-5 plausible options.\n3. The correct answer is unambiguously correct.\n4. A detailed rationale/explanation is provided for why the correct answer is right and why the distractors are wrong.\nOutput strictly in JSON format as an array of objects with keys: \"question\", \"options\" (array of strings), \"answer\" (string), \"explanation\" (string).</instructions>\n<material>\n{material}\n</material>",
                        variablesJson = "[\"material\"]",
                        outputType = PromptOutputType.QUIZ,
                        sortOrder = 1,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    PromptCardEntity(
                        deckId = deckId,
                        title = "Flashcard generator",
                        promptText = "<system_role>You are an expert learning designer.</system_role>\n<instructions>Convert the core concepts from the provided material into concise, high-yield spaced-repetition flashcards.\nFollow these rules:\n1. Make the front of the card a clear, unambiguous question or cloze deletion.\n2. Keep the back of the card concise and to the point.\n3. Provide an optional brief hint to aid recall.\nOutput strictly in JSON format as an array of objects with keys: \"front\", \"back\", \"hint\".</instructions>\n<material>\n{material}\n</material>",
                        variablesJson = "[\"material\"]",
                        outputType = PromptOutputType.FLASHCARDS,
                        sortOrder = 2,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    PromptCardEntity(
                        deckId = deckId,
                        title = "Blueprint maker",
                        promptText = "<system_role>You are an expert technical writer and educator.</system_role>\n<instructions>Synthesize the provided material into a highly structured, comprehensive, yet easy-to-digest study blueprint.\nFormat the output in Markdown with the following sections:\n- **Core Summary**: A brief 2-3 sentence overview.\n- **Key Concepts**: Bulleted list of the most important takeaways.\n- **Detailed Breakdown**: A structured analysis with subheadings.\n- **Common Pitfalls / Mistakes to Avoid**: What students usually get wrong about this.\n- **Review Cues**: Short questions to test memory.</instructions>\n<material>\n{material}\n</material>",
                        variablesJson = "[\"material\"]",
                        outputType = PromptOutputType.BLUEPRINT,
                        sortOrder = 3,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            promptCardDao.insertCards(cards)
        }

        private suspend fun seedExplainPromptCard(deckId: Long) {
            val now = System.currentTimeMillis()
            val explainCard = getExplainPromptCardEntity(deckId, now, 0)
            promptCardDao.insertCard(explainCard)
        }

        private fun getExplainPromptCardEntity(
            deckId: Long,
            now: Long,
            sortOrder: Int,
        ): PromptCardEntity {
            return PromptCardEntity(
                deckId = deckId,
                title = "Explain & Teach",
                promptText = "<system_role>You are an expert tutor in {relevant_speciality}.</system_role>\n<instructions>Please explain the following question part: {part_to_explain}.\n\nHere is the full question context:\n{question}</instructions>",
                variablesJson = "[\"relevant_speciality\", \"part_to_explain\", \"question\"]",
                outputType = PromptOutputType.NOTE,
                sortOrder = sortOrder,
                createdAt = now,
                updatedAt = now,
            )
        }

        suspend fun updateCourseSlide(slide: CourseSlideEntity) {
            var finalSlide = slide
            // Download image if it's an HTTP URL
            if (slide.imagePath?.startsWith("http", ignoreCase = true) == true) {
                val localPath = fileManager.downloadAndSaveImage(slide.imagePath!!)
                if (localPath != null) finalSlide = slide.copy(imagePath = localPath)
            }
            val updated = finalSlide.copy(updatedAt = System.currentTimeMillis())
            courseSlideDao.updateSlide(updated)
            assetRepositoryProvider.get().replaceOwnerAssetReferences("course_slide", updated.id, listOf(updated.imagePath))
            refreshCourseStats(slide.courseId)
        }

        suspend fun updateNoteBlueprint(note: NoteBlueprintEntity) {
            noteBlueprintDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
        }

        suspend fun updatePromptCard(card: PromptCardEntity) {
            val now = System.currentTimeMillis()
            promptCardDao.updateCard(card.copy(updatedAt = now))
            promptDeckDao.getDeckById(card.deckId)?.let { promptDeckDao.updateDeck(it.copy(updatedAt = now)) }
        }

        suspend fun updatePromptCards(cards: List<PromptCardEntity>) {
            val now = System.currentTimeMillis()
            promptCardDao.updateCards(cards.map { it.copy(updatedAt = now) })
            if (cards.isNotEmpty()) {
                promptDeckDao.getDeckById(cards.first().deckId)?.let { promptDeckDao.updateDeck(it.copy(updatedAt = now)) }
            }
        }

        suspend fun updatePromptDeck(deck: PromptDeckEntity) {
            promptDeckDao.updateDeck(deck.copy(updatedAt = System.currentTimeMillis()))
        }

        suspend fun updateSlideshowCourse(course: SlideshowCourseEntity) {
            var finalCourse = course
            // Download cover image if it's an HTTP URL
            if (course.coverImage?.startsWith("http", ignoreCase = true) == true) {
                val localPath = fileManager.downloadAndSaveImage(course.coverImage!!)
                if (localPath != null) finalCourse = course.copy(coverImage = localPath)
            }
            val updated = finalCourse.copy(updatedAt = System.currentTimeMillis(), lastEditedAt = System.currentTimeMillis())
            slideshowCourseDao.updateCourse(updated)
            assetRepositoryProvider.get().replaceOwnerAssetReferences("slideshow_course", updated.id, listOf(updated.coverImage))
        }

        suspend fun getOrCreateDefaultNoteCollection(bookId: Long): Long {
            val collections = noteCollectionDao.getCollectionsByBookIdNow(bookId)
            return if (collections.isNotEmpty()) {
                collections.first().id
            } else {
                noteCollectionDao.insertCollection(
                    com.ahmedyejam.mks.data.local.entity.NoteCollectionEntity(
                        bookId = bookId,
                        title = "Default Collection",
                        externalId = java.util.UUID.randomUUID().toString(),
                    ),
                )
            }
        }

        private fun encodeLongList(list: List<Long>): String = if (list.isEmpty()) "[]" else list.joinToString(prefix = "[", postfix = "]")

        private fun parseLongList(json: String): List<Long> =
            if (json.isBlank() || json == "[]") {
                emptyList()
            } else {
                json.removePrefix("[").removeSuffix("]").split(",").mapNotNull { it.trim().toLongOrNull() }
            }

        suspend fun insertQuiz(quiz: com.ahmedyejam.mks.data.local.entity.QuizEntity): Long = quizDao.insertQuiz(quiz)

        suspend fun insertQuestions(questions: List<com.ahmedyejam.mks.data.local.entity.QuestionEntity>) =
            questionDao.insertQuestions(
                questions,
            )

        suspend fun insertQuestion(question: com.ahmedyejam.mks.data.local.entity.QuestionEntity): Long =
            questionDao.insertQuestion(
                question,
            )
    }
