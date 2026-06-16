package com.ahmedyejam.mks.ui.quiz


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.focus.FocusManager
import com.ahmedyejam.mks.data.local.entity.CategoryMetadataEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.ahmedyejam.mks.data.model.CategoryWithMetadata
import com.ahmedyejam.mks.data.preferences.DataStoreManager
import com.ahmedyejam.mks.data.repository.AssetRepository
import com.ahmedyejam.mks.data.repository.KnowledgeRepository
import com.ahmedyejam.mks.data.repository.QuizRepository
import com.ahmedyejam.mks.data.repository.StudyRepository
import com.ahmedyejam.mks.data.validation.SessionStateValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents the UI state for the Quiz screen.
 *
 * @property quizId The ID of the quiz being taken.
 * @property sessionId The ID of the current session, if any.
 * @property sessionLabel A display label for the current session.
 * @property questions The list of questions in the current quiz sequence.
 * @property currentIndex The index of the currently displayed question.
 * @property shuffledOptions The list of options for the current question, possibly shuffled.
 * @property optionMapping Maps shuffled option indices back to their original indices in the [QuestionEntity].
 * @property selectedOptions The set of original indices of currently selected options.
 * @property isAnswered Whether the current question has been answered.
 * @property isCorrect Whether the answer given for the current question is correct.
 * @property score The number of correctly answered questions.
 * @property droppedOptions The set of original indices of options that have been "dropped" (excluded) by the user.
 * @property isCompleted Whether the quiz session is finished.
 * @property isLoading Whether the quiz data is still being loaded.
 * @property initialQuestionCount The number of unique questions in the original sequence.
 * @property showHint Whether the hint for the current question is currently visible.
 * @property hintsEnabled Whether hints are globally enabled for this session.
 * @property isRapidMode Whether rapid mode (auto-submit/auto-advance) is enabled.
 * @property isOneByOne Whether options are revealed one by one.
 * @property visibleOptionsCount Number of options currently visible in one-by-one mode.
 * @property navigationFilter The current filter applied to the question navigation.
 * @property showCategorization Whether the categorization UI is visible.
 * @property allCategoriesWithMetadata List of all available categories with their metadata (emoji, color, etc.).
 * @property questionResultsByIndex Map of question index to its correctness result (true for correct, false for incorrect, null for unanswered).
 * @property quizTimerSeconds Total allowed time for the entire quiz (0 for no limit).
 * @property questionTimerSeconds Allowed time per question (0 for no limit).
 * @property shuffleQuestions Whether questions were shuffled when starting the session.
 * @property shuffleOptions Whether options are shuffled for each question.
 * @property repeatWrong Whether incorrect questions are added to the end of the sequence to be repeated.
 * @property skipUnansweredGlobal Whether to automatically skip unanswered questions.
 * @property currentStreak The current number of consecutive correct answers.
 * @property maxStreak The maximum streak achieved in the current session.
 */
data class QuizState(
    val quizId: Long = 0,
    val sessionId: Long? = null,
    val sessionLabel: String? = null,
    val questions: List<QuestionEntity> = emptyList(),
    val currentIndex: Int = 0,
    val shuffledOptions: List<String> = emptyList(),
    val optionMapping: List<Int> = emptyList(), // Maps shuffled index to original index
    val selectedOptions: Set<Int> = emptySet(), // These are original indices
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val score: Int = 0,
    val droppedOptions: Set<Int> = emptySet(), // Original indices
    val isCompleted: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val initialQuestionCount: Int = 0,
    val showHint: Boolean = false,
    val hintsEnabled: Boolean = true,
    val isRapidMode: Boolean = false,
    val isOneByOne: Boolean = false,
    val visibleOptionsCount: Int = 0,
    val navigationFilter: NavigationFilter = NavigationFilter.ALL,
    val showCategorization: Boolean = false,
    val allCategoriesWithMetadata: List<CategoryWithMetadata> = emptyList(),
    val questionResultsByIndex: Map<Int, Boolean?> = emptyMap(), // Map<IndexInSequence, isCorrect?>
    val quizTimerSeconds: Int = 0,
    val questionTimerSeconds: Int = 0,
    val shuffleQuestions: Boolean = true,
    val shuffleOptions: Boolean = true,
    val repeatWrong: Boolean = true,
    val skipUnansweredGlobal: Boolean = false,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val eliminationModeEnabled: Boolean = false,
    val doubleTapToSubmitEnabled: Boolean = false,
    val focusModeEnabled: Boolean = false,
)

/**
 * Represents the timer state for the Quiz screen.
 * Separated from QuizState to minimize recomposition.
 */
data class TimerState(
    val timeLeft: Int = 0,
    val quizTimeLeft: Int = 0,
    val questionTimeLeft: Int = 0,
)

/**
 * Represents the status of a question in the quiz navigation.
 */
enum class QuestionStatus {
    UNANSWERED, CORRECT, INCORRECT, CURRENT
}

/**
 * Filter options for navigating through quiz questions.
 */
enum class NavigationFilter {
    ALL, ANSWERED, UNANSWERED, MISSED, MARKED, DROPPED
}

