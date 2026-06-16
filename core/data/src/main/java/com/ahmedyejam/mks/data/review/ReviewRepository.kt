package com.ahmedyejam.mks.data.review

import com.ahmedyejam.mks.data.local.dao.CourseSlideDao
import com.ahmedyejam.mks.data.local.dao.FlashcardDao
import com.ahmedyejam.mks.data.local.dao.MistakeLogDao
import com.ahmedyejam.mks.data.local.dao.NoteBlueprintDao
import com.ahmedyejam.mks.data.local.dao.QuestionDao
import com.ahmedyejam.mks.ui.MksRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val flashcardDao: FlashcardDao,
    private val noteBlueprintDao: NoteBlueprintDao,
    private val mistakeLogDao: MistakeLogDao,
    private val questionDao: QuestionDao,
    private val courseSlideDao: CourseSlideDao,
) {
    suspend fun loadSummary(now: Long = System.currentTimeMillis()): ReviewDashboardSummary =
        withContext(Dispatchers.IO) {
            val weakCutoff = now - 7L * 24L * 60L * 60L * 1000L
            ReviewDashboardSummary(
                dueFlashcards = flashcardDao.countDueFlashcards(now),
                dueBlueprints = noteBlueprintDao.countDueBlueprints(now),
                dueMistakes = mistakeLogDao.countDueMistakes(now),
                pendingMistakes = mistakeLogDao.countPendingMistakes(now),
                markedQuestions = questionDao.getMarkedQuestionsForReview(now, 200).size,
                weakQuestions = questionDao.getWeakQuestionsDue(weakCutoff, 200).size,
                unfinishedSlides = courseSlideDao.countUnfinishedSlides(),
            )
        }

    suspend fun loadQueues(now: Long = System.currentTimeMillis()): List<ReviewQueueItem> =
        withContext(Dispatchers.IO) {
            val flashcards =
                flashcardDao.getDueFlashcards(now, 20).map {
                    ReviewQueueItem(
                        it.id.toString(),
                        ReviewQueueType.FLASHCARD,
                        it.frontText.take(90),
                        it.backText.take(120),
                        it.dueAt,
                        MksRoutes.flashcards(it.deckId),
                    )
                }
            val blueprints =
                noteBlueprintDao.getDueBlueprints(now, 20).map {
                    ReviewQueueItem(
                        it.id.toString(),
                        ReviewQueueType.BLUEPRINT,
                        it.title,
                        it.summary,
                        it.lastReviewedAt,
                        MksRoutes.blueprint(it.id),
                    )
                }
            val mistakes =
                mistakeLogDao.getDueMistakes(now, 20).map {
                    ReviewQueueItem(
                        it.id.toString(),
                        ReviewQueueType.MISTAKE,
                        it.correctConcept ?: "Mistake #${it.id}",
                        it.preventionNote ?: it.userReason,
                        it.reviewAt,
                        MksRoutes.REVIEW_DASHBOARD,
                    )
                }
            val marked =
                questionDao.getMarkedQuestionsForReview(now, 20).map {
                    ReviewQueueItem(
                        it.id.toString(),
                        ReviewQueueType.MARKED_QUESTION,
                        it.text.take(90),
                        it.markReason,
                        it.markReviewAt,
                        MksRoutes.quizQuestions(it.quizId),
                    )
                }
            val weakCutoff = now - 7L * 24L * 60L * 60L * 1000L
            val weak =
                questionDao.getWeakQuestionsDue(weakCutoff, 20).map {
                    ReviewQueueItem(
                        it.id.toString(),
                        ReviewQueueType.WEAK_QUESTION,
                        it.text.take(90),
                        "${it.correctCount}/${it.attempts} correct",
                        it.lastStudiedAt,
                        MksRoutes.quizQuestions(it.quizId),
                    )
                }
            flashcards + blueprints + mistakes + marked + weak
        }

    suspend fun markReviewed(item: ReviewQueueItem) =
        withContext(Dispatchers.IO) {
            val itemId = item.id.toLongOrNull() ?: return@withContext
            val now = System.currentTimeMillis()
            when (item.type) {
                ReviewQueueType.FLASHCARD -> flashcardDao.markReviewed(itemId, now, now + 3L * 24L * 60L * 60L * 1000L)
                ReviewQueueType.BLUEPRINT -> noteBlueprintDao.markReviewed(itemId, now)
                ReviewQueueType.MISTAKE -> mistakeLogDao.markFixed(itemId, now)
                ReviewQueueType.MARKED_QUESTION -> questionDao.clearQuestionMark(itemId, now)
                ReviewQueueType.WEAK_QUESTION -> questionDao.markQuestionReviewed(itemId, now)
                else -> Unit
            }
        }

    suspend fun snooze(
        item: ReviewQueueItem,
        millis: Long,
    ) = withContext(Dispatchers.IO) {
        val itemId = item.id.toLongOrNull() ?: return@withContext
        val now = System.currentTimeMillis()
        val due = now + millis
        when (item.type) {
            ReviewQueueType.FLASHCARD -> flashcardDao.snooze(itemId, due, now)
            ReviewQueueType.MISTAKE -> mistakeLogDao.snooze(itemId, due, now)
            ReviewQueueType.BLUEPRINT -> noteBlueprintDao.snoozeBlueprint(itemId, due)
            ReviewQueueType.WEAK_QUESTION -> questionDao.markQuestionReviewed(itemId, now)
            ReviewQueueType.MARKED_QUESTION -> questionDao.snoozeMarkedQuestion(itemId, due, now)
            else -> Unit
        }
    }
}
