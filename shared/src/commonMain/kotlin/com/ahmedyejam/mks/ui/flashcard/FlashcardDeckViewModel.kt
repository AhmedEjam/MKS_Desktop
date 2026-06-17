package com.ahmedyejam.mks.ui.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.local.entity.FlashcardDeckEntity
import com.ahmedyejam.mks.data.local.entity.FlashcardEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.repository.*
import com.ahmedyejam.mks.util.currentTimeMillis
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

const val FLASHCARD_RATING_AGAIN = "again"
const val FLASHCARD_RATING_GOOD = "good"
const val FLASHCARD_RATING_EASY = "easy"

data class FlashcardDeckUiState(
    val deck: FlashcardDeckEntity? = null,
    val cards: List<FlashcardEntity> = emptyList(),
    val availableDecks: List<FlashcardDeckEntity> = emptyList(),
    val quizzes: List<QuizEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isLoading: Boolean = true,
    val isStudyMode: Boolean = false,
    val selectedCardIds: Set<Long> = emptySet(),
    val message: String? = null,
    val error: String? = null
) {
    val currentCard: FlashcardEntity?
        get() = cards.getOrNull(currentIndex)
}

class FlashcardDeckViewModel(
    private val bookRepository: BookRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val assetRepository: AssetRepository,
    private val quizRepository: QuizRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(FlashcardDeckUiState())
    val uiState = _uiState.asStateFlow()
    private var deckId: Long? = null
    private var loadJob: Job? = null

    private var activeSessionId: Long? = null
    private var sessionTimeAccumulatedMs: Long = 0L
    private var sessionLastStartedTimestamp: Long = 0L
    private var isSessionTimerRunning: Boolean = false
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()
    private var timerJob: Job? = null

    fun loadDeck(id: Long) {
        if (deckId == id && loadJob?.isActive == true) return
        deckId = id
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val deck = knowledgeRepository.getDeckById(id)
            if (deck == null) {
                _uiState.update { it.copy(isLoading = false, error = "Deck not found") }
                return@launch
            }

            combine(
                knowledgeRepository.observeCardsByDeck(id),
                knowledgeRepository.observeDecksByBook(deck.bookId),
                quizRepository.getAllCategoriesWithMetadata()
            ) { cards, allDecks, cats ->
                val current = _uiState.value
                _uiState.update { it.copy(
                    deck = deck,
                    cards = cards,
                    availableDecks = allDecks.filter { it.id != id },
                    categories = cats.map { it.name },
                    currentIndex = current.currentIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0)),
                    isLoading = false
                )}
            }.collect()
        }
    }

    fun flipCard() { _uiState.update { it.copy(isFlipped = !it.isFlipped) } }

    fun nextCard() {
        _uiState.update { it.copy(currentIndex = (it.currentIndex + 1).coerceAtMost(it.cards.lastIndex.coerceAtLeast(0)), isFlipped = false) }
    }

    fun previousCard() {
        _uiState.update { it.copy(currentIndex = (it.currentIndex - 1).coerceAtLeast(0), isFlipped = false) }
    }

    fun rateCurrentCard(rating: String) {
        val card = _uiState.value.currentCard ?: return
        viewModelScope.launch {
            // knowledgeRepository.rateFlashcard(card, rating)
            nextCard()
        }
    }

    fun deleteCard(card: FlashcardEntity) {
        viewModelScope.launch {
            // knowledgeRepository.deleteFlashcard(card)
        }
    }
}
