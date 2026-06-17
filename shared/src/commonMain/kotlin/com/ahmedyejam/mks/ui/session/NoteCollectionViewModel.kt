package com.ahmedyejam.mks.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.local.entity.NoteBlueprintEntity
import com.ahmedyejam.mks.data.local.entity.NoteCollectionEntity
import com.ahmedyejam.mks.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NoteCollectionUiState(
    val collection: NoteCollectionEntity? = null,
    val blueprints: List<NoteBlueprintEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class NoteCollectionViewModel(
    private val knowledgeRepository: KnowledgeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoteCollectionUiState())
    val uiState = _uiState.asStateFlow()

    fun loadCollection(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Placeholder: KnowledgeRepository needs nt_collectionSelectById and nt_blueprintSelectByCollection
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
