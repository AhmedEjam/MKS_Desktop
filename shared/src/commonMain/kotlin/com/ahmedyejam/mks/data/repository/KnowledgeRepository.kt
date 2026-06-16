package com.ahmedyejam.mks.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ahmedyejam.mks.data.local.entity.*
import com.ahmedyejam.mks.db.MksDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class KnowledgeRepository(private val db: MksDatabase) : KoinComponent {
    private val quizRepo: QuizRepository by inject()
    private val bookRepo: BookRepository by inject()
    private val assetRepo: AssetRepository by inject()

    // ---- Flashcard Decks ----
    fun observeDecksByBook(bookId: Long): Flow<List<FlashcardDeckEntity>> =
        db.flashcardQueriesQueries.fc_deckSelectByBook(bookId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDeckEntity() } }

    suspend fun getDeckById(id: Long): FlashcardDeckEntity? = withContext(Dispatchers.IO) {
        db.flashcardQueriesQueries.fc_deckSelectById(id).executeAsOneOrNull()?.toDeckEntity()
    }

    suspend fun createDeck(deck: FlashcardDeckEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.flashcardQueriesQueries.fc_deckInsert(
            externalId = deck.externalId.ifBlank { "deck_$now" }, bookId = deck.bookId,
            title = deck.title, description = deck.description, iconName = deck.iconName,
            coverImage = deck.coverImage, tags = "[]", cardCount = 0L, studiedCount = 0L,
            masteryPercentage = 0.0, isSystem = deck.isSystem.toLong(), isPinned = deck.isPinned.toLong(),
            createdAt = now, updatedAt = now, lastStudiedAt = 0L, lastEditedAt = now
        )
        db.flashcardQueriesQueries.fc_deckLastInsertId().executeAsOne()
    }

    // ---- Flashcards ----
    fun observeCardsByDeck(deckId: Long): Flow<List<FlashcardEntity>> =
        db.flashcardQueriesQueries.fc_cardSelectByDeck(deckId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toCardEntity() } }

    suspend fun createCard(card: FlashcardEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.flashcardQueriesQueries.fc_cardInsert(
            externalId = card.externalId.ifBlank { "card_$now" }, deckId = card.deckId,
            frontText = card.frontText, backText = card.backText, hint = card.hint,
            imagePath = card.imagePath, tags = "[]", orderIndex = card.orderIndex.toLong(),
            attempts = 0L, correctCount = 0L, difficulty = null,
            dueAt = 0L, reviewCount = 0L, lastReviewedAt = 0L,
            createdAt = now, updatedAt = now, sourceQuestionId = card.sourceQuestionId,
            syncConfig = "{}"
        )
        db.flashcardQueriesQueries.fc_deckLastInsertId().executeAsOne()
    }

    // ---- Slideshow Courses ----
    fun observeCoursesByBook(bookId: Long): Flow<List<SlideshowCourseEntity>> =
        db.slideshowQueriesQueries.sl_courseSelectByBook(bookId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toCourseEntity() } }

    suspend fun createCourse(course: SlideshowCourseEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.slideshowQueriesQueries.sl_courseInsert(
            externalId = course.externalId.ifBlank { "course_$now" }, bookId = course.bookId,
            title = course.title, description = course.description, iconName = course.iconName,
            coverImage = course.coverImage, tags = "[]", slideCount = 0L, studiedSlideCount = 0L,
            progress = 0.0, isSystem = course.isSystem.toLong(), isPinned = course.isPinned.toLong(),
            createdAt = now, updatedAt = now, lastStudiedAt = 0L, lastEditedAt = now,
            isDerived = course.isDerived.toLong(), sourceQuizId = course.sourceQuizId
        )
        db.slideshowQueriesQueries.sl_courseLastInsertId().executeAsOne()
    }

    fun observeSlidesByCourse(courseId: Long): Flow<List<CourseSlideEntity>> =
        db.slideshowQueriesQueries.sl_slideSelectByCourse(courseId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toSlideEntity() } }

    suspend fun createSlide(slide: CourseSlideEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.slideshowQueriesQueries.sl_slideInsert(
            externalId = slide.externalId.ifBlank { "slide_$now" }, courseId = slide.courseId,
            title = slide.title, body = slide.body, speakerNotes = slide.speakerNotes,
            imagePath = slide.imagePath, orderIndex = slide.orderIndex.toLong(),
            isCompleted = slide.isCompleted.toLong(), tags = "[]",
            difficulty = null, dueAt = 0L, reviewCount = 0L, lastReviewedAt = 0L,
            createdAt = now, updatedAt = now, sourceQuestionId = slide.sourceQuestionId,
            syncConfig = "{}"
        )
        db.flashcardQueriesQueries.fc_deckLastInsertId().executeAsOne()
    }

    // ---- Note Blueprints ----
    suspend fun getBlueprintById(id: Long): NoteBlueprintEntity? = withContext(Dispatchers.IO) {
        db.noteQueriesQueries.nt_blueprintSelectById(id).executeAsOneOrNull()?.toBlueprintEntity()
    }

    suspend fun createBlueprint(blueprint: NoteBlueprintEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.noteQueriesQueries.nt_blueprintInsert(
            externalId = blueprint.externalId.ifBlank { "note_$now" },
            collectionId = blueprint.collectionId, title = blueprint.title,
            description = blueprint.description, summary = blueprint.summary,
            iconName = blueprint.iconName, coverImage = blueprint.coverImage,
            body = blueprint.body, bulletPoints = "[]", tags = "[]",
            blueprintMode = blueprint.blueprintMode, linkedQuestionsJson = "[]",
            linkedAssetsJson = "[]", reviewStatus = "NEW",
            reviewCount = 0L, lastReviewedAt = 0L,
            createdAt = now, updatedAt = now, sourceQuestionId = blueprint.sourceQuestionId
        )
        db.noteQueriesQueries.nt_collectionLastInsertId().executeAsOne()
    }

    // ---- Prompt Decks ----
    fun observePromptDecksByBook(bookId: Long): Flow<List<PromptDeckEntity>> =
        db.promptQueriesQueries.pr_deckSelectByBook(bookId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDeckEntity() } }

    suspend fun createPromptDeck(deck: PromptDeckEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.promptQueriesQueries.pr_deckInsert(
            bookId = deck.bookId, title = deck.title, description = deck.description,
            iconName = deck.iconName, coverImage = deck.coverImage, tags = "[]",
            createdAt = now, updatedAt = now
        )
        db.promptQueriesQueries.pr_deckLastInsertId().executeAsOne()
    }

    fun observePromptCardsByDeck(deckId: Long): Flow<List<PromptCardEntity>> =
        db.promptQueriesQueries.pr_cardSelectByDeck(deckId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toCardEntity() } }

    private fun currentTime(): Long = com.ahmedyejam.mks.util.currentTimeMillis()
    private fun Boolean.toLong() = if (this) 1L else 0L
}

