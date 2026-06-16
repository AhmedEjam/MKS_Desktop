package com.ahmedyejam.mks.ui.booktools


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.local.entity.BlueprintMode
import com.ahmedyejam.mks.data.local.entity.BlueprintReviewStatus
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.CourseSlideEntity
import com.ahmedyejam.mks.data.local.entity.FlashcardDeckEntity
import com.ahmedyejam.mks.data.local.entity.MistakeLogEntryEntity
import com.ahmedyejam.mks.data.local.entity.NoteBlueprintEntity
import com.ahmedyejam.mks.data.local.entity.PromptCardEntity
import com.ahmedyejam.mks.data.local.entity.PromptDeckEntity
import com.ahmedyejam.mks.data.local.entity.PromptOutputType
import com.ahmedyejam.mks.data.local.entity.PromptRunEntity
import com.ahmedyejam.mks.data.local.entity.QuestionAssetEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.local.entity.SlideshowCourseEntity
import com.ahmedyejam.mks.data.local.entity.SourceDocumentEntity
import com.ahmedyejam.mks.data.local.entity.SourceDocumentTypes
import com.ahmedyejam.mks.data.preferences.DataStoreManager
import com.ahmedyejam.mks.data.repository.AssetRepository
import com.ahmedyejam.mks.data.repository.BookKnowledgeSummary
import com.ahmedyejam.mks.data.repository.BookRepository
import com.ahmedyejam.mks.data.repository.KnowledgeRepository
import com.ahmedyejam.mks.data.repository.OllamaRepository
import com.ahmedyejam.mks.data.repository.QuizRepository
import com.ahmedyejam.mks.data.repository.SortOption
import com.ahmedyejam.mks.data.repository.StudyRepository
import com.ahmedyejam.mks.ui.library.components.QuizCreationFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun String.escapeJsonValue(): String = buildString {
    this@escapeJsonValue.forEach { ch ->
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(ch)
        }
    }
}

private fun Map<String, String>.toSimpleJson(): String = entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
    "\"${key.escapeJsonValue()}\":\"${value.escapeJsonValue()}\""
}

data class BookToolsUiState(
    val book: BookEntity? = null,
    val quizzes: List<QuizEntity> = emptyList(),
    val questions: List<QuestionEntity> = emptyList(),
    val questionsByQuiz: Map<Long, List<QuestionEntity>> = emptyMap(),
    val questionAssetsByQuestion: Map<Long, List<QuestionAssetEntity>> = emptyMap(),
    val flashcardDecks: List<FlashcardDeckEntity> = emptyList(),
    val allCourses: List<SlideshowCourseEntity> = emptyList(),
    val allNotes: List<NoteBlueprintEntity> = emptyList(),
    val allSources: List<SourceDocumentEntity> = emptyList(),
    val allPromptDecks: List<PromptDeckEntity> = emptyList(),
    val noteBlueprint: NoteBlueprintEntity? = null,
    val promptDeck: PromptDeckEntity? = null,
    val promptCards: List<PromptCardEntity> = emptyList(),
    val promptRuns: List<PromptRunEntity> = emptyList(),
    val slideshowCourse: SlideshowCourseEntity? = null,
    val courseSlides: List<CourseSlideEntity> = emptyList(),
    val mistakes: List<MistakeLogEntryEntity> = emptyList(),
    val bookSummary: BookKnowledgeSummary? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isGenerating: Boolean = false
)

