package com.ahmedyejam.mks.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ahmedyejam.mks.data.local.entity.*
import com.ahmedyejam.mks.db.MksDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class StudyRepository(private val db: MksDatabase) {

    fun observeSessions(quizId: Long): Flow<List<SessionEntity>> =
        db.sessionQueriesQueries.se_selectByQuiz(quizId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toSessionEntity() } }

    fun observeMistakes(bookId: Long): Flow<List<MistakeLogEntryEntity>> =
        db.mistakeQueriesQueries.mk_selectByBook(bookId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toMistakeEntity() } }

    suspend fun updateFlashcardProgress(id: Long, correct: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val card = db.flashcardQueriesQueries.fc_cardSelectById(id).executeAsOneOrNull()
                    ?: return@withContext false
                val newAttempts = card.attempts + 1
                val newCorrect = if (correct) card.correctCount + 1 else card.correctCount
                db.flashcardQueriesQueries.fc_cardUpdateProgress(
                    attempts = newAttempts, correctCount = newCorrect,
                    difficulty = card.difficulty, dueAt = card.dueAt,
                    reviewCount = card.reviewCount, lastReviewedAt = currentTime(),
                    id = id
                )
                true
            } catch (_: Exception) { false }
        }

    suspend fun updateSlideProgress(id: Long, completed: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            try {
                db.slideshowQueriesQueries.sl_slideUpdateProgress(
                    isCompleted = if (completed) 1L else 0L, updatedAt = currentTime(), id = id
                )
                true
            } catch (_: Exception) { false }
        }

    suspend fun fixMistake(id: Long): Boolean = withContext(Dispatchers.IO) {
        try { db.mistakeQueriesQueries.mk_markFixed(updatedAt = currentTime(), id = id); true }
        catch (_: Exception) { false }
    }

    suspend fun logMistake(entry: MistakeLogEntryEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.mistakeQueriesQueries.mk_insert(
            bookId = entry.bookId, quizId = entry.quizId, questionId = entry.questionId,
            sessionId = entry.sessionId, selectedAnswer = entry.selectedAnswer,
            correctAnswer = entry.correctAnswer, userReason = entry.userReason,
            correctConcept = entry.correctConcept, preventionNote = entry.preventionNote,
            linkedFlashcardId = entry.linkedFlashcardId,
            linkedBlueprintId = entry.linkedBlueprintId,
            linkedAssetId = entry.linkedAssetId,
            isFixed = 0L, reviewAt = null, createdAt = now, updatedAt = now
        )
        0L // TODO: return lastInsertId
    }

    suspend fun updateQuestionMetrics(id: Long, isCorrect: Boolean, timeSpentMs: Long = 0) = withContext(Dispatchers.IO) {
        val q = db.questionQueriesQueries.qu_selectById(id).executeAsOneOrNull() ?: return@withContext
        val newAttempts = q.attempts + 1
        val newCorrect = if (isCorrect) q.correctCount + 1 else q.correctCount
        val newConsecutive = if (isCorrect) q.consecutiveCorrect + 1 else 0
        db.questionQueriesQueries.qu_updateAttempt(
            attempts = newAttempts,
            correctCount = newCorrect,
            lastStudiedAt = currentTime(),
            timeSpentMs = q.timeSpentMs + timeSpentMs,
            lastAttemptResult = if (isCorrect) 1L else 0L,
            consecutiveCorrect = newConsecutive,
            id = id
        )
    }

    private fun currentTime(): Long = com.ahmedyejam.mks.util.currentTimeMillis()
}

private fun com.ahmedyejam.mks.db.Sessions.toSessionEntity() = SessionEntity(
    id = id, quizId = quizId, label = label, currentQuestionIndex = currentQuestionIndex.toInt(),
    score = score.toInt(), incorrectCount = incorrectCount.toInt(),
    answers = emptyMap(), answersByIndex = emptyMap(), isCompleted = isCompleted != 0L,
    createdAt = createdAt, updatedAt = updatedAt, lastModifiedAt = lastModifiedAt,
    lastStudiedAt = lastStudiedAt, lastEditedAt = lastEditedAt,
    questionIds = emptyList(), originalQuestionCount = originalQuestionCount.toInt(),
    shuffleQuestions = shuffleQuestions != 0L, shuffleOptions = shuffleOptions != 0L,
    rapidMode = rapidMode != 0L, repeatWrong = repeatWrong != 0L,
    quizTimerSeconds = quizTimerSeconds.toInt(), questionTimerSeconds = questionTimerSeconds.toInt(),
    rangeFrom = rangeFrom.toInt(), rangeTo = rangeTo.toInt(),
    includeFilters = emptyList(), droppedOptions = emptyMap(), droppedOptionsByIndex = emptyMap(),
    visibleOptionsCount = emptyMap(), visibleOptionsCountByIndex = emptyMap(),
    currentStreak = currentStreak.toInt(), maxStreak = maxStreak.toInt(),
    resultTaxonomy = emptyMap(), deletedAt = deletedAt
)

private fun com.ahmedyejam.mks.db.Mistake_log_entries.toMistakeEntity() = MistakeLogEntryEntity(
    id = id, bookId = bookId, quizId = quizId, questionId = questionId,
    sessionId = sessionId, selectedAnswer = selectedAnswer, correctAnswer = correctAnswer,
    userReason = userReason, correctConcept = correctConcept, preventionNote = preventionNote,
    linkedFlashcardId = linkedFlashcardId, linkedBlueprintId = linkedBlueprintId,
    linkedAssetId = linkedAssetId, isFixed = isFixed != 0L, reviewAt = reviewAt,
    createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt
)
