package com.ahmedyejam.mks.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ahmedyejam.mks.data.local.entity.*
import com.ahmedyejam.mks.db.*
import com.ahmedyejam.mks.util.fromJson
import com.ahmedyejam.mks.util.toJson
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
            .map { list -> list.map { mapFlashcardDeck(it) } }

    suspend fun getDeckById(id: Long): FlashcardDeckEntity? = withContext(Dispatchers.IO) {
        db.flashcardQueriesQueries.fc_deckSelectById(id).executeAsOneOrNull()?.let { mapFlashcardDeck(it) }
    }

    suspend fun createDeck(deck: FlashcardDeckEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.flashcardQueriesQueries.fc_deckInsert(
            externalId = deck.externalId.ifBlank { "deck_$now" }, bookId = deck.bookId,
            title = deck.title, description = deck.description, iconName = deck.iconName,
            coverImage = deck.coverImage, tags = deck.tags.toJson(), cardCount = 0L, studiedCount = 0L,
            masteryPercentage = 0.0, isSystem = deck.isSystem.toLong(), isPinned = deck.isPinned.toLong(),
            createdAt = now, updatedAt = now, lastStudiedAt = 0L, lastEditedAt = now
        )
        db.flashcardQueriesQueries.fc_deckLastInsertId().executeAsOne()
    }

    suspend fun updateDeck(deck: FlashcardDeckEntity) = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.flashcardQueriesQueries.fc_deckUpdate(
            title = deck.title, description = deck.description, iconName = deck.iconName,
            coverImage = deck.coverImage, tags = deck.tags.toJson(),
            cardCount = deck.cardCount.toLong(), studiedCount = deck.studiedCount.toLong(),
            masteryPercentage = deck.masteryPercentage.toDouble(),
            updatedAt = now, lastEditedAt = now, id = deck.id
        )
    }

    suspend fun moveDeck(deckId: Long, targetBookId: Long) = withContext(Dispatchers.IO) {
        db.flashcardQueriesQueries.fc_deckUpdateBook(targetBookId, currentTime(), deckId)
    }

    // ---- Flashcards ----
    fun observeCardsByDeck(deckId: Long): Flow<List<FlashcardEntity>> =
        db.flashcardQueriesQueries.fc_cardSelectByDeck(deckId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { mapFlashcard(it) } }

    suspend fun createCard(card: FlashcardEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.flashcardQueriesQueries.fc_cardInsert(
            externalId = card.externalId.ifBlank { "card_$now" }, deckId = card.deckId,
            frontText = card.frontText, backText = card.backText, hint = card.hint,
            imagePath = card.imagePath, tags = card.tags.toJson(), orderIndex = card.orderIndex.toLong(),
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
            .map { list -> list.map { mapSlideshowCourse(it) } }

    fun observeSlideshowCoursesByBook(bookId: Long): Flow<List<SlideshowCourseEntity>> = observeCoursesByBook(bookId)

    suspend fun getSlideshowCourseById(id: Long): SlideshowCourseEntity? = withContext(Dispatchers.IO) {
        db.slideshowQueriesQueries.sl_courseSelectById(id).executeAsOneOrNull()?.let { mapSlideshowCourse(it) }
    }

    suspend fun createCourse(course: SlideshowCourseEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.slideshowQueriesQueries.sl_courseInsert(
            externalId = course.externalId.ifBlank { "course_$now" }, bookId = course.bookId,
            title = course.title, description = course.description, iconName = course.iconName,
            coverImage = course.coverImage, tags = course.tags.toJson(), slideCount = 0L, studiedSlideCount = 0L,
            progress = 0.0, isSystem = course.isSystem.toLong(), isPinned = course.isPinned.toLong(),
            createdAt = now, updatedAt = now, lastStudiedAt = 0L, lastEditedAt = now,
            isDerived = course.isDerived.toLong(), sourceQuizId = course.sourceQuizId
        )
        db.slideshowQueriesQueries.sl_courseLastInsertId().executeAsOne()
    }

    suspend fun updateCourse(course: SlideshowCourseEntity) = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.slideshowQueriesQueries.sl_courseUpdate(
            title = course.title, description = course.description, iconName = course.iconName,
            coverImage = course.coverImage, tags = course.tags.toJson(),
            slideCount = course.slideCount.toLong(), studiedSlideCount = course.studiedSlideCount.toLong(),
            progress = course.progress.toDouble(), updatedAt = now, lastEditedAt = now, id = course.id
        )
    }

    suspend fun moveCourse(courseId: Long, targetBookId: Long) = withContext(Dispatchers.IO) {
        db.slideshowQueriesQueries.sl_courseUpdateBook(targetBookId, currentTime(), courseId)
    }

    fun observeSlidesByCourse(courseId: Long): Flow<List<CourseSlideEntity>> =
        db.slideshowQueriesQueries.sl_slideSelectByCourse(courseId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { mapCourseSlide(it) } }

    fun getSlidesByCourseId(courseId: Long): Flow<List<CourseSlideEntity>> = observeSlidesByCourse(courseId)

    suspend fun createSlide(slide: CourseSlideEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.slideshowQueriesQueries.sl_slideInsert(
            externalId = slide.externalId.ifBlank { "slide_$now" }, courseId = slide.courseId,
            title = slide.title, body = slide.body, speakerNotes = slide.speakerNotes,
            imagePath = slide.imagePath, orderIndex = slide.orderIndex.toLong(),
            isCompleted = slide.isCompleted.toLong(), tags = slide.tags.toJson(),
            difficulty = null, dueAt = 0L, reviewCount = 0L, lastReviewedAt = 0L,
            createdAt = now, updatedAt = now, sourceQuestionId = slide.sourceQuestionId,
            syncConfig = "{}"
        )
        db.flashcardQueriesQueries.fc_deckLastInsertId().executeAsOne()
    }

    suspend fun updateCourseSlide(slide: CourseSlideEntity) = withContext(Dispatchers.IO) {
        db.slideshowQueriesQueries.sl_slideUpdateProgress(
            isCompleted = slide.isCompleted.toLong(),
            updatedAt = currentTime(),
            id = slide.id
        )
    }

    suspend fun deleteCourseSlide(slide: CourseSlideEntity) = withContext(Dispatchers.IO) {
        db.slideshowQueriesQueries.sl_slideSoftDelete(currentTime(), slide.id)
    }

    // ---- Note Blueprints ----
    fun observeNoteCollectionsByBook(bookId: Long): Flow<List<NoteCollectionEntity>> =
        db.noteQueriesQueries.nt_collectionSelectByBook(bookId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { mapNoteCollection(it) } }

    suspend fun getBlueprintById(id: Long): NoteBlueprintEntity? = withContext(Dispatchers.IO) {
        db.noteQueriesQueries.nt_blueprintSelectById(id).executeAsOneOrNull()?.let { mapNoteBlueprint(it) }
    }

    suspend fun createCollection(collection: NoteCollectionEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.noteQueriesQueries.nt_collectionInsert(
            externalId = collection.externalId.ifBlank { "collection_$now" }, bookId = collection.bookId,
            title = collection.title, description = collection.description, iconName = collection.iconName,
            coverImage = collection.coverImage, tags = collection.tags.toJson(),
            noteCount = 0L, isSystem = collection.isSystem.toLong(), isPinned = collection.isPinned.toLong(),
            createdAt = now, updatedAt = now, lastStudiedAt = 0L, lastEditedAt = now
        )
        db.noteQueriesQueries.nt_collectionLastInsertId().executeAsOne()
    }

    suspend fun updateCollection(collection: NoteCollectionEntity) = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.noteQueriesQueries.nt_collectionUpdate(
            title = collection.title, description = collection.description,
            iconName = collection.iconName, coverImage = collection.coverImage,
            tags = collection.tags.toJson(), updatedAt = now, lastEditedAt = now, id = collection.id
        )
    }

    suspend fun moveCollection(collectionId: Long, targetBookId: Long) = withContext(Dispatchers.IO) {
        db.noteQueriesQueries.nt_collectionUpdateBook(targetBookId, currentTime(), collectionId)
    }

    suspend fun createBlueprint(blueprint: NoteBlueprintEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.noteQueriesQueries.nt_blueprintInsert(
            externalId = blueprint.externalId.ifBlank { "note_$now" },
            collectionId = blueprint.collectionId, title = blueprint.title,
            description = blueprint.description, summary = blueprint.summary,
            iconName = blueprint.iconName, coverImage = blueprint.coverImage,
            body = blueprint.body, bulletPoints = blueprint.bulletPoints.toJson(), tags = blueprint.tags.toJson(),
            blueprintMode = blueprint.blueprintMode, linkedQuestionsJson = blueprint.linkedQuestionsJson,
            linkedAssetsJson = blueprint.linkedAssetsJson, reviewStatus = "NEW",
            reviewCount = 0L, lastReviewedAt = 0L,
            createdAt = now, updatedAt = now, sourceQuestionId = blueprint.sourceQuestionId
        )
        db.noteQueriesQueries.nt_collectionLastInsertId().executeAsOne()
    }

    // ---- Prompt Decks ----
    fun observePromptDecksByBook(bookId: Long): Flow<List<PromptDeckEntity>> =
        db.promptQueriesQueries.pr_deckSelectByBook(bookId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { mapPromptDeck(it) } }

    suspend fun createPromptDeck(deck: PromptDeckEntity): Long = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.promptQueriesQueries.pr_deckInsert(
            bookId = deck.bookId, title = deck.title, description = deck.description,
            iconName = deck.iconName, coverImage = deck.coverImage, tags = deck.tags.toJson(),
            createdAt = now, updatedAt = now
        )
        db.promptQueriesQueries.pr_deckLastInsertId().executeAsOne()
    }

    suspend fun updatePromptDeck(deck: PromptDeckEntity) = withContext(Dispatchers.IO) {
        val now = currentTime()
        db.promptQueriesQueries.pr_deckUpdate(
            title = deck.title, description = deck.description,
            iconName = deck.iconName, coverImage = deck.coverImage,
            tags = deck.tags.toJson(), updatedAt = now, id = deck.id
        )
    }

    suspend fun movePromptDeck(deckId: Long, targetBookId: Long) = withContext(Dispatchers.IO) {
        db.promptQueriesQueries.pr_deckUpdateBook(targetBookId, currentTime(), deckId)
    }

    fun observePromptCardsByDeck(deckId: Long): Flow<List<PromptCardEntity>> =
        db.promptQueriesQueries.pr_cardSelectByDeck(deckId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { mapPromptCard(it) } }

    // ---- Trash & Recovery ----
    fun getDeletedFlashcardDecks(workspaceId: Long): Flow<List<FlashcardDeckEntity>> =
        db.flashcardQueriesQueries.fc_selectDeleted(workspaceId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> 
                list.map { 
                    FlashcardDeckEntity(
                        id = it.id, externalId = it.externalId, bookId = it.bookId, title = it.title,
                        description = it.description, iconName = it.iconName, coverImage = it.coverImage, tags = it.tags.fromJson(emptyList()),
                        cardCount = it.cardCount.toInt(), studiedCount = it.studiedCount.toInt(),
                        masteryPercentage = it.masteryPercentage.toFloat(), isSystem = it.isSystem != 0L,
                        isPinned = it.isPinned != 0L, createdAt = it.createdAt, updatedAt = it.updatedAt,
                        lastStudiedAt = it.lastStudiedAt, lastEditedAt = it.lastEditedAt, deletedAt = it.deletedAt
                    )
                } 
            }

    suspend fun restoreFlashcardDeck(id: Long) = withContext(Dispatchers.IO) {
        db.flashcardQueriesQueries.fc_deckRestore(id)
    }

    suspend fun permanentlyDeleteFlashcardDeck(id: Long) = withContext(Dispatchers.IO) {
        db.flashcardQueriesQueries.fc_deckPermanentDelete(id)
    }

    fun getDeletedSlideshowCourses(workspaceId: Long): Flow<List<SlideshowCourseEntity>> =
        db.slideshowQueriesQueries.sl_selectDeleted(workspaceId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> 
                list.map { 
                    SlideshowCourseEntity(
                        id = it.id, externalId = it.externalId, bookId = it.bookId, title = it.title,
                        description = it.description, iconName = it.iconName, coverImage = it.coverImage, tags = it.tags.fromJson(emptyList()),
                        slideCount = it.slideCount.toInt(), studiedSlideCount = it.studiedSlideCount.toInt(),
                        progress = it.progress.toFloat(), isSystem = it.isSystem != 0L, isPinned = it.isPinned != 0L,
                        createdAt = it.createdAt, updatedAt = it.updatedAt, lastStudiedAt = it.lastStudiedAt,
                        lastEditedAt = it.lastEditedAt, isDerived = it.isDerived != 0L,
                        sourceQuizId = it.sourceQuizId, deletedAt = it.deletedAt
                    )
                } 
            }

    suspend fun restoreSlideshowCourse(id: Long) = withContext(Dispatchers.IO) {
        db.slideshowQueriesQueries.sl_courseRestore(id)
    }

    suspend fun permanentlyDeleteSlideshowCourse(id: Long) = withContext(Dispatchers.IO) {
        db.slideshowQueriesQueries.sl_coursePermanentDelete(id)
    }

    fun getDeletedNoteBlueprints(workspaceId: Long): Flow<List<NoteBlueprintEntity>> =
        db.noteQueriesQueries.nt_selectDeleted(workspaceId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> 
                list.map { 
                    NoteBlueprintEntity(
                        id = it.id, externalId = it.externalId, collectionId = it.collectionId, title = it.title,
                        description = it.description, summary = it.summary, iconName = it.iconName, coverImage = it.coverImage,
                        body = it.body, bulletPoints = it.bulletPoints.fromJson(emptyList()), tags = it.tags.fromJson(emptyList()),
                        blueprintMode = it.blueprintMode, linkedQuestionsJson = it.linkedQuestionsJson,
                        linkedAssetsJson = it.linkedAssetsJson, reviewStatus = it.reviewStatus,
                        reviewCount = it.reviewCount.toInt(), lastReviewedAt = it.lastReviewedAt,
                        createdAt = it.createdAt, updatedAt = it.updatedAt,
                        sourceQuestionId = it.sourceQuestionId, deletedAt = it.deletedAt
                    )
                } 
            }

    suspend fun restoreNoteBlueprint(id: Long) = withContext(Dispatchers.IO) {
        db.noteQueriesQueries.nt_blueprintRestore(id)
    }

    suspend fun permanentlyDeleteNoteBlueprint(id: Long) = withContext(Dispatchers.IO) {
        db.noteQueriesQueries.nt_blueprintPermanentDelete(id)
    }

    fun getDeletedPromptDecks(workspaceId: Long): Flow<List<PromptDeckEntity>> =
        db.promptQueriesQueries.pr_selectDeleted(workspaceId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> 
                list.map { 
                    PromptDeckEntity(
                        id = it.id, bookId = it.bookId, title = it.title, description = it.description,
                        iconName = it.iconName, coverImage = it.coverImage, tags = it.tags.fromJson(emptyList()),
                        createdAt = it.createdAt, updatedAt = it.updatedAt, deletedAt = it.deletedAt
                    )
                } 
            }

    suspend fun restorePromptDeck(id: Long) = withContext(Dispatchers.IO) {
        db.promptQueriesQueries.pr_deckRestore(id)
    }

    suspend fun permanentlyDeletePromptDeck(id: Long) = withContext(Dispatchers.IO) {
        db.promptQueriesQueries.pr_deckPermanentDelete(id)
    }

    private fun currentTime(): Long = com.ahmedyejam.mks.util.currentTimeMillis()
    private fun Boolean.toLong() = if (this) 1L else 0L

    // SQLDelight → Entity mappers
    private fun mapFlashcardDeck(row: Flashcard_decks) = FlashcardDeckEntity(
        id = row.id, externalId = row.externalId, bookId = row.bookId, title = row.title,
        description = row.description, iconName = row.iconName, coverImage = row.coverImage, tags = row.tags.fromJson(emptyList()),
        cardCount = row.cardCount.toInt(), studiedCount = row.studiedCount.toInt(),
        masteryPercentage = row.masteryPercentage.toFloat(), isSystem = row.isSystem != 0L,
        isPinned = row.isPinned != 0L, createdAt = row.createdAt, updatedAt = row.updatedAt,
        lastStudiedAt = row.lastStudiedAt, lastEditedAt = row.lastEditedAt, deletedAt = row.deletedAt
    )
    private fun mapFlashcard(row: Flashcards) = FlashcardEntity(
        id = row.id, externalId = row.externalId, deckId = row.deckId, frontText = row.frontText,
        backText = row.backText, hint = row.hint, imagePath = row.imagePath, tags = row.tags.fromJson(emptyList()),
        orderIndex = row.orderIndex.toInt(), attempts = row.attempts.toInt(), correctCount = row.correctCount.toInt(),
        difficulty = row.difficulty, dueAt = row.dueAt, reviewCount = row.reviewCount.toInt(),
        lastReviewedAt = row.lastReviewedAt, createdAt = row.createdAt, updatedAt = row.updatedAt,
        sourceQuestionId = row.sourceQuestionId, syncConfig = row.syncConfig.fromJson(emptyMap()), deletedAt = row.deletedAt
    )
    private fun mapSlideshowCourse(row: Slideshow_courses) = SlideshowCourseEntity(
        id = row.id, externalId = row.externalId, bookId = row.bookId, title = row.title,
        description = row.description, iconName = row.iconName, coverImage = row.coverImage, tags = row.tags.fromJson(emptyList()),
        slideCount = row.slideCount.toInt(), studiedSlideCount = row.studiedSlideCount.toInt(),
        progress = row.progress.toFloat(), isSystem = row.isSystem != 0L, isPinned = row.isPinned != 0L,
        createdAt = row.createdAt, updatedAt = row.updatedAt, lastStudiedAt = row.lastStudiedAt,
        lastEditedAt = row.lastEditedAt, isDerived = row.isDerived != 0L,
        sourceQuizId = row.sourceQuizId, deletedAt = row.deletedAt
    )
    private fun mapCourseSlide(row: Course_slides) = CourseSlideEntity(
        id = row.id, externalId = row.externalId, courseId = row.courseId, title = row.title, body = row.body,
        speakerNotes = row.speakerNotes, imagePath = row.imagePath, orderIndex = row.orderIndex.toInt(),
        isCompleted = row.isCompleted != 0L, tags = row.tags.fromJson(emptyList()), difficulty = row.difficulty,
        dueAt = row.dueAt, reviewCount = row.reviewCount.toInt(), lastReviewedAt = row.lastReviewedAt,
        createdAt = row.createdAt, updatedAt = row.updatedAt, sourceQuestionId = row.sourceQuestionId,
        syncConfig = row.syncConfig.fromJson(emptyMap()), deletedAt = row.deletedAt
    )
    private fun mapNoteCollection(row: Note_collections) = NoteCollectionEntity(
        id = row.id, externalId = row.externalId, bookId = row.bookId, title = row.title,
        description = row.description, iconName = row.iconName, coverImage = row.coverImage, tags = row.tags.fromJson(emptyList()),
        noteCount = row.noteCount.toInt(), isSystem = row.isSystem != 0L, isPinned = row.isPinned != 0L,
        createdAt = row.createdAt, updatedAt = row.updatedAt, lastStudiedAt = row.lastStudiedAt,
        lastEditedAt = row.lastEditedAt, deletedAt = row.deletedAt
    )
    private fun mapNoteBlueprint(row: Note_blueprints) = NoteBlueprintEntity(
        id = row.id, externalId = row.externalId, collectionId = row.collectionId, title = row.title,
        description = row.description, summary = row.summary, iconName = row.iconName, coverImage = row.coverImage,
        body = row.body, bulletPoints = row.bulletPoints.fromJson(emptyList()), tags = row.tags.fromJson(emptyList()),
        blueprintMode = row.blueprintMode, linkedQuestionsJson = row.linkedQuestionsJson,
        linkedAssetsJson = row.linkedAssetsJson, reviewStatus = row.reviewStatus,
        reviewCount = row.reviewCount.toInt(), lastReviewedAt = row.lastReviewedAt,
        createdAt = row.createdAt, updatedAt = row.updatedAt,
        sourceQuestionId = row.sourceQuestionId, deletedAt = row.deletedAt
    )
    private fun mapPromptDeck(row: Prompt_decks) = PromptDeckEntity(
        id = row.id, bookId = row.bookId, title = row.title, description = row.description,
        iconName = row.iconName, coverImage = row.coverImage, tags = row.tags.fromJson(emptyList()),
        createdAt = row.createdAt, updatedAt = row.updatedAt, deletedAt = row.deletedAt
    )
    private fun mapPromptCard(row: Prompt_cards) = PromptCardEntity(
        id = row.id, deckId = row.deckId, title = row.title, promptText = row.promptText,
        variablesJson = row.variablesJson, outputType = row.outputType, tags = row.tags.fromJson(emptyList()),
        usageCount = row.usageCount.toInt(), lastUsedAt = row.lastUsedAt, sortOrder = row.sortOrder.toInt(),
        createdAt = row.createdAt, updatedAt = row.updatedAt, deletedAt = row.deletedAt
    )
}
