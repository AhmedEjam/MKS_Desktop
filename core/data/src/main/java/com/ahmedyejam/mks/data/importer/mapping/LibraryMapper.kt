package com.ahmedyejam.mks.data.importer.mapping

import com.ahmedyejam.mks.data.importer.dto.BookDto
import com.ahmedyejam.mks.data.importer.dto.CategoryMetadataDto
import com.ahmedyejam.mks.data.importer.dto.CourseSlideDto
import com.ahmedyejam.mks.data.importer.dto.FlashcardDeckDto
import com.ahmedyejam.mks.data.importer.dto.FlashcardDto
import com.ahmedyejam.mks.data.importer.dto.NoteBlueprintDto
import com.ahmedyejam.mks.data.importer.dto.OptionDto
import com.ahmedyejam.mks.data.importer.dto.PromptCardDto
import com.ahmedyejam.mks.data.importer.dto.PromptDeckDto
import com.ahmedyejam.mks.data.importer.dto.QuestionDto
import com.ahmedyejam.mks.data.importer.dto.QuizDto
import com.ahmedyejam.mks.data.importer.dto.SessionDto
import com.ahmedyejam.mks.data.importer.dto.SlideshowCourseDto
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.CategoryMetadataEntity
import com.ahmedyejam.mks.data.local.entity.CourseSlideEntity
import com.ahmedyejam.mks.data.local.entity.FlashcardDeckEntity
import com.ahmedyejam.mks.data.local.entity.FlashcardEntity
import com.ahmedyejam.mks.data.local.entity.NoteBlueprintEntity
import com.ahmedyejam.mks.data.local.entity.PromptCardEntity
import com.ahmedyejam.mks.data.local.entity.PromptDeckEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.local.entity.SessionEntity
import com.ahmedyejam.mks.data.local.entity.SlideshowCourseEntity

class LibraryMapper {
    fun mapToBookEntity(
        dto: BookDto,
        workspaceId: Long,
        coverPath: String?,
    ): BookEntity {
        return BookEntity(
            workspaceId = workspaceId,
            externalId = dto.id,
            title = dto.title,
            description = dto.note,
            iconName = dto.coverIcon,
            coverImage = coverPath,
            createdAt = dto.createdAt ?: System.currentTimeMillis(),
            updatedAt = dto.updatedAt ?: System.currentTimeMillis(),
            contentUpdatedAt = dto.contentUpdatedAt ?: System.currentTimeMillis(),
            lastStudiedAt = dto.lastStudiedAt ?: 0,
        )
    }

    fun mapToQuizEntity(
        dto: QuizDto,
        localBookId: Long,
        coverPath: String?,
    ): QuizEntity {
        return QuizEntity(
            externalId = dto.id,
            bookId = localBookId,
            title = dto.title,
            description = dto.note,
            category = dto.storageKey,
            iconName = dto.coverIcon,
            coverImage = coverPath,
            createdAt = dto.createdAt ?: System.currentTimeMillis(),
            updatedAt = dto.updatedAt ?: System.currentTimeMillis(),
            contentUpdatedAt = dto.contentUpdatedAt ?: System.currentTimeMillis(),
            lastStudiedAt = dto.lastStudiedAt ?: 0,
        )
    }

    fun mapToQuestionEntity(
        dto: QuestionDto,
        localQuizId: Long,
        imagePath: String?,
    ): QuestionEntity {
        val type = if (dto.answerMode == "multiple") QuestionType.MULTIPLE_CHOICE else QuestionType.SINGLE_CHOICE
        val options = dto.options.map { it.text }
        val correctAnswers =
            dto.correct.mapNotNull { correctId ->
                dto.options.indexOfFirst { it.id == correctId }.takeIf { it != -1 }
            }

        // Sanitize imageSource to never store raw Base64 blocks
        val rawSource = dto.imageSource.ifBlank { dto.imageDataUrl }
        val sanitizedSource =
            if ((rawSource.startsWith("data:")) || (rawSource.length > 1000)) {
                null
            } else {
                rawSource
            }

        // Sanitize imageName to prevent path injection
        val sanitizedImageName = dto.imageName.takeIf { !it.startsWith("/") }

        return QuestionEntity(
            externalId = dto.id,
            quizId = localQuizId,
            text = dto.stem,
            type = type,
            options = options,
            correctAnswers = correctAnswers,
            explanation = dto.explanation,
            hint = dto.hint,
            reference = dto.reference,
            imagePath = imagePath ?: sanitizedImageName,
            imageName = sanitizedImageName,
            imageSource = sanitizedSource,
            categories = dto.categories,
            additionalInfo = dto.additionalInfo,
            sourceBookId = dto.sourceBookId,
            sourceQuizId = dto.sourceQuizId,
            sourceQuestionId = dto.sourceQuestionId,
        )
    }

