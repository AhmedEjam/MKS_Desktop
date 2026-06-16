package com.ahmedyejam.mks.ui.review

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.local.entity.AnnotationEntity
import com.ahmedyejam.mks.data.repository.BookRepository
import com.ahmedyejam.mks.data.repository.AssetRepository
import com.ahmedyejam.mks.data.repository.StudyRepository
import com.ahmedyejam.mks.data.local.entity.MistakeLogEntryEntity
import com.ahmedyejam.mks.data.preferences.DataStoreManager
import com.ahmedyejam.mks.data.repository.KnowledgeSummary
import com.ahmedyejam.mks.data.repository.SortOption
import com.ahmedyejam.mks.data.review.ReviewDashboardSummary
import com.ahmedyejam.mks.data.review.ReviewQueueItem
import com.ahmedyejam.mks.data.review.ReviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReviewDashboardUiState(
    val isLoading: Boolean = true,
    val summary: ReviewDashboardSummary = ReviewDashboardSummary(),
    val knowledgeSummary: KnowledgeSummary? = null,
    val queue: List<ReviewQueueItem> = emptyList(),
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReviewDashboardViewModel @Inject constructor(
    private val repository: ReviewRepository,
    private val dataStoreManager: DataStoreManager,
    private val bookRepository: BookRepository,
    private val assetRepository: AssetRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ReviewDashboardUiState())
    val state: StateFlow<ReviewDashboardUiState> = _state.asStateFlow()

    val currentWorkspaceId = dataStoreManager.currentWorkspaceId
        .map { storedId -> storedId ?: assetRepository.getOrCreateDefaultWorkspace().id }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val annotations = currentWorkspaceId.flatMapLatest { wsId ->
        if (wsId <= 0L) flowOf(emptyList()) else studyRepository.getAnnotationsByWorkspaceId(wsId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mistakes = currentWorkspaceId.flatMapLatest { wsId ->
        if (wsId <= 0L) flowOf(emptyList()) else studyRepository.getAllMistakes().map { list ->
            val bookIds = bookRepository.getBooksByWorkspace(wsId, SortOption.TITLE).first().map { it.id }.toSet()
            list.filter { it.bookId in bookIds && it.deletedAt == null }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val summary = repository.loadSummary()
                val knowledgeSummary = studyRepository.getLibraryKnowledgeSummary()
                val queue = repository.loadQueues()
                Triple(summary, knowledgeSummary, queue)
            }.onSuccess { (summary, knowledgeSummary, queue) ->
                _state.update { it.copy(isLoading = false, summary = summary, knowledgeSummary = knowledgeSummary, queue = queue) }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message ?: "Could not load review dashboard") }
            }
        }
    }

    fun markReviewed(item: ReviewQueueItem) {
        viewModelScope.launch {
            runCatching { repository.markReviewed(item) }
            refresh()
        }
    }

    fun snoozeOneWeek(item: ReviewQueueItem) {
        viewModelScope.launch {
            runCatching { repository.snooze(item, 7L * 24L * 60L * 60L * 1000L) }
            refresh()
        }
    }

    fun snoozeCustom(item: ReviewQueueItem, delayMillis: Long) {
        viewModelScope.launch {
            runCatching { repository.snooze(item, delayMillis) }
            refresh()
        }
    }

    fun snoozeMistake(mistakeId: Long, snoozeTime: Long) {
        viewModelScope.launch {
            runCatching { studyRepository.snoozeMistake(mistakeId, snoozeTime) }
            refresh()
        }
    }

    fun markMistakeFixed(mistakeId: Long) {
        viewModelScope.launch {
            runCatching { studyRepository.markMistakeFixed(mistakeId) }
            refresh()
        }
    }

    fun deleteMistake(entry: MistakeLogEntryEntity) {
        viewModelScope.launch {
            runCatching { studyRepository.deleteMistake(entry) }
            refresh()
        }
    }

    fun updateAnnotation(annotation: AnnotationEntity) {
        viewModelScope.launch {
            runCatching { studyRepository.updateAnnotation(annotation) }
        }
    }

    fun deleteAnnotation(annotationId: Long) {
        viewModelScope.launch {
            runCatching { assetRepository.softDeleteAnnotation(annotationId) }
        }
    }
}