@HiltViewModel
class BookToolsViewModel @Inject constructor(
    private val ollamaRepository: OllamaRepository,
    private val dataStoreManager: DataStoreManager,
    private val fileManager: com.ahmedyejam.mks.data.local.FileManager,
    private val bookRepository: BookRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val assetRepository: AssetRepository,
    private val quizRepository: QuizRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookToolsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val bundle = bookRepository.getBookStudyBundle(bookId)
                if (bundle == null) {
                    _uiState.value = BookToolsUiState(isLoading = false, error = "Book not found")
                    return@launch
                }
                _uiState.value = BookToolsUiState(
                    book = bundle.book.copy(coverImage = assetRepository.resolveImagePath(bundle.book.coverImage)),
                    quizzes = bundle.quizzes.map { it.copy(coverImage = assetRepository.resolveImagePath(it.coverImage)) },
                    questions = bundle.questions.map { it.copy(imagePath = assetRepository.resolveImagePath(it.imagePath)) },
                    questionsByQuiz = bundle.questionsByQuiz.mapValues { (_, questions) ->
                        questions.map { it.copy(imagePath = assetRepository.resolveImagePath(it.imagePath)) }
                    },
                    questionAssetsByQuestion = bundle.questionAssets.groupBy { it.questionId },
                    flashcardDecks = bundle.flashcardDecks.map { it.copy(coverImage = assetRepository.resolveImagePath(it.coverImage)) },
                    allCourses = bundle.slideshowCourses.map { it.copy(coverImage = assetRepository.resolveImagePath(it.coverImage)) },
                    allNotes = bundle.noteBlueprints,
                    allSources = bundle.sourceDocuments,
                    allPromptDecks = bundle.promptDecks,
                    mistakes = bundle.mistakes,
                    bookSummary = bookRepository.getBookKnowledgeSummary(bookId),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load book")
            }
        }
    }



    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val note = knowledgeRepository.getNoteBlueprintById(noteId)
            _uiState.value = if (note == null) {
                _uiState.value.copy(isLoading = false, error = "Blueprint not found")
            } else {
                _uiState.value.copy(noteBlueprint = note, isLoading = false)
            }
        }
    }

    fun updateQuestionNote(questionId: Long, note: String) {
        val question = _uiState.value.questions.firstOrNull { it.id == questionId } ?: return
        viewModelScope.launch {
            assetRepository.updateQuestion(question.copy(notes = note))
            _uiState.value = _uiState.value.copy(
                questions = _uiState.value.questions.map { if (it.id == questionId) it.copy(notes = note) else it },
                questionsByQuiz = _uiState.value.questionsByQuiz.mapValues { (_, questions) ->
                    questions.map { if (it.id == questionId) it.copy(notes = note) else it }
                },
                successMessage = "Question note updated"
            )
        }
    }

    fun toggleMistakeFixed(mistakeId: Long) {
        viewModelScope.launch {
            val mistake = _uiState.value.mistakes.find { it.id == mistakeId } ?: return@launch
            try {
                val updatedMistake = mistake.copy(isFixed = !mistake.isFixed)
                studyRepository.updateMistake(updatedMistake)
                _uiState.value = _uiState.value.copy(
                    mistakes = _uiState.value.mistakes.map { if (it.id == mistakeId) updatedMistake else it },
                    successMessage = if (updatedMistake.isFixed) "Mistake marked as fixed" else "Mistake unmarked"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to update mistake: ${e.message}")
            }
        }
    }

    fun deleteMistake(mistake: MistakeLogEntryEntity) {
        viewModelScope.launch {
            runCatching { studyRepository.deleteMistake(mistake) }
        }
    }

    fun snoozeMistake(mistakeId: Long, snoozeTime: Long) {
        viewModelScope.launch {
            runCatching { studyRepository.snoozeMistake(mistakeId, snoozeTime) }
        }
    }

    fun createNote(bookId: Long, title: String, body: String, sourceQuestionId: Long? = null, mode: String = BlueprintMode.SIMPLE_NOTE) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                val note = NoteBlueprintEntity(
                    externalId = java.util.UUID.randomUUID().toString(),
                    collectionId = knowledgeRepository.getOrCreateDefaultNoteCollection(bookId),
                    title = title.ifBlank { "Untitled blueprint" },
                    body = body,
                    bulletPoints = emptyList(),
                    tags = emptyList(),
                    blueprintMode = mode,
                    sourceQuestionId = sourceQuestionId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val id = knowledgeRepository.insertNoteBlueprint(note)
                val saved = knowledgeRepository.getNoteBlueprintById(id) ?: note.copy(id = id)
                _uiState.value = _uiState.value.copy(
                    noteBlueprint = saved,
                    allNotes = (_uiState.value.allNotes.filter { it.id != id } + saved).sortedByDescending { it.updatedAt },
                    isSaving = false,
                    successMessage = "Blueprint created"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = "Failed to create blueprint: ${e.message}")
            }
        }
    }

    fun updateNote(note: NoteBlueprintEntity) {
        viewModelScope.launch {
            try {
                val updated = note.copy(updatedAt = System.currentTimeMillis())
                knowledgeRepository.updateNoteBlueprint(updated)
                _uiState.value = _uiState.value.copy(
                    noteBlueprint = updated,
                    allNotes = _uiState.value.allNotes.map { if (it.id == updated.id) updated else it },
                    successMessage = "Blueprint updated"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to update blueprint: ${e.message}")
            }
        }
    }

    fun deleteNote(note: NoteBlueprintEntity) {
        viewModelScope.launch {
            try {
                knowledgeRepository.deleteNoteBlueprint(note)
                _uiState.value = _uiState.value.copy(
                    noteBlueprint = null,
                    allNotes = _uiState.value.allNotes.filter { it.id != note.id },
                    successMessage = "Blueprint deleted"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete blueprint: ${e.message}")
            }
        }
    }

    fun recordNoteReview(note: NoteBlueprintEntity) {
        viewModelScope.launch {
            val updated = note.copy(
                reviewCount = note.reviewCount + 1,
                lastReviewedAt = System.currentTimeMillis(),
                reviewStatus = BlueprintReviewStatus.REVIEWED,
                updatedAt = System.currentTimeMillis()
            )
            knowledgeRepository.updateNoteBlueprint(updated)
            _uiState.value = _uiState.value.copy(
                noteBlueprint = updated,
                allNotes = _uiState.value.allNotes.map { if (it.id == updated.id) updated else it },
                successMessage = "Blueprint reviewed"
            )
        }
    }

    fun createSource(bookId: Long, title: String, type: String, details: String = "", url: String = "") {
        viewModelScope.launch {
            try {
                val source = SourceDocumentEntity(
                    bookId = bookId,
                    title = title.trim().ifBlank { "Untitled source" },
                    sourceType = type.ifBlank { SourceDocumentTypes.OTHER },
                    description = details.takeIf { it.isNotBlank() },
                    externalUrl = url.takeIf { it.isNotBlank() && (it.startsWith("http") || it.startsWith("content://")) },
                    localPath = url.takeIf { it.isNotBlank() && !(it.startsWith("http") || it.startsWith("content://")) }
                )
                val id = assetRepository.insertSourceDocument(source)
                val saved = assetRepository.getSourceDocumentById(id) ?: source.copy(id = id)
                _uiState.value = _uiState.value.copy(
                    allSources = (_uiState.value.allSources.filter { it.id != id } + saved).sortedByDescending { it.updatedAt },
                    successMessage = "Source created"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create source: ${e.message}")
            }
        }
    }

    fun updateSource(source: SourceDocumentEntity) {
        viewModelScope.launch {
            try {
                val updated = source.copy(updatedAt = System.currentTimeMillis())
                assetRepository.updateSourceDocument(updated)
                _uiState.value = _uiState.value.copy(
                    allSources = _uiState.value.allSources.map { if (it.id == updated.id) updated else it },
                    successMessage = "Source updated"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to update source: ${e.message}")
            }
        }
    }

    fun deleteSource(source: SourceDocumentEntity) {
        viewModelScope.launch {
            try {
                assetRepository.permanentlyDeleteSourceDocument(source)
                _uiState.value = _uiState.value.copy(
                    allSources = _uiState.value.allSources.filter { it.id != source.id },
                    successMessage = "Source deleted"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete source: ${e.message}")
            }
        }
    }




    fun generateArticlesFromQuestions(questionIds: List<Long>, config: com.ahmedyejam.mks.data.model.ArticleGenerationConfig) {
        val bookId = _uiState.value.book?.id ?: return
        viewModelScope.launch {
            try {
                val newIds = knowledgeRepository.addArticlesFromQuestions(bookId, questionIds, config)
                _uiState.value = _uiState.value.copy(
                    allNotes = knowledgeRepository.getNoteBlueprintsByBookId(bookId).first(),
                    successMessage = "Generated ${newIds.size} Articles"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to generate articles: ${e.message}")
            }
        }
    }

    fun generateArticlesFromMarked(config: com.ahmedyejam.mks.data.model.ArticleGenerationConfig = com.ahmedyejam.mks.data.model.ArticleGenerationConfig.DEFAULT) {
        val questions = _uiState.value.questions.filter { it.isMarked }
        generateArticlesFromQuestions(questions.map { it.id }, config)
    }

    fun generateArticlesFromMissed(config: com.ahmedyejam.mks.data.model.ArticleGenerationConfig = com.ahmedyejam.mks.data.model.ArticleGenerationConfig.DEFAULT) {
        val questions = _uiState.value.questions.filter { it.attempts > 0 && (it.correctCount < it.attempts || it.lastAttemptResult == false) }
        generateArticlesFromQuestions(questions.map { it.id }, config)
    }

    fun generateArticlesFromQuiz(quizId: Long, config: com.ahmedyejam.mks.data.model.ArticleGenerationConfig = com.ahmedyejam.mks.data.model.ArticleGenerationConfig.DEFAULT) {
        val questions = _uiState.value.questionsByQuiz[quizId] ?: return
        generateArticlesFromQuestions(questions.map { it.id }, config)
    }

    fun generateArticlesFromCategory(categoryId: Long, config: com.ahmedyejam.mks.data.model.ArticleGenerationConfig = com.ahmedyejam.mks.data.model.ArticleGenerationConfig.DEFAULT) {
        val questions = _uiState.value.questions.filter { categoryId.toString() in it.categories }
        generateArticlesFromQuestions(questions.map { it.id }, config)
    }

    fun importArticlesFromText(text: String, mode: com.ahmedyejam.mks.data.importer.parser.TextArticleParseMode = com.ahmedyejam.mks.data.importer.parser.TextArticleParseMode.BASIC) {
        val bookId = _uiState.value.book?.id ?: return
        viewModelScope.launch {
            try {
                val parser = com.ahmedyejam.mks.data.importer.parser.TextArticleParser()
                val parsedNotes = parser.parse(text, bookId, mode)

                if (parsedNotes.isEmpty()) {
                    _uiState.value = _uiState.value.copy(error = "No valid articles found in text")
                    return@launch
                }

                parsedNotes.forEach { note ->
                    knowledgeRepository.insertNoteBlueprint(note)
                }

                _uiState.value = _uiState.value.copy(
                    allNotes = knowledgeRepository.getNoteBlueprintsByBookId(bookId).first(),
                    successMessage = "Imported ${parsedNotes.size} Articles"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to import articles: ${e.message}")
            }
        }
    }

    fun createBlueprintFromQuestion(bookId: Long, questionId: Long, mode: String = BlueprintMode.CONCEPT_TEMPLATE) {
        viewModelScope.launch {
            try {
                val id = knowledgeRepository.createBlueprintFromQuestion(bookId, questionId, mode)
                val saved = knowledgeRepository.getNoteBlueprintById(id)
                _uiState.value = _uiState.value.copy(
                    allNotes = saved?.let { (_uiState.value.allNotes.filter { note -> note.id != id } + it).sortedByDescending { note -> note.updatedAt } } ?: _uiState.value.allNotes,
                    successMessage = "Article created from question"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create article: ${e.message}")
            }
        }
    }

    fun createBlueprintFromMarked(bookId: Long, title: String) {
        viewModelScope.launch {
            try {
                val id = knowledgeRepository.createBlueprintFromMarkedQuestions(bookId, title)
                val saved = knowledgeRepository.getNoteBlueprintById(id)
                _uiState.value = _uiState.value.copy(
                    allNotes = saved?.let { (_uiState.value.allNotes.filter { note -> note.id != id } + it).sortedByDescending { note -> note.updatedAt } } ?: _uiState.value.allNotes,
                    successMessage = "Article created from marked questions"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create marked article: ${e.message}")
            }
        }
    }

    fun createBlueprintFromMissed(bookId: Long, title: String) {
        viewModelScope.launch {
            try {
                val id = knowledgeRepository.createBlueprintFromMissedQuestions(bookId, title)
                val saved = knowledgeRepository.getNoteBlueprintById(id)
                _uiState.value = _uiState.value.copy(
                    allNotes = saved?.let { (_uiState.value.allNotes.filter { note -> note.id != id } + it).sortedByDescending { note -> note.updatedAt } } ?: _uiState.value.allNotes,
                    successMessage = "Article created from missed questions"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create missed article: ${e.message}")
            }
        }
    }

    fun createFlashcardsFromBlueprint(noteId: Long) {
        viewModelScope.launch {
            try {
                knowledgeRepository.createFlashcardDeckFromBlueprint(noteId)
                _uiState.value = _uiState.value.copy(successMessage = "Flashcards created from blueprint")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create flashcards: ${e.message}")
            }
        }
    }

    fun appendBlueprintToQuestionNote(noteId: Long) {
        viewModelScope.launch {
            try {
                assetRepository.appendBlueprintToSourceQuestionNote(noteId)
                _uiState.value = _uiState.value.copy(successMessage = "Blueprint appended to question note")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to append blueprint: ${e.message}")
            }
        }
    }

    fun createSlideshowCourse(bookId: Long, title: String, description: String? = null) {
        viewModelScope.launch {
            try {
                val course = SlideshowCourseEntity(
                    externalId = java.util.UUID.randomUUID().toString(),
                    bookId = bookId,
                    title = title.ifBlank { "Untitled slideshow" },
                    description = description,
                    coverImage = null,
                    slideCount = 0,
                    studiedSlideCount = 0,
                    progress = 0f,
                    isSystem = false,
                    isPinned = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    lastStudiedAt = 0,
                    lastEditedAt = System.currentTimeMillis(),
                    isDerived = false,
                    sourceQuizId = null
                )
                val id = knowledgeRepository.insertSlideshowCourse(course)
                val saved = knowledgeRepository.getSlideshowCourseById(id) ?: course.copy(id = id)
                _uiState.value = _uiState.value.copy(
                    slideshowCourse = saved,
                    allCourses = (_uiState.value.allCourses.filter { it.id != id } + saved).sortedByDescending { it.lastEditedAt },
                    successMessage = "Slideshow created"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create slideshow: ${e.message}")
            }
        }
    }

    fun updateSlideshowCourse(course: SlideshowCourseEntity) {
        viewModelScope.launch {
            try {
                val updated = course.copy(updatedAt = System.currentTimeMillis(), lastEditedAt = System.currentTimeMillis())
                knowledgeRepository.updateSlideshowCourse(updated)
                _uiState.value = _uiState.value.copy(
                    slideshowCourse = updated,
                    allCourses = _uiState.value.allCourses.map { if (it.id == updated.id) updated else it },
                    successMessage = "Slideshow updated"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to update slideshow: ${e.message}")
            }
        }
    }

    fun deleteSlideshowCourse(course: SlideshowCourseEntity) {
        viewModelScope.launch {
            try {
                knowledgeRepository.deleteSlideshowCourse(course)
                _uiState.value = _uiState.value.copy(
                    slideshowCourse = null,
                    courseSlides = emptyList(),
                    allCourses = _uiState.value.allCourses.filter { it.id != course.id },
                    successMessage = "Slideshow deleted"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete slideshow: ${e.message}")
            }
        }
    }


    fun loadPromptDeck(deckId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val deck = knowledgeRepository.getPromptDeckById(deckId)
            if (deck == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Prompt deck not found")
                return@launch
            }
            val bundle = bookRepository.getBookStudyBundle(deck.bookId)
            _uiState.value = _uiState.value.copy(
                promptDeck = deck,
                promptCards = knowledgeRepository.getPromptCardsByDeckIdNow(deckId),
                promptRuns = knowledgeRepository.getPromptRunsByDeckIdNow(deckId),
                book = bundle?.book,
                questions = bundle?.questions ?: emptyList(),
                allNotes = bundle?.noteBlueprints ?: emptyList(),
                allSources = bundle?.sourceDocuments ?: emptyList(),
                quizzes = bundle?.quizzes ?: emptyList(),
                isLoading = false
            )
        }
    }

    fun createPromptDeck(bookId: Long, title: String, description: String? = null, seedDefaultCards: Boolean = false) {
        viewModelScope.launch {
            try {
                val id = knowledgeRepository.createDefaultPromptDeck(bookId, title, description, seedDefaultCards)
                val saved = knowledgeRepository.getPromptDeckById(id)
                _uiState.value = _uiState.value.copy(
                    allPromptDecks = saved?.let { (_uiState.value.allPromptDecks.filter { deck -> deck.id != id } + it).sortedByDescending { deck -> deck.updatedAt } } ?: _uiState.value.allPromptDecks,
                    successMessage = "Prompt deck created"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create prompt deck: ${e.message}")
            }
        }
    }

    fun updatePromptDeck(deck: PromptDeckEntity) {
        viewModelScope.launch {
            try {
                val updated = deck.copy(updatedAt = System.currentTimeMillis())
                knowledgeRepository.updatePromptDeck(updated)
                _uiState.value = _uiState.value.copy(
                    promptDeck = updated,
                    allPromptDecks = _uiState.value.allPromptDecks.map { if (it.id == updated.id) updated else it },
                    successMessage = "Prompt deck updated"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to update prompt deck: ${e.message}")
            }
        }
    }

    fun deletePromptDeck(deck: PromptDeckEntity) {
        viewModelScope.launch {
            try {
                knowledgeRepository.deletePromptDeck(deck)
                _uiState.value = _uiState.value.copy(
                    promptDeck = null,
                    promptCards = emptyList(),
                    promptRuns = emptyList(),
                    allPromptDecks = _uiState.value.allPromptDecks.filter { it.id != deck.id },
                    successMessage = "Prompt deck deleted"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete prompt deck: ${e.message}")
            }
        }
    }

    fun createPromptCard(deckId: Long, title: String, promptText: String, outputType: String = PromptOutputType.OTHER) {
        viewModelScope.launch {
            try {
                val variablesJson = extractVariables(promptText).joinToString(prefix = "[", postfix = "]") { "\"${it.escapeJsonValue()}\"" }
                val id = knowledgeRepository.insertPromptCard(
                    PromptCardEntity(
                        deckId = deckId,
                        title = title.ifBlank { "Untitled prompt" },
                        promptText = promptText,
                        variablesJson = variablesJson,
                        outputType = outputType,
                        sortOrder = _uiState.value.promptCards.size
                    )
                )
                val saved = knowledgeRepository.getPromptCardsByDeckIdNow(deckId).firstOrNull { it.id == id }
                _uiState.value = _uiState.value.copy(
                    promptCards = knowledgeRepository.getPromptCardsByDeckIdNow(deckId),
                    successMessage = if (saved != null) "Prompt card created" else "Prompt card created"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create prompt card: ${e.message}")
            }
        }
    }

    fun createTemplatePromptCard(deckId: Long, templateType: String) {
        viewModelScope.launch {
            try {
                val title = when (templateType) {
                    "QUIZ" -> "Quiz generator"
                    "FLASHCARDS" -> "Flashcard generator"
                    "BLUEPRINT" -> "Blueprint maker"
                    "EXPLAIN" -> "Explain & Teach"
                    else -> "Template Prompt"
                }
                val promptText = when (templateType) {
                    "QUIZ" -> "<system_role>You are an expert test creator and medical educator.</system_role>\n<instructions>Analyze the provided material and generate 3 to 5 high-quality, challenging multiple-choice questions.\nEnsure that:\n1. The stem is clinically relevant or focuses on key conceptual understanding.\n2. There are 4-5 plausible options.\n3. The correct answer is unambiguously correct.\n4. A detailed rationale/explanation is provided for why the correct answer is right and why the distractors are wrong.\nOutput strictly in JSON format as an array of objects with keys: \"question\", \"options\" (array of strings), \"answer\" (string), \"explanation\" (string).</instructions>\n<material>\n{material}\n</material>"
                    "FLASHCARDS" -> "<system_role>You are an expert learning designer.</system_role>\n<instructions>Convert the core concepts from the provided material into concise, high-yield spaced-repetition flashcards.\nFollow these rules:\n1. Make the front of the card a clear, unambiguous question or cloze deletion.\n2. Keep the back of the card concise and to the point.\n3. Provide an optional brief hint to aid recall.\nOutput strictly in JSON format as an array of objects with keys: \"front\", \"back\", \"hint\".</instructions>\n<material>\n{material}\n</material>"
                    "BLUEPRINT" -> "<system_role>You are an expert technical writer and educator.</system_role>\n<instructions>Synthesize the provided material into a highly structured, comprehensive, yet easy-to-digest study blueprint.\nFormat the output in Markdown with the following sections:\n- **Core Summary**: A brief 2-3 sentence overview.\n- **Key Concepts**: Bulleted list of the most important takeaways.\n- **Detailed Breakdown**: A structured analysis with subheadings.\n- **Common Pitfalls / Mistakes to Avoid**: What students usually get wrong about this.\n- **Review Cues**: Short questions to test memory.</instructions>\n<material>\n{material}\n</material>"
                    "EXPLAIN" -> "<system_role>You are an expert tutor in {relevant_speciality}.</system_role>\n<instructions>Please explain the following question part: {part_to_explain}.\n\nHere is the full question context:\n{question}</instructions>"
                    else -> ""
                }
                val outputType = when (templateType) {
                    "QUIZ" -> PromptOutputType.QUIZ
                    "FLASHCARDS" -> PromptOutputType.FLASHCARDS
                    "BLUEPRINT" -> PromptOutputType.BLUEPRINT
                    "EXPLAIN" -> PromptOutputType.NOTE
                    else -> PromptOutputType.OTHER
                }
                val variablesJson = extractVariables(promptText).joinToString(prefix = "[", postfix = "]") { "\"${it.escapeJsonValue()}\"" }
                val id = knowledgeRepository.insertPromptCard(
                    PromptCardEntity(
                        deckId = deckId,
                        title = title,
                        promptText = promptText,
                        variablesJson = variablesJson,
                        outputType = outputType,
                        sortOrder = _uiState.value.promptCards.size
                    )
                )
                _uiState.value = _uiState.value.copy(
                    promptCards = knowledgeRepository.getPromptCardsByDeckIdNow(deckId),
                    successMessage = "Template prompt card created"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create template card: ${e.message}")
            }
        }
    }

    fun updatePromptCard(card: PromptCardEntity) {
        viewModelScope.launch {
            try {
                val updated = card.copy(
                    variablesJson = extractVariables(card.promptText).joinToString(prefix = "[", postfix = "]") { "\"${it.escapeJsonValue()}\"" },
                    updatedAt = System.currentTimeMillis()
                )
                knowledgeRepository.updatePromptCard(updated)
                _uiState.value = _uiState.value.copy(
                    promptCards = _uiState.value.promptCards.map { if (it.id == updated.id) updated else it },
                    successMessage = "Prompt card updated"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to update prompt card: ${e.message}")
            }
        }
    }

    fun deletePromptCard(card: PromptCardEntity) {
        viewModelScope.launch {
            try {
                knowledgeRepository.deletePromptCard(card)
                _uiState.value = _uiState.value.copy(
                    promptCards = _uiState.value.promptCards.filter { it.id != card.id },
                    successMessage = "Prompt card deleted"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete prompt card: ${e.message}")
            }
        }
    }

    fun reorderPromptCards(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.promptCards.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices) return
        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        val updatedList = currentList.mapIndexed { index, card -> card.copy(sortOrder = index) }
        _uiState.value = _uiState.value.copy(promptCards = updatedList)
        viewModelScope.launch {
            try {
                knowledgeRepository.updatePromptCards(updatedList)
            } catch (e: Exception) {
                // Revert or show error
                _uiState.value = _uiState.value.copy(error = "Failed to save card order: ${e.message}")
            }
        }
    }

    fun movePromptCard(card: PromptCardEntity, up: Boolean) {
        val currentList = _uiState.value.promptCards
        val index = currentList.indexOfFirst { it.id == card.id }
        if (index == -1) return
        val targetIndex = if (up) index - 1 else index + 1
        reorderPromptCards(index, targetIndex)
    }

    fun recordPromptCardRun(card: PromptCardEntity, inputValues: Map<String, String>, renderedPrompt: String, outputText: String? = null) {
        viewModelScope.launch {
            try {
                knowledgeRepository.recordPromptRun(card.id, inputValues.toSimpleJson(), renderedPrompt, outputText)
                _uiState.value = _uiState.value.copy(
                    promptCards = knowledgeRepository.getPromptCardsByDeckIdNow(card.deckId),
                    promptRuns = knowledgeRepository.getPromptRunsByDeckIdNow(card.deckId),
                    successMessage = if (outputText.isNullOrBlank()) "Prompt run recorded" else "Prompt output saved"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to record prompt run: ${e.message}")
            }
        }
    }

    private var generationJob: kotlinx.coroutines.Job? = null

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    fun generateWithOllamaStream(prompt: String, images: List<String> = emptyList(), onUpdate: (String) -> Unit) {
        cancelGeneration()
        generationJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
            try {
                val baseUrl = dataStoreManager.ollamaBaseUrl.first()
                val model = dataStoreManager.ollamaModelName.first()
                val apiKey = dataStoreManager.ollamaApiKey.first().takeIf { it.isNotBlank() }
                
                val systemPrompt = "You are an expert educational AI assistant inside the MKS knowledge-bank app. Generate high-quality, structured output exactly as requested by the user's prompt. Do NOT include conversational filler, greetings, or explanations unless explicitly requested."
                
                var accumulatedResponse = ""
                ollamaRepository.generateCompletionStream(
                    baseUrl,
                    model,
                    prompt,
                    systemPrompt,
                    null,
                    images.takeIf { it.isNotEmpty() },
                    apiKey
                )
                    .collect { chunk ->
                        accumulatedResponse += chunk
                        onUpdate(accumulatedResponse)
                    }
                _uiState.value = _uiState.value.copy(isGenerating = false)
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.value = _uiState.value.copy(isGenerating = false)
            } catch (e: com.ahmedyejam.mks.data.repository.OllamaApiException) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false, 
                    error = "Ollama Error: ${e.message}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false, 
                    error = "Ollama connection failed: ${e.message}. Ensure Ollama is running and accessible."
                )
            }
        }
    }

    fun savePromptOutputAsNote(card: PromptCardEntity, outputText: String, title: String) {
        val bookId = _uiState.value.promptDeck?.bookId ?: return
        viewModelScope.launch {
            try {
                val id = knowledgeRepository.convertPromptOutputToNote(bookId, title, outputText, card.id)
                _uiState.value = _uiState.value.copy(
                    allNotes = knowledgeRepository.getNoteBlueprintsByBookId(bookId).first(),
                    promptRuns = knowledgeRepository.getPromptRunsByDeckIdNow(card.deckId),
                    successMessage = "Prompt output saved as note #$id"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to save note: ${e.message}")
            }
        }
    }

    fun savePromptOutputAsBlueprint(card: PromptCardEntity, outputText: String, title: String) {
        val bookId = _uiState.value.promptDeck?.bookId ?: return
        viewModelScope.launch {
            try {
                val id = knowledgeRepository.convertPromptOutputToBlueprint(bookId, title, outputText, card.id)
                _uiState.value = _uiState.value.copy(
                    allNotes = knowledgeRepository.getNoteBlueprintsByBookId(bookId).first(),
                    promptRuns = knowledgeRepository.getPromptRunsByDeckIdNow(card.deckId),
                    successMessage = "Prompt output saved as blueprint #$id"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to save blueprint: ${e.message}")
            }
        }
    }

    fun savePromptOutputAsFlashcards(card: PromptCardEntity, outputText: String, title: String) {
        val bookId = _uiState.value.promptDeck?.bookId ?: return
        viewModelScope.launch {
            try {
                val id = knowledgeRepository.convertPromptOutputToFlashcards(bookId, title, outputText, card.id)
                _uiState.value = _uiState.value.copy(
                    flashcardDecks = knowledgeRepository.getFlashcardDecksByBookId(bookId).first(),
                    promptRuns = knowledgeRepository.getPromptRunsByDeckIdNow(card.deckId),
                    successMessage = "Prompt output saved as flashcards deck #$id"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to save flashcards: ${e.message}")
            }
        }
    }

    fun savePromptOutputAsQuiz(card: PromptCardEntity, outputText: String, title: String) {
        val bookId = _uiState.value.promptDeck?.bookId ?: return
        viewModelScope.launch {
            try {
                val id = knowledgeRepository.convertPromptOutputToQuiz(bookId, title, outputText, card.id)
                _uiState.value = _uiState.value.copy(
                    quizzes = quizRepository.getQuizzesByBookId(bookId, SortOption.TITLE).first(),
                    promptRuns = knowledgeRepository.getPromptRunsByDeckIdNow(card.deckId),
                    successMessage = "Prompt output saved as quiz #$id"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to save quiz: ${e.message}")
            }
        }
    }

    fun extractVariables(text: String): List<String> =
        Regex("""\{[^}]+\}|\[[^]]+\]|\([^)]+\)""").findAll(text).map { it.value }.distinct()
            .toList()

    fun createFlashcardDeckFromMarkedQuestions(bookId: Long, title: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            runCatching {
                knowledgeRepository.createFlashcardDeckFromMarkedQuestions(bookId, title, "Generated from marked questions")
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    successMessage = "Flashcard deck created from marked questions"
                )
                loadBook(bookId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isSaving = false, error = "Failed to create flashcard deck: ${e.message}")
            }
        }
    }

    fun createFlashcardDeckFromMissedQuestions(bookId: Long, title: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            runCatching {
                knowledgeRepository.createFlashcardDeckFromMissedQuestions(bookId, title, "Generated from missed questions")
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    successMessage = "Flashcard deck created from missed questions"
                )
                loadBook(bookId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isSaving = false, error = "Failed to create flashcard deck: ${e.message}")
            }
        }
    }

    fun createSlideshowCourseFromQuestions(bookId: Long, title: String, questionIds: List<Long>) {
        viewModelScope.launch {
            try {
                knowledgeRepository.createSlideshowCourseFromQuestions(bookId, title, "Generated from questions", questionIds)
                _uiState.value = _uiState.value.copy(successMessage = "Slideshow course generated")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to generate slideshow course: ${e.message}")
            }
        }
    }

    fun createFlashcardDeckFromQuestions(bookId: Long, title: String, questionIds: List<Long>) {
        viewModelScope.launch {
            try {
                knowledgeRepository.createFlashcardDeckFromQuestions(bookId, title, "Generated from questions", questionIds)
                _uiState.value = _uiState.value.copy(successMessage = "Flashcard deck generated")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to generate flashcard deck: ${e.message}")
            }
        }
    }

    fun createBlueprintFromQuestions(bookId: Long, title: String, questionIds: List<Long>) {
        viewModelScope.launch {
            try {
                knowledgeRepository.createBlueprintFromQuestions(
                    bookId,
                    title,
                    questionIds,
                    BlueprintMode.SIMPLE_NOTE
                )
                _uiState.value = _uiState.value.copy(successMessage = "Note blueprint generated")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to generate note blueprint: ${e.message}")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    fun deleteQuiz(quiz: QuizEntity) {
        viewModelScope.launch {
            try {
                quizRepository.deleteQuiz(quiz)
                _uiState.value = _uiState.value.copy(
                    quizzes = _uiState.value.quizzes.filter { it.id != quiz.id },
                    successMessage = "Quiz deleted"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete quiz: ${e.message}")
            }
        }
    }

    fun deleteFlashcardDeck(deck: FlashcardDeckEntity) {
        viewModelScope.launch {
            try {
                knowledgeRepository.deleteFlashcardDeck(deck)
                _uiState.value = _uiState.value.copy(
                    flashcardDecks = _uiState.value.flashcardDecks.filter { it.id != deck.id },
                    successMessage = "Flashcard deck deleted"
                )
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(error = "Failed to delete flashcard deck: ${e.message}")
            }
        }
    }

    fun createFlashcardDeckFromBook(
        bookId: Long,
        title: String,
        description: String = "",
        coverUri: String? = null,
        onCreated: (Long) -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                val deckId = knowledgeRepository.insertFlashcardDeck(
                    FlashcardDeckEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        bookId = bookId,
                        title = title.ifBlank { "Untitled deck" },
                        description = description.trim().ifBlank { null },
                        coverImage = coverUri,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        lastEditedAt = System.currentTimeMillis()
                    )
                )
                val savedList = knowledgeRepository.getFlashcardDecksByBookId(bookId).first()
                val saved = savedList.find { it.id == deckId }
                _uiState.value = _uiState.value.copy(
                    flashcardDecks = (_uiState.value.flashcardDecks.filter { it.id != deckId } + (saved
                        ?: return@launch)).sortedByDescending { it.updatedAt },
                    successMessage = "Flashcard deck created"
                )
                onCreated(deckId)
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(error = "Failed to create flashcard deck: ${e.message}")
            }
        }
    }

    fun createNewQuiz(
        bookId: Long,
        title: String,
        description: String,
        coverImage: String?,
        sourceQuizIds: List<Long>,
        sourceCategories: List<String>,
        filters: QuizCreationFilters = QuizCreationFilters()
    ) {
        viewModelScope.launch {
            try {
                val newQuizId = knowledgeRepository.insertQuiz(
                    QuizEntity(
                        externalId = java.util.UUID.randomUUID().toString(),
                        bookId = bookId,
                        title = title.ifBlank { "New Quiz" },
                        description = description,
                        coverImage = coverImage,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )

                // Duplicate questions if sources provided
                if (sourceQuizIds.isNotEmpty() || sourceCategories.isNotEmpty()) {
                    val allQuestions = mutableListOf<QuestionEntity>()

                    if (sourceQuizIds.isNotEmpty()) {
                        for (qId in sourceQuizIds) {
                            allQuestions.addAll(quizRepository.getQuestionsByQuizId(qId).first())
                        }
                    } else if (sourceCategories.isNotEmpty()) {
                        val bookQuizzes = quizRepository.getQuizzesByBookId(bookId, SortOption.TITLE).first()
                        for (quiz in bookQuizzes) {
                            allQuestions.addAll(quizRepository.getQuestionsByQuizId(quiz.id).first())
                        }
                    }

                    var filteredQuestions = if (sourceCategories.isNotEmpty()) {
                        allQuestions.filter { q ->
                            q.categories.any { it in sourceCategories }
                        }
                    } else {
                        allQuestions
                    }
                    
                    // Apply extra filters
                    if (filters.mistakesOnly) {
                        filteredQuestions = filteredQuestions.filter { (it.attempts > 0 && it.correctCount < it.attempts) || (it.lastAttemptResult == false) }
                    }
                    if (filters.markedOnly) {
                        filteredQuestions = filteredQuestions.filter { it.isMarked }
                    }
                    if (filters.unansweredOnly) {
                        filteredQuestions = filteredQuestions.filter { it.attempts == 0 }
                    }

                    val duplicatedQuestions = filteredQuestions.distinctBy { it.id }.map { q ->
                        q.copy(
                            id = 0,
                            externalId = java.util.UUID.randomUUID().toString(),
                            quizId = newQuizId,
                            attempts = 0,
                            correctCount = 0,
                            isMarked = false,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                        )
                    }

                    if (duplicatedQuestions.isNotEmpty()) {
                        knowledgeRepository.insertQuestions(duplicatedQuestions)
                    }
                }

                val savedList = quizRepository.getQuizzesByBookId(bookId, SortOption.TITLE).first()
                val saved = savedList.find { it.id == newQuizId }
                _uiState.value = _uiState.value.copy(
                    quizzes = (_uiState.value.quizzes.filter { it.id != newQuizId } + (saved
                        ?: return@launch)).sortedByDescending { it.updatedAt },
                    successMessage = "Quiz created"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create quiz: ${e.message}")
            }
        }
    }

    fun getImagesForSource(sourceId: Long, onResult: (List<String>) -> Unit) {
        onResult(emptyList())
    }
}
