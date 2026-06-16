package com.ahmedyejam.mks.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ahmedyejam.mks.data.local.entity.*
import com.ahmedyejam.mks.data.model.CategoryWithMetadata
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.db.MksDatabase
import com.ahmedyejam.mks.util.fromJson
import com.ahmedyejam.mks.util.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class QuizRepository(private val db: MksDatabase) : KoinComponent {
    private val bookRepo: BookRepository by inject()
    private val assetRepo: AssetRepository by inject()

    fun observeQuizById(quizId: Long): Flow<QuizEntity?> =
        db.quizQueriesQueries.qz_selectById(quizId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { it.firstOrNull()?.toQuizEntity() }

    suspend fun getQuizById(id: Long): QuizEntity? = withContext(Dispatchers.IO) {
        db.quizQueriesQueries.qz_selectById(id).executeAsOneOrNull()?.toQuizEntity()
    }

    suspend fun createQuiz(quiz: QuizEntity): MksResult<Long> = withContext(Dispatchers.IO) {
        try {
            val now = currentTime()
            val extId = quiz.externalId.ifBlank { "quiz_$now" }
            db.quizQueriesQueries.qz_insert(
                externalId = extId,
                bookId = quiz.bookId, title = quiz.title, description = quiz.description,
                category = quiz.category, tags = quiz.tags.toJson(), iconName = quiz.iconName,
                coverImage = quiz.coverImage, createdAt = now, updatedAt = now,
                contentUpdatedAt = now, lastStudiedAt = 0L, lastEditedAt = now,
                isPinned = quiz.isPinned.toLong(), isSystem = quiz.isSystem.toLong()
            )
            val id = db.quizQueriesQueries.qz_selectByExternalId(extId).executeAsOne().id; MksResult.Success(id)
        } catch (e: Exception) { MksResult.Error("Failed to create quiz", e) }
    }

    suspend fun deleteQuiz(id: Long): MksResult<Boolean> = withContext(Dispatchers.IO) {
        try { db.quizQueriesQueries.qz_softDelete(currentTime(), id); MksResult.Success(true) }
        catch (e: Exception) { MksResult.Error("Failed to delete quiz", e) }
    }

    fun observeQuestionsByQuiz(quizId: Long): Flow<List<QuestionEntity>> =
        db.questionQueriesQueries.qu_selectByQuiz(quizId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toQuestionEntity() } }

    suspend fun getQuestionsByIds(ids: List<Long>): List<QuestionEntity> = withContext(Dispatchers.IO) {
        ids.mapNotNull { getQuestionById(it) }
    }

    suspend fun getQuestionById(id: Long): QuestionEntity? = withContext(Dispatchers.IO) {
        db.questionQueriesQueries.qu_selectById(id).executeAsOneOrNull()?.toQuestionEntity()
    }

    suspend fun createQuestion(question: QuestionEntity): MksResult<Long> = withContext(Dispatchers.IO) {
        try {
            val now = currentTime()
            val qExtId = question.externalId.ifBlank { "q_$now" }
            db.questionQueriesQueries.qu_insert(
                externalId = qExtId,
                quizId = question.quizId, text = question.text,
                type = question.type.name, options = question.options.toJson(), 
                correctAnswers = question.correctAnswers.toJson(),
                explanation = question.explanation, hint = question.hint,
                reference = question.reference, weight = question.weight.toLong(),
                imagePath = question.imagePath, imageName = question.imageName,
                imageSource = question.imageSource, attempts = 0L, correctCount = 0L,
                isDropped = 0L, droppedAt = null, droppedReason = null,
                isMarked = 0L, markedAt = null, markReason = null, markReviewAt = null,
                notes = question.notes, categories = question.categories.toJson(), 
                tags = question.tags.toJson(),
                difficulty = null, dueAt = 0L, reviewCount = 0L, lastReviewedAt = 0L,
                additionalInfo = question.additionalInfo,
                sourceBookId = null, sourceQuizId = null, sourceQuestionId = null,
                createdAt = now, updatedAt = now, lastStudiedAt = 0L, lastEditedAt = now,
                timeSpentMs = 0L, lastAttemptResult = null, consecutiveCorrect = 0L
            )
            val id = db.questionQueriesQueries.qu_selectByExternalId(qExtId).executeAsOne().id; MksResult.Success(id)
        } catch (e: Exception) { MksResult.Error("Failed to create question", e) }
    }

    suspend fun updateQuestion(question: QuestionEntity) = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.questionQueriesQueries.qu_update(
            text = question.text,
            type = question.type.name,
            options = question.options.toJson(),
            correctAnswers = question.correctAnswers.toJson(),
            explanation = question.explanation,
            hint = question.hint,
            reference = question.reference,
            weight = question.weight.toLong(),
            imagePath = question.imagePath,
            imageName = question.imageName,
            imageSource = question.imageSource,
            notes = question.notes,
            categories = question.categories.toJson(),
            tags = question.tags.toJson(),
            updatedAt = now,
            lastEditedAt = now,
            id = question.id
        )
    }

    suspend fun deleteQuestion(id: Long): Boolean = withContext(Dispatchers.IO) {
        try { db.questionQueriesQueries.qu_softDelete(currentTime(), id); true }
        catch (_: Exception) { false }
    }

    fun observeSessionsByQuiz(quizId: Long): Flow<List<SessionEntity>> =
        db.sessionQueriesQueries.se_selectByQuiz(quizId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toSessionEntity() } }

    suspend fun getSessionById(id: Long): SessionEntity? = withContext(Dispatchers.IO) {
        db.sessionQueriesQueries.se_selectById(id).executeAsOneOrNull()?.toSessionEntity()
    }

    suspend fun createSession(session: SessionEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.sessionQueriesQueries.se_insert(
            quizId = session.quizId, label = session.label,
            currentQuestionIndex = 0L, score = 0L, incorrectCount = 0L,
            answers = "{}", answersByIndex = "{}", isCompleted = 0L,
            createdAt = now, updatedAt = now, lastModifiedAt = now,
            lastStudiedAt = 0L, lastEditedAt = now,
            questionIds = session.questionIds.toJson(), originalQuestionCount = session.originalQuestionCount.toLong(),
            shuffleQuestions = session.shuffleQuestions.toLong(), 
            shuffleOptions = session.shuffleOptions.toLong(), 
            rapidMode = session.rapidMode.toLong(), 
            repeatWrong = session.repeatWrong.toLong(),
            quizTimerSeconds = session.quizTimerSeconds.toLong(), 
            questionTimerSeconds = session.questionTimerSeconds.toLong(), 
            rangeFrom = session.rangeFrom.toLong(), 
            rangeTo = session.rangeTo.toLong(),
            includeFilters = session.includeFilters.toJson(), 
            droppedOptions = "{}", droppedOptionsByIndex = "{}",
            visibleOptionsCount = "{}", visibleOptionsCountByIndex = "{}",
            currentStreak = 0L, maxStreak = 0L, resultTaxonomy = "{}"
        )
        db.sessionQueriesQueries.se_lastInsertId().executeAsOne()
    }

    suspend fun updateSession(session: SessionEntity) = withContext(Dispatchers.IO) {
        db.sessionQueriesQueries.se_update(
            currentQuestionIndex = session.currentQuestionIndex.toLong(),
            score = session.score.toLong(),
            incorrectCount = session.incorrectCount.toLong(),
            answers = session.answers.toJson(),
            answersByIndex = session.answersByIndex.toJson(),
            isCompleted = session.isCompleted.toLong(),
            updatedAt = currentTime(),
            lastModifiedAt = session.lastModifiedAt,
            lastEditedAt = currentTime(),
            droppedOptions = session.droppedOptions.toJson(),
            droppedOptionsByIndex = session.droppedOptionsByIndex.toJson(),
            visibleOptionsCount = session.visibleOptionsCount.toJson(),
            visibleOptionsCountByIndex = session.visibleOptionsCountByIndex.toJson(),
            currentStreak = session.currentStreak.toLong(),
            maxStreak = session.maxStreak.toLong(),
            id = session.id
        )
    }

    fun getAllCategoriesWithMetadata(): Flow<List<CategoryWithMetadata>> =
        db.mistakeQueriesQueries.cm_selectAll()
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toCategoryWithMetadata() } }

    suspend fun updateCategoryMetadata(metadata: CategoryMetadataEntity) = withContext(Dispatchers.IO) {
        db.mistakeQueriesQueries.cm_upsert(
            name = metadata.name,
            emoji = metadata.emoji,
            color = metadata.color?.toLong(),
            isPinned = metadata.isPinned.toLong()
        )
    }

    suspend fun getMergePreview(source: String, target: String): Int = withContext(Dispatchers.IO) {
        0
    }

    suspend fun renameCategory(oldName: String, newName: String) = withContext(Dispatchers.IO) {
        db.mistakeQueriesQueries.cm_rename(newName, oldName)
    }

    suspend fun deleteCategory(name: String) = withContext(Dispatchers.IO) {
        db.mistakeQueriesQueries.cm_softDelete(currentTime(), name)
    }

    suspend fun mergeCategory(source: String, target: String) = withContext(Dispatchers.IO) {
        // Complex merge logic...
    }

    private fun currentTime(): Long = com.ahmedyejam.mks.util.currentTimeMillis()

    private fun com.ahmedyejam.mks.db.Quizzes.toQuizEntity() = QuizEntity(
        id = id, externalId = externalId, bookId = bookId, title = title,
        description = description, category = category, tags = tags.fromJson(emptyList()),
        iconName = iconName, coverImage = coverImage, createdAt = createdAt,
        updatedAt = updatedAt, contentUpdatedAt = contentUpdatedAt,
        lastStudiedAt = lastStudiedAt, lastEditedAt = lastEditedAt,
        isPinned = isPinned != 0L, isSystem = isSystem != 0L,
        questionCount = questionCount.toInt(), answeredCount = answeredCount.toInt(),
        totalAttempts = totalAttempts.toInt(),
        completionPercentage = completionPercentage.toFloat(),
        accuracyPercentage = accuracyPercentage.toFloat(), deletedAt = deletedAt
    )

    private fun com.ahmedyejam.mks.db.Questions.toQuestionEntity() = QuestionEntity(
        id = id, externalId = externalId, quizId = quizId, text = text,
        type = try { QuestionType.valueOf(type) } catch (_: Exception) { QuestionType.SINGLE_CHOICE },
        options = options.fromJson(emptyList()), 
        correctAnswers = correctAnswers.fromJson(emptyList()),
        explanation = explanation, hint = hint, reference = reference, weight = weight.toInt(),
        imagePath = imagePath, imageName = imageName, imageSource = imageSource,
        attempts = attempts.toInt(), correctCount = correctCount.toInt(),
        isDropped = isDropped != 0L, droppedAt = droppedAt, droppedReason = droppedReason,
        isMarked = isMarked != 0L, markedAt = markedAt, markReason = markReason, markReviewAt = markReviewAt,
        notes = notes, categories = categories.fromJson(emptyList()), 
        tags = tags.fromJson(emptyList()),
        difficulty = difficulty, dueAt = dueAt, reviewCount = reviewCount.toInt(),
        lastReviewedAt = lastReviewedAt, additionalInfo = additionalInfo,
        sourceBookId = sourceBookId, sourceQuizId = sourceQuizId, sourceQuestionId = sourceQuestionId,
        createdAt = createdAt, updatedAt = updatedAt, lastStudiedAt = lastStudiedAt,
        lastEditedAt = lastEditedAt, timeSpentMs = timeSpentMs, lastAttemptResult = lastAttemptResult != 0L,
        consecutiveCorrect = consecutiveCorrect.toInt(), deletedAt = deletedAt
    )

    private fun com.ahmedyejam.mks.db.Sessions.toSessionEntity() = SessionEntity(
        id = id, quizId = quizId, label = label, currentQuestionIndex = currentQuestionIndex.toInt(),
        score = score.toInt(), incorrectCount = incorrectCount.toInt(),
        answers = answers.fromJson(emptyMap()), 
        answersByIndex = answersByIndex.fromJson(emptyMap()), 
        isCompleted = isCompleted != 0L,
        createdAt = createdAt, updatedAt = updatedAt, lastModifiedAt = lastModifiedAt,
        lastStudiedAt = lastStudiedAt, lastEditedAt = lastEditedAt,
        questionIds = questionIds.fromJson(emptyList()), 
        originalQuestionCount = originalQuestionCount.toInt(),
        shuffleQuestions = shuffleQuestions != 0L, shuffleOptions = shuffleOptions != 0L,
        rapidMode = rapidMode != 0L, repeatWrong = repeatWrong != 0L,
        quizTimerSeconds = quizTimerSeconds.toInt(), 
        questionTimerSeconds = questionTimerSeconds.toInt(),
        rangeFrom = rangeFrom.toInt(), rangeTo = rangeTo.toInt(),
        includeFilters = includeFilters.fromJson(emptyList()), 
        droppedOptions = droppedOptions.fromJson(emptyMap()), 
        droppedOptionsByIndex = droppedOptionsByIndex.fromJson(emptyMap()),
        visibleOptionsCount = visibleOptionsCount.fromJson(emptyMap()), 
        visibleOptionsCountByIndex = visibleOptionsCountByIndex.fromJson(emptyMap()),
        currentStreak = currentStreak.toInt(), maxStreak = maxStreak.toInt(),
        resultTaxonomy = resultTaxonomy.fromJson(emptyMap()), 
        deletedAt = deletedAt
    )

    private fun com.ahmedyejam.mks.db.Category_metadata.toCategoryWithMetadata() = CategoryWithMetadata(
        name = name, 
        questionCount = 0, 
        metadata = CategoryMetadataEntity(
            name = name,
            emoji = emoji,
            color = color?.toInt(),
            isPinned = isPinned != 0L
        )
    )

    private fun Boolean.toLong() = if (this) 1L else 0L
}
