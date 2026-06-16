package com.ahmedyejam.mks.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.focus.FocusManager
import com.ahmedyejam.mks.data.local.entity.CategoryMetadataEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.ahmedyejam.mks.data.local.entity.SessionEntity
import com.ahmedyejam.mks.data.model.CategoryWithMetadata
import com.ahmedyejam.mks.data.preferences.DataStoreManager
import com.ahmedyejam.mks.data.repository.AssetRepository
import com.ahmedyejam.mks.data.repository.KnowledgeRepository
import com.ahmedyejam.mks.data.repository.QuizRepository
import com.ahmedyejam.mks.data.repository.StudyRepository
import com.ahmedyejam.mks.data.validation.SessionStateValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class QuizViewModel(
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

    fun getQuestionStatus(index: Int): QuestionStatus {
        val state = _uiState.value
        if (index == state.currentIndex) return QuestionStatus.CURRENT
        return when (state.questionResultsByIndex[index]) {
            true -> QuestionStatus.CORRECT
            false -> QuestionStatus.INCORRECT
            else -> QuestionStatus.UNANSWERED
        }
    }

    fun startQuiz(quizId: Long, sessionId: Long? = null) {
        val currentState = _uiState.value
        if ((currentState.quizId == quizId) && (currentState.sessionId == sessionId)) {
            if (currentState.questions.isNotEmpty() || currentState.isLoading) return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, quizId = quizId, sessionId = sessionId, error = null) }
                
                val shuffleQ = dataStoreManager.defShuffleQuestions.first()
                val shuffleOpt = dataStoreManager.defShuffleOptions.first()
                val rapidMode = dataStoreManager.defRapidMode.first()
                val repeatWrong = dataStoreManager.defRepeatWrong.first()
                val quizTimer = dataStoreManager.defQuizTimer.first()
                val qTimer = dataStoreManager.defQuestionTimer.first()
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
                    } else session.currentQuestionIndex
                    
                    val currentQId = session.questionIds.getOrNull(startIndex)
                    if ((currentQId != null) && (!sessionQuestionsMap.containsKey(currentQId))) {
                        _uiState.update { it.copy(isLoading = false, error = "Current question data is missing.") }
                        return@launch
                    }

                    sessionLabel = session.label
                    sessionScore = session.score
                    sessionCurrentStreak = session.currentStreak
                    sessionMaxStreak = session.maxStreak
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
                    val allRawQuestions = quizRepository.observeQuestionsByQuiz(quizId).first()
                        .filter { !it.isDropped }
                    if (shuffleQ) allRawQuestions.shuffled() else allRawQuestions
                }

                if (quizQuestions.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "No active questions found.") }
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
                            val elapsedMillis = com.ahmedyejam.mks.util.currentTimeMillis() - session.lastModifiedAt
                            val elapsedSeconds = (elapsedMillis / 1000).toInt()
                            (session.quizTimerSeconds - elapsedSeconds).coerceAtLeast(0)
                        } else quizTimer,
                        questionTimeLeft = qTimer,
                        timeLeft = 0 
                    )
                }
                
                if (session != null && session.questionIds.isEmpty()) {
                    quizRepository.updateSession(session.copy(
                        questionIds = quizQuestions.map { it.id },
                        originalQuestionCount = finalOriginalCount
                    ))
                }

                val restoredAnswers = session?.answersByIndex?.get(_uiState.value.currentIndex)?.toSet()
                val restoredDropped = session?.droppedOptionsByIndex?.get(_uiState.value.currentIndex)?.toSet()
                val restoredVisibleCount = session?.visibleOptionsCountByIndex?.get(_uiState.value.currentIndex)
                
                prepareQuestion(restoredAnswers, restoredDropped, restoredVisibleCount)
                startTimer()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load quiz: ${e.message}") }
            }
        }
    }

    private fun prepareQuestion(
        restoredSelection: Set<Int>? = null,
        restoredDropped: Set<Int>? = null,
        restoredVisibleCount: Int? = null
    ) {
        val state = _uiState.value
        if (state.questions.isEmpty() || state.currentIndex >= state.questions.size) return
        
        val question = state.questions[state.currentIndex]
        val optionsWithIndices = question.options.mapIndexed { index, s -> index to s }
        
        val seed = (state.sessionId ?: state.quizId) + question.id + state.currentIndex
        val processedOptions = if (state.shuffleOptions) optionsWithIndices.shuffled(java.util.Random(seed)) else optionsWithIndices
        
        val isAnswered = restoredSelection != null
        val isCorrect = restoredSelection != null && question.correctAnswers.toSet() == restoredSelection
        
        _uiState.update {
            it.copy(
                shuffledOptions = processedOptions.map { it.second },
                optionMapping = processedOptions.map { it.first },
                selectedOptions = restoredSelection ?: emptySet(),
                isAnswered = isAnswered,
                isCorrect = isCorrect,
                droppedOptions = restoredDropped ?: emptySet(),
                showHint = false,
                visibleOptionsCount = restoredVisibleCount ?: if (it.isOneByOne && !isAnswered) 0 else processedOptions.size
            )
        }
        _timerState.update { it.copy(questionTimeLeft = _uiState.value.questionTimerSeconds) }
    }

    fun toggleRapidMode() {
        val newMode = !_uiState.value.isRapidMode
        _uiState.update { it.copy(isRapidMode = newMode) }
    }

    fun toggleOneByOne() {
        val newIsOneByOne = !_uiState.value.isOneByOne
        _uiState.update { 
            it.copy(
                isOneByOne = newIsOneByOne,
                visibleOptionsCount = if (newIsOneByOne && !it.isAnswered) 0 else it.shuffledOptions.size
            )
        }
        viewModelScope.launch { dataStoreManager.setOneByOneMode(newIsOneByOne) }
    }

    fun toggleEliminationMode() {
        viewModelScope.launch { dataStoreManager.setEliminationModeEnabled(!_uiState.value.eliminationModeEnabled) }
    }

    fun toggleFocusMode() {
        val newEnabled = !_uiState.value.focusModeEnabled
        if (newEnabled && !focusManager.hasNotificationPolicyAccess()) {
            focusManager.requestNotificationPolicyAccess()
            return
        }
        viewModelScope.launch { dataStoreManager.setFocusModeEnabled(newEnabled) }
    }

    fun toggleMarked() {
        val state = _uiState.value
        if (state.questions.isEmpty()) return
        val currentQuestion = state.questions[state.currentIndex]
        val newMarked = !currentQuestion.isMarked
        
        viewModelScope.launch {
            val updatedQuestion = currentQuestion.copy(isMarked = newMarked, markedAt = com.ahmedyejam.mks.util.currentTimeMillis())
            quizRepository.updateQuestion(updatedQuestion)
            _uiState.update { s ->
                val newQuestions = s.questions.toMutableList()
                newQuestions[s.currentIndex] = updatedQuestion
                s.copy(questions = newQuestions)
            }
        }
    }

    fun finishSession() {
        _uiState.update { it.copy(isCompleted = true) }
        timerJob?.cancel()
        autoAdvanceJob?.cancel()
        viewModelScope.launch {
            val state = _uiState.value
            state.sessionId?.let { sessionId ->
                quizRepository.getSessionById(sessionId)?.let { session ->
                    quizRepository.updateSession(session.copy(
                        isCompleted = true,
                        score = state.score,
                        currentStreak = state.currentStreak,
                        maxStreak = state.maxStreak,
                        lastModifiedAt = com.ahmedyejam.mks.util.currentTimeMillis(),
                    ))
                }
            }
            dataStoreManager.clearSession()
        }
    }

    fun toggleCategorization() {
        val newEnabled = !_uiState.value.showCategorization
        _uiState.update { it.copy(showCategorization = newEnabled) }
        viewModelScope.launch { dataStoreManager.setShowCategorization(newEnabled) }
    }

    fun toggleQuestionCategory(category: String) {
        val currentState = _uiState.value
        val question = currentState.questions[currentState.currentIndex]
        val newCats = if (question.categories.contains(category)) question.categories - category else question.categories + category
        
        val updatedQuestion = question.copy(categories = newCats)
        _uiState.update { s ->
            val newQuestions = s.questions.toMutableList()
            newQuestions[s.currentIndex] = updatedQuestion
            s.copy(questions = newQuestions)
        }
        viewModelScope.launch { quizRepository.updateQuestion(updatedQuestion) }
    }

    fun setNavigationFilter(filter: NavigationFilter) {
        _uiState.update { it.copy(navigationFilter = filter) }
    }

    fun jumpToQuestion(index: Int) {
        val state = _uiState.value
        if (index in state.questions.indices) {
            questionStartTime = com.ahmedyejam.mks.util.currentTimeMillis()
            viewModelScope.launch {
                val session = state.sessionId?.let { quizRepository.getSessionById(it) }
                val restoredAnswers = session?.answersByIndex?.get(index)?.toSet()
                val restoredDropped = session?.droppedOptionsByIndex?.get(index)?.toSet()
                val restoredVisibleCount = session?.visibleOptionsCountByIndex?.get(index)

                _uiState.update { it.copy(currentIndex = index) }
                prepareQuestion(restoredAnswers, restoredDropped, restoredVisibleCount)
                
                state.sessionId?.let { sId ->
                    session?.let { currentSession ->
                        quizRepository.updateSession(currentSession.copy(
                            currentQuestionIndex = index,
                            lastModifiedAt = com.ahmedyejam.mks.util.currentTimeMillis(),
                        ))
                    }
                }
                dataStoreManager.saveSession(state.quizId, index)
            }
        }
    }

    fun previousQuestion() {
        val state = _uiState.value
        if (state.currentIndex > 0) jumpToQuestion(state.currentIndex - 1)
    }

    fun toggleHint() {
        _uiState.update { it.copy(showHint = !it.showHint) }
    }

    fun onOptionSelected(shuffledIndex: Int) {
        val currentState = _uiState.value
        val originalIndex = currentState.optionMapping[shuffledIndex]
        if (currentState.isAnswered || currentState.droppedOptions.contains(originalIndex)) return

        val currentQuestion = currentState.questions[currentState.currentIndex]
        
        if (currentState.isRapidMode && currentQuestion.type == QuestionType.SINGLE_CHOICE) {
            applyAnswer(setOf(originalIndex), currentQuestion, currentState)
        } else {
            _uiState.update { state ->
                val newSelection = if (currentQuestion.type == QuestionType.SINGLE_CHOICE) setOf(originalIndex)
                else if (state.selectedOptions.contains(originalIndex)) state.selectedOptions - originalIndex
                else state.selectedOptions + originalIndex
                state.copy(selectedOptions = newSelection)
            }
        }
    }

    fun submitAnswer(isTimeout: Boolean = false) {
        val currentState = _uiState.value
        if (!isTimeout && currentState.isOneByOne && !currentState.isAnswered && currentState.visibleOptionsCount < currentState.shuffledOptions.size) {
            val nextVisibleCount = currentState.visibleOptionsCount + 1
            _uiState.update { it.copy(visibleOptionsCount = nextVisibleCount) }
            return
        }

        if (currentState.isAnswered) {
            nextQuestion()
            return
        }
        
        if (currentState.selectedOptions.isEmpty() && !isTimeout) {
            if (currentState.skipUnansweredGlobal) nextQuestion()
            return
        }

        val currentIndex = currentState.currentIndex
        if (currentState.questionResultsByIndex.containsKey(currentIndex)) return

        val currentQuestion = currentState.questions[currentIndex]
        applyAnswer(currentState.selectedOptions, currentQuestion, currentState)
    }

    private fun applyAnswer(newSelection: Set<Int>, currentQuestion: QuestionEntity, currentState: QuizState) {
        val isCorrect = currentQuestion.correctAnswers.toSet() == newSelection
        val currentIndex = currentState.currentIndex
        val isFirstAttempt = currentIndex < currentState.initialQuestionCount
        val newStreak = if (isFirstAttempt) (if (isCorrect) currentState.currentStreak + 1 else 0) else currentState.currentStreak
        val newMaxStreak = maxOf(currentState.maxStreak, newStreak)
        val newScore = if (isCorrect && isFirstAttempt) currentState.score + 1 else currentState.score

        _uiState.update { state ->
            val updatedQuestions = if (!isCorrect && state.repeatWrong) state.questions + currentQuestion else state.questions
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

        finalizeSubmission(currentQuestion, isCorrect, currentIndex, newSelection, currentState.sessionId, newScore, newStreak, newMaxStreak, currentState.initialQuestionCount, currentState.repeatWrong, currentState.shuffledOptions.size, currentState.isRapidMode)
    }

    private var questionStartTime: Long = com.ahmedyejam.mks.util.currentTimeMillis()

    private fun finalizeSubmission(currentQuestion: QuestionEntity, isCorrect: Boolean, currentIndex: Int, selectedOptions: Set<Int>, sessionId: Long?, newScore: Int, newCurrentStreak: Int, newMaxStreak: Int, initialQuestionCount: Int, repeatWrong: Boolean, shuffledOptionsSize: Int, isRapidMode: Boolean) {
        val timeSpent = com.ahmedyejam.mks.util.currentTimeMillis() - questionStartTime
        val isFirstAttempt = currentIndex < initialQuestionCount

        viewModelScope.launch {
            if (isFirstAttempt) {
                studyRepository.updateQuestionMetrics(currentQuestion.id, isCorrect, timeSpent)
            }

            sessionId?.let { sId ->
                quizRepository.getSessionById(sId)?.let { currentSession ->
                    val newIncorrectCount = if (!isCorrect && isFirstAttempt) currentSession.incorrectCount + 1 else currentSession.incorrectCount
                    val updatedAnswersByIndex = currentSession.answersByIndex.toMutableMap()
                    updatedAnswersByIndex[currentIndex] = selectedOptions.toList()

                    val updatedVisible = currentSession.visibleOptionsCountByIndex.toMutableMap()
                    updatedVisible[currentIndex] = shuffledOptionsSize
                    
                    val updatedQuestionIds = if (!isCorrect && repeatWrong) currentSession.questionIds + currentQuestion.id else currentSession.questionIds

                    quizRepository.updateSession(currentSession.copy(
                        score = newScore,
                        currentStreak = newCurrentStreak,
                        maxStreak = newMaxStreak,
                        incorrectCount = newIncorrectCount,
                        answersByIndex = updatedAnswersByIndex,
                        visibleOptionsCountByIndex = updatedVisible,
                        questionIds = updatedQuestionIds,
                        lastModifiedAt = com.ahmedyejam.mks.util.currentTimeMillis()
                    ))
                }
            }
            
            if (isRapidMode) {
                autoAdvanceJob = launch { delay(autoAdvanceDelayMs); nextQuestion() }
            }
        }
    }

    fun dropOption(shuffledIndex: Int) {
        val currentState = _uiState.value
        if (currentState.isAnswered) return
        val originalIndex = currentState.optionMapping[shuffledIndex]
        val currentQuestion = currentState.questions[currentState.currentIndex]
        val isCorrect = currentQuestion.correctAnswers.contains(originalIndex)

        if (isCorrect) {
            applyAnswer(setOf(originalIndex), currentQuestion, currentState)
        } else {
            val nextDropped = currentState.droppedOptions + originalIndex
            _uiState.update { it.copy(droppedOptions = nextDropped, selectedOptions = it.selectedOptions - originalIndex) }
            
            val remainingOptions = currentState.optionMapping.filter { !nextDropped.contains(it) }
            if (remainingOptions.size == 1) {
                val lastIdx = currentState.optionMapping.indexOf(remainingOptions.first())
                if (lastIdx != -1) onOptionSelected(lastIdx)
            }
        }
    }

    fun nextQuestion() {
        autoAdvanceJob?.cancel()
        val currentState = _uiState.value
        val nextIndex = currentState.currentIndex + 1

        if (nextIndex < currentState.questions.size) {
            jumpToQuestion(nextIndex)
        } else {
            val firstUnansweredIndex = (0 until currentState.questions.size).firstOrNull { currentState.questionResultsByIndex[it] == null }
            if (firstUnansweredIndex != null) jumpToQuestion(firstUnansweredIndex)
            else finishSession()
        }
    }

    private fun startTimer() {
        if (_uiState.value.quizTimerSeconds <= 0 && _uiState.value.questionTimerSeconds <= 0) {
            timerJob?.cancel(); return
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (_uiState.value.isCompleted) break
                
                var shouldSubmit = false
                var quizFinished = false
                
                _timerState.update { state ->
                    var nq = state.quizTimeLeft
                    var nqt = state.questionTimeLeft
                    if (_uiState.value.quizTimerSeconds > 0 && nq > 0) { nq--; if (nq == 0) quizFinished = true }
                    if (!_uiState.value.isAnswered && _uiState.value.questionTimerSeconds > 0 && nqt > 0) { nqt--; if (nqt == 0) shouldSubmit = true }
                    state.copy(timeLeft = state.timeLeft + 1, quizTimeLeft = nq, questionTimeLeft = nqt)
                }
                if (shouldSubmit) submitAnswer(isTimeout = true)
                if (quizFinished) finishSession()
            }
        }
    }

    fun updateQuestionNote(note: String) {
        val state = _uiState.value
        if (state.questions.isEmpty()) return
        val currentQuestion = state.questions[state.currentIndex]
        val updatedQuestion = currentQuestion.copy(notes = note)
        _uiState.update { s ->
            val nq = s.questions.toMutableList()
            nq[s.currentIndex] = updatedQuestion
            s.copy(questions = nq)
        }
        viewModelScope.launch { quizRepository.updateQuestion(updatedQuestion) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        autoAdvanceJob?.cancel()
        focusManager.disableFocusMode()
    }
}
