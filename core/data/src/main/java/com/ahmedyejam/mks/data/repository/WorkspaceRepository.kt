package com.ahmedyejam.mks.data.repository

import com.ahmedyejam.mks.data.importer.repository.ImportLibraryManager
import com.ahmedyejam.mks.data.local.FileManager
import com.ahmedyejam.mks.data.local.WorkspaceDefaults
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
import com.ahmedyejam.mks.data.local.entity.WorkspaceEntity
import com.ahmedyejam.mks.data.local.entity.WorkspaceSettingsEntity
import com.ahmedyejam.mks.data.preview.CategoryMergePreviewService
import com.ahmedyejam.mks.data.preview.ClearMarksPreviewService
import com.ahmedyejam.mks.data.preview.DeletePreviewService
import com.ahmedyejam.mks.data.repair.AssetReferenceAuditService
import com.ahmedyejam.mks.data.seeder.MksDatabaseSeeder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepository
    @Inject
    constructor(
        private val workspaceDao: WorkspaceDao,
        private val database: com.ahmedyejam.mks.data.local.MksDatabase,
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
        private val seederProvider: javax.inject.Provider<MksDatabaseSeeder>,
        private val deletePreviewService: DeletePreviewService? = null,
        private val categoryMergePreviewService: CategoryMergePreviewService? = null,
        private val clearMarksPreviewService: ClearMarksPreviewService? = null,
        private val assetReferenceAuditService: AssetReferenceAuditService? = null,
    ) {
        fun getAllWorkspaces(): Flow<List<WorkspaceEntity>> = workspaceDao.getAllWorkspacesFlow()

        suspend fun getDefaultWorkspace(): WorkspaceEntity? = workspaceDao.getDefaultWorkspace()

        suspend fun getOrCreateDefaultWorkspace(): WorkspaceEntity {
            workspaceDao.getWorkspaceByExternalId(WorkspaceDefaults.DEFAULT_EXTERNAL_ID)?.let { return it }
            workspaceDao.getDefaultWorkspace()?.let { existing ->
                val updated =
                    existing.copy(
                        externalId = WorkspaceDefaults.DEFAULT_EXTERNAL_ID,
                        name = WorkspaceDefaults.DEFAULT_NAME,
                        description = WorkspaceDefaults.DEFAULT_DESCRIPTION,
                        isDefault = true,
                        deletedAt = null,
                        updatedAt = System.currentTimeMillis(),
                    )
                workspaceDao.updateWorkspace(updated)
                ensureWorkspaceSettings(updated.id)
                seederProvider.get().seedDatabase(updated.id)
                return updated
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
            ensureWorkspaceSettings(workspaceId)
            seederProvider.get().seedDatabase(workspaceId)
            return workspaceDao.getWorkspaceById(workspaceId)
                ?: WorkspaceEntity(
                    id = workspaceId,
                    externalId = WorkspaceDefaults.DEFAULT_EXTERNAL_ID,
                    name = WorkspaceDefaults.DEFAULT_NAME,
                    description = WorkspaceDefaults.DEFAULT_DESCRIPTION,
                    isDefault = true,
                )
        }

        private suspend fun ensureWorkspaceSettings(workspaceId: Long) {
            if (workspaceDao.getSettingsByWorkspaceId(workspaceId) == null) {
                workspaceDao.insertSettings(WorkspaceSettingsEntity(workspaceId = workspaceId))
            }
            seederProvider.get().seedDatabase(workspaceId)
        }

        suspend fun getWorkspaceById(id: Long): WorkspaceEntity? = workspaceDao.getWorkspaceById(id)

        suspend fun getWorkspaceByIdIncludingDeleted(id: Long): WorkspaceEntity? = workspaceDao.getWorkspaceByIdIncludingDeleted(id)

        fun getDeletedWorkspaces(): Flow<List<WorkspaceEntity>> = workspaceDao.getDeletedWorkspacesFlow()

        suspend fun getWorkspaceByExternalId(externalId: String): WorkspaceEntity? = workspaceDao.getWorkspaceByExternalId(externalId)

        suspend fun insertWorkspace(workspace: WorkspaceEntity): Long = workspaceDao.insertWorkspace(workspace)

        suspend fun updateWorkspace(workspace: WorkspaceEntity) = workspaceDao.updateWorkspace(workspace)

        suspend fun deleteWorkspace(workspace: WorkspaceEntity) {
            val now = System.currentTimeMillis()
            workspaceDao.softDeleteWorkspaceById(workspace.id, now)
            bookDao.softDeleteBooksByWorkspaceId(workspace.id, now)
        }

        suspend fun restoreWorkspace(workspaceId: Long) {
            val workspace = workspaceDao.getWorkspaceByIdIncludingDeleted(workspaceId) ?: return
            val deletedAtFilter = workspace.deletedAt ?: return
            val now = System.currentTimeMillis()
            workspaceDao.restoreWorkspaceById(workspaceId, now)
            bookDao.restoreBooksByWorkspaceId(workspaceId, now, deletedAtFilter)
        }

        suspend fun permanentlyDeleteWorkspace(workspace: WorkspaceEntity) = workspaceDao.hardDeleteWorkspace(workspace)

        suspend fun getWorkspaceSettings(workspaceId: Long): WorkspaceSettingsEntity? = workspaceDao.getSettingsByWorkspaceId(workspaceId)

        suspend fun insertWorkspaceSettings(settings: WorkspaceSettingsEntity): Long = workspaceDao.insertSettings(settings)

        suspend fun updateWorkspaceSettings(settings: WorkspaceSettingsEntity) = workspaceDao.updateSettings(settings)

        suspend fun resetDatabase() {
            database.clearAllTables()
            val workspaceId =
                workspaceDao.getWorkspaceByExternalId(WorkspaceDefaults.DEFAULT_EXTERNAL_ID)?.id
                    ?: workspaceDao.getDefaultWorkspace()?.id
                    ?: workspaceDao.insertWorkspace(
                        WorkspaceEntity(
                            externalId = WorkspaceDefaults.DEFAULT_EXTERNAL_ID,
                            name = WorkspaceDefaults.DEFAULT_NAME,
                            description = WorkspaceDefaults.DEFAULT_DESCRIPTION,
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
            if (workspaceDao.getSettingsByWorkspaceId(workspaceId) == null) {
                workspaceDao.insertSettings(WorkspaceSettingsEntity(workspaceId = workspaceId))
            }
            seederProvider.get().seedDatabase(workspaceId)
        }
    }
