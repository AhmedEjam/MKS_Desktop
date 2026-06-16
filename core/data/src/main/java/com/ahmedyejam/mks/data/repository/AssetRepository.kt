package com.ahmedyejam.mks.data.repository

import android.net.Uri
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
import com.ahmedyejam.mks.data.local.entity.AnnotationColorLabel
import com.ahmedyejam.mks.data.local.entity.AnnotationEntity
import com.ahmedyejam.mks.data.local.entity.AnnotationOwnerType
import com.ahmedyejam.mks.data.local.entity.AssetReferenceEntity
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.QuestionAssetEntity
import com.ahmedyejam.mks.data.local.entity.QuestionAssetType
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.local.entity.SourceDocumentEntity
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.data.preview.CategoryMergePreviewService
import com.ahmedyejam.mks.data.preview.ClearMarksPreviewService
import com.ahmedyejam.mks.data.preview.DeletePreviewService
import com.ahmedyejam.mks.data.repair.AssetReferenceAuditService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRepository
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
    ) {
        fun getQuestionAssets(questionId: Long): Flow<List<QuestionAssetEntity>> = questionAssetDao.getAssetsByQuestionId(questionId)

        suspend fun deleteQuestionAsset(asset: QuestionAssetEntity) {
            val now = System.currentTimeMillis()
            softDeleteOwnerAnnotations(AnnotationOwnerType.ASSET, asset.id, now)
            questionAssetDao.softDeleteAssetById(asset.id, now)
        }

        suspend fun replaceOwnerAssetReferences(
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

        suspend fun releaseOwnerAssets(
            ownerType: String,
            ownerId: Long,
        ) {
            val references = assetReferenceDao.getReferencesForOwner(ownerType, ownerId)
            assetReferenceDao.deleteReferencesForOwner(ownerType, ownerId)
            references.map { it.path }.distinct().forEach { path ->
                if (assetReferenceDao.countReferencesForPath(path) == 0) {
                    fileManager.deleteImage(path)
                }
            }
        }

        suspend fun softDeleteOwnerAnnotations(
            ownerType: String,
            ownerId: Long,
            deletedAt: Long,
        ) {
            if (ownerType in AnnotationOwnerType.all) {
                annotationDao.softDeleteAnnotationsByOwner(ownerType, ownerId, deletedAt)
            }
        }

        suspend fun restoreOwnerAnnotations(
            ownerType: String,
            ownerId: Long,
            updatedAt: Long,
        ) {
            if (ownerType in AnnotationOwnerType.all) {
                annotationDao.restoreAnnotationsByOwner(ownerType, ownerId, updatedAt)
            }
        }

        suspend fun permanentlyDeleteOwnerAnnotations(
            ownerType: String,
            ownerId: Long,
        ) {
            if (ownerType in AnnotationOwnerType.all) {
                annotationDao.permanentlyDeleteAnnotationsByOwner(ownerType, ownerId)
            }
        }

        suspend fun softDeleteQuestionAnnotationTree(
            questionId: Long,
            deletedAt: Long,
        ) {
            questionAssetDao.getAssetsByQuestionIdNow(questionId).forEach { asset ->
                softDeleteOwnerAnnotations(AnnotationOwnerType.ASSET, asset.id, deletedAt)
            }
            softDeleteOwnerAnnotations(AnnotationOwnerType.QUESTION, questionId, deletedAt)
        }

        suspend fun permanentlyDeleteQuestionAnnotationTree(questionId: Long) {
            questionAssetDao.getAssetsByQuestionIdIncludingDeleted(questionId).forEach { asset ->
                permanentlyDeleteOwnerAnnotations(AnnotationOwnerType.ASSET, asset.id)
            }
            permanentlyDeleteOwnerAnnotations(AnnotationOwnerType.QUESTION, questionId)
        }

        suspend fun softDeleteQuizAnnotationTree(
            quizId: Long,
            deletedAt: Long,
        ) {
            questionDao.getQuestionsByQuizIdNow(quizId).forEach { question ->
                softDeleteQuestionAnnotationTree(question.id, deletedAt)
            }
        }

        suspend fun permanentlyDeleteQuizAnnotationTree(quizId: Long) {
            questionDao.getQuestionsByQuizIdIncludingDeleted(quizId).forEach { question ->
                permanentlyDeleteQuestionAnnotationTree(question.id)
            }
        }

        suspend fun softDeleteSlideshowAnnotationTree(
            courseId: Long,
            deletedAt: Long,
        ) {
            courseSlideDao.getSlidesByCourseIdNow(courseId).forEach { slide ->
                softDeleteOwnerAnnotations(AnnotationOwnerType.SLIDE, slide.id, deletedAt)
            }
        }

        suspend fun restoreSlideshowAnnotationTree(
            courseId: Long,
            updatedAt: Long,
        ) {
            courseSlideDao.getSlidesByCourseIdNow(courseId).forEach { slide ->
                restoreOwnerAnnotations(AnnotationOwnerType.SLIDE, slide.id, updatedAt)
            }
        }

        suspend fun permanentlyDeleteSlideshowAnnotationTree(courseId: Long) {
            courseSlideDao.getSlidesByCourseIdIncludingDeleted(courseId).forEach { slide ->
                permanentlyDeleteOwnerAnnotations(AnnotationOwnerType.SLIDE, slide.id)
            }
        }

        suspend fun syncQuestionCategories(
            questionId: Long,
            categories: List<String>,
        ) {
            questionCategoryDao.replaceCategories(questionId, categories)
        }

        suspend fun releaseQuestionAssets(question: QuestionEntity) {
            questionAssetDao.getAssetsByQuestionIdIncludingDeleted(question.id).forEach { asset ->
                releaseOwnerAssets("question_asset", asset.id)
            }
            releaseOwnerAssets("question", question.id)
        }

        suspend fun releaseQuizTreeAssets(quiz: QuizEntity) {
            releaseOwnerAssets("quiz", quiz.id)
            questionDao.getQuestionsByQuizIdIncludingDeleted(quiz.id).forEach { releaseQuestionAssets(it) }
        }

        suspend fun releaseBookTreeAssets(book: BookEntity) {
            releaseOwnerAssets("book", book.id)
            quizDao.getQuizzesByBookId(book.id).first().forEach { releaseQuizTreeAssets(it) }
            flashcardDeckDao.getFlashcardDecksByBookId(book.id).first().forEach { deck ->
                releaseOwnerAssets("flashcard_deck", deck.id)
                flashcardDao.getFlashcardsByDeckId(deck.id).first().forEach { card ->
                    releaseOwnerAssets("flashcard", card.id)
                }
            }
            slideshowCourseDao.getCoursesByBookId(book.id).first().forEach { course ->
                releaseOwnerAssets("slideshow_course", course.id)
                courseSlideDao.getSlidesByCourseId(course.id).first().forEach { slide ->
                    releaseOwnerAssets("course_slide", slide.id)
                }
            }
            sourceDocumentDao.getSourcesByBookIdNow(book.id).forEach { source ->
                releaseOwnerAssets("source_document", source.id)
            }
        }

        suspend fun appendBlueprintToSourceQuestionNote(noteId: Long) {
            val note = noteBlueprintDao.getNoteById(noteId) ?: return
            val questionId = note.sourceQuestionId ?: parseLongList(note.linkedQuestionsJson).firstOrNull() ?: return
            val question = questionDao.getQuestionById(questionId) ?: return
            val appended =
                buildString {
                    question.notes?.takeIf { it.isNotBlank() }?.let {
                        append(it.trim())
                        append("\n\n")
                    }
                    append("Blueprint: ")
                    append(note.title)
                    append("\n")
                    append(note.body.trim())
                }
            updateQuestion(question.copy(notes = appended))
        }

        suspend fun rebuildDerivedIndexes() {
            questionCategoryDao.clearAllCategories()
            questionDao.getAllQuestionsFlow().first().forEach { question ->
                syncQuestionCategories(question.id, question.categories)
            }

            assetReferenceDao.clearAllReferences()
            bookDao.getAllBooksFlow().first().forEach { book ->
                replaceOwnerAssetReferences("book", book.id, listOf(book.coverImage))
                quizDao.getQuizzesByBookId(book.id).first().forEach { quiz ->
                    replaceOwnerAssetReferences("quiz", quiz.id, listOf(quiz.coverImage))
                    questionDao.getQuestionsByQuizId(quiz.id).first().forEach { question ->
                        replaceOwnerAssetReferences("question", question.id, listOf(question.imagePath))
                        questionAssetDao.getAssetsByQuestionIdNow(question.id).forEach { asset ->
                            replaceOwnerAssetReferences("question_asset", asset.id, listOf(asset.localPath))
                        }
                    }
                }
            }
            sourceDocumentDao.getAllSourcesIncludingDeleted().forEach { source ->
                replaceOwnerAssetReferences("source_document", source.id, listOf(source.localPath))
            }
        }

        suspend fun auditAssetReferences() = assetReferenceAuditService?.audit()

        suspend fun cleanupDeletedAnnotationsOlderThan(olderThan: Long): Int = annotationDao.cleanupDeletedAnnotationsOlderThan(olderThan)

        suspend fun createAnnotationForOwner(
            bookId: Long,
            ownerType: String,
            ownerId: Long,
            selectedText: String? = null,
            noteBody: String? = null,
            colorLabel: String = AnnotationColorLabel.YELLOW,
            positionDataJson: String? = null,
        ): Long {
            require(ownerType in AnnotationOwnerType.all) { "Unsupported annotation ownerType: $ownerType" }
            val workspaceId = bookDao.getBookById(bookId)?.workspaceId ?: getOrCreateDefaultWorkspace().id
            val now = System.currentTimeMillis()
            return annotationDao.insertAnnotation(
                AnnotationEntity(
                    workspaceId = workspaceId,
                    bookId = bookId,
                    ownerType = ownerType,
                    ownerId = ownerId,
                    selectedText = selectedText?.trim()?.takeIf { it.isNotBlank() },
                    noteBody = noteBody?.trim()?.takeIf { it.isNotBlank() },
                    colorLabel = colorLabel.ifBlank { AnnotationColorLabel.YELLOW },
                    positionDataJson = positionDataJson?.trim()?.takeIf { it.isNotBlank() },
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

        suspend fun createSourceDocumentAndQuestionAsset(
            source: SourceDocumentEntity,
            asset: QuestionAssetEntity,
        ): Long {
            val sourceId = insertSourceDocument(source)
            return insertQuestionAsset(asset.copy(sourceDocumentId = sourceId, assetType = QuestionAssetType.SOURCE_REFERENCE))
        }

        suspend fun deleteSourceDocument(source: SourceDocumentEntity) {
            val now = System.currentTimeMillis()
            softDeleteOwnerAnnotations(AnnotationOwnerType.SOURCE, source.id, now)
            sourceDocumentDao.softDeleteSourceById(source.id, now)
        }

        suspend fun getQuestionAssetsNow(questionId: Long): List<QuestionAssetEntity> =
            questionAssetDao.getAssetsByQuestionIdNow(
                questionId,
            )

        fun getQuestionIdsWithAssetsFlow(): Flow<List<Long>> = questionAssetDao.getQuestionIdsWithAssetsFlow()

        fun getQuestionIdsWithAssetsForQuizFlow(quizId: Long): Flow<List<Long>> =
            questionAssetDao.getQuestionIdsWithAssetsForQuizFlow(
                quizId,
            )

        suspend fun getSourceDocumentById(id: Long): SourceDocumentEntity? = sourceDocumentDao.getSourceById(id)

        fun getSourceDocumentsByBookId(bookId: Long): Flow<List<SourceDocumentEntity>> = sourceDocumentDao.getSourcesByBookId(bookId)

        suspend fun getSourceDocumentsByBookIdNow(bookId: Long): List<SourceDocumentEntity> =
            sourceDocumentDao.getSourcesByBookIdNow(
                bookId,
            )

        suspend fun insertQuestionAsset(asset: QuestionAssetEntity): Long {
            val prepared =
                prepareQuestionAsset(asset).copy(
                    createdAt = if (asset.createdAt == 0L) System.currentTimeMillis() else asset.createdAt,
                    updatedAt = System.currentTimeMillis(),
                )
            val id = questionAssetDao.insertAsset(prepared)
            if (prepared.isPrimary) {
                questionAssetDao.clearOtherPrimaryAssets(prepared.questionId, id, System.currentTimeMillis())
            }
            replaceOwnerAssetReferences("question_asset", id, listOf(prepared.localPath))
            return id
        }

        suspend fun insertSourceDocument(source: SourceDocumentEntity): Long {
            val now = System.currentTimeMillis()
            val prepared =
                prepareSourceDocument(source).copy(
                    createdAt = if (source.createdAt == 0L) now else source.createdAt,
                    updatedAt = now,
                )
            val id = sourceDocumentDao.insertSource(prepared)
            replaceOwnerAssetReferences("source_document", id, listOf(prepared.localPath))
            return id
        }

        suspend fun permanentlyDeleteAnnotation(annotationId: Long) = annotationDao.permanentlyDeleteAnnotationById(annotationId)

        suspend fun permanentlyDeleteAnnotationsByOwner(
            ownerType: String,
            ownerId: Long,
        ) {
            require(ownerType in AnnotationOwnerType.all) { "Unsupported annotation ownerType: $ownerType" }
            annotationDao.permanentlyDeleteAnnotationsByOwner(ownerType, ownerId)
        }

        suspend fun permanentlyDeleteQuestionAsset(asset: QuestionAssetEntity) {
            permanentlyDeleteOwnerAnnotations(AnnotationOwnerType.ASSET, asset.id)
            releaseOwnerAssets("question_asset", asset.id)
            questionAssetDao.hardDeleteAsset(asset)
        }

        suspend fun permanentlyDeleteSourceDocument(source: SourceDocumentEntity) {
            permanentlyDeleteOwnerAnnotations(AnnotationOwnerType.SOURCE, source.id)
            questionAssetDao.clearSourceReference(source.id, System.currentTimeMillis())
            releaseOwnerAssets("source_document", source.id)
            sourceDocumentDao.hardDeleteSource(source)
        }

        private fun prepareQuestionAsset(asset: QuestionAssetEntity): QuestionAssetEntity {
            val trimmedLocal = asset.localPath?.trim()?.takeIf { it.isNotBlank() }
            val trimmedUrl = asset.externalUrl?.trim()?.takeIf { it.isNotBlank() }
            val now = System.currentTimeMillis()

            if (trimmedLocal != null && trimmedLocal.startsWith("http", ignoreCase = true)) {
                return asset.copy(
                    localPath = null,
                    externalUrl = trimmedUrl ?: trimmedLocal,
                    updatedAt = now,
                )
            }

            if (trimmedLocal != null && (trimmedLocal.startsWith("content://") || trimmedLocal.startsWith("file://"))) {
                val result = fileManager.copyAssetUriToInternalStorage(trimmedLocal, asset.fileName)
                if (result is MksResult.Success) {
                    val copied = result.data
                    return asset.copy(
                        localPath = copied.path,
                        mimeType = asset.mimeType ?: copied.mimeType,
                        fileName = asset.fileName ?: copied.fileName,
                        fileSizeBytes = asset.fileSizeBytes ?: copied.fileSizeBytes,
                        updatedAt = now,
                    )
                }
            }

            return asset.copy(localPath = trimmedLocal, externalUrl = trimmedUrl, updatedAt = now)
        }

        // Source documents / citations

        private fun prepareSourceDocument(source: SourceDocumentEntity): SourceDocumentEntity {
            val trimmedLocal = source.localPath?.trim()?.takeIf { it.isNotBlank() }
            val trimmedUrl = source.externalUrl?.trim()?.takeIf { it.isNotBlank() }
            val now = System.currentTimeMillis()
            if (trimmedLocal != null && trimmedLocal.startsWith("http", ignoreCase = true)) {
                return source.copy(localPath = null, externalUrl = trimmedUrl ?: trimmedLocal, updatedAt = now)
            }
            if (trimmedLocal != null && (trimmedLocal.startsWith("content://") || trimmedLocal.startsWith("file://"))) {
                val result = fileManager.copyAssetUriToInternalStorage(trimmedLocal, source.title)
                if (result is MksResult.Success) {
                    val copied = result.data
                    return source.copy(localPath = copied.path, updatedAt = now)
                }
            }
            return source.copy(localPath = trimmedLocal, externalUrl = trimmedUrl, updatedAt = now)
        }

        private suspend fun registerAssetReference(
            ownerType: String,
            ownerId: Long,
            path: String?,
        ) {
            val assetPath = path?.trim()?.takeIf { isTrackableLocalAsset(it) } ?: return
            assetReferenceDao.insertReference(
                AssetReferenceEntity(
                    path = assetPath,
                    ownerType = ownerType,
                    ownerId = ownerId,
                ),
            )
        }

        private suspend fun releaseAssetReference(
            ownerType: String,
            ownerId: Long,
            path: String?,
        ) {
            val assetPath = path?.trim()?.takeIf { isTrackableLocalAsset(it) } ?: return
            assetReferenceDao.deleteReference(ownerType, ownerId, assetPath)
            if (assetReferenceDao.countReferencesForPath(assetPath) == 0) {
                fileManager.deleteImage(assetPath)
            }
        }

        suspend fun reorderQuestionAssets(assets: List<QuestionAssetEntity>) {
            val now = System.currentTimeMillis()
            assets.forEachIndexed { index, asset ->
                questionAssetDao.updateAssetOrder(asset.id, index, now)
            }
        }

        fun resolveImagePath(path: String?): String? {
            if (path.isNullOrBlank()) return null
            if (path.startsWith("http") || path.startsWith("content://") || path.startsWith("assets/") || path.startsWith("data:")) return path

            // If it's an absolute path, verify it exists and is in the allowed images directory
            if (path.startsWith("/")) {
                return if (fileManager.getFile(path) != null) path else null
            }

            // Assume relative to internal images directory
            val imagesDir = File(fileManager.getContext().filesDir, "images")
            val file = File(imagesDir, path)
            return if (file.exists()) file.absolutePath else null
        }

        suspend fun restoreAnnotation(annotationId: Long) = annotationDao.restoreAnnotationById(annotationId, System.currentTimeMillis())

        suspend fun restoreAnnotationsByOwner(
            ownerType: String,
            ownerId: Long,
        ) {
            require(ownerType in AnnotationOwnerType.all) { "Unsupported annotation ownerType: $ownerType" }
            annotationDao.restoreAnnotationsByOwner(ownerType, ownerId, System.currentTimeMillis())
        }

        suspend fun restoreQuestionAsset(assetId: Long) {
            val now = System.currentTimeMillis()
            questionAssetDao.restoreAssetById(assetId, now)
            restoreOwnerAnnotations(AnnotationOwnerType.ASSET, assetId, now)
        }

        suspend fun restoreSourceDocument(sourceId: Long) {
            val now = System.currentTimeMillis()
            sourceDocumentDao.restoreSourceById(sourceId, now)
            restoreOwnerAnnotations(AnnotationOwnerType.SOURCE, sourceId, now)
        }

        fun saveImage(uri: Uri): String? = fileManager.saveImage(uri)

        suspend fun softDeleteAnnotation(annotationId: Long) =
            annotationDao.softDeleteAnnotationById(
                annotationId,
                System.currentTimeMillis(),
            )

        suspend fun softDeleteAnnotationsByOwner(
            ownerType: String,
            ownerId: Long,
        ) {
            require(ownerType in AnnotationOwnerType.all) { "Unsupported annotation ownerType: $ownerType" }
            annotationDao.softDeleteAnnotationsByOwner(ownerType, ownerId, System.currentTimeMillis())
        }

        suspend fun syncDerivedAssets(questionId: Long) {
            val question = questionDao.getQuestionById(questionId) ?: return

            // Sync Flashcards
            val cards = flashcardDao.getFlashcardsBySourceQuestionId(questionId)
            cards.forEach { card ->
                if (card.syncConfig["text"] == true) {
                    flashcardDao.updateFlashcard(card.copy(frontText = question.text, updatedAt = System.currentTimeMillis()))
                }
            }

            // Sync Slides
            val slides = courseSlideDao.getSlidesBySourceQuestionId(questionId)
            slides.forEach { slide ->
                if (slide.syncConfig["body"] == true) {
                    courseSlideDao.updateSlide(slide.copy(body = question.text, updatedAt = System.currentTimeMillis()))
                }
            }

            // Sync Notes - update timestamp when source question changes
            val notes = noteBlueprintDao.getNotesBySourceQuestionId(questionId)
            notes.forEach { note ->
                noteBlueprintDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            }
        }

        // Learning Sessions Management

        suspend fun updateQuestionAsset(asset: QuestionAssetEntity) {
            val old = questionAssetDao.getAssetById(asset.id)
            val prepared = prepareQuestionAsset(asset).copy(updatedAt = System.currentTimeMillis())
            questionAssetDao.updateAsset(prepared)
            if (prepared.isPrimary) {
                questionAssetDao.clearOtherPrimaryAssets(prepared.questionId, prepared.id, System.currentTimeMillis())
            }
            if (old?.localPath != prepared.localPath) {
                old?.localPath?.let { releaseAssetReference("question_asset", old.id, it) }
            }
            replaceOwnerAssetReferences("question_asset", prepared.id, listOf(prepared.localPath))
        }

        suspend fun updateSourceDocument(source: SourceDocumentEntity) {
            val old = sourceDocumentDao.getSourceById(source.id)
            val prepared = prepareSourceDocument(source).copy(updatedAt = System.currentTimeMillis())
            sourceDocumentDao.updateSource(prepared)
            if (old?.localPath != prepared.localPath) {
                old?.localPath?.let { releaseAssetReference("source_document", old.id, it) }
            }
            replaceOwnerAssetReferences("source_document", prepared.id, listOf(prepared.localPath))
        }

        private fun isTrackableLocalAsset(path: String?): Boolean {
            val value = path?.trim().orEmpty()
            if (value.isBlank()) return false
            return !value.startsWith("http://", ignoreCase = true) &&
                !value.startsWith("https://", ignoreCase = true) &&
                !value.startsWith("content://", ignoreCase = true) &&
                !value.startsWith("data:", ignoreCase = true) &&
                !value.startsWith("file:///android_asset/", ignoreCase = true)
        }

        private fun parseLongList(json: String): List<Long> =
            if (json.isBlank() || json == "[]") {
                emptyList()
            } else {
                json.removePrefix("[").removeSuffix("]").split(",").mapNotNull { it.trim().toLongOrNull() }
            }

    suspend fun updateQuestion(question: QuestionEntity) = questionDao.updateQuestion(question)

        suspend fun getOrCreateDefaultWorkspace(): com.ahmedyejam.mks.data.local.entity.WorkspaceEntity {
            val workspace = workspaceDao.getDefaultWorkspace()
            return if (workspace != null) {
                workspace
            } else {
                val id =
                    workspaceDao.insertWorkspace(
                        com.ahmedyejam.mks.data.local.entity.WorkspaceEntity(
                            name = "Default",
                            isDefault = true,
                            externalId = java.util.UUID.randomUUID().toString(),
                        ),
                    )
                workspaceDao.insertSettings(com.ahmedyejam.mks.data.local.entity.WorkspaceSettingsEntity(workspaceId = id))
                workspaceDao.getWorkspaceById(id) ?: com.ahmedyejam.mks.data.local.entity.WorkspaceEntity(id = id, name = "Default", isDefault = true, externalId = java.util.UUID.randomUUID().toString())
            }
        }
    }
