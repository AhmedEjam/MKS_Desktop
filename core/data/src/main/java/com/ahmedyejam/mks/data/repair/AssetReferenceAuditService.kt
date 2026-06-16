package com.ahmedyejam.mks.data.repair

import com.ahmedyejam.mks.data.local.dao.AssetReferenceDao
import com.ahmedyejam.mks.data.local.dao.BookDao
import com.ahmedyejam.mks.data.local.dao.CourseSlideDao
import com.ahmedyejam.mks.data.local.dao.FlashcardDao
import com.ahmedyejam.mks.data.local.dao.QuestionAssetDao
import com.ahmedyejam.mks.data.local.dao.QuestionDao
import com.ahmedyejam.mks.data.local.dao.QuizDao
import com.ahmedyejam.mks.data.local.dao.SourceDocumentDao
import com.ahmedyejam.mks.data.local.entity.AssetReferenceEntity
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class AssetReferenceRepairResult(
    val checkedPaths: Int,
    val missingFiles: Int,
    val orphanedReferences: Int,
    val deletedReferences: Int,
    val deletedFiles: Int,
)

@Singleton
class AssetReferenceAuditService @Inject constructor(
    private val assetReferenceDao: AssetReferenceDao,
    private val bookDao: BookDao,
    private val quizDao: QuizDao,
    private val questionDao: QuestionDao,
    private val flashcardDao: FlashcardDao,
    private val courseSlideDao: CourseSlideDao,
    private val sourceDocumentDao: SourceDocumentDao,
    private val questionAssetDao: QuestionAssetDao,
) {
    suspend fun audit(): AssetReferenceRepairResult {
        val references = assetReferenceDao.getAllReferences()
        var missingFiles = 0
        var orphanedReferences = 0

        references.forEach { ref: AssetReferenceEntity ->
            if (!File(ref.path).exists()) {
                missingFiles++
            }
            // Check if owner still exists
            val ownerExists =
                when (ref.ownerType) {
                    "book" -> bookDao.getBookById(ref.ownerId) != null
                    "quiz" -> quizDao.getQuizById(ref.ownerId) != null
                    "question" -> questionDao.getQuestionById(ref.ownerId) != null
                    "flashcard_deck" -> true // Assuming deck ID is correct for now
                    "flashcard" -> flashcardDao.getFlashcardById(ref.ownerId) != null
                    "slideshow_course" -> true
                    "course_slide" -> true
                    "source_document" -> sourceDocumentDao.getSourceById(ref.ownerId) != null
                    "question_asset" -> questionAssetDao.getAssetById(ref.ownerId) != null
                    else -> false
                }
            if (!ownerExists) {
                orphanedReferences++
            }
        }

        return AssetReferenceRepairResult(
            checkedPaths = references.size,
            missingFiles = missingFiles,
            orphanedReferences = orphanedReferences,
            deletedReferences = 0,
            deletedFiles = 0,
        )
    }
}