// SQLDelight → Entity mappers
private fun com.ahmedyejam.mks.db.Flashcard_decks.toDeckEntity() = FlashcardDeckEntity(
    id = id, externalId = externalId, bookId = bookId, title = title,
    description = description, iconName = iconName, coverImage = coverImage, tags = emptyList(),
    cardCount = cardCount.toInt(), studiedCount = studiedCount.toInt(),
    masteryPercentage = masteryPercentage.toFloat(), isSystem = isSystem != 0L,
    isPinned = isPinned != 0L, createdAt = createdAt, updatedAt = updatedAt,
    lastStudiedAt = lastStudiedAt, lastEditedAt = lastEditedAt, deletedAt = deletedAt
)
private fun com.ahmedyejam.mks.db.Flashcards.toCardEntity() = FlashcardEntity(
    id = id, externalId = externalId, deckId = deckId, frontText = frontText,
    backText = backText, hint = hint, imagePath = imagePath, tags = emptyList(),
    orderIndex = orderIndex.toInt(), attempts = attempts.toInt(), correctCount = correctCount.toInt(),
    difficulty = difficulty, dueAt = dueAt, reviewCount = reviewCount.toInt(),
    lastReviewedAt = lastReviewedAt, createdAt = createdAt, updatedAt = updatedAt,
    sourceQuestionId = sourceQuestionId, syncConfig = emptyMap(), deletedAt = deletedAt
)
private fun com.ahmedyejam.mks.db.Slideshow_courses.toCourseEntity() = SlideshowCourseEntity(
    id = id, externalId = externalId, bookId = bookId, title = title,
    description = description, iconName = iconName, coverImage = coverImage, tags = emptyList(),
    slideCount = slideCount.toInt(), studiedSlideCount = studiedSlideCount.toInt(),
    progress = progress.toFloat(), isSystem = isSystem != 0L, isPinned = isPinned != 0L,
    createdAt = createdAt, updatedAt = updatedAt, lastStudiedAt = lastStudiedAt,
    lastEditedAt = lastEditedAt, isDerived = isDerived != 0L,
    sourceQuizId = sourceQuizId, deletedAt = deletedAt
)
private fun com.ahmedyejam.mks.db.Course_slides.toSlideEntity() = CourseSlideEntity(
    id = id, externalId = externalId, courseId = courseId, title = title, body = body,
    speakerNotes = speakerNotes, imagePath = imagePath, orderIndex = orderIndex.toInt(),
    isCompleted = isCompleted != 0L, tags = emptyList(), difficulty = difficulty,
    dueAt = dueAt, reviewCount = reviewCount.toInt(), lastReviewedAt = lastReviewedAt,
    createdAt = createdAt, updatedAt = updatedAt, sourceQuestionId = sourceQuestionId,
    syncConfig = emptyMap(), deletedAt = deletedAt
)
private fun com.ahmedyejam.mks.db.Note_blueprints.toBlueprintEntity() = NoteBlueprintEntity(
    id = id, externalId = externalId, collectionId = collectionId, title = title,
    description = description, summary = summary, iconName = iconName, coverImage = coverImage,
    body = body, bulletPoints = emptyList(), tags = emptyList(),
    blueprintMode = blueprintMode, linkedQuestionsJson = linkedQuestionsJson,
    linkedAssetsJson = linkedAssetsJson, reviewStatus = reviewStatus,
    reviewCount = reviewCount.toInt(), lastReviewedAt = lastReviewedAt,
    createdAt = createdAt, updatedAt = updatedAt,
    sourceQuestionId = sourceQuestionId, deletedAt = deletedAt
)
private fun com.ahmedyejam.mks.db.Prompt_decks.toDeckEntity() = PromptDeckEntity(
    id = id, bookId = bookId, title = title, description = description,
    iconName = iconName, coverImage = coverImage, tags = emptyList(),
    createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt
)
private fun com.ahmedyejam.mks.db.Prompt_cards.toCardEntity() = PromptCardEntity(
    id = id, deckId = deckId, title = title, promptText = promptText,
    variablesJson = variablesJson, outputType = outputType, tags = emptyList(),
    usageCount = usageCount.toInt(), lastUsedAt = lastUsedAt, sortOrder = sortOrder.toInt(),
    createdAt = createdAt, updatedAt = updatedAt, deletedAt = deletedAt
)
