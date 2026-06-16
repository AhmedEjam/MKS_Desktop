package com.ahmedyejam.mks.ui.search

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.search.GlobalSearchRepository
import com.ahmedyejam.mks.data.search.GlobalSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GlobalSearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<GlobalSearchResult> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class GlobalSearchViewModel @Inject constructor(private val repository: GlobalSearchRepository) : ViewModel() {
    private val _state = MutableStateFlow(GlobalSearchUiState())
    val state: StateFlow<GlobalSearchUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            search(query)
        }
    }

    fun search(query: String = _state.value.query) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.trim().length < 2) {
                _state.update { it.copy(isLoading = false, results = emptyList(), error = null) }
                return@launch
            }
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.search(query) }
                .onSuccess { results -> _state.update { it.copy(isLoading = false, results = results) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, error = error.message ?: "Search failed") } }
        }
    }
}