    fun mapToSessionEntity(
        dto: SessionDto,
        localQuizId: Long,
        questionIdMap: Map<String, Long>,
    ): SessionEntity {
        // Map answer indices from external IDs to local indices
        val mappedAnswers =
            dto.answers.mapNotNull { (qExtId, optIndices) ->
                questionIdMap[qExtId]?.let { it to optIndices }
            }.toMap()

        val mappedDroppedOptions =
            dto.droppedOptions?.mapNotNull { (qExtId, optIndices) ->
                questionIdMap[qExtId]?.let { it to optIndices }
            }?.toMap() ?: emptyMap()

        val mappedVisibleOptionsCount =
            dto.visibleOptionsCount?.mapNotNull { (qExtId, count) ->
                questionIdMap[qExtId]?.let { it to count }
            }?.toMap() ?: emptyMap()

        return SessionEntity(
            quizId = localQuizId,
            label = dto.label ?: "Imported Session",
            currentQuestionIndex = dto.cursor ?: 0,
            score = dto.score,
            incorrectCount = dto.incorrectCount,
            answers = mappedAnswers,
            isCompleted = dto.finishedAt != null,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            lastModifiedAt = dto.lastModifiedAt ?: dto.updatedAt,
            lastStudiedAt = dto.lastStudiedAt ?: 0,
            shuffleQuestions = dto.shuffleQuestions ?: true,
            shuffleOptions = dto.shuffleOptions ?: true,
            rapidMode = dto.rapidMode ?: false,
            repeatWrong = dto.repeatWrong ?: true,
            quizTimerSeconds = dto.quizTimerSeconds ?: 0,
            questionTimerSeconds = dto.questionTimerSeconds ?: 0,
            rangeFrom = dto.rangeFrom ?: 0,
            rangeTo = dto.rangeTo ?: -1,
            includeFilters = dto.includeFilters ?: emptyList(),
            droppedOptions = mappedDroppedOptions,
            visibleOptionsCount = mappedVisibleOptionsCount,
        )
    }

    fun mapToCategoryMetadataEntity(dto: CategoryMetadataDto): CategoryMetadataEntity {
        return CategoryMetadataEntity(
            name = dto.name,
            emoji = dto.emoji,
            color = dto.color,
            isPinned = dto.isPinned,
        )
    }

    fun mapToBookDto(
        entity: BookEntity,
        workspaceExternalId: String? = null,
    ): BookDto {
        return BookDto(
            id = entity.externalId,
            workspaceExternalId = workspaceExternalId,
            title = entity.title,
            note = entity.description,
            coverIcon = entity.iconName ?: "📚",
            coverImage = entity.coverImage ?: "",
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            contentUpdatedAt = entity.contentUpdatedAt,
            lastStudiedAt = entity.lastStudiedAt,
        )
    }

    fun mapToQuizDto(
        entity: QuizEntity,
        questions: List<QuestionEntity>,
        bookExternalId: String = "",
    ): QuizDto {
        return QuizDto(
            id = entity.externalId,
            storageKey = entity.category,
            bookId = bookExternalId.ifBlank { "" },
            title = entity.title,
            note = entity.description,
            coverIcon = entity.iconName ?: "🎯",
            coverImage = entity.coverImage ?: "",
            createdAt = entity.createdAt,
            contentUpdatedAt = entity.contentUpdatedAt,
            updatedAt = entity.updatedAt,
            lastStudiedAt = entity.lastStudiedAt,
            questions = questions.map { mapToQuestionDto(it) },
        )
    }

    fun mapToQuestionDto(entity: QuestionEntity): QuestionDto {
        return QuestionDto(
            id = entity.externalId,
            stem = entity.text,
            options = entity.options.mapIndexed { i, opt -> OptionDto(id = "opt_$i", text = opt) },
            correct = entity.correctAnswers.map { "opt_$it" },
            explanation = entity.explanation ?: "",
            hint = entity.hint ?: "",
            reference = entity.reference ?: "",
            imageDataUrl = entity.imagePath ?: "",
            imageSource = entity.imageSource ?: "",
            imageName = entity.imageName ?: "",
            categories = entity.categories,
            answerMode = if (entity.type == QuestionType.MULTIPLE_CHOICE) "multiple" else "single",
            sourceQuizId = entity.sourceQuizId ?: "",
            sourceQuestionId = entity.sourceQuestionId ?: "",
            sourceBookId = entity.sourceBookId ?: "",
            additionalInfo = entity.additionalInfo ?: "",
        )
    }

