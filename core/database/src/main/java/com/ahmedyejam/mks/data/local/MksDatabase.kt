package com.ahmedyejam.mks.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ahmedyejam.mks.data.local.dao.AnnotationDao
import com.ahmedyejam.mks.data.local.dao.AssetReferenceDao
import com.ahmedyejam.mks.data.local.dao.BookDao
import com.ahmedyejam.mks.data.local.dao.GlobalSearchDao
import com.ahmedyejam.mks.data.local.dao.MistakeLogDao
import com.ahmedyejam.mks.data.local.dao.PromptCardDao
import com.ahmedyejam.mks.data.local.dao.PromptDeckDao
import com.ahmedyejam.mks.data.local.dao.PromptRunDao
import com.ahmedyejam.mks.data.local.dao.QuestionAssetDao
import com.ahmedyejam.mks.data.local.dao.QuestionCategoryDao
import com.ahmedyejam.mks.data.local.dao.QuestionDao
import com.ahmedyejam.mks.data.local.dao.QuizDao
import com.ahmedyejam.mks.data.local.dao.SessionDao
import com.ahmedyejam.mks.data.local.dao.SourceDocumentDao
import com.ahmedyejam.mks.data.local.dao.WorkspaceDao
import com.ahmedyejam.mks.data.local.entity.AnnotationEntity
import com.ahmedyejam.mks.data.local.entity.AssetReferenceEntity
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.CategoryMetadataEntity
import com.ahmedyejam.mks.data.local.entity.CourseSlideEntity
import com.ahmedyejam.mks.data.local.entity.FlashcardDeckEntity
import com.ahmedyejam.mks.data.local.entity.FlashcardEntity
import com.ahmedyejam.mks.data.local.entity.KnowledgeStudySessionEntity
import com.ahmedyejam.mks.data.local.entity.LearningSessionEntity
import com.ahmedyejam.mks.data.local.entity.MistakeLogEntryEntity
import com.ahmedyejam.mks.data.local.entity.NoteBlueprintEntity
import com.ahmedyejam.mks.data.local.entity.NoteCollectionEntity
import com.ahmedyejam.mks.data.local.entity.PromptCardEntity
import com.ahmedyejam.mks.data.local.entity.PromptDeckEntity
import com.ahmedyejam.mks.data.local.entity.PromptEntity
import com.ahmedyejam.mks.data.local.entity.PromptRunEntity
import com.ahmedyejam.mks.data.local.entity.QuestionAssetEntity
import com.ahmedyejam.mks.data.local.entity.QuestionCategoryEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.local.entity.SessionEntity
import com.ahmedyejam.mks.data.local.entity.SlideshowCourseEntity
import com.ahmedyejam.mks.data.local.entity.SourceDocumentEntity
import com.ahmedyejam.mks.data.local.entity.StudySessionEntity
import com.ahmedyejam.mks.data.local.entity.WorkspaceEntity
import com.ahmedyejam.mks.data.local.entity.WorkspaceSettingsEntity

const val MKS_DATABASE_VERSION = 30

@Database(
    entities = [
        WorkspaceEntity::class,
        WorkspaceSettingsEntity::class,
        BookEntity::class,
        QuizEntity::class,
        QuestionEntity::class,
        SessionEntity::class,
        CategoryMetadataEntity::class,
        FlashcardDeckEntity::class,
        FlashcardEntity::class,
        LearningSessionEntity::class,
        SlideshowCourseEntity::class,
        CourseSlideEntity::class,
        NoteBlueprintEntity::class,
        PromptEntity::class,
        PromptDeckEntity::class,
        PromptCardEntity::class,
        PromptRunEntity::class,
        KnowledgeStudySessionEntity::class,
        QuestionCategoryEntity::class,
        AssetReferenceEntity::class,
        QuestionAssetEntity::class,
        SourceDocumentEntity::class,
        MistakeLogEntryEntity::class,
        AnnotationEntity::class,
        NoteCollectionEntity::class,
        StudySessionEntity::class
    ],
    version = MKS_DATABASE_VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MksDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun bookDao(): BookDao
    abstract fun quizDao(): QuizDao
    abstract fun questionDao(): QuestionDao
    abstract fun sessionDao(): SessionDao
    abstract fun categoryMetadataDao(): com.ahmedyejam.mks.data.local.dao.CategoryMetadataDao
    abstract fun flashcardDeckDao(): com.ahmedyejam.mks.data.local.dao.FlashcardDeckDao
    abstract fun flashcardDao(): com.ahmedyejam.mks.data.local.dao.FlashcardDao
    abstract fun learningSessionDao(): com.ahmedyejam.mks.data.local.dao.LearningSessionDao
    abstract fun slideshowCourseDao(): com.ahmedyejam.mks.data.local.dao.SlideshowCourseDao
    abstract fun courseSlideDao(): com.ahmedyejam.mks.data.local.dao.CourseSlideDao
    abstract fun noteBlueprintDao(): com.ahmedyejam.mks.data.local.dao.NoteBlueprintDao
    abstract fun promptDao(): com.ahmedyejam.mks.data.local.dao.PromptDao
    abstract fun promptDeckDao(): PromptDeckDao
    abstract fun promptCardDao(): PromptCardDao
    abstract fun promptRunDao(): PromptRunDao
    abstract fun knowledgeStudySessionDao(): com.ahmedyejam.mks.data.local.dao.KnowledgeStudySessionDao
    abstract fun questionCategoryDao(): QuestionCategoryDao
    abstract fun assetReferenceDao(): AssetReferenceDao
    abstract fun questionAssetDao(): QuestionAssetDao
    abstract fun sourceDocumentDao(): SourceDocumentDao
    abstract fun mistakeLogDao(): MistakeLogDao
    abstract fun globalSearchDao(): GlobalSearchDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun noteCollectionDao(): com.ahmedyejam.mks.data.local.dao.NoteCollectionDao
    abstract fun studySessionDao(): com.ahmedyejam.mks.data.local.dao.StudySessionDao

    companion object {
        const val DATABASE_NAME = "mks_database"
        const val DB_VERSION = MKS_DATABASE_VERSION
        val MIGRATION_15_16 = MksMigrations.MIGRATION_15_16
        val MIGRATION_16_17 = MksMigrations.MIGRATION_16_17
        val MIGRATION_26_27 = MksMigrations.MIGRATION_26_27
        val MIGRATION_27_28 = MksMigrations.MIGRATION_27_28
        val MIGRATION_28_29 = MksMigrations.MIGRATION_28_29

        fun addColumnIfMissing(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnDefinition: String
        ) {
            if (!columnExists(db, tableName, columnName)) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
            }
        }

        fun columnExists(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            tableName: String,
            columnName: String
        ): Boolean {
            var exists = false
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex >= 0) {
                    while (cursor.moveToNext() && !exists) {
                        exists = cursor.getString(nameIndex) == columnName
                    }
                }
            }
            return exists
        }
    }
}
