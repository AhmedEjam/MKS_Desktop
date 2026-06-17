package com.ahmedyejam.mks.data.review

import com.ahmedyejam.mks.db.MksDatabase
import com.ahmedyejam.mks.util.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReviewRepository(private val db: MksDatabase) {
    suspend fun loadSummary(now: Long = currentTimeMillis()): ReviewDashboardSummary =
        withContext(Dispatchers.IO) {
            val weakCutoff = now - 7L * 24L * 60L * 60L * 1000L
            ReviewDashboardSummary(
                dueFlashcards = db.flashcardQueriesQueries.fc_countDue(now).executeAsOne().toInt(),
                dueBlueprints = db.noteQueriesQueries.nt_countDue(now).executeAsOne().toInt(),
                dueMistakes = db.mistakeQueriesQueries.mk_countDue(now).executeAsOne().toInt(),
                pendingMistakes = db.mistakeQueriesQueries.mk_countPending().executeAsOne().toInt(),
                markedQuestions = db.questionQueriesQueries.qu_countMarked().executeAsOne().toInt(),
                weakQuestions = db.questionQueriesQueries.qu_countWeak(weakCutoff).executeAsOne().toInt(),
                unfinishedSlides = 0, // Slideshow integration in a later phase
            )
        }

    suspend fun loadQueues(now: Long = currentTimeMillis()): List<ReviewQueueItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<ReviewQueueItem>()
            
            // Flashcards
            items.addAll(db.flashcardQueriesQueries.fc_selectDue(now, 20).executeAsList().map {
                ReviewQueueItem(it.id.toString(), ReviewQueueType.FLASHCARD, it.frontText.take(90), it.backText.take(120), it.dueAt)
            })

            // Blueprints
            items.addAll(db.noteQueriesQueries.nt_selectDue(now, 20).executeAsList().map {
                ReviewQueueItem(it.id.toString(), ReviewQueueType.BLUEPRINT, it.title, it.summary, it.lastReviewedAt)
            })

            // Mistakes
            items.addAll(db.mistakeQueriesQueries.mk_selectDue(now, 20).executeAsList().map {
                ReviewQueueItem(it.id.toString(), ReviewQueueType.MISTAKE, it.correctConcept ?: "Mistake #${it.id}", it.preventionNote ?: it.userReason, it.reviewAt)
            })

            // Marked Questions
            items.addAll(db.questionQueriesQueries.qu_selectMarked().executeAsList().take(20).map {
                ReviewQueueItem(it.id.toString(), ReviewQueueType.MARKED_QUESTION, it.text.take(90), it.markReason, it.markReviewAt)
            })

            // Weak Questions
            val weakCutoff = now - 7L * 24L * 60L * 60L * 1000L
            items.addAll(db.questionQueriesQueries.qu_selectWeak(weakCutoff, 20).executeAsList().map {
                ReviewQueueItem(it.id.toString(), ReviewQueueType.WEAK_QUESTION, it.text.take(90), "${it.correctCount}/${it.attempts} correct", it.lastStudiedAt)
            })

            items
        }

    suspend fun markReviewed(item: ReviewQueueItem) = withContext(Dispatchers.IO) {
        val id = item.id.toLongOrNull() ?: return@withContext
        val now = currentTimeMillis()
        when (item.type) {
            ReviewQueueType.FLASHCARD -> {
                val card = db.flashcardQueriesQueries.fc_cardSelectById(id).executeAsOneOrNull() ?: return@withContext
                db.flashcardQueriesQueries.fc_cardUpdateProgress(
                    attempts = card.attempts + 1,
                    correctCount = card.correctCount + 1,
                    difficulty = card.difficulty,
                    dueAt = now + 3L * 24L * 60L * 60L * 1000L,
                    reviewCount = card.reviewCount + 1,
                    lastReviewedAt = now,
                    id = id
                )
            }
            ReviewQueueType.BLUEPRINT -> db.noteQueriesQueries.nt_blueprintUpdateReviewCount(1, now, id)
            ReviewQueueType.MISTAKE -> db.mistakeQueriesQueries.mk_markFixed(now, id)
            ReviewQueueType.MARKED_QUESTION -> db.questionQueriesQueries.qu_clearMark(now, id)
            ReviewQueueType.WEAK_QUESTION -> db.questionQueriesQueries.qu_markReviewed(now, now, id)
            else -> Unit
        }
    }

    suspend fun snooze(item: ReviewQueueItem, millis: Long) = withContext(Dispatchers.IO) {
        val id = item.id.toLongOrNull() ?: return@withContext
        val now = currentTimeMillis()
        val due = now + millis
        when (item.type) {
            ReviewQueueType.FLASHCARD -> db.flashcardQueriesQueries.fc_snooze(due, now, id)
            ReviewQueueType.BLUEPRINT -> db.noteQueriesQueries.nt_snooze(due, now, id)
            ReviewQueueType.MISTAKE -> db.mistakeQueriesQueries.mk_snooze(due, now, id)
            ReviewQueueType.MARKED_QUESTION -> db.questionQueriesQueries.qu_updateMarkedStatus(1, now, "Snoozed", due, id)
            else -> Unit
        }
    }
}
