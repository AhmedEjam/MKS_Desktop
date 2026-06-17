package com.ahmedyejam.mks.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.local.entity.PromptCardEntity
import com.ahmedyejam.mks.data.local.entity.PromptDeckEntity
import com.ahmedyejam.mks.data.preferences.DataStoreManager
import com.ahmedyejam.mks.data.repository.*
import com.ahmedyejam.mks.data.repository.ai.OllamaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PromptUiState(
    val deck: PromptDeckEntity? = null,
    val cards: List<PromptCardEntity> = emptyList(),
    val currentCard: PromptCardEntity? = null,
    val aiResponse: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class PromptViewModel(
    private val knowledgeRepository: KnowledgeRepository,
    private val ollamaRepository: OllamaRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(PromptUiState())
    val uiState = _uiState.asStateFlow()

    fun loadDeck(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            knowledgeRepository.observePromptCardsByDeck(id).collect { cards ->
                _uiState.update { it.copy(cards = cards, isLoading = false) }
            }
        }
    }

    fun selectCard(card: PromptCardEntity) {
        _uiState.update { it.copy(currentCard = card, aiResponse = "") }
    }

    fun runPrompt() {
        val card = _uiState.value.currentCard ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, aiResponse = "") }
            val baseUrl = dataStoreManager.ollamaBaseUrl.first()
            val model = dataStoreManager.ollamaModelName.first()
            
            ollamaRepository.generateCompletionStream(baseUrl, model, card.promptText)
                .collect { chunk ->
                    _uiState.update { it.copy(aiResponse = it.aiResponse + chunk) }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