    fun mapToSessionDto(
        entity: SessionEntity,
        quizExternalId: String,
        questions: List<QuestionEntity>,
    ): SessionDto {
        val questionIdMap = questions.associateBy({ it.id }) { it.externalId }

        val mappedAnswers =
            entity.answers.mapNotNull { (qId, optIndices) ->
                questionIdMap[qId]?.let { it to optIndices }
            }.toMap()

        val mappedDroppedOptions =
            entity.droppedOptions.mapNotNull { (qId, optIndices) ->
                questionIdMap[qId]?.let { it to optIndices }
            }.toMap()

        val mappedVisibleOptionsCount =
            entity.visibleOptionsCount.mapNotNull { (qId, count) ->
                questionIdMap[qId]?.let { it to count }
            }.toMap()

        return SessionDto(
            id = "session_${entity.id}",
            quizId = quizExternalId,
            label = entity.label,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            finishedAt = if (entity.isCompleted) entity.lastModifiedAt else null,
            cursor = entity.currentQuestionIndex,
            score = entity.score,
            incorrectCount = entity.incorrectCount,
            answers = mappedAnswers,
            lastModifiedAt = entity.lastModifiedAt,
            lastStudiedAt = entity.lastStudiedAt,
            lastEditedAt = entity.lastModifiedAt,
            shuffleQuestions = entity.shuffleQuestions,
            shuffleOptions = entity.shuffleOptions,
            rapidMode = entity.rapidMode,
            repeatWrong = entity.repeatWrong,
            quizTimerSeconds = entity.quizTimerSeconds,
            questionTimerSeconds = entity.questionTimerSeconds,
            rangeFrom = entity.rangeFrom,
            rangeTo = entity.rangeTo,
            includeFilters = entity.includeFilters,
            droppedOptions = mappedDroppedOptions,
            visibleOptionsCount = mappedVisibleOptionsCount,
        )
    }

    fun mapToCategoryMetadataDto(entity: CategoryMetadataEntity): CategoryMetadataDto {
        return CategoryMetadataDto(
            name = entity.name,
            emoji = entity.emoji,
            color = entity.color,
            isPinned = entity.isPinned,
        )
    }

    // Knowledge Bank Mappings

    @Suppress("unused")
    fun mapToFlashcardDeckEntity(
        dto: FlashcardDeckDto,
        localBookId: Long,
        coverPath: String?,
    ): FlashcardDeckEntity {
        return FlashcardDeckEntity(
            externalId = dto.id,
            bookId = localBookId,
            title = dto.title,
            description = dto.description,
            iconName = dto.iconName,
            coverImage = coverPath,
            isPinned = dto.isPinned,
            createdAt = dto.createdAt ?: System.currentTimeMillis(),
            updatedAt = dto.updatedAt ?: System.currentTimeMillis(),
        )
    }

    @Suppress("unused")
    fun mapToFlashcardEntity(
        dto: FlashcardDto,
        localDeckId: Long,
        imagePath: String?,
    ): FlashcardEntity {
        return FlashcardEntity(
            externalId = dto.id,
            deckId = localDeckId,
            frontText = dto.frontText,
            backText = dto.backText,
            hint = dto.hint,
            imagePath = imagePath ?: dto.imagePath,
            tags = dto.tags,
            orderIndex = dto.orderIndex,
            createdAt = dto.createdAt ?: System.currentTimeMillis(),
            updatedAt = dto.updatedAt ?: System.currentTimeMillis(),
        )
    }

    @Suppress("unused")
    fun mapToSlideshowCourseEntity(
        dto: SlideshowCourseDto,
        localBookId: Long,
        coverPath: String?,
    ): SlideshowCourseEntity {
        return SlideshowCourseEntity(
            externalId = dto.id,
            bookId = localBookId,
            title = dto.title,
            description = dto.description,
            coverImage = coverPath,
            isPinned = dto.isPinned,
            createdAt = dto.createdAt ?: System.currentTimeMillis(),
            updatedAt = dto.updatedAt ?: System.currentTimeMillis(),
            lastEditedAt = dto.updatedAt ?: System.currentTimeMillis(),
        )
    }

    @Suppress("unused")
    fun mapToCourseSlideEntity(
        dto: CourseSlideDto,
        localCourseId: Long,
        imagePath: String?,
    ): CourseSlideEntity {
        return CourseSlideEntity(
            externalId = dto.id,
            courseId = localCourseId,
            title = dto.title,
            body = dto.body,
            speakerNotes = dto.speakerNotes,
            imagePath = imagePath ?: dto.imagePath,
            orderIndex = dto.orderIndex,
            isCompleted = dto.isCompleted,
            createdAt = dto.createdAt ?: System.currentTimeMillis(),
            updatedAt = dto.updatedAt ?: System.currentTimeMillis(),
        )
    }

