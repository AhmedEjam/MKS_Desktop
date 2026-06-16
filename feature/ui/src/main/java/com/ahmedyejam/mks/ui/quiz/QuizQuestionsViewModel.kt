package com.ahmedyejam.mks.ui.quiz

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.local.entity.AnnotationColorLabel
import com.ahmedyejam.mks.data.repository.KnowledgeRepository
import com.ahmedyejam.mks.data.repository.AssetRepository
import com.ahmedyejam.mks.data.repository.QuizRepository
import com.ahmedyejam.mks.data.repository.StudyRepository
import com.ahmedyejam.mks.data.local.entity.AnnotationEntity
import com.ahmedyejam.mks.data.local.entity.AnnotationOwnerType
import com.ahmedyejam.mks.data.local.entity.QuestionAssetEntity
import com.ahmedyejam.mks.data.local.entity.SourceDocumentEntity
import com.ahmedyejam.mks.data.local.entity.SourceDocumentTypes
import com.ahmedyejam.mks.data.local.entity.QuestionAssetType
import com.ahmedyejam.mks.data.local.entity.BlueprintMode
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.repository.QuizKnowledgeSummary
import com.ahmedyejam.mks.ui.category.VisibilityState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.ahmedyejam.mks.data.validation.QuestionValidator
import com.ahmedyejam.mks.data.validation.QuestionValidationResult

