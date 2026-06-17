package com.ahmedyejam.mks.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.repository.KnowledgeSummary
import com.ahmedyejam.mks.data.repository.StudyRepository
import com.ahmedyejam.mks.data.review.ReviewDashboardSummary
import com.ahmedyejam.mks.data.review.ReviewQueueItem
import com.ahmedyejam.mks.data.review.ReviewRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReviewDashboardUiState(
    val isLoading: Boolean = true,
    val summary: ReviewDashboardSummary = ReviewDashboardSummary(),
    val knowledgeSummary: KnowledgeSummary? = null,
    val queue: List<ReviewQueueItem> = emptyList(),
    val error: String? = null
)

class ReviewDashboardViewModel(
    private val repository: ReviewRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ReviewDashboardUiState())
    val state: StateFlow<ReviewDashboardUiState> = _state.asStateFlow()

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

    fun snooze(item: ReviewQueueItem, millis: Long) {
        viewModelScope.launch {
            runCatching { repository.snooze(item, millis) }
            refresh()
        }
    }
}
