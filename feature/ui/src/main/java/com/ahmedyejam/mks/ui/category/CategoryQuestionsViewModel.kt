package com.ahmedyejam.mks.ui.category

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class VisibilityState(
    val showStem: Boolean = true,
    val showOptions: Boolean = true,
    val showCorrectAnswer: Boolean = false,
    val showExplanation: Boolean = false,
    val showReference: Boolean = false,
    val showHint: Boolean = false,
    val showAdditionalInfo: Boolean = false,
    val showOnlyMarked: Boolean = false,
    val showOnlyWithAssets: Boolean = false,
)

data class CategoryQuestionsUiState(
    val categoryName: String = "",
    val allQuestions: List<QuestionEntity> = emptyList(),
    val filteredQuestions: List<QuestionEntity> = emptyList(),
    val searchQuery: String = "",
    val visibility: VisibilityState = VisibilityState(),
    val isLoading: Boolean = true,
    val selectedQuestionIds: Set<Long> = emptySet(),
    val questionIdsWithAssets: Set<Long> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryQuestionsViewModel @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository,
    private val assetRepository: AssetRepository,
    private val quizRepository: QuizRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _categoryName = MutableStateFlow("")
    private val _searchQuery = MutableStateFlow("")
    private val _visibility = MutableStateFlow(VisibilityState())
    private val _isLoading = MutableStateFlow(value = false)
    private val _selectedQuestionIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _questionIdsWithAssets = assetRepository.getQuestionIdsWithAssetsFlow()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val allQuizzes: StateFlow<List<QuizEntity>> = quizRepository.getAllQuizzesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allQuestions = _categoryName
        .filter { it.isNotBlank() }
        .onEach { _isLoading.value = true }
        .flatMapLatest { category ->
            quizRepository.getQuestionsByCategoryFlow(category)
        }
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<CategoryQuestionsUiState> = combine(
        _categoryName,
        _allQuestions,
        _searchQuery,
        _visibility,
        _isLoading,
        _selectedQuestionIds,
        _questionIdsWithAssets
    ) { args: Array<Any> ->
        val category = args[0] as String
        @Suppress("UNCHECKED_CAST")
        val questions = args[1] as List<QuestionEntity>
        val query = args[2] as String
        val visibility = args[3] as VisibilityState
        val loading = args[4] as Boolean
        @Suppress("UNCHECKED_CAST")
        val selectedIds = args[5] as Set<Long>
        @Suppress("UNCHECKED_CAST")
        val assetIds = args[6] as Set<Long>

        CategoryQuestionsUiState(
            categoryName = category,
            allQuestions = questions,
            filteredQuestions = applyFilter(questions, query, visibility.showOnlyMarked, visibility.showOnlyWithAssets, assetIds),
            searchQuery = query,
            visibility = visibility,
            isLoading = loading,
            selectedQuestionIds = selectedIds,
            questionIdsWithAssets = assetIds
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryQuestionsUiState())

    fun loadCategory(category: String) {
        _categoryName.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(questionId: Long) {
        _selectedQuestionIds.update { current ->
            if (current.contains(questionId)) current - questionId else current + questionId
        }
    }

    fun clearSelection() {
        _selectedQuestionIds.value = emptySet()
    }

    fun deleteSelectedQuestions() {
        val idsToDelete = _selectedQuestionIds.value.toList()
        if (idsToDelete.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            // Fetch fresh questions by IDs to ensure we have correct image paths and quiz IDs for cleanup
            val questionsToDelete = quizRepository.getQuestionsByIds(idsToDelete)
            val affectedQuizIds = questionsToDelete.asSequence()
                .map { it.quizId }
                .distinct()
                .toList()
            questionsToDelete.forEach { question ->
                quizRepository.deleteQuestion(question)
            }
            affectedQuizIds.forEach { quizRepository.refreshQuizStats(it) }
            clearSelection()
            _isLoading.value = false
        }
    }

    fun moveSelectedQuestionsToQuiz(targetQuizId: Long) {
        val idsToMove = _selectedQuestionIds.value
        if (idsToMove.isEmpty()) return

        viewModelScope.launch {
            val questionsToMove = _allQuestions.value.filter { it.id in idsToMove }
            val affectedQuizIds = questionsToMove.asSequence()
                .map { it.quizId }
                .distinct()
                .toList()
            questionsToMove.forEach { question ->
                assetRepository.updateQuestion(question.copy(quizId = targetQuizId, updatedAt = System.currentTimeMillis()))
            }
            affectedQuizIds.forEach { quizRepository.refreshQuizStats(it) }
            quizRepository.refreshQuizStats(targetQuizId)
            clearSelection()
        }
    }


    fun copySelectedQuestionsToQuiz(targetQuizId: Long) {
        val idsToCopy = _selectedQuestionIds.value
        if (idsToCopy.isEmpty()) return

        viewModelScope.launch {
            val questionsToCopy = _allQuestions.value.filter { it.id in idsToCopy }
            val newQuestions = questionsToCopy.map { question ->
                question.copy(
                    id = 0,
                    quizId = targetQuizId,
                    externalId = java.util.UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            knowledgeRepository.insertQuestions(newQuestions)
            quizRepository.refreshQuizStats(targetQuizId)
            clearSelection()
        }
    }

    fun exportSelectedQuestionsToNewQuiz(title: String) {
        val idsToExport = _selectedQuestionIds.value
        if (idsToExport.isEmpty() || title.isBlank()) return

        viewModelScope.launch {
            val questionsToExport = _allQuestions.value.filter { it.id in idsToExport }
            val sourceQuizId = questionsToExport.firstOrNull()?.quizId ?: return@launch
            val sourceQuiz = allQuizzes.value.firstOrNull { it.id == sourceQuizId } ?: quizRepository.getQuizById(sourceQuizId)
            val sourceBookId = sourceQuiz?.bookId ?: return@launch
            val newQuizId = knowledgeRepository.insertQuiz(
                QuizEntity(
                    bookId = sourceBookId,
                    externalId = java.util.UUID.randomUUID().toString(),
                    title = title.trim(),
                    description = "Exported from category: ${_categoryName.value}"
                )
            )
            val newQuestions = questionsToExport.map { question ->
                question.copy(
                    id = 0,
                    quizId = newQuizId,
                    externalId = java.util.UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            knowledgeRepository.insertQuestions(newQuestions)
            quizRepository.refreshQuizStats(newQuizId)
            clearSelection()
        }
    }

    fun markSelectedQuestions(marked: Boolean) {
        val idsToMark = _selectedQuestionIds.value
        if (idsToMark.isEmpty()) return

        viewModelScope.launch {
            val questionsToMark = _allQuestions.value.filter { it.id in idsToMark }
            questionsToMark.forEach { question ->
                assetRepository.updateQuestion(question.copy(isMarked = marked, updatedAt = System.currentTimeMillis()))
            }
            clearSelection()
        }
    }

    fun updateQuestion(question: QuestionEntity) {
        viewModelScope.launch {
            assetRepository.updateQuestion(question.copy(updatedAt = System.currentTimeMillis()))
            quizRepository.refreshQuizStats(question.quizId)
        }
    }

    fun toggleAllSelection() {
        val currentSelected = _selectedQuestionIds.value
        val allVisibleIds = uiState.value.filteredQuestions.map { it.id }.toSet()
        
        if (currentSelected.containsAll(allVisibleIds)) {
            _selectedQuestionIds.value = emptySet()
        } else {
            _selectedQuestionIds.value = allVisibleIds
        }
    }

    private fun applyFilter(questions: List<QuestionEntity>, query: String, onlyMarked: Boolean, onlyWithAssets: Boolean, questionIdsWithAssets: Set<Long>): List<QuestionEntity> {
        var filtered = questions
        if (onlyMarked) {
            filtered = filtered.filter { it.isMarked }
        }
        if (onlyWithAssets) {
            filtered = filtered.filter { it.id in questionIdsWithAssets }
        }
        if (query.isBlank()) return filtered
        
        return filtered.filter { q ->
            q.text.contains(query, ignoreCase = true) ||
            q.options.any { it.contains(query, ignoreCase = true) } ||
            (q.explanation?.contains(query, ignoreCase = true) ?: false) ||
            (q.additionalInfo?.contains(query, ignoreCase = true) ?: false)
        }
    }

    fun toggleVisibility(update: VisibilityState.() -> VisibilityState) {
        _visibility.update { it.update() }
    }

    fun toggleMarked(question: QuestionEntity) {
        viewModelScope.launch {
            assetRepository.updateQuestion(question.copy(isMarked = !question.isMarked))
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
        if (selected.isEmpty() || title.isBlank()) return
        viewModelScope.launch {
            val questions = quizRepository.getQuestionsByIds(selected)
            val sourceQuiz = questions.firstOrNull()?.let { quizRepository.getQuizById(it.quizId) } ?: return@launch
            knowledgeRepository.createFlashcardDeckFromQuestions(sourceQuiz.bookId, title, "Generated from selected questions", selected, clearMarksAfter)
            clearSelection()
        }
    }
}
