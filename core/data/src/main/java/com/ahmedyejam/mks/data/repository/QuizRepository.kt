package com.ahmedyejam.mks.data.repository

import android.net.Uri
import com.ahmedyejam.mks.data.importer.model.ImportFormat
import com.ahmedyejam.mks.data.importer.model.ImportResult
import com.ahmedyejam.mks.data.importer.model.MergeStrategy
import com.ahmedyejam.mks.data.importer.model.ParsedQuestion
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
import com.ahmedyejam.mks.data.local.entity.CategoryMetadataEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.local.entity.SessionEntity
import com.ahmedyejam.mks.data.model.CategoryWithMetadata
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.data.preview.CategoryMergePreviewService
import com.ahmedyejam.mks.data.preview.ClearMarksPreviewService
import com.ahmedyejam.mks.data.preview.DeletePreviewService
import com.ahmedyejam.mks.data.repair.AssetReferenceAuditService
import com.ahmedyejam.mks.data.simulation.ChangeSimulationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepository
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
        private val annotationDao: AnnotationDao,
        private val deletePreviewService: DeletePreviewService? = null,
        private val categoryMergePreviewService: CategoryMergePreviewService? = null,
        private val clearMarksPreviewService: ClearMarksPreviewService? = null,
        private val assetReferenceAuditService: AssetReferenceAuditService? = null,
        private val bookRepositoryProvider: javax.inject.Provider<BookRepository>,
        private val assetRepositoryProvider: javax.inject.Provider<AssetRepository>,
    ) {
        fun getQuizzesByBookId(
            bookId: Long,
            sortBy: SortOption = SortOption.TITLE,
        ): Flow<List<QuizEntity>> = quizDao.getQuizzesByBookIdSorted(bookId, sortBy.name)

        fun getQuizzesByCategory(
            category: String,
            sortBy: SortOption = SortOption.TITLE,
        ): Flow<List<QuizEntity>> =
            quizDao.getQuizzesByCategorySorted(
                category,
                sortBy.name,
            )

        fun getAllQuizzesFlow(): Flow<List<QuizEntity>> = quizDao.getAllQuizzesFlow()

        fun getAllCategories(): Flow<List<String>> =
            combine(
                quizDao.getAllCategories(),
                questionCategoryDao.getAllQuestionCategories(),
            ) { quizCategories, questionCategories ->
                (quizCategories + questionCategories).filter { it.isNotBlank() }.distinct().sorted()
            }

        suspend fun getQuizById(id: Long) = quizDao.getQuizById(id)

        suspend fun insertQuiz(quiz: QuizEntity): Long {
            val finalQuiz =
                if (quiz.coverImage?.startsWith("http", ignoreCase = true) == true) {
                    val localPath = fileManager.downloadAndSaveImage(quiz.coverImage!!)
                    if (localPath != null) quiz.copy(coverImage = localPath) else quiz
                } else {
                    quiz
                }

            val saved =
                finalQuiz.copy(
                    createdAt = if (finalQuiz.createdAt == 0L) System.currentTimeMillis() else finalQuiz.createdAt,
                    updatedAt = System.currentTimeMillis(),
                )
            val id = quizDao.insertQuiz(saved)
            assetRepositoryProvider.get().replaceOwnerAssetReferences("quiz", id, listOf(saved.coverImage))
            bookRepositoryProvider.get().refreshBookStats(finalQuiz.bookId)
            return id
        }

        suspend fun updateQuiz(quiz: QuizEntity) {
            val updated = quiz.copy(updatedAt = System.currentTimeMillis(), lastEditedAt = System.currentTimeMillis())
            quizDao.updateQuiz(updated)
            assetRepositoryProvider.get().replaceOwnerAssetReferences("quiz", updated.id, listOf(updated.coverImage))
            bookRepositoryProvider.get().refreshBookStats(quiz.bookId)
        }

        // Questions

        suspend fun deleteQuiz(quiz: QuizEntity): QuizEntity {
            if (quiz.isSystem) throw IllegalStateException("Cannot delete system quiz")
            val now = System.currentTimeMillis()
            assetRepositoryProvider.get().softDeleteQuizAnnotationTree(quiz.id, now)
            sessionDao.softDeleteSessionsByQuizId(quiz.id, now)
            questionDao.softDeleteQuestionsByQuizId(quiz.id, now)
            quizDao.softDeleteQuizById(quiz.id, now)
            bookRepositoryProvider.get().refreshBookStats(quiz.bookId)
            return quiz.copy(deletedAt = now, updatedAt = now)
        }

        suspend fun restoreQuiz(quizId: Long) {
            val quiz = quizDao.getQuizByIdIncludingDeleted(quizId) ?: return
            val book = bookDao.getBookByIdIncludingDeleted(quiz.bookId)
            if (book != null && book.deletedAt != null) {
                bookRepositoryProvider.get().restoreBook(book.id)
            }
            val now = System.currentTimeMillis()
            val deletedAtFilter = quiz.deletedAt ?: now
            quizDao.restoreQuizById(quizId, now)
            questionDao.restoreQuestionsByQuizId(quizId, now, deletedAtFilter)
            sessionDao.restoreSessionsByQuizId(quizId, now, deletedAtFilter)
            assetRepositoryProvider.get().restoreOwnerAnnotations("quiz", quizId, now)
            bookRepositoryProvider.get().refreshBookStats(quiz.bookId)
        }

        suspend fun permanentlyDeleteQuiz(quiz: QuizEntity): QuizEntity {
            if (quiz.isSystem) throw IllegalStateException("Cannot delete system quiz")
            assetRepositoryProvider.get().permanentlyDeleteQuizAnnotationTree(quiz.id)
            assetRepositoryProvider.get().releaseQuizTreeAssets(quiz)
            quizDao.hardDeleteQuiz(quiz)
            bookRepositoryProvider.get().refreshBookStats(quiz.bookId)
            return quiz
        }

        suspend fun previewQuizDeletion(quizId: Long) = deletePreviewService?.previewQuizDeletion(quizId)

        suspend fun previewCategoryMerge(
            source: String,
            target: String,
        ) = categoryMergePreviewService?.previewMerge(source, target)

        suspend fun clearMarksForQuizWithPreview(quizId: Long): ChangeSimulationResult {
            return clearMarksPreviewService?.previewClearMarksForQuiz(quizId)
                ?: ChangeSimulationResult("Clear Marks", "Preview unavailable")
        }

        suspend fun applyClearMarksForQuiz(quizId: Long) {
            questionDao.clearMarksForQuiz(quizId, System.currentTimeMillis())
        }

        suspend fun refreshQuizStats(quizId: Long) {
            quizDao.refreshQuestionCount(quizId)

            val questions = questionDao.getQuestionsByQuizId(quizId).first()
            val total = questions.size
            val answered = questions.count { it.attempts > 0 }
            val completion = if (total == 0) 0f else answered.toFloat() / total

            val totalAttempts = questions.sumOf { it.attempts }
            val totalCorrect = questions.sumOf { it.correctCount }
            val accuracy = if (totalAttempts == 0) 0f else totalCorrect.toFloat() / totalAttempts

            quizDao.updateCompletionPercentage(quizId, completion)
            quizDao.updateAnsweredCount(quizId, answered)
            quizDao.updateTotalAttempts(quizId, totalAttempts)
            quizDao.updateAccuracyPercentage(quizId, accuracy)

            // Propagate to book
            val quiz = quizDao.getQuizById(quizId) ?: return
            bookRepositoryProvider.get().refreshBookStats(quiz.bookId)
        }

        fun getQuestionsByQuizId(quizId: Long): Flow<List<QuestionEntity>> = questionDao.getQuestionsByQuizId(quizId)

        fun searchAllQuestions(query: String): Flow<List<QuestionEntity>> = questionDao.searchAllQuestionsFlow(query)

        fun getQuestionsByCategoryFlow(category: String): Flow<List<QuestionEntity>> =
            questionCategoryDao.getQuestionsByCategoryFlow(
                category,
            )

        suspend fun getQuestionsByCategory(category: String): List<QuestionEntity> = questionCategoryDao.getQuestionsByCategory(category)

        suspend fun getQuestionById(id: Long) = questionDao.getQuestionById(id)

        suspend fun getQuestionsByIds(ids: List<Long>) = questionDao.getQuestionsByIds(ids)

        suspend fun insertQuestion(question: QuestionEntity): Long {
            val finalQuestion =
                if (question.imagePath?.startsWith("http", ignoreCase = true) == true) {
                    val localPath = fileManager.downloadAndSaveImage(question.imagePath!!)
                    if (localPath != null) question.copy(imagePath = localPath) else question
                } else {
                    question
                }

            val saved =
                finalQuestion.copy(
                    createdAt = if (finalQuestion.createdAt == 0L) System.currentTimeMillis() else finalQuestion.createdAt,
                    updatedAt = System.currentTimeMillis(),
                )
            val id = questionDao.insertQuestion(saved)
            assetRepositoryProvider.get().syncQuestionCategories(id, saved.categories)
            assetRepositoryProvider.get().replaceOwnerAssetReferences("question", id, listOf(saved.imagePath))
            refreshQuizStats(finalQuestion.quizId)
            return id
        }

        suspend fun insertQuestions(questions: List<QuestionEntity>): List<Long> {
            val now = System.currentTimeMillis()
            val updatedQuestions =
                questions.map { question ->
                    val finalQuestion =
                        if (question.imagePath?.startsWith("http", ignoreCase = true) == true) {
                            val localPath = fileManager.downloadAndSaveImage(question.imagePath!!)
                            if (localPath != null) question.copy(imagePath = localPath) else question
                        } else {
                            question
                        }

                    finalQuestion.copy(
                        createdAt = if (finalQuestion.createdAt == 0L) now else finalQuestion.createdAt,
                        updatedAt = now,
                    )
                }
            val ids = questionDao.insertQuestions(updatedQuestions)
            ids.zip(updatedQuestions).forEach { (id, question) ->
                assetRepositoryProvider.get().syncQuestionCategories(id, question.categories)
                assetRepositoryProvider.get().replaceOwnerAssetReferences("question", id, listOf(question.imagePath))
            }
            questions.asSequence().map { it.quizId }.distinct().forEach { refreshQuizStats(it) }
            return ids
        }

        suspend fun updateQuestion(question: QuestionEntity) {
            val updated = question.copy(updatedAt = System.currentTimeMillis(), lastEditedAt = System.currentTimeMillis())
            questionDao.updateQuestion(updated)
            assetRepositoryProvider.get().syncQuestionCategories(updated.id, updated.categories)
            assetRepositoryProvider.get().replaceOwnerAssetReferences("question", updated.id, listOf(updated.imagePath))
            refreshQuizStats(question.quizId)
        }

        suspend fun deleteQuestion(question: QuestionEntity) {
            val now = System.currentTimeMillis()
            assetRepositoryProvider.get().softDeleteQuestionAnnotationTree(question.id, now)
            questionAssetDao.softDeleteAssetsForQuestion(question.id, now)
            questionDao.softDeleteQuestionById(question.id, now)
            refreshQuizStats(question.quizId)
        }

        suspend fun restoreQuestion(questionId: Long) {
            val now = System.currentTimeMillis()
            questionDao.restoreQuestionById(questionId, now)
            assetRepositoryProvider.get().restoreOwnerAnnotations(AnnotationOwnerType.QUESTION, questionId, now)
        }

        suspend fun permanentlyDeleteQuestion(question: QuestionEntity) {
            assetRepositoryProvider.get().permanentlyDeleteQuestionAnnotationTree(question.id)
            assetRepositoryProvider.get().releaseQuestionAssets(question)
            questionCategoryDao.deleteCategoriesForQuestion(question.id)
            questionDao.hardDeleteQuestion(question)
            refreshQuizStats(question.quizId)
        }

        suspend fun updateQuestionMark(
            questionId: Long,
            isMarked: Boolean,
            reason: String? = null,
        ) {
            val question = questionDao.getQuestionById(questionId) ?: return
            val now = System.currentTimeMillis()
            questionDao.updateQuestion(
                question.copy(
                    isMarked = isMarked,
                    markedAt = if (isMarked) now else null,
                    markReason = if (isMarked) reason else null,
                    updatedAt = now,
                    lastEditedAt = now,
                ),
            )
        }

        suspend fun updateQuestionDrop(
            questionId: Long,
            isDropped: Boolean,
            reason: String? = null,
        ) {
            val question = questionDao.getQuestionById(questionId) ?: return
            val now = System.currentTimeMillis()
            questionDao.updateQuestion(
                question.copy(
                    isDropped = isDropped,
                    droppedAt = if (isDropped) now else null,
                    droppedReason = if (isDropped) reason else null,
                    updatedAt = now,
                    lastEditedAt = now,
                ),
            )
        }

        suspend fun updateCategoryMetadata(metadata: CategoryMetadataEntity) = categoryMetadataDao.insertMetadata(metadata)

        suspend fun clearAllCategories() {
            val now = System.currentTimeMillis()
            quizDao.clearAllQuizCategories(now)
            questionDao.clearAllQuestionCategories(now)
            questionCategoryDao.clearAllCategories()
            categoryMetadataDao.deleteAllMetadata()
        }

        suspend fun deleteCategory(category: String) {
            // 1. Clear category from quizzes
            val quizzes = quizDao.getQuizzesByCategory(category).first()
            quizzes.forEach { quiz ->
                if (quiz.category == category) {
                    quizDao.updateQuiz(quiz.copy(category = null))
                }
            }

            // 2. Remove category from questions
            val questions = questionCategoryDao.getQuestionsByCategory(category)
            val updatedQuestions =
                questions.map { question ->
                    question.copy(categories = question.categories.filter { it != category })
                }
            questionDao.updateQuestions(updatedQuestions)

            // 3. Delete metadata
            categoryMetadataDao.deleteMetadataByName(category)
        }

        fun getCategoryMetadata() = categoryMetadataDao.getAllMetadata()

        fun getAllCategoriesWithMetadata(): Flow<List<CategoryWithMetadata>> =
            combine(
                questionDao.getAllQuestionsFlow(),
                quizDao.getAllQuizzesFlow(),
                categoryMetadataDao.getAllMetadata(),
            ) { allQuestions, allQuizzes, metadataList ->
                val quizCategoryMap = allQuizzes.associateBy({ it.id }) { it.category }
                val metadataMap = metadataList.associateBy { it.name }

                // Map to store running stats per category
                class CatStats {
                    var qCount = 0
                    var answered = 0
                    var attempts = 0L
                    var correct = 0L
                    var lastEditedAt = 0L
                }
                val categoryStats = mutableMapOf<String, CatStats>()

                allQuestions.forEach { question ->
                    // Collect unique categories for this question to avoid double-counting if
                    // it's in both question.categories and quizCategoryMap
                    val uniqueCats = mutableSetOf<String>()
                    question.categories.forEach { if (it.isNotBlank()) uniqueCats.add(it) }
                    quizCategoryMap[question.quizId]?.let { if (it.isNotBlank()) uniqueCats.add(it) }

                    uniqueCats.forEach { cat ->
                        val stats = categoryStats.getOrPut(cat) { CatStats() }
                        stats.qCount++
                        if (question.attempts > 0) stats.answered++
                        stats.attempts += question.attempts
                        stats.correct += question.correctCount
                        stats.lastEditedAt = maxOf(stats.lastEditedAt, question.lastEditedAt)
                    }
                }

                // Ensure categories from quizzes with no questions are included
                allQuizzes.forEach { quiz ->
                    quiz.category?.let { cat ->
                        if (cat.isNotBlank()) {
                            categoryStats.getOrPut(cat) { CatStats() }
                        }
                    }
                }

                categoryStats.map { (name, stats) ->
                    val accuracy = if (stats.attempts == 0L) 0f else stats.correct.toFloat() / stats.attempts

                    CategoryWithMetadata(
                        name = name,
                        questionCount = stats.qCount,
                        answeredCount = stats.answered,
                        accuracyPercentage = accuracy,
                        lastEditedAt = stats.lastEditedAt,
                        metadata = metadataMap[name],
                    )
                }.sortedWith(compareByDescending<CategoryWithMetadata> { it.isPinned }.thenBy { it.name })
            }.flowOn(Dispatchers.Default)

        fun getCategoryQuestionCount(category: String): Flow<Int> = questionCategoryDao.getQuestionsByCategoryFlow(category).map { it.size }

        private suspend fun updateQuestionsInPlace(questions: List<QuestionEntity>) {
            questionDao.updateQuestions(questions)
            questions.forEach { questionCategoryDao.replaceCategories(it.id, it.categories) }
        }

        suspend fun renameCategory(
            oldName: String,
            newName: String,
        ) {
            // 1. Update quizzes
            val quizzes = quizDao.getQuizzesByCategory(oldName).first()
            quizzes.forEach { quiz ->
                if (quiz.category == oldName) {
                    quizDao.updateQuiz(quiz.copy(category = newName))
                }
            }

            // 2. Update questions
            val questions = questionCategoryDao.getQuestionsByCategory(oldName)
            val updatedQuestions =
                questions.map { question ->
                    val newCategories = question.categories.map { if (it == oldName) newName else it }.distinct()
                    question.copy(categories = newCategories)
                }
            updateQuestionsInPlace(updatedQuestions)

            // 3. Migrate metadata
            val oldMetadata = categoryMetadataDao.getMetadataForCategory(oldName)
            if (oldMetadata != null) {
                val newMetadata = oldMetadata.copy(name = newName)
                categoryMetadataDao.insertMetadata(newMetadata)
            }
            categoryMetadataDao.deleteMetadataByName(oldName)
        }

        suspend fun mergeCategory(
            sourceCategory: String,
            targetCategory: String,
        ) {
            if (sourceCategory == targetCategory) return

            // 1. Update quizzes
            val quizzes = quizDao.getQuizzesByCategory(sourceCategory).first()
            quizzes.forEach { quiz ->
                if (quiz.category == sourceCategory) {
                    quizDao.updateQuiz(quiz.copy(category = targetCategory))
                }
            }

            // 2. Update questions
            val questions = questionCategoryDao.getQuestionsByCategory(sourceCategory)
            val updatedQuestions =
                questions.map { question ->
                    val newCategories =
                        question.categories.map {
                            if (it == sourceCategory) targetCategory else it
                        }.distinct()
                    question.copy(categories = newCategories)
                }
            updateQuestionsInPlace(updatedQuestions)

            // 3. Update metadata: Keep source's metadata (color/emoji/isPinned) if target has none, else target's wins.
            val sourceMeta = categoryMetadataDao.getMetadataForCategory(sourceCategory)
            val targetMeta = categoryMetadataDao.getMetadataForCategory(targetCategory)
            if (sourceMeta != null && targetMeta == null) {
                categoryMetadataDao.insertMetadata(sourceMeta.copy(name = targetCategory))
            }
            categoryMetadataDao.deleteMetadataByName(sourceCategory)
        }

        suspend fun getMergePreview(
            source: String,
            target: String,
        ): Int {
            val questionsFromSource = questionCategoryDao.getQuestionsByCategory(source)
            val questionsInTarget = questionCategoryDao.getQuestionsByCategory(target).asSequence().map { it.id }.toSet()
            // Count questions that will be moved and are NOT already in target
            return questionsFromSource.count { it.id !in questionsInTarget }
        }

        suspend fun getSessionById(id: Long) = sessionDao.getSessionById(id)

        suspend fun insertSession(session: SessionEntity): Long {
            val id =
                sessionDao.insertSession(
                    session.copy(
                        createdAt = if (session.createdAt == 0L) System.currentTimeMillis() else session.createdAt,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            bookRepositoryProvider.get().updateLastStudied(session.quizId)
            return id
        }

        suspend fun updateSession(session: SessionEntity) {
            sessionDao.updateSession(session.copy(updatedAt = System.currentTimeMillis()))
            if (session.isCompleted) {
                refreshQuizStats(session.quizId)
            }
            bookRepositoryProvider.get().updateLastStudied(session.quizId)
        }

        suspend fun deleteSession(session: SessionEntity) = sessionDao.softDeleteSessionById(session.id, System.currentTimeMillis())

        suspend fun createQuizFromCategory(
            category: String,
            title: String,
            bookId: Long,
        ): Long {
            val quizId =
                quizDao.insertQuiz(
                    QuizEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        bookId = bookId,
                        title = title,
                        description = "Quiz created from category: $category",
                        category = category,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )

            val questions = questionCategoryDao.getQuestionsByCategory(category)
            val duplicatedQuestions =
                questions.map { q ->
                    q.copy(
                        id = 0, // New ID for new quiz
                        externalId = java.util.UUID.randomUUID().toString(),
                        quizId = quizId,
                        attempts = 0,
                        correctCount = 0,
                    )
                }
            insertQuestions(duplicatedQuestions)
            refreshQuizStats(quizId)
            return quizId
        }

        // Category Metadata

        fun getDeletedQuizzes(workspaceId: Long): Flow<List<QuizEntity>> = quizDao.getDeletedQuizzesByWorkspaceFlow(workspaceId)

        fun getQuizCompletion(quizId: Long): Flow<Float> =
            combine(
                quizDao.getQuestionCount(quizId),
                sessionDao.getLatestSessionForQuiz(quizId),
            ) { total, session ->
                when {
                    total == 0 -> 0f
                    session == null -> 0f
                    session.isCompleted -> 1f
                    else -> {
                        val answered = session.answers.size
                        (answered.toFloat() / total).coerceIn(0f, 1f)
                    }
                }
            }

        // --- Flashcard decks and cards ---

        suspend fun getQuizKnowledgeSummary(quizId: Long): QuizKnowledgeSummary {
            val allQuestions = questionDao.getQuestionsByQuizId(quizId).first()
            val questions = allQuestions.filter { !it.isDropped }
            val assets = questionAssetDao.getAssetsByQuizId(quizId).first()
            return QuizKnowledgeSummary(
                quizId = quizId,
                totalQuestions = questions.size,
                unansweredQuestions = questions.count { it.attempts == 0 },
                markedQuestions = questions.count { it.isMarked },
                droppedQuestions = allQuestions.count { it.isDropped },
                missedQuestions = questions.count { it.attempts > 0 && (it.correctCount < it.attempts || it.lastAttemptResult == false) },
                questionsWithNotes = questions.count { !it.notes.isNullOrBlank() },
                questionsWithAssets = assets.map { it.questionId }.distinct().size,
                questionsWithSources = assets.filter { it.sourceDocumentId != null }.map { it.questionId }.distinct().size,
            )
        }

        // Sync Logic

        fun getQuizQuestionCount(quizId: Long): Flow<Int> = quizDao.getQuestionCount(quizId)

        suspend fun importCompiledQuestions(
            title: String,
            targetBookId: Long?,
            targetQuizId: Long? = null,
            newBookTitle: String? = null,
            questions: List<ParsedQuestion>,
            activeWorkspaceId: Long? = null,
        ): MksResult<ImportResult>? {
            val mksResult =
                importManager?.importQuestions(
                    title = title,
                    questions = questions,
                    targetBookId = targetBookId,
                    targetQuizId = targetQuizId,
                    newBookTitle = newBookTitle,
                    activeWorkspaceId = activeWorkspaceId,
                )

            if (mksResult is MksResult.Success) {
                val result = mksResult.data
                result.affectedQuizIds.forEach { refreshQuizStats(it) }
                for (id in result.affectedBookIds) {
                    bookRepositoryProvider.get().refreshBookStats(id)
                }
                if (targetQuizId != null && targetQuizId !in result.affectedQuizIds) {
                    refreshQuizStats(targetQuizId)
                }
            }
            return mksResult
        }

        suspend fun getImportPreview(uri: Uri) = importManager?.getImportPreview(uri)

        suspend fun importFromUri(
            uri: Uri,
            strategy: MergeStrategy = MergeStrategy.SKIP_EXISTING,
            targetBookId: Long? = null,
            targetQuizId: Long? = null,
            allowInsecureRemoteImages: Boolean = false,
            activeWorkspaceId: Long? = null,
            onProgress: (Float, String) -> Unit = { _, _ -> },
        ): MksResult<ImportResult>? {
            return importManager?.importLibrary(
                uri = uri,
                strategy = strategy,
                targetBookId = targetBookId,
                targetQuizId = targetQuizId,
                allowInsecureRemoteImages = allowInsecureRemoteImages,
                activeWorkspaceId = activeWorkspaceId,
                onProgress = onProgress,
            )
        }

        fun detectFormat(uri: Uri): ImportFormat {
            return importManager?.detectFormat(uri) ?: ImportFormat.UNKNOWN
        }
    }
