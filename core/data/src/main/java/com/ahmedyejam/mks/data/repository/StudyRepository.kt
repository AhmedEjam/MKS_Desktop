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
import com.ahmedyejam.mks.data.local.entity.AnnotationEntity
import com.ahmedyejam.mks.data.local.entity.AnnotationOwnerType
import com.ahmedyejam.mks.data.local.entity.KnowledgeStudySessionEntity
import com.ahmedyejam.mks.data.local.entity.MistakeLogEntryEntity
import com.ahmedyejam.mks.data.local.entity.SessionEntity
import com.ahmedyejam.mks.data.preview.CategoryMergePreviewService
import com.ahmedyejam.mks.data.preview.ClearMarksPreviewService
import com.ahmedyejam.mks.data.preview.DeletePreviewService
import com.ahmedyejam.mks.data.repair.AssetReferenceAuditService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyRepository
    @Inject
    constructor(
        private val workspaceDao: WorkspaceDao,
        private val bookDao: BookDao,
        private val quizDao: QuizDao,
        private val questionDao: QuestionDao,
        private val quizRepositoryProvider: javax.inject.Provider<QuizRepository>,
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
    ) {
        fun getAllMistakes(): Flow<List<MistakeLogEntryEntity>> = mistakeLogDao.getAllMistakes()

        fun getMistakesByBookId(bookId: Long): Flow<List<MistakeLogEntryEntity>> = mistakeLogDao.getMistakesByBookId(bookId)

        fun getMistakesByQuizId(quizId: Long): Flow<List<MistakeLogEntryEntity>> = mistakeLogDao.getMistakesByQuizId(quizId)

        suspend fun insertMistake(entry: MistakeLogEntryEntity): Long = mistakeLogDao.insertMistake(entry)

        suspend fun updateMistake(entry: MistakeLogEntryEntity) {
            mistakeLogDao.updateMistake(entry.copy(updatedAt = System.currentTimeMillis()))
        }

        // Annotations: polymorphic highlights/margin notes owned by QUESTION, SLIDE, NOTE, SOURCE, or ASSET.

        suspend fun deleteMistake(entry: MistakeLogEntryEntity) {
            mistakeLogDao.softDeleteMistakeById(entry.id, System.currentTimeMillis())
        }

        fun getAnnotationsByWorkspaceId(workspaceId: Long): Flow<List<AnnotationEntity>> =
            annotationDao.getAnnotationsByWorkspaceId(workspaceId)

        fun getAnnotationsByBookId(bookId: Long): Flow<List<AnnotationEntity>> = annotationDao.getAnnotationsByBookId(bookId)

        fun getAnnotationsByOwner(
            ownerType: String,
            ownerId: Long,
        ): Flow<List<AnnotationEntity>> = annotationDao.getAnnotationsByOwner(ownerType, ownerId)

        suspend fun getAnnotationById(annotationId: Long): AnnotationEntity? = annotationDao.getAnnotationById(annotationId)

        suspend fun insertAnnotation(annotation: AnnotationEntity): Long {
            require(annotation.ownerType in AnnotationOwnerType.all) { "Unsupported annotation ownerType: ${annotation.ownerType}" }
            val now = System.currentTimeMillis()
            return annotationDao.insertAnnotation(
                annotation.copy(createdAt = annotation.createdAt.takeIf { it > 0 } ?: now, updatedAt = now),
            )
        }

        suspend fun updateAnnotation(annotation: AnnotationEntity) {
            require(annotation.ownerType in AnnotationOwnerType.all) { "Unsupported annotation ownerType: ${annotation.ownerType}" }
            annotationDao.updateAnnotation(annotation.copy(updatedAt = System.currentTimeMillis()))
        }

        suspend fun autoLogWrongAnswer(
            bookId: Long,
            quizId: Long,
            questionId: Long,
            sessionId: Long?,
            selectedAnswer: String?,
            correctAnswer: String?,
        ): Long {
            val existing = mistakeLogDao.findByQuestionAndSession(questionId, sessionId)
            if (existing != null) return existing.id

            val now = System.currentTimeMillis()
            return mistakeLogDao.insertMistake(
                MistakeLogEntryEntity(
                    bookId = bookId,
                    quizId = quizId,
                    questionId = questionId,
                    sessionId = sessionId,
                    selectedAnswer = selectedAnswer,
                    correctAnswer = correctAnswer,
                    reviewAt = now + 24L * 60L * 60L * 1000L,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

        suspend fun completeLearningSession(sessionId: Long) {
            val session = knowledgeStudySessionDao.getSessionById(sessionId) ?: return
            knowledgeStudySessionDao.updateSession(session.copy(isCompleted = true, updatedAt = System.currentTimeMillis()))
        }

        suspend fun createLearningSession(
            targetType: String,
            targetId: Long,
            stateJson: String = "",
        ): Long {
            val now = System.currentTimeMillis()
            val session =
                KnowledgeStudySessionEntity(
                    targetType = targetType,
                    targetId = targetId,
                    stateJson = stateJson,
                    isCompleted = false,
                    createdAt = now,
                    updatedAt = now,
                )
            return knowledgeStudySessionDao.insertSession(session)
        }

        suspend fun deleteLearningSession(session: KnowledgeStudySessionEntity) {
            knowledgeStudySessionDao.softDeleteSessionById(session.id, System.currentTimeMillis())
        }

        suspend fun getActiveSessionByTarget(
            targetType: String,
            targetId: Long,
        ): KnowledgeStudySessionEntity? = knowledgeStudySessionDao.getActiveSessionByTarget(targetType, targetId)

        suspend fun getAdaptiveQuestionsByBook(
            bookId: Long,
            limit: Int,
        ) = questionDao.getAdaptiveQuestionsByBook(bookId, limit)

        // Sessions

        suspend fun getLearningSessionById(sessionId: Long): KnowledgeStudySessionEntity? =
            knowledgeStudySessionDao.getSessionById(
                sessionId,
            )

        fun getLearningSessionsByTarget(
            targetType: String,
            targetId: Long,
        ): Flow<List<KnowledgeStudySessionEntity>> = knowledgeStudySessionDao.getSessionsByTarget(targetType, targetId)

        fun getLearningSessionsByTargetType(targetType: String): Flow<List<KnowledgeStudySessionEntity>> =
            knowledgeStudySessionDao.getSessionsByTargetType(targetType)

        fun getSessionsByQuizId(quizId: Long): Flow<List<SessionEntity>> = sessionDao.getSessionsByQuizId(quizId)

        suspend fun markMistakeFixed(mistakeId: Long) {
            mistakeLogDao.markFixed(mistakeId, System.currentTimeMillis())
        }

        suspend fun permanentlyDeleteLearningSession(session: KnowledgeStudySessionEntity) {
            knowledgeStudySessionDao.hardDeleteSession(session)
        }

        suspend fun permanentlyDeleteMistake(entry: MistakeLogEntryEntity) {
            mistakeLogDao.hardDeleteMistake(entry)
        }

        suspend fun permanentlyDeleteSession(session: SessionEntity) = sessionDao.hardDeleteSession(session)

        suspend fun restoreLearningSession(sessionId: Long) {
            knowledgeStudySessionDao.restoreSessionById(sessionId, System.currentTimeMillis())
        }

        suspend fun restoreMistake(mistakeId: Long) {
            mistakeLogDao.restoreMistakeById(mistakeId, System.currentTimeMillis())
        }

        suspend fun restoreSession(sessionId: Long) = sessionDao.restoreSessionById(sessionId, System.currentTimeMillis())

        suspend fun snoozeMistake(
            mistakeId: Long,
            reviewAt: Long,
        ) {
            mistakeLogDao.snooze(mistakeId, reviewAt, System.currentTimeMillis())
        }

        suspend fun updateLearningSession(session: KnowledgeStudySessionEntity) {
            knowledgeStudySessionDao.updateSession(session.copy(updatedAt = System.currentTimeMillis()))
        }

        suspend fun updateQuestionMetrics(
            id: Long,
            isCorrect: Boolean,
            timeSpentMs: Long = 0,
        ) {
            val question = questionDao.getQuestionById(id) ?: return
            val now = System.currentTimeMillis()

            questionDao.updatePerformanceMetrics(
                id = id,
                isCorrect = isCorrect,
                isCorrectInt = if (isCorrect) 1 else 0,
                timeSpentMs = timeSpentMs,
                now = now,
            )

            quizRepositoryProvider.get().refreshQuizStats(question.quizId)
        }

        suspend fun getLibraryKnowledgeSummary(): KnowledgeSummary {
            val now = System.currentTimeMillis()
            return KnowledgeSummary(
                totalBooks = bookDao.countAll(),
                totalQuizzes = quizDao.countAll(),
                totalQuestions = questionDao.countAll(),
                unansweredQuestions = questionDao.countUnanswered(),
                questionsWithNotes = questionDao.countWithNotes(),
                questionsWithAssets = questionAssetDao.countDistinctQuestionsWithAssets(),
                questionsWithSources = questionAssetDao.countDistinctQuestionsWithSources(),
                markedQuestions = questionDao.countMarked(),
                droppedQuestions = questionDao.countDropped(),
                missedQuestions = questionDao.countMissed(),
                weakQuestions = questionDao.countWeak(),
                flashcardDecks = flashcardDeckDao.countAll(),
                totalFlashcards = flashcardDao.countAll(),
                dueFlashcards = flashcardDao.countDueFlashcards(now),
                weakFlashcards = flashcardDao.countWeakFlashcards(),
                totalBlueprints = noteBlueprintDao.countAll(),
                blueprintsDueForReview = noteBlueprintDao.countDueBlueprints(now),
                linkedBlueprints = noteBlueprintDao.countLinkedBlueprints(),
                promptDecks = promptDeckDao.countAll(),
                promptCards = promptCardDao.countAll(),
                promptRuns = promptRunDao.countAll(),
                savedPromptOutputs = promptRunDao.countSavedOutputs(),
                openMistakes = mistakeLogDao.countOpenMistakes(),
                fixedMistakes = mistakeLogDao.countFixedMistakes(),
                mistakesDueForReview = mistakeLogDao.countDueMistakes(now),
            )
        }

        // Question assets / media attachments
    }