    @Suppress("unused")
    fun mapToNoteBlueprintEntity(
        dto: NoteBlueprintDto,
        localCollectionId: Long,
    ): NoteBlueprintEntity {
        return NoteBlueprintEntity(
            externalId = dto.id,
            collectionId = localCollectionId,
            title = dto.title,
            summary = dto.summary,
            body = dto.body,
            bulletPoints = dto.bulletPoints,
            tags = dto.tags,
            blueprintMode = dto.mode,
            reviewStatus = dto.reviewStatus,
            createdAt = dto.createdAt ?: System.currentTimeMillis(),
            updatedAt = dto.updatedAt ?: System.currentTimeMillis(),
        )
    }

    @Suppress("unused")
    fun mapToPromptDeckEntity(
        dto: PromptDeckDto,
        localBookId: Long,
    ): PromptDeckEntity {
        return PromptDeckEntity(
            bookId = localBookId,
            title = dto.title,
            description = dto.description,
            tags = dto.tags,
            createdAt = dto.createdAt ?: System.currentTimeMillis(),
            updatedAt = dto.updatedAt ?: System.currentTimeMillis(),
        )
    }

    @Suppress("unused")
    fun mapToPromptCardEntity(
        dto: PromptCardDto,
        localDeckId: Long,
    ): PromptCardEntity {
        return PromptCardEntity(
            deckId = localDeckId,
            title = dto.title,
            promptText = dto.promptText,
            variablesJson = dto.variablesJson,
            outputType = dto.outputType,
            sortOrder = dto.sortOrder,
            createdAt = dto.createdAt ?: System.currentTimeMillis(),
            updatedAt = dto.updatedAt ?: System.currentTimeMillis(),
        )
    }

    fun mapToFlashcardDeckDto(
        entity: FlashcardDeckEntity,
        cards: List<FlashcardEntity>,
        bookExternalId: String,
    ): FlashcardDeckDto {
        return FlashcardDeckDto(
            id = entity.externalId,
            bookId = bookExternalId,
            title = entity.title,
            description = entity.description,
            iconName = entity.iconName,
            coverImage = entity.coverImage,
            isPinned = entity.isPinned,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            cards = cards.map { mapToFlashcardDto(it) },
        )
    }

    fun mapToFlashcardDto(entity: FlashcardEntity): FlashcardDto {
        return FlashcardDto(
            id = entity.externalId,
            frontText = entity.frontText,
            backText = entity.backText,
            hint = entity.hint,
            imagePath = entity.imagePath,
            tags = entity.tags,
            orderIndex = entity.orderIndex,
            sourceQuestionId = entity.sourceQuestionId?.toString(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    fun mapToSlideshowCourseDto(
        entity: SlideshowCourseEntity,
        slides: List<CourseSlideEntity>,
        bookExternalId: String,
    ): SlideshowCourseDto {
        return SlideshowCourseDto(
            id = entity.externalId,
            bookId = bookExternalId,
            title = entity.title,
            description = entity.description,
            coverImage = entity.coverImage,
            isPinned = entity.isPinned,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            slides = slides.map { mapToCourseSlideDto(it) },
        )
    }

    fun mapToCourseSlideDto(entity: CourseSlideEntity): CourseSlideDto {
        return CourseSlideDto(
            id = entity.externalId,
            title = entity.title,
            body = entity.body,
            speakerNotes = entity.speakerNotes,
            imagePath = entity.imagePath,
            orderIndex = entity.orderIndex,
            isCompleted = entity.isCompleted,
            sourceQuestionId = entity.sourceQuestionId?.toString(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    fun mapToNoteBlueprintDto(
        entity: NoteBlueprintEntity,
        bookExternalId: String,
    ): NoteBlueprintDto {
        return NoteBlueprintDto(
            id = entity.externalId,
            bookId = bookExternalId,
            title = entity.title,
            summary = entity.summary,
            body = entity.body,
            bulletPoints = entity.bulletPoints,
            tags = entity.tags,
            mode = entity.blueprintMode,
            reviewStatus = entity.reviewStatus,
            sourceQuestionId = entity.sourceQuestionId?.toString(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    fun mapToPromptDeckDto(
        entity: PromptDeckEntity,
        cards: List<PromptCardEntity>,
        bookExternalId: String,
    ): PromptDeckDto {
        return PromptDeckDto(
            id = "prompt-deck-${entity.id}",
            bookId = bookExternalId,
            title = entity.title,
            description = entity.description,
            tags = entity.tags,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            cards = cards.map { mapToPromptCardDto(it) },
        )
    }

    fun mapToPromptCardDto(entity: PromptCardEntity): PromptCardDto {
        return PromptCardDto(
            id = "prompt-card-${entity.id}",
            title = entity.title,
            promptText = entity.promptText,
            variablesJson = entity.variablesJson,
            outputType = entity.outputType,
            sortOrder = entity.sortOrder,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
