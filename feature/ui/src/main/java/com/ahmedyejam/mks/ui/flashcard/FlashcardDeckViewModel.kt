package com.ahmedyejam.mks.ui.flashcard


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.importer.parser.TextFlashcardParser
import com.ahmedyejam.mks.data.local.entity.FlashcardDeckEntity
import com.ahmedyejam.mks.data.local.entity.FlashcardEntity
import com.ahmedyejam.mks.data.model.FlashcardGenerationConfig
import com.ahmedyejam.mks.data.repository.AssetRepository
import com.ahmedyejam.mks.data.repository.BookRepository
import com.ahmedyejam.mks.data.repository.KnowledgeRepository
import com.ahmedyejam.mks.data.repository.QuizRepository
import com.ahmedyejam.mks.data.repository.StudyRepository
import com.ahmedyejam.mks.di.ApplicationScope
import com.ahmedyejam.mks.util.MksLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

const val FLASHCARD_RATING_AGAIN = "again"
const val FLASHCARD_RATING_GOOD = "good"
const val FLASHCARD_RATING_EASY = "easy"

data class FlashcardDeckUiState(
    val deck: FlashcardDeckEntity? = null,
    val cards: List<FlashcardEntity> = emptyList(),
    val availableDecks: List<FlashcardDeckEntity> = emptyList(),
    val quizzes: List<com.ahmedyejam.mks.data.local.entity.QuizEntity> = emptyList(),
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
        get() = cards.getOrNull(currentIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0)))
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class FlashcardDeckViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: kotlinx.coroutines.CoroutineScope,
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
    private var timerJob: Job? = null

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()

    private val moshi = com.squareup.moshi.Moshi.Builder().build()
    private val sessionStateAdapter = moshi.adapter(com.ahmedyejam.mks.data.model.LearningSessionState::class.java)

    private var activeSessionId: Long? = null
    private var sessionTimeAccumulatedMs: Long = 0L
    private var sessionLastStartedTimestamp: Long = 0L
    private var isSessionTimerRunning: Boolean = false

    private var correctAttemptsCount: Int = 0
    private var totalAttemptsCount: Int = 0
    private val reviewedCardIds = mutableSetOf<Long>()
    private val cardScores = mutableMapOf<Long, Float>()

    fun loadDeck(id: Long) {
        if (deckId == id && loadJob?.isActive == true) return
        deckId = id
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            kotlinx.coroutines.flow.combine(
                knowledgeRepository.observeFlashcardDeckById(id),
                knowledgeRepository.getFlashcardsByDeckId(id)
            ) { deck, cards -> Pair(deck, cards) }
            .flatMapLatest { pair ->
                val deck = pair.first
                val cards = pair.second
                val decksFlow = deck?.bookId?.let { knowledgeRepository.getFlashcardDecksByBookId(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList<FlashcardDeckEntity>())
                val quizzesFlow = deck?.bookId?.let { quizRepository.getQuizzesByBookId(it, com.ahmedyejam.mks.data.repository.SortOption.TITLE) } ?: kotlinx.coroutines.flow.flowOf(emptyList<com.ahmedyejam.mks.data.local.entity.QuizEntity>())
                val categoriesFlow = quizRepository.getAllCategoriesWithMetadata()

                kotlinx.coroutines.flow.combine(decksFlow, quizzesFlow, categoriesFlow) { allDecks, quizzes, cats ->
                    Triple(deck, cards, Triple(allDecks, quizzes, cats))
                }
            }
            .collect { result ->
                val deck = result.first
                val cards = result.second
                val extras = result.third
                val allDecks = extras.first
                val quizzes = extras.second
                val cats = extras.third
                val current = _uiState.value
                _uiState.value = current.copy(
                    deck = deck,
                    cards = cards,
                    availableDecks = allDecks.filter { it.id != id },
                    quizzes = quizzes,
                    categories = cats.map { it.name },
                    currentIndex = current.currentIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0)),
                    isLoading = false
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    fun startSessionTimer() {
        if (!isSessionTimerRunning) {
            sessionLastStartedTimestamp = System.currentTimeMillis()
            isSessionTimerRunning = true
            MksLogger.d("FlashcardDeckViewModel", "Stopwatch started/resumed")
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (true) {
                    _elapsedSeconds.value = getSessionElapsedTimeMs() / 1000L
                    kotlinx.coroutines.delay(kotlin.time.Duration.parseIsoString("PT1S"))
                }
            }
        }
    }

    fun pauseSessionTimer() {
        if (isSessionTimerRunning) {
            val elapsed = System.currentTimeMillis() - sessionLastStartedTimestamp
            sessionTimeAccumulatedMs += elapsed
            isSessionTimerRunning = false
            MksLogger.d("FlashcardDeckViewModel", "Stopwatch paused. Accumulated: $sessionTimeAccumulatedMs ms")
            timerJob?.cancel()
            saveSessionStateIncremental()
        }
    }

    fun getSessionElapsedTimeMs(): Long {
        return if (isSessionTimerRunning) {
            sessionTimeAccumulatedMs + (System.currentTimeMillis() - sessionLastStartedTimestamp)
        } else {
            sessionTimeAccumulatedMs
        }
    }

    private fun saveSessionStateIncremental() {
        val sessionId = activeSessionId ?: return
        val deckId = deckId ?: return
        val current = _uiState.value
        val elapsed = getSessionElapsedTimeMs()
        
        val stateObj = com.ahmedyejam.mks.data.model.LearningSessionState(
            targetType = "FLASHCARD",
            targetId = deckId,
            deckId = deckId,
            reviewedCardIds = reviewedCardIds.toSet(),
            cardScores = cardScores.toMap(),
            currentCardIndex = current.currentIndex,
            timersActive = mapOf(0L to elapsed),
            startedAt = System.currentTimeMillis() - elapsed,
            totalAttempts = totalAttemptsCount,
            correctAttempts = correctAttemptsCount
        )
        
        val json = try {
            sessionStateAdapter.toJson(stateObj)
        } catch (_: Exception) {
            ""
        }

        applicationScope.launch {
            studyRepository.getLearningSessionById(sessionId)?.let { session ->
                studyRepository.updateLearningSession(session.copy(stateJson = json))
            }
        }
    }

    fun endAndSaveSession() {
        val sessionId = activeSessionId ?: return
        activeSessionId = null
        pauseSessionTimer()
        
        val deckId = deckId ?: return
        val current = _uiState.value
        val elapsed = getSessionElapsedTimeMs()
        
        val stateObj = com.ahmedyejam.mks.data.model.LearningSessionState(
            targetType = "FLASHCARD",
            targetId = deckId,
            deckId = deckId,
            reviewedCardIds = reviewedCardIds.toSet(),
            cardScores = cardScores.toMap(),
            currentCardIndex = current.currentIndex,
            timersActive = mapOf(0L to elapsed),
            startedAt = System.currentTimeMillis() - elapsed,
            completedAt = System.currentTimeMillis(),
            totalAttempts = totalAttemptsCount,
            correctAttempts = correctAttemptsCount
        )
        
        val json = try {
            sessionStateAdapter.toJson(stateObj)
        } catch (_: Exception) {
            ""
        }

        applicationScope.launch {
            studyRepository.getLearningSessionById(sessionId)?.let { session ->
                studyRepository.updateLearningSession(session.copy(stateJson = json, isCompleted = true))
                studyRepository.completeLearningSession(sessionId)
            }
        }
    }

    fun setStudyMode(enabled: Boolean) {
        if (enabled) {
            _uiState.value = _uiState.value.copy(isStudyMode = true, isFlipped = false, currentIndex = 0)
            correctAttemptsCount = 0
            totalAttemptsCount = 0
            reviewedCardIds.clear()
            cardScores.clear()
            sessionTimeAccumulatedMs = 0L
            isSessionTimerRunning = false
            
            viewModelScope.launch {
                val deckId = deckId ?: return@launch
                val sessionId = studyRepository.createLearningSession("FLASHCARD", deckId, "")
                activeSessionId = sessionId
                startSessionTimer()
            }
        } else {
            endAndSaveSession()
            _uiState.value = _uiState.value.copy(isStudyMode = false, isFlipped = false, currentIndex = 0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        endAndSaveSession()
    }

    fun flipCard() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun nextCard() {
        val current = _uiState.value
        val next = if (current.cards.isEmpty()) 0 else (current.currentIndex + 1).coerceAtMost(current.cards.lastIndex)
        _uiState.value = current.copy(currentIndex = next, isFlipped = false)
        saveSessionStateIncremental()
    }

    fun previousCard() {
        val current = _uiState.value
        val previous = (current.currentIndex - 1).coerceAtLeast(0)
        _uiState.value = current.copy(currentIndex = previous, isFlipped = false)
        saveSessionStateIncremental()
    }

    fun updateDeck(title: String, description: String?, coverImage: String?) {
        val deck = _uiState.value.deck ?: return
        viewModelScope.launch {
            knowledgeRepository.updateFlashcardDeck(deck.copy(title = title, description = description, coverImage = coverImage))
            _uiState.value = _uiState.value.copy(message = "Deck updated")
        }
    }

    fun addCard(front: String, back: String, hint: String?, tags: List<String>) {
        val deck = _uiState.value.deck ?: return
        viewModelScope.launch {
            val order = _uiState.value.cards.size
            knowledgeRepository.insertFlashcard(
                FlashcardEntity(
                    externalId = java.util.UUID.randomUUID().toString(),
                    deckId = deck.id,
                    frontText = front,
                    backText = back,
                    hint = hint,
                    tags = tags,
                    orderIndex = order
                )
            )
            _uiState.value = _uiState.value.copy(message = "Card added")
        }
    }

    fun updateCard(card: FlashcardEntity, front: String, back: String, hint: String?, tags: List<String>) {
        viewModelScope.launch {
            knowledgeRepository.updateFlashcard(
                card.copy(
                    frontText = front,
                    backText = back,
                    hint = hint,
                    tags = tags
                )
            )
            _uiState.value = _uiState.value.copy(message = "Card updated")
        }
    }

    fun deleteCard(card: FlashcardEntity) {
        viewModelScope.launch {
            knowledgeRepository.deleteFlashcard(card)
            _uiState.value = _uiState.value.copy(message = "Card deleted")
        }
    }

    fun moveCard(card: FlashcardEntity, direction: Int) {
        val cards = _uiState.value.cards.toMutableList()
        val index = cards.indexOfFirst { it.id == card.id }
        val target = index + direction
        if (index !in cards.indices || target !in cards.indices) return
        val moved = cards.removeAt(index)
        cards.add(target, moved)
        viewModelScope.launch { knowledgeRepository.reorderFlashcards(cards) }
    }

    fun toggleCardSelection(id: Long) {
        val current = _uiState.value.selectedCardIds
        val next = if (current.contains(id)) current - id else current + id
        _uiState.value = _uiState.value.copy(selectedCardIds = next)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedCardIds = emptySet())
    }

    fun selectAllCards() {
        val allIds = _uiState.value.cards.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedCardIds = allIds)
    }

    fun deleteSelectedCards() {
        val ids = _uiState.value.selectedCardIds
        if (ids.isEmpty()) return
        val cardsToDelete = _uiState.value.cards.filter { it.id in ids }
        viewModelScope.launch {
            cardsToDelete.forEach { knowledgeRepository.deleteFlashcard(it) }
            _uiState.value = _uiState.value.copy(selectedCardIds = emptySet(), message = "${ids.size} cards deleted")
        }
    }

    fun moveSelectedCards(targetDeckId: Long) {
        val ids = _uiState.value.selectedCardIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            knowledgeRepository.moveFlashcards(ids, targetDeckId)
            _uiState.value = _uiState.value.copy(selectedCardIds = emptySet(), message = "${ids.size} cards moved")
        }
    }

    fun copySelectedCards(targetDeckId: Long) {
        val ids = _uiState.value.selectedCardIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            knowledgeRepository.copyFlashcards(ids, targetDeckId)
            _uiState.value = _uiState.value.copy(selectedCardIds = emptySet(), message = "${ids.size} cards copied")
        }
    }

    fun rateCurrentCard(rating: String) {
        val card = _uiState.value.currentCard ?: return
        reviewedCardIds.add(card.id)
        totalAttemptsCount++
        if (rating == FLASHCARD_RATING_GOOD || rating == FLASHCARD_RATING_EASY) {
            correctAttemptsCount++
            cardScores[card.id] = if (rating == FLASHCARD_RATING_EASY) 1.0f else 0.8f
        } else {
            cardScores[card.id] = 0.2f
        }
        viewModelScope.launch {
            knowledgeRepository.rateFlashcard(card, rating)
            nextCard()
            saveSessionStateIncremental()
        }
    }

    fun generateFromMarked(
        @Suppress("UNUSED_PARAMETER") title: String,
        clearMarksAfter: Boolean,
        config: FlashcardGenerationConfig
    ) {
        val deck = _uiState.value.deck ?: return
        viewModelScope.launch {
            runCatching {
                val markedQuestions = bookRepository.getBookStudyBundle(deck.bookId)?.questions.orEmpty().filter { it.isMarked }
                val count = knowledgeRepository.addFlashcardsFromQuestionsToDeck(deck.id, markedQuestions.map { it.id }, config)
                if (clearMarksAfter) {
                    markedQuestions.forEach { question ->
                        assetRepository.updateQuestion(question.copy(isMarked = false, updatedAt = System.currentTimeMillis()))
                    }
                }
                count
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "$count marked-question card(s) added")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Failed to generate cards")
            }
        }
    }

    fun generateFromMissed(config: FlashcardGenerationConfig) {
        val deck = _uiState.value.deck ?: return
        viewModelScope.launch {
            runCatching {
                val missedIds = bookRepository.getBookStudyBundle(deck.bookId)?.questions.orEmpty()
                    .filter { it.attempts > 0 && (it.correctCount < it.attempts || it.lastAttemptResult == false) }
                    .map { it.id }
                knowledgeRepository.addFlashcardsFromQuestionsToDeck(deck.id, missedIds, config)
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "$count missed-question card(s) added")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Failed to generate cards")
            }
        }
    }

    fun generateFromBook(clearMarksAfter: Boolean, config: FlashcardGenerationConfig) {
        val deck = _uiState.value.deck ?: return
        viewModelScope.launch {
            runCatching {
                val questions = bookRepository.getBookStudyBundle(deck.bookId)?.questions.orEmpty()
                val count = knowledgeRepository.addFlashcardsFromQuestionsToDeck(deck.id, questions.map { it.id }, config)
                if (clearMarksAfter) {
                    questions.filter { it.isMarked }.forEach { question ->
                        assetRepository.updateQuestion(question.copy(isMarked = false, updatedAt = System.currentTimeMillis()))
                    }
                }
                count
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "$count card(s) added from book")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Failed to generate cards")
            }
        }
    }

    fun generateFromQuiz(quizId: Long, clearMarksAfter: Boolean, config: FlashcardGenerationConfig) {
        val deck = _uiState.value.deck ?: return
        viewModelScope.launch {
            runCatching {
                val bundle = bookRepository.getBookStudyBundle(deck.bookId)
                val questions = bundle?.questionsByQuiz?.get(quizId).orEmpty()
                val count = knowledgeRepository.addFlashcardsFromQuestionsToDeck(deck.id, questions.map { it.id }, config)
                if (clearMarksAfter) {
                    questions.filter { it.isMarked }.forEach { question ->
                        assetRepository.updateQuestion(question.copy(isMarked = false, updatedAt = System.currentTimeMillis()))
                    }
                }
                count
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "$count card(s) added from quiz")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Failed to generate cards")
            }
        }
    }

    fun generateFromCategory(categoryName: String, clearMarksAfter: Boolean, config: FlashcardGenerationConfig) {
        val deck = _uiState.value.deck ?: return
        viewModelScope.launch {
            runCatching {
                val questions = bookRepository.getBookStudyBundle(deck.bookId)?.questions.orEmpty()
                    .filter { it.categories.contains(categoryName) }
                val count = knowledgeRepository.addFlashcardsFromQuestionsToDeck(deck.id, questions.map { it.id }, config)
                if (clearMarksAfter) {
                    questions.filter { it.isMarked }.forEach { question ->
                        assetRepository.updateQuestion(question.copy(isMarked = false, updatedAt = System.currentTimeMillis()))
                    }
                }
                count
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "$count card(s) added from category '$categoryName'")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Failed to generate cards")
            }
        }
    }

    fun importFromText(text: String, mode: com.ahmedyejam.mks.data.importer.parser.TextParseMode) {
        val deck = _uiState.value.deck ?: return
        viewModelScope.launch {
            runCatching {
                val currentOrder = _uiState.value.cards.size
                val parser = TextFlashcardParser()
                val flashcards = parser.parse(text, deck.id, currentOrder, mode)
                knowledgeRepository.insertFlashcards(flashcards)
                flashcards.size
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "$count card(s) imported")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Failed to import cards")
            }
        }
    }
}