/**
 * ViewModel for the Quiz screen, managing the state and logic of a quiz session.
 *
 * @property quizRepository The repository for accessing quiz and question data.
 * @property dataStoreManager Manager for persistent user preferences.
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val focusManager: FocusManager,
    private val knowledgeRepository: KnowledgeRepository,
    private val assetRepository: AssetRepository,
    private val quizRepository: QuizRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizState())
    val uiState: StateFlow<QuizState> = _uiState.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    val themeMode: StateFlow<String> = dataStoreManager.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DAWN")

    private var timerJob: Job? = null
    private var autoAdvanceJob: Job? = null
    private var autoAdvanceDelayMs: Long = 2000L

    init {
        combine(
            quizRepository.getAllCategoriesWithMetadata(),
            dataStoreManager.showCategorization,
            dataStoreManager.oneByOneMode,
            dataStoreManager.eliminationModeEnabled,
            dataStoreManager.doubleTapToSubmit,
            dataStoreManager.focusModeEnabled,
        ) { array ->
            @Suppress("UNCHECKED_CAST")
            val categories = array[0] as List<CategoryWithMetadata>
            val categorization = array[1] as Boolean
            val oneByOne = array[2] as Boolean
            val elimination = array[3] as Boolean
            val doubleTap = array[4] as Boolean
            val focus = array[5] as Boolean

            _uiState.update { 
                it.copy(
                    allCategoriesWithMetadata = categories,
                    showCategorization = categorization,
                    isOneByOne = oneByOne,
                    eliminationModeEnabled = elimination,
                    doubleTapToSubmitEnabled = doubleTap,
                    focusModeEnabled = focus,
                )
            }
            if (focus) focusManager.enableFocusMode() else focusManager.disableFocusMode()
        }.launchIn(viewModelScope)

        dataStoreManager.autoAdvanceDelay
            .onEach { delay -> autoAdvanceDelayMs = delay.toLong() }
            .launchIn(viewModelScope)
    }

    /**
     * Determines the status of a question at a specific index for UI coloring.
     */
    fun getQuestionStatus(index: Int): QuestionStatus {
        val state = _uiState.value
        if (index == state.currentIndex) return QuestionStatus.CURRENT
        return when (state.questionResultsByIndex[index]) {
            true -> QuestionStatus.CORRECT
            false -> QuestionStatus.INCORRECT
            else -> QuestionStatus.UNANSWERED
        }
    }

    /**
     * Initializes and starts a quiz session.
     *
     * @param quizId The ID of the quiz to start.
     * @param sessionId Optional ID of an existing session to resume.
     */
    fun startQuiz(quizId: Long, sessionId: Long? = null) {
        val currentState = _uiState.value
        if ((currentState.quizId == quizId) && (currentState.sessionId == sessionId)) {
            if (currentState.questions.isNotEmpty() || currentState.isLoading) {
                return
            }
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, quizId = quizId, sessionId = sessionId, error = null) }
                
                var shuffleQ = dataStoreManager.defShuffleQuestions.first()
                var shuffleOpt = dataStoreManager.defShuffleOptions.first()
                var rapidMode = dataStoreManager.defRapidMode.first()
                var repeatWrong = dataStoreManager.defRepeatWrong.first()
                var rangeFrom = 0
                var rangeTo = -1
                var quizTimer = dataStoreManager.defQuizTimer.first()
                var qTimer = dataStoreManager.defQuestionTimer.first()
                val skipUnanswered = dataStoreManager.unansweredSkipEnabled.first()
                
                var startIndex = 0
                var sessionLabel: String? = null
                var sessionScore = 0
                var sessionCurrentStreak = 0
                var sessionMaxStreak = 0
                var initialResultsByIndex: Map<Int, Boolean?> = emptyMap()
                var sessionQuestionIds: List<Long>? = null
                var sessionOriginalCount = 0

                val session = sessionId?.let { quizRepository.getSessionById(it) }
                
                // Pre-fetch session questions if any to avoid many DB calls
                val sessionQuestionsMap = if ((session != null) && session.questionIds.isNotEmpty()) {
                    quizRepository.getQuestionsByIds(session.questionIds).associateBy { it.id }
                } else emptyMap()

                if (session != null) {
                    val validation = SessionStateValidator.validate(session)
                    startIndex = if (!validation.isValid) {
                        if (validation.canBeRepaired) {
                            session.currentQuestionIndex.coerceIn(0, (session.questionIds.size - 1).coerceAtLeast(0))
                        } else {
                            _uiState.update { it.copy(isLoading = false, error = validation.message ?: "Invalid session.") }
                            return@launch
                        }
                    } else {
                        session.currentQuestionIndex
                    }
                    
                    // Final safety: if the current question data is missing, we must repair or block
                    val currentQId = session.questionIds.getOrNull(startIndex)
                    if ((currentQId != null) && (!sessionQuestionsMap.containsKey(currentQId))) {
                        _uiState.update { it.copy(isLoading = false, error = "Current question data is missing. This session may be corrupted.") }
                        return@launch
                    }

                    sessionLabel = session.label
                    sessionScore = session.score
                    sessionCurrentStreak = session.currentStreak
                    sessionMaxStreak = session.maxStreak
                    shuffleQ = session.shuffleQuestions
                    shuffleOpt = session.shuffleOptions
                    rapidMode = session.rapidMode
                    repeatWrong = session.repeatWrong
                    rangeFrom = session.rangeFrom
                    rangeTo = session.rangeTo
                    quizTimer = session.quizTimerSeconds
                    qTimer = session.questionTimerSeconds
                    sessionQuestionIds = session.questionIds
                    sessionOriginalCount = session.originalQuestionCount
                    
                    initialResultsByIndex = session.answersByIndex.mapValues { (index, selection) ->
                        val qId = session.questionIds.getOrNull(index)
                        val q = sessionQuestionsMap[qId ?: -1]
                        q?.let { it.correctAnswers.toSet() == selection.toSet() }
                    }
                }

                val quizQuestions = if (!sessionQuestionIds.isNullOrEmpty()) {
                    sessionQuestionIds.mapNotNull { sessionQuestionsMap[it] }
                } else {
                    val allRawQuestions = quizRepository.getQuestionsByQuizId(quizId).first()
                        .filter { !it.isDropped }

                    val includeFilters = session?.includeFilters ?: emptyList()

                    val filteredQuestions = if (includeFilters.isNotEmpty()) {
                        allRawQuestions.filter { q ->
                            includeFilters.any { filter ->
                                when (filter) {
                                    "unanswered" -> q.attempts == 0
                                    "missed" -> (q.attempts > 0) && ((q.correctCount < q.attempts) || (q.lastAttemptResult == false))
                                    "marked" -> q.isMarked
                                    "categorized" -> q.categories.isNotEmpty()
                                    "uncategorized" -> q.categories.isEmpty()
                                    else -> true
                                }
                            }
                        }
                    } else {
                        allRawQuestions
                    }
                    
                    // Apply range first. SessionEntity stores zero-based indices.
                    val effectiveRangeFrom = rangeFrom.coerceAtLeast(0)
                        .coerceAtMost((filteredQuestions.size - 1).coerceAtLeast(0))
                    
                    val effectiveRangeTo = if ((rangeTo < 0) || (rangeTo >= filteredQuestions.size)) {
                        filteredQuestions.size - 1
                    } else {
                        rangeTo
                    }
                    
                    val rangedQuestions = if ((filteredQuestions.isNotEmpty()) && (effectiveRangeFrom <= effectiveRangeTo)) {
                        filteredQuestions.subList(effectiveRangeFrom, (effectiveRangeTo + 1).coerceAtMost(filteredQuestions.size))
                    } else {
                        filteredQuestions
                    }

                    if (shuffleQ) {
                        val seed = sessionId ?: quizId
                        rangedQuestions.shuffled(java.util.Random(seed))
                    } else {
                        rangedQuestions
                    }
                }

                if (quizQuestions.isEmpty()) {
                    val errorMessage = if (session?.includeFilters?.isNotEmpty() == true) {
                        "No questions match your current filters. Try changing filters in session settings."
                    } else if (rangeFrom > 0 || rangeTo > 0) {
                        "No questions found in the selected range."
                    } else {
                        "No active questions found for this quiz."
                    }
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    return@launch
                }
                
                val finalOriginalCount = if (sessionOriginalCount > 0) sessionOriginalCount else quizQuestions.size

                _uiState.update {
                    it.copy(
                        questions = quizQuestions,
                        currentIndex = startIndex.coerceIn(0, quizQuestions.size.coerceAtLeast(1) - 1),
                        isLoading = false,
                        initialQuestionCount = finalOriginalCount,
                        sessionId = sessionId,
                        sessionLabel = sessionLabel,
                        score = sessionScore,
                        currentStreak = sessionCurrentStreak,
                        maxStreak = sessionMaxStreak,
                        isRapidMode = rapidMode,
                        questionResultsByIndex = initialResultsByIndex,
                        shuffleQuestions = shuffleQ,
                        shuffleOptions = shuffleOpt,
                        repeatWrong = repeatWrong,
                        quizTimerSeconds = quizTimer,
                        questionTimerSeconds = qTimer,
                        skipUnansweredGlobal = skipUnanswered,
                    )
                }

            _timerState.update {
                it.copy(
                    quizTimeLeft = if (session != null && session.quizTimerSeconds > 0) {
                        val elapsedMillis = System.currentTimeMillis() - session.lastModifiedAt
                        val elapsedSeconds = (elapsedMillis / 1000).toInt()
                        (session.quizTimerSeconds - elapsedSeconds).coerceAtLeast(0)
                    } else {
                        quizTimer
                    },
                    questionTimeLeft = qTimer,
                    timeLeft = session?.let {
                        // We don't store exact total elapsed time in SessionEntity currently, 
                        // but it could be inferred or added. For now, reset or approximate.
                        0 
                    } ?: 0
                )
            }
            
            // Save initial sequence if new session
            if (session != null && session.questionIds.isEmpty()) {
                quizRepository.updateSession(session.copy(
                    questionIds = quizQuestions.map { it.id },
                    originalQuestionCount = finalOriginalCount
                ))
            }

            val restoredAnswers = session?.answersByIndex?.get(_uiState.value.currentIndex)?.toSet()
            val restoredDropped = session?.droppedOptionsByIndex?.get(_uiState.value.currentIndex)?.toSet()
            val restoredVisibleCount = session?.visibleOptionsCountByIndex?.get(_uiState.value.currentIndex)
            
            prepareQuestion(
                restoredSelection = restoredAnswers,
                restoredDropped = restoredDropped,
                restoredVisibleCount = restoredVisibleCount
            )
            startTimer()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, error = "Failed to load quiz: ${e.message}") }
            }
        }
    }


    /**
     * Prepares the state for the current question, including option shuffling.
     *
     * @param restoredSelection Previously selected options if resuming.
     * @param restoredDropped Previously dropped options if resuming.
     * @param restoredVisibleCount Number of visible options if resuming in one-by-one mode.
     */
    private fun prepareQuestion(
        restoredSelection: Set<Int>? = null,
        restoredDropped: Set<Int>? = null,
        restoredVisibleCount: Int? = null
    ) {
        val state = _uiState.value
        if (state.questions.isEmpty() || state.currentIndex >= state.questions.size) return
        
        val question = state.questions[state.currentIndex]
        val shuffleOpt = state.shuffleOptions
        val optionsWithIndices = question.options.mapIndexed { index, s -> index to s }
        
        // Use index in seed for unique shuffling even if question repeats
        val seed = (state.sessionId ?: state.quizId) + question.id + state.currentIndex
        val processedOptions = if (shuffleOpt) {
            optionsWithIndices.shuffled(java.util.Random(seed))
        } else {
            optionsWithIndices
        }
        
        val isAnswered = restoredSelection != null
        val isCorrect = restoredSelection != null && question.correctAnswers.toSet() == restoredSelection
        
        _uiState.update {
            it.copy(
                shuffledOptions = processedOptions.map { pair -> pair.second },
                optionMapping = processedOptions.map { pair -> pair.first },
                selectedOptions = restoredSelection ?: emptySet(),
                isAnswered = isAnswered,
                isCorrect = isCorrect,
                droppedOptions = restoredDropped ?: emptySet(),
                showHint = false,
                visibleOptionsCount = restoredVisibleCount ?: if (it.isOneByOne && !isAnswered) 0 else processedOptions.size
            )
        }

        _timerState.update {
            it.copy(questionTimeLeft = _uiState.value.questionTimerSeconds)
        }
    }

    /**
     * Starts a personalized adaptive training session.
     *
     * @param type The type of training ("BOOK", "CATEGORY", or "ALL").
     * @param id The ID associated with the type (book ID or category name).
     * @param limit Maximum number of questions to include.
     */
    fun startAdaptiveTraining(type: String, id: String?, limit: Int = 20) {
        if (_uiState.value.questions.isNotEmpty() && !_uiState.value.isLoading) {
            // Simple check to avoid restart on rotation for adaptive
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val questions = when (type) {
                    "BOOK" -> id?.toLongOrNull()?.let { studyRepository.getAdaptiveQuestionsByBook(it, limit) } ?: emptyList()
                    "CATEGORY" -> if (id != null) quizRepository.getQuestionsByCategory(id) else emptyList()
                    "ALL" -> studyRepository.getAdaptiveQuestionsByBook(-1L, limit)
                    else -> emptyList()
                }.shuffled()

                _uiState.update {
                    it.copy(
                        questions = questions,
                        currentIndex = 0,
                        isLoading = false,
                        initialQuestionCount = questions.size,
                        sessionLabel = when (type) {
                            "CATEGORY" -> "Category: $id"
                            "BOOK" -> "Review: Book $id"
                            else -> "Personalized Training"
                        }
                    )
                }
                prepareQuestion()
                startTimer()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, error = "Failed to load adaptive training: ${e.message}") }
            }
        }
    }

    /**
     * Toggles rapid mode where answers are submitted immediately upon selection.
     */
    fun toggleRapidMode() {
        val newMode = !_uiState.value.isRapidMode
        _uiState.update { it.copy(isRapidMode = newMode) }
        viewModelScope.launch {
            _uiState.value.sessionId?.let { sessionId ->
                quizRepository.getSessionById(sessionId)?.let { session ->
                    quizRepository.updateSession(session.copy(rapidMode = newMode))
                }
            }
        }
    }

    /**
     * Toggles one-by-one mode where options are revealed one at a time.
     */
    fun toggleOneByOne() {
        val newIsOneByOne = !_uiState.value.isOneByOne
        _uiState.update { 
            it.copy(
                isOneByOne = newIsOneByOne,
                visibleOptionsCount = if (newIsOneByOne && !it.isAnswered) 0 else it.shuffledOptions.size
            )
        }
        viewModelScope.launch {
            dataStoreManager.setOneByOneMode(newIsOneByOne)
        }
    }

    /**
     * Toggles elimination mode where options can be crossed out.
     */
    fun toggleEliminationMode() {
        viewModelScope.launch {
            dataStoreManager.setEliminationModeEnabled(!_uiState.value.eliminationModeEnabled)
        }
    }

    /**
     * Toggles focus mode where notifications are blocked.
     */
    fun toggleFocusMode() {
        val newEnabled = !_uiState.value.focusModeEnabled
        if (newEnabled && !focusManager.hasNotificationPolicyAccess()) {
            focusManager.requestNotificationPolicyAccess()
            return
        }

        viewModelScope.launch {
            dataStoreManager.setFocusModeEnabled(newEnabled)
        }
    }

    /**
     * Toggles the "marked" (flagged) status of the current question.
     */
    fun toggleMarked() {
        val state = _uiState.value
        if (state.questions.isEmpty()) return
        val currentQuestion = state.questions[state.currentIndex]
        val newMarked = !currentQuestion.isMarked
        
        viewModelScope.launch {
            val updatedQuestion = currentQuestion.copy(isMarked = newMarked)
            assetRepository.updateQuestion(updatedQuestion)
            
            _uiState.update { s ->
                val newQuestions = s.questions.toMutableList()
                newQuestions[s.currentIndex] = updatedQuestion
                s.copy(questions = newQuestions)
            }
        }
    }

    /**
     * Marks the current question as dropped and moves to the next one.
     */
    fun dropQuestion() {
        val currentState = _uiState.value
        if (currentState.questions.isEmpty()) return
        val currentQuestion = currentState.questions[currentState.currentIndex]
        val droppedQuestion = currentQuestion.copy(isDropped = true, updatedAt = System.currentTimeMillis())
        
        viewModelScope.launch {
            assetRepository.updateQuestion(droppedQuestion)
            _uiState.update { state ->
                val updatedQuestions = state.questions.toMutableList()
                updatedQuestions[state.currentIndex] = droppedQuestion
                state.copy(questions = updatedQuestions)
            }
            
            // Sync session if active
            currentState.sessionId?.let { sessionId ->
                quizRepository.getSessionById(sessionId)?.let { currentSession ->
                quizRepository.updateSession(
                    currentSession.copy(
                        lastModifiedAt = System.currentTimeMillis(),
                    ),
                )
            }
            }

            nextQuestion()
        }
    }

    /** Restores the current dropped question so it can rejoin normal review flows. */
    fun restoreCurrentDroppedQuestion() {
        val currentState = _uiState.value
        if (currentState.questions.isEmpty()) return
        val currentQuestion = currentState.questions[currentState.currentIndex]
        if (!currentQuestion.isDropped) return
        val restoredQuestion = currentQuestion.copy(isDropped = false, updatedAt = System.currentTimeMillis())

        viewModelScope.launch {
            assetRepository.updateQuestion(restoredQuestion)
            _uiState.update { state ->
                val updatedQuestions = state.questions.toMutableList()
                updatedQuestions[state.currentIndex] = restoredQuestion
                state.copy(questions = updatedQuestions)
            }
        }
    }

    /**
     * Marks the current session as completed and saves progress.
     */
    fun finishSession() {
        _uiState.update { it.copy(isCompleted = true) }
        timerJob?.cancel()
        autoAdvanceJob?.cancel()
        viewModelScope.launch {
            val state = _uiState.value
            state.sessionId?.let { sessionId ->
                val session = quizRepository.getSessionById(sessionId)
                session?.let {
                    quizRepository.updateSession(
                        it.copy(
                            isCompleted = true,
                            score = state.score,
                            currentStreak = state.currentStreak,
                            maxStreak = state.maxStreak,
                            lastModifiedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }
            dataStoreManager.clearSession()
        }
    }

    /**
     * Toggles the visibility of the categorization management UI.
     */
    fun toggleCategorization() {
        val newEnabled = !_uiState.value.showCategorization
        _uiState.update { it.copy(showCategorization = newEnabled) }
        viewModelScope.launch {
            dataStoreManager.setShowCategorization(newEnabled)
        }
    }

    /**
     * Toggles a category for the current question.
     */
    fun toggleQuestionCategory(category: String) {
        val currentState = _uiState.value
        val question = currentState.questions[currentState.currentIndex]
        val newCats = if (question.categories.contains(category)) {
            question.categories - category
        } else {
            question.categories + category
        }
        
        val updatedQuestion = question.copy(categories = newCats)
        val updatedQuestions = currentState.questions.toMutableList()
        updatedQuestions[currentState.currentIndex] = updatedQuestion
        
        _uiState.update { it.copy(questions = updatedQuestions) }
        viewModelScope.launch {
            assetRepository.updateQuestion(updatedQuestion)
        }
    }

    /**
     * Toggles the pinned status of a category.
     *
     * @param categoryName The name of the category to pin or unpin.
     */
    fun onToggleCategoryPin(categoryName: String) {
        viewModelScope.launch {
            val metadata = _uiState.value.allCategoriesWithMetadata.find { it.name == categoryName }?.metadata
                ?: CategoryMetadataEntity(name = categoryName)
            quizRepository.updateCategoryMetadata(metadata.copy(isPinned = !metadata.isPinned))
        }
    }

    /**
     * Updates the emoji associated with a category.
     *
     * @param categoryName The name of the category.
     * @param emoji The new emoji string, or null to clear it.
     */
    fun onUpdateCategoryEmoji(categoryName: String, emoji: String?) {
        viewModelScope.launch {
            val metadata = _uiState.value.allCategoriesWithMetadata.find { it.name == categoryName }?.metadata
                ?: CategoryMetadataEntity(name = categoryName)
            quizRepository.updateCategoryMetadata(metadata.copy(emoji = emoji))
        }
    }

    /**
     * Updates the theme color associated with a category.
     *
     * @param categoryName The name of the category.
     * @param color The new color integer, or null to clear it.
     */
    fun onUpdateCategoryColor(categoryName: String, color: Int?) {
        viewModelScope.launch {
            val metadata = _uiState.value.allCategoriesWithMetadata.find { it.name == categoryName }?.metadata
                ?: CategoryMetadataEntity(name = categoryName)
            quizRepository.updateCategoryMetadata(metadata.copy(color = color))
        }
    }

    /**
     * Gets a preview of how many questions would be affected by merging two categories.
     *
     * @param source The source category name.
     * @param target The target category name.
     * @return The number of questions that would be updated.
     */
    suspend fun getMergePreview(source: String, target: String): Int {
        return quizRepository.getMergePreview(source, target)
    }

    /**
     * Renames an existing category globally.
     *
     * @param oldName The current name of the category.
     * @param newName The new name for the category.
     */
    fun renameCategory(oldName: String, newName: String) {
        viewModelScope.launch {
            quizRepository.renameCategory(oldName, newName)
        }
    }

    /**
     * Deletes a category globally from all questions and metadata.
     *
     * @param name The name of the category to delete.
     */
    fun deleteCategory(name: String) {
        viewModelScope.launch {
            quizRepository.deleteCategory(name)
        }
    }

    /**
     * Merges one category into another.
     *
     * @param source The category to be merged and removed.
     * @param target The category that will receive the questions from the source.
     */
    fun mergeCategory(source: String, target: String) {
        viewModelScope.launch {
            quizRepository.mergeCategory(source, target)
        }
    }

    /**
     * Sets the navigation filter for questions.
     */
    fun setNavigationFilter(filter: NavigationFilter) {
        _uiState.update { it.copy(navigationFilter = filter) }
    }

    /**
     * Jumps to a specific question in the sequence.
     */
    fun jumpToQuestion(index: Int) {
        val state = _uiState.value
        if (index in state.questions.indices) {
            questionStartTime = System.currentTimeMillis()
            viewModelScope.launch {
                val session = state.sessionId?.let { quizRepository.getSessionById(it) }
                val restoredAnswers = session?.answersByIndex?.get(index)?.toSet()
                val restoredDropped = session?.droppedOptionsByIndex?.get(index)?.toSet()
                val restoredVisibleCount = session?.visibleOptionsCountByIndex?.get(index)

                _uiState.update { it.copy(currentIndex = index) }
                
                prepareQuestion(
                    restoredSelection = restoredAnswers,
                    restoredDropped = restoredDropped,
                    restoredVisibleCount = restoredVisibleCount
                )

                // Save progress to session if active
                state.sessionId?.let {
                    session?.let { currentSession ->
                    quizRepository.updateSession(
                        currentSession.copy(
                            currentQuestionIndex = index,
                            lastModifiedAt = System.currentTimeMillis(),
                        ),
                    )
                }
                }
                dataStoreManager.saveSession(state.quizId, index)
            }
        }
    }

    /**
     * Navigates to the previous question in the sequence.
     */
    fun previousQuestion() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            jumpToQuestion(state.currentIndex - 1)
        }
    }

    /**
     * Toggles the visibility of the current question's hint.
     */
    fun toggleHint() {
        _uiState.update { it.copy(showHint = !it.showHint) }
    }

    /**
     * Handles option selection by the user.
     *
     * @param shuffledIndex The index of the selected option in the shuffled list.
     */
    fun onOptionSelected(shuffledIndex: Int) {
        val currentState = _uiState.value
        val originalIndex = currentState.optionMapping[shuffledIndex]
        
        if (currentState.isAnswered || currentState.droppedOptions.contains(originalIndex)) return

        val currentQuestion = currentState.questions[currentState.currentIndex]
        
        if (currentState.isRapidMode && currentQuestion.type == QuestionType.SINGLE_CHOICE) {
            submitSingleChoice(originalIndex, currentQuestion, currentState)
        } else {
            _uiState.update { state ->
                val newSelection = if (currentQuestion.type == QuestionType.SINGLE_CHOICE) {
                    setOf(originalIndex)
                } else {
                    if (state.selectedOptions.contains(originalIndex)) {
                        state.selectedOptions - originalIndex
                    } else {
                        state.selectedOptions + originalIndex
                    }
                }
                state.copy(selectedOptions = newSelection)
            }
        }
    }

    /**
     * Handles option double click by the user.
     *
     * @param shuffledIndex The index of the selected option in the shuffled list.
     */
    fun onOptionDoubleClicked(shuffledIndex: Int) {
        val currentState = _uiState.value
        val originalIndex = currentState.optionMapping[shuffledIndex]

        if (currentState.isAnswered || currentState.droppedOptions.contains(originalIndex)) return
        if (!currentState.doubleTapToSubmitEnabled || currentState.isRapidMode) return

        val currentQuestion = currentState.questions[currentState.currentIndex]

        if (currentQuestion.type == QuestionType.SINGLE_CHOICE) {
            submitSingleChoice(originalIndex, currentQuestion, currentState)
        }
    }

    private fun submitSingleChoice(originalIndex: Int, currentQuestion: QuestionEntity, currentState: QuizState) {
        val currentIndex = currentState.currentIndex
        val isAlreadyAnswered = currentState.questionResultsByIndex.containsKey(currentIndex)
        if (isAlreadyAnswered) return

        // Merge selection and submission for Rapid Mode or Double Tap to avoid double recomposition
        val newSelection = setOf(originalIndex)
        applyAnswer(newSelection, currentQuestion, currentState)
    }

    /**
     * Submits the current selection as an answer.
     *
     * @param isTimeout Whether the submission was triggered by a timer timeout.
     */
    fun submitAnswer(isTimeout: Boolean = false) {
        val currentState = _uiState.value
        
        // One-by-one logic: If not all options shown, "Next" (or "Reveal") shows next option
        // Skip this on timeout to immediately show the answer
        if (!isTimeout && currentState.isOneByOne && !currentState.isAnswered && currentState.visibleOptionsCount < currentState.shuffledOptions.size) {
            val nextVisibleCount = currentState.visibleOptionsCount + 1
            val currentIndex = currentState.currentIndex
            _uiState.update { it.copy(visibleOptionsCount = nextVisibleCount) }
            
            // Sync visible count to session
            viewModelScope.launch {
                currentState.sessionId?.let { sessionId ->
                    quizRepository.getSessionById(sessionId)?.let { session ->
                        val updatedVisible = session.visibleOptionsCountByIndex.toMutableMap()
                        updatedVisible[currentIndex] = nextVisibleCount
                        quizRepository.updateSession(session.copy(visibleOptionsCountByIndex = updatedVisible))
                    }
                }
            }
            return
        }

        if (currentState.isAnswered) {
            nextQuestion()
            return
        }
        
        if (currentState.selectedOptions.isEmpty() && !isTimeout) {
            if (currentState.skipUnansweredGlobal) {
                nextQuestion()
            }
            return
        }

        val currentIndex = currentState.currentIndex
        val isAlreadyAnswered = currentState.questionResultsByIndex.containsKey(currentIndex)
        if (isAlreadyAnswered) return

        val currentQuestion = currentState.questions[currentIndex]
        applyAnswer(currentState.selectedOptions, currentQuestion, currentState)
    }

    private fun applyAnswer(
        newSelection: Set<Int>,
        currentQuestion: QuestionEntity,
        currentState: QuizState
    ) {
        val isCorrect = currentQuestion.correctAnswers.toSet() == newSelection
        val currentIndex = currentState.currentIndex

        val isFirstAttempt = currentIndex < currentState.initialQuestionCount
        val newStreak = if (isFirstAttempt) {
            if (isCorrect) currentState.currentStreak + 1 else 0
        } else {
            currentState.currentStreak
        }
        val newMaxStreak = maxOf(currentState.maxStreak, newStreak)
        val newScore =
            if (isCorrect && isFirstAttempt) currentState.score + 1 else currentState.score

        _uiState.update { state ->
            val updatedQuestions = if (!isCorrect && state.repeatWrong) {
                state.questions + currentQuestion
            } else {
                state.questions
            }

            state.copy(
                selectedOptions = newSelection,
                isAnswered = true,
                isCorrect = isCorrect,
                score = newScore,
                currentStreak = newStreak,
                maxStreak = newMaxStreak,
                visibleOptionsCount = state.shuffledOptions.size,
                questionResultsByIndex = state.questionResultsByIndex + (currentIndex to isCorrect),
                questions = updatedQuestions
            )
        }

        // Trigger side effects (database updates, auto-advance)
        finalizeSubmission(
            currentQuestion = currentQuestion,
            isCorrect = isCorrect,
            currentIndex = currentIndex,
            selectedOptions = newSelection,
            sessionId = currentState.sessionId,
            newScore = newScore,
            newCurrentStreak = newStreak,
            newMaxStreak = newMaxStreak,
            initialQuestionCount = currentState.initialQuestionCount,
            repeatWrong = currentState.repeatWrong,
            shuffledOptionsSize = currentState.shuffledOptions.size,
            isRapidMode = currentState.isRapidMode
        )
    }

    private var questionStartTime: Long = System.currentTimeMillis()

    private fun finalizeSubmission(
        currentQuestion: QuestionEntity,
        isCorrect: Boolean,
        currentIndex: Int,
        selectedOptions: Set<Int>,
        sessionId: Long?,
        newScore: Int,
        newCurrentStreak: Int,
        newMaxStreak: Int,
        initialQuestionCount: Int,
        repeatWrong: Boolean,
        shuffledOptionsSize: Int,
        isRapidMode: Boolean
    ) {
        val timeSpent = System.currentTimeMillis() - questionStartTime
        val isFirstAttempt = currentIndex < initialQuestionCount

        viewModelScope.launch {
            // Only update long-term metrics on first attempt to avoid inflation
            if (isFirstAttempt) {
                studyRepository.updateQuestionMetrics(currentQuestion.id, isCorrect, timeSpent)
            }

            if (!isCorrect) {
                val quiz = quizRepository.getQuizById(currentQuestion.quizId)
                val selectedText = selectedOptions.asSequence().sorted().mapNotNull { currentQuestion.options.getOrNull(it) }.joinToString(", ").ifBlank { selectedOptions.joinToString(",") }
                val correctText = currentQuestion.correctAnswers.asSequence().sorted().mapNotNull { currentQuestion.options.getOrNull(it) }.joinToString(", ").ifBlank { currentQuestion.correctAnswers.joinToString(",") }
                studyRepository.autoLogWrongAnswer(
                    bookId = quiz?.bookId ?: 0L,
                    quizId = currentQuestion.quizId,
                    questionId = currentQuestion.id,
                    sessionId = sessionId,
                    selectedAnswer = selectedText,
                    correctAnswer = correctText
                )
            }
            
            // Save score and incorrect count to session
            sessionId?.let { sId ->
                val session = quizRepository.getSessionById(sId)
                session?.let { currentSession ->
                    val newIncorrectCount = if (!isCorrect && isFirstAttempt) currentSession.incorrectCount + 1 else currentSession.incorrectCount
                    
                    // Update session answers map (by ID)
                    val updatedAnswersById = currentSession.answers.toMutableMap()
                    // Always update answers map to keep latest answer for summary
                    updatedAnswersById[currentQuestion.id] = selectedOptions.toList()
                    
                    // Update session answers map (by Index)
                    val updatedAnswersByIndex = currentSession.answersByIndex.toMutableMap()
                    updatedAnswersByIndex[currentIndex] = selectedOptions.toList()

                    val updatedVisible = currentSession.visibleOptionsCountByIndex.toMutableMap()
                    updatedVisible[currentIndex] = shuffledOptionsSize
                    
                    val updatedQuestionIds = if (!isCorrect && repeatWrong) {
                        currentSession.questionIds + currentQuestion.id
                    } else {
                        currentSession.questionIds
                    }

                    // Update result taxonomy
                    val updatedTaxonomy = currentSession.resultTaxonomy.toMutableMap()
                    if (isFirstAttempt) {
                        updatedTaxonomy[currentIndex] =
                            if (isCorrect) "CORRECT_FIRST_TRY" else "WRONG"
                    } else {
                        // For repeats, find the original index this question belongs to
                        // This is a simplification: assuming the original indices are 0..initialQuestionCount-1
                        // and repeats are appended. We need to find which original index we are repeating.
                        // In a simple sequence, we could track the original index, but here we can try to find the last 
                        // WRONG entry for this question ID.
                        val originalIndex = currentSession.questionIds.take(initialQuestionCount)
                            .indexOf(currentQuestion.id)

                        if (originalIndex != -1 && isCorrect && updatedTaxonomy[originalIndex] == "WRONG") {
                            updatedTaxonomy[originalIndex] = "CORRECTED_AFTER_REPEAT"
                        }
                    }

                    quizRepository.updateSession(
                        currentSession.copy(
                            score = newScore,
                            currentStreak = newCurrentStreak,
                            maxStreak = newMaxStreak,
                            incorrectCount = newIncorrectCount,
                            answers = updatedAnswersById,
                            answersByIndex = updatedAnswersByIndex,
                            visibleOptionsCountByIndex = updatedVisible,
                            questionIds = updatedQuestionIds,
                            resultTaxonomy = updatedTaxonomy,
                            lastModifiedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            
            if (isRapidMode) {
                autoAdvanceJob = launch {
                    delay(autoAdvanceDelayMs)
                    nextQuestion()
                }
            }
        }
    }

    /**
     * Drops (excludes) an option from consideration.
     *
     * @param shuffledIndex The index of the option to drop in the shuffled list.
     */
    fun dropOption(shuffledIndex: Int) {
        val currentState = _uiState.value
        if (currentState.isAnswered) return
        
        val originalIndex = currentState.optionMapping[shuffledIndex]
        val currentQuestion = currentState.questions[currentState.currentIndex]
        val isCorrect = currentQuestion.correctAnswers.contains(originalIndex)
        val currentIndex = currentState.currentIndex

        if (isCorrect) {
            // Dropping the correct option counts as wrong outcome immediately
            val isFirstAttempt = currentIndex < currentState.initialQuestionCount
            _uiState.update {
                val newStreak = if (isFirstAttempt) 0 else it.currentStreak
                it.copy(
                    isAnswered = true,
                    isCorrect = false,
                    selectedOptions = setOf(originalIndex),
                    currentStreak = newStreak,
                    visibleOptionsCount = it.shuffledOptions.size,
                    questionResultsByIndex = it.questionResultsByIndex + (currentIndex to false)
                )
            }
            
            viewModelScope.launch {
                studyRepository.updateQuestionMetrics(currentQuestion.id, isCorrect = false)
                _uiState.value.sessionId?.let { sessionId ->
                    val session = quizRepository.getSessionById(sessionId)
                    session?.let { currentSession ->
                        val newIncorrectCount = if (isFirstAttempt) currentSession.incorrectCount + 1 else currentSession.incorrectCount
                        val newCurrentStreak = _uiState.value.currentStreak
                        val newMaxStreak = _uiState.value.maxStreak
                        val updatedAnswers = currentSession.answersByIndex.toMutableMap()
                        updatedAnswers[currentIndex] = listOf(originalIndex)
                        
                        val updatedQuestionIds = if (_uiState.value.repeatWrong) {
                            session.questionIds + currentQuestion.id
                        } else {
                            session.questionIds
                        }

                        quizRepository.updateSession(session.copy(
                            incorrectCount = newIncorrectCount,
                            currentStreak = newCurrentStreak,
                            maxStreak = newMaxStreak,
                            answersByIndex = updatedAnswers,
                            questionIds = updatedQuestionIds,
                            lastModifiedAt = System.currentTimeMillis()
                        ))

                        if (_uiState.value.repeatWrong) {
                            _uiState.update { state -> state.copy(questions = state.questions + currentQuestion) }
                        }
                    }
                }

                if (currentState.isRapidMode) {
                    val delayMs = dataStoreManager.autoAdvanceDelay.first().toLong()
                    autoAdvanceJob = launch {
                        delay(delayMs)
                        nextQuestion()
                    }
                }
            }
        } else {
            val nextDropped = currentState.droppedOptions + originalIndex
            _uiState.update { 
                it.copy(
                    droppedOptions = nextDropped,
                    selectedOptions = it.selectedOptions - originalIndex
                )
            }
            
            // Auto-selection logic: if only one option remains (not dropped), select it
            val remainingOptions = currentState.optionMapping.filter { !nextDropped.contains(it) }
            if (remainingOptions.size == 1) {
                val lastShuffledIndex = currentState.optionMapping.indexOf(remainingOptions.first())
                if (lastShuffledIndex != -1) {
                    onOptionSelected(lastShuffledIndex)
                }
            }

            viewModelScope.launch {
                currentState.sessionId?.let { sessionId ->
                    quizRepository.getSessionById(sessionId)?.let { session ->
                        val updatedDropped = session.droppedOptionsByIndex.toMutableMap()
                        updatedDropped[currentIndex] = nextDropped.toList()
                        quizRepository.updateSession(session.copy(droppedOptionsByIndex = updatedDropped))
                    }
                }
            }
        }
    }


    /**
     * Advances to the next question in the sequence.
     */
    fun nextQuestion() {
        autoAdvanceJob?.cancel()
        val currentState = _uiState.value
        
        val nextIndex = currentState.currentIndex + 1

        if (nextIndex < currentState.questions.size) {
            questionStartTime = System.currentTimeMillis()
            viewModelScope.launch {
                val session = currentState.sessionId?.let { quizRepository.getSessionById(it) }
                val restoredAnswers = session?.answersByIndex?.get(nextIndex)?.toSet()
                val restoredDropped = session?.droppedOptionsByIndex?.get(nextIndex)?.toSet()
                val restoredVisibleCount = session?.visibleOptionsCountByIndex?.get(nextIndex)

                // Atomically update index and prepare question state to avoid double recomposition
                val question = currentState.questions[nextIndex]
                val shuffleOpt = currentState.shuffleOptions
                val optionsWithIndices = question.options.mapIndexed { index, s -> index to s }
                val seed = (currentState.sessionId ?: currentState.quizId) + question.id + nextIndex
                val processedOptions = if (shuffleOpt) {
                    optionsWithIndices.shuffled(java.util.Random(seed))
                } else {
                    optionsWithIndices
                }
                
                val isAnswered = restoredAnswers != null
                val isCorrect = restoredAnswers != null && question.correctAnswers.toSet() == restoredAnswers
                
                _uiState.update {
                    it.copy(
                        currentIndex = nextIndex,
                        shuffledOptions = processedOptions.map { pair -> pair.second },
                        optionMapping = processedOptions.map { pair -> pair.first },
                        selectedOptions = restoredAnswers ?: emptySet(),
                        isAnswered = isAnswered,
                        isCorrect = isCorrect,
                        droppedOptions = restoredDropped ?: emptySet(),
                        showHint = false,
                        visibleOptionsCount = restoredVisibleCount ?: if (it.isOneByOne && !isAnswered) 0 else processedOptions.size
                    )
                }

                _timerState.update {
                    it.copy(questionTimeLeft = _uiState.value.questionTimerSeconds)
                }
                
                // Save progress to session if active
                currentState.sessionId?.let {
                    session?.let { currentSession ->
                    quizRepository.updateSession(
                        currentSession.copy(
                            currentQuestionIndex = nextIndex,
                            lastModifiedAt = System.currentTimeMillis(),
                        ),
                    )
                }
                }
                dataStoreManager.saveSession(currentState.quizId, nextIndex)
            }
        } else {
            // Check if there are any unanswered (null) results in the sequence
            val firstUnansweredIndex = (0 until currentState.questions.size).firstOrNull { 
                currentState.questionResultsByIndex[it] == null 
            }

            if (firstUnansweredIndex != null) {
                // Loop back to the first unfinished question
                jumpToQuestion(firstUnansweredIndex)
            } else {
                // Actually finished everything
                _uiState.update { it.copy(isCompleted = true) }
                viewModelScope.launch {
                    val state = _uiState.value
                    state.sessionId?.let { sessionId ->
                        val session = quizRepository.getSessionById(sessionId)
                        session?.let { currentSession ->
                            quizRepository.updateSession(
                                currentSession.copy(
                                    isCompleted = true,
                                    score = state.score,
                                    currentStreak = state.currentStreak,
                                    maxStreak = state.maxStreak,
                                    lastModifiedAt = System.currentTimeMillis(),
                                ),
                            )
                        }
                    }
                    dataStoreManager.clearSession()
                }
            }
        }
    }

    /**
     * Starts the session and question timers.
     */
    private fun startTimer() {
        val initialState = _uiState.value
        if (initialState.quizTimerSeconds <= 0 && initialState.questionTimerSeconds <= 0) {
            timerJob?.cancel()
            return
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (_uiState.value.isCompleted) break

                var shouldSubmit = false
                var quizFinished = false
                
                val currentQuizState = _uiState.value
                
                _timerState.update { state ->
                    var newQuizTimeLeft = state.quizTimeLeft
                    var newQuestionTimeLeft = state.questionTimeLeft

                    if (currentQuizState.quizTimerSeconds > 0 && state.quizTimeLeft > 0) {
                        newQuizTimeLeft--
                        if (newQuizTimeLeft == 0) {
                            quizFinished = true
                        }
                    }

                    if (!currentQuizState.isAnswered && currentQuizState.questionTimerSeconds > 0 && state.questionTimeLeft > 0) {
                        newQuestionTimeLeft--
                        if (newQuestionTimeLeft == 0) {
                            shouldSubmit = true
                        }
                    }

                    state.copy(
                        timeLeft = state.timeLeft + 1,
                        quizTimeLeft = newQuizTimeLeft,
                        questionTimeLeft = newQuestionTimeLeft
                    )
                }
                if (shouldSubmit) submitAnswer(isTimeout = true)
                if (quizFinished) finishSession()
            }
        }
    }

    /**
     * Updates the global note for the current question.
     */
    fun updateQuestionNote(note: String) {
        val state = _uiState.value
        if (state.questions.isEmpty()) return
        val index = state.currentIndex
        val currentQuestion = state.questions[index]
        
        val updatedQuestion = currentQuestion.copy(notes = note)
        
        // Update state immediately for UI responsiveness
        _uiState.update { s ->
            val newQuestions = s.questions.toMutableList()
            if (index in newQuestions.indices) {
                newQuestions[index] = updatedQuestion
            }
            s.copy(questions = newQuestions)
        }

        viewModelScope.launch {
            assetRepository.updateQuestion(updatedQuestion)
        }
    }

    fun assetsForQuestion(questionId: Long) = assetRepository.getQuestionAssets(questionId)

    fun sourceDocumentsForCurrentQuiz(): Flow<List<com.ahmedyejam.mks.data.local.entity.SourceDocumentEntity>> = flow {
        val quiz = quizRepository.getQuizById(_uiState.value.quizId)
        if (quiz == null) emit(emptyList()) else emitAll(assetRepository.getSourceDocumentsByBookId(quiz.bookId))
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        autoAdvanceJob?.cancel()
        focusManager.disableFocusMode()
    }

    fun getAskAiPromptDeckId(onResult: (Long) -> Unit) {
        viewModelScope.launch {
            val quizId = uiState.value.quizId
            if (quizId == 0L) return@launch
            val quiz = quizRepository.getQuizById(quizId)
            val bookId = quiz?.bookId ?: return@launch
            val expectedDeckTitle = "Explanations of - ${quiz.title}"
            val decks = knowledgeRepository.getPromptDecksByBookId(bookId).firstOrNull() ?: emptyList()
            val existingDeck = decks.find { it.title.equals(expectedDeckTitle, ignoreCase = true) }
            
            if (existingDeck != null) {
                onResult(existingDeck.id)
            } else {
                val newDeckId = knowledgeRepository.createExplainPromptDeck(
                    bookId = bookId,
                    title = expectedDeckTitle,
                    description = "AI Agent configured to explain concepts and answer questions for ${quiz.title}.",
                    seedDefaultCards = true
                )
                onResult(newDeckId)
            }
        }
    }
}