data class QuizQuestionsUiState(
    val quiz: QuizEntity? = null,
    val allQuestions: List<QuestionEntity> = emptyList(),
    val filteredQuestions: List<QuestionEntity> = emptyList(),
    val searchQuery: String = "",
    val visibility: VisibilityState = VisibilityState(),
    val isLoading: Boolean = true,
    val selectedQuestionIds: Set<Long> = emptySet(),
    val questionIdsWithAssets: Set<Long> = emptySet(),
    val quizSummary: QuizKnowledgeSummary? = null,
    val validationResults: Map<Long, QuestionValidationResult> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuizQuestionsViewModel @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository,
    private val assetRepository: AssetRepository,
    private val quizRepository: QuizRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _quizId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _visibility = MutableStateFlow(VisibilityState())
    private val _isLoading = MutableStateFlow(true)
    private val _selectedQuestionIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _quizSummary = MutableStateFlow<QuizKnowledgeSummary?>(null)
    private val _questionIdsWithAssets = _quizId.flatMapLatest { id ->
        if (id != null) assetRepository.getQuestionIdsWithAssetsForQuizFlow(id) else flowOf(emptyList())
    }.map { it.toSet() }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val allQuizzes: StateFlow<List<QuizEntity>> = quizRepository.getAllQuizzesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quiz: Flow<QuizEntity?> = _quizId.flatMapLatest { id ->
        if (id != null) {
            flow { 
                val quiz = quizRepository.getQuizById(id)
                emit(quiz) 
            }
        } else {
            flowOf(null)
        }
    }

    private val _allQuestions: Flow<List<QuestionEntity>> = _quizId.flatMapLatest { id ->
        if (id != null) {
            quizRepository.getQuestionsByQuizId(id)
        } else {
            flowOf(emptyList())
        }
    }

    val uiState: StateFlow<QuizQuestionsUiState> = combine(
        _quiz,
        _allQuestions,
        _searchQuery,
        _visibility,
        _isLoading,
        _selectedQuestionIds,
        _questionIdsWithAssets,
        _quizSummary
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val quiz = params[0] as QuizEntity?
        @Suppress("UNCHECKED_CAST")
        val questions = params[1] as List<QuestionEntity>
        @Suppress("UNCHECKED_CAST")
        val query = params[2] as String
        @Suppress("UNCHECKED_CAST")
        val vis = params[3] as VisibilityState
        @Suppress("UNCHECKED_CAST")
        val loading = params[4] as Boolean
        @Suppress("UNCHECKED_CAST")
        val selected = params[5] as Set<Long>
        @Suppress("UNCHECKED_CAST")
        val assetIds = params[6] as Set<Long>
        @Suppress("UNCHECKED_CAST")
        val summary = params[7] as QuizKnowledgeSummary?

        val validation = questions.associate { it.id to QuestionValidator.validate(it) }

        QuizQuestionsUiState(
            quiz = quiz,
            allQuestions = questions,
            filteredQuestions = applyFilter(questions, query, vis.showOnlyMarked, vis.showOnlyWithAssets, assetIds),
            searchQuery = query,
            visibility = vis,
            isLoading = loading,
            selectedQuestionIds = selected,
            questionIdsWithAssets = assetIds,
            quizSummary = summary,
            validationResults = validation
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuizQuestionsUiState())

    fun loadQuiz(quizId: Long) {
        _quizId.value = quizId
        _isLoading.value = false
        viewModelScope.launch {
            _quizSummary.value = runCatching { quizRepository.getQuizKnowledgeSummary(quizId) }.getOrNull()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(questionId: Long) {
        val current = _selectedQuestionIds.value
        _selectedQuestionIds.value = if (current.contains(questionId)) {
            current - questionId
        } else {
            current + questionId
        }
    }

    fun clearSelection() {
        _selectedQuestionIds.value = emptySet()
    }

    fun deleteSelectedQuestions() {
        viewModelScope.launch {
            val selectedIds = _selectedQuestionIds.value
            val questionsToDelete = uiState.value.allQuestions.filter { selectedIds.contains(it.id) }
            questionsToDelete.forEach { quizRepository.deleteQuestion(it) }
            clearSelection()
            _quizId.value?.let { quizRepository.refreshQuizStats(it) }
        }
    }

    fun moveSelectedQuestionsToQuiz(targetQuizId: Long) {
        viewModelScope.launch {
            val selectedIds = _selectedQuestionIds.value
            val currentQuizId = _quizId.value
            val questionsToMove = uiState.value.allQuestions.filter { selectedIds.contains(it.id) }
            questionsToMove.forEach { 
                assetRepository.updateQuestion(it.copy(quizId = targetQuizId))
            }
            clearSelection()
            currentQuizId?.let { quizRepository.refreshQuizStats(it) }
            quizRepository.refreshQuizStats(targetQuizId)
        }
    }

    fun copySelectedQuestionsToQuiz(targetQuizId: Long) {
        viewModelScope.launch {
            val selectedIds = _selectedQuestionIds.value
            val questionsToCopy = uiState.value.allQuestions.filter { selectedIds.contains(it.id) }
            val newQuestions = questionsToCopy.map { 
                it.copy(id = 0, quizId = targetQuizId, externalId = java.util.UUID.randomUUID().toString()) 
            }
            knowledgeRepository.insertQuestions(newQuestions)
            clearSelection()
            quizRepository.refreshQuizStats(targetQuizId)
        }
    }

    fun exportSelectedQuestionsToNewQuiz(title: String) {
        viewModelScope.launch {
            val quiz = uiState.value.quiz ?: return@launch
            val selectedIds = _selectedQuestionIds.value
            val questionsToExport = uiState.value.allQuestions.filter { selectedIds.contains(it.id) }
            
            val newQuizId = knowledgeRepository.insertQuiz(
                QuizEntity(
                    bookId = quiz.bookId,
                    externalId = java.util.UUID.randomUUID().toString(),
                    title = title,
                    description = "Exported from ${quiz.title}"
                )
            )
            
            val newQuestions = questionsToExport.map { 
                it.copy(id = 0, quizId = newQuizId, externalId = java.util.UUID.randomUUID().toString()) 
            }
            knowledgeRepository.insertQuestions(newQuestions)
            clearSelection()
            quizRepository.refreshQuizStats(newQuizId)
        }
    }

    fun addQuestion(question: QuestionEntity) {
        if (!QuestionValidator.validate(question).isValid) return
        viewModelScope.launch {
            val quizId = _quizId.value ?: return@launch
            knowledgeRepository.insertQuestion(question.copy(quizId = quizId))
            quizRepository.refreshQuizStats(quizId)
        }
    }

    fun updateQuestion(question: QuestionEntity) {
        if (!QuestionValidator.validate(question).isValid) return
        viewModelScope.launch {
            assetRepository.updateQuestion(question)
            _quizId.value?.let { quizRepository.refreshQuizStats(it) }
        }
    }


    fun markSelectedQuestions(marked: Boolean) {
        viewModelScope.launch {
            val selectedIds = _selectedQuestionIds.value
            val questionsToMark = uiState.value.allQuestions.filter { selectedIds.contains(it.id) }
            questionsToMark.forEach { question ->
                assetRepository.updateQuestion(question.copy(isMarked = marked, updatedAt = System.currentTimeMillis()))
            }
            clearSelection()
        }
    }

    fun toggleAllSelection() {
        val currentSelected = _selectedQuestionIds.value
        val allIds = uiState.value.filteredQuestions.map { it.id }.toSet()
        _selectedQuestionIds.value = if (currentSelected.size == allIds.size) emptySet() else allIds
    }

    private fun applyFilter(questions: List<QuestionEntity>, query: String, onlyMarked: Boolean, onlyWithAssets: Boolean, questionIdsWithAssets: Set<Long>): List<QuestionEntity> {
        return questions.filter { question ->
            (query.isBlank() || question.text.contains(query, ignoreCase = true) || question.explanation?.contains(query, ignoreCase = true) == true) &&
            (!onlyMarked || question.isMarked) &&
            (!onlyWithAssets || question.id in questionIdsWithAssets)
        }
    }

    fun toggleVisibility(update: VisibilityState.() -> VisibilityState) {
        _visibility.value = _visibility.value.update()
    }

    fun toggleMarked(question: QuestionEntity) {
        viewModelScope.launch {
            quizRepository.updateQuestionMark(question.id, !question.isMarked)
        }
    }

    fun toggleDropped(question: QuestionEntity) {
        viewModelScope.launch {
            quizRepository.updateQuestionDrop(question.id, !question.isDropped)
        }
    }

    fun assetsForQuestion(questionId: Long) = assetRepository.getQuestionAssets(questionId)

    fun annotationsForQuestion(questionId: Long) =
        studyRepository.getAnnotationsByOwner(AnnotationOwnerType.QUESTION, questionId)

    fun addQuestionAnnotation(bookId: Long, questionId: Long, selectedText: String?, noteBody: String, colorLabel: String = AnnotationColorLabel.YELLOW) {
        viewModelScope.launch {
            assetRepository.createAnnotationForOwner(
                bookId = bookId,
                ownerType = AnnotationOwnerType.QUESTION,
                ownerId = questionId,
                selectedText = selectedText,
                noteBody = noteBody,
                colorLabel = colorLabel
            )
        }
    }

    fun updateAnnotation(annotation: AnnotationEntity) {
        viewModelScope.launch { studyRepository.updateAnnotation(annotation) }
    }

    fun deleteAnnotation(annotation: AnnotationEntity) {
        viewModelScope.launch { assetRepository.softDeleteAnnotation(annotation.id) }
    }

    fun sourceDocumentsForBook(bookId: Long) = assetRepository.getSourceDocumentsByBookId(bookId)

    fun linkedBlueprintsForQuestion(bookId: Long, questionId: Long) =
        knowledgeRepository.getLinkedBlueprintsForQuestion(bookId, questionId)

    fun addQuestionAsset(asset: QuestionAssetEntity) {
        viewModelScope.launch { assetRepository.insertQuestionAsset(asset) }
    }

    fun createSourceAndAddQuestionAsset(asset: QuestionAssetEntity, source: SourceDocumentEntity) {
        viewModelScope.launch {
            val preparedSource = source.copy(
                sourceType = source.sourceType.ifBlank { SourceDocumentTypes.OTHER },
                bookId = source.bookId ?: asset.bookId
            )
            assetRepository.createSourceDocumentAndQuestionAsset(preparedSource, asset.copy(assetType = QuestionAssetType.SOURCE_REFERENCE))
        }
    }

    fun createBlueprintFromQuestion(bookId: Long, questionId: Long) {
        viewModelScope.launch { knowledgeRepository.createBlueprintFromQuestion(bookId, questionId, BlueprintMode.CONCEPT_TEMPLATE) }
    }

    fun updateQuestionAsset(asset: QuestionAssetEntity) {
        viewModelScope.launch { assetRepository.updateQuestionAsset(asset) }
    }

    fun deleteQuestionAsset(asset: QuestionAssetEntity) {
        viewModelScope.launch { assetRepository.deleteQuestionAsset(asset) }
    }

    fun createFlashcardsFromSelected(title: String, clearMarksAfter: Boolean) {
        val selected = _selectedQuestionIds.value.toList()
        val quiz = uiState.value.quiz ?: return
        if (selected.isEmpty() || title.isBlank()) return
        viewModelScope.launch {
            knowledgeRepository.createFlashcardDeckFromQuestions(quiz.bookId, title, "Generated from selected questions", selected, clearMarksAfter)
            clearSelection()
        }
    }
}
