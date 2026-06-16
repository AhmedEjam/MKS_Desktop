package com.ahmedyejam.mks.ui.slideshow


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.importer.parser.TextParseMode
import com.ahmedyejam.mks.data.importer.parser.TextSlideParser
import com.ahmedyejam.mks.data.local.entity.CourseSlideEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.local.entity.SlideshowCourseEntity
import com.ahmedyejam.mks.data.model.SlideGenerationConfig
import com.ahmedyejam.mks.data.repository.AssetRepository
import com.ahmedyejam.mks.data.repository.BookRepository
import com.ahmedyejam.mks.data.repository.KnowledgeRepository
import com.ahmedyejam.mks.data.repository.QuizRepository
import com.ahmedyejam.mks.data.repository.SortOption
import com.ahmedyejam.mks.data.repository.StudyRepository
import com.ahmedyejam.mks.di.ApplicationScope
import com.ahmedyejam.mks.util.MksLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SlideshowCourseUiState(
    val course: SlideshowCourseEntity? = null,
    val slides: List<CourseSlideEntity> = emptyList(),
    val availableCourses: List<SlideshowCourseEntity> = emptyList(),
    val quizzes: List<QuizEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val isPresentationMode: Boolean = false,
    val selectedSlideIds: Set<Long> = emptySet(),
    val message: String? = null,
    val error: String? = null
) {
    val currentSlide: CourseSlideEntity?
        get() = slides.getOrNull(currentIndex.coerceIn(0, (slides.size - 1).coerceAtLeast(0)))
}

@HiltViewModel
class SlideshowCourseViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    @ApplicationScope private val applicationScope: kotlinx.coroutines.CoroutineScope,
    private val bookRepository: BookRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val assetRepository: AssetRepository,
    private val quizRepository: QuizRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SlideshowCourseUiState())
    val uiState = _uiState.asStateFlow()
    private var courseId: Long? = null
    private var loadJob: Job? = null

    private val moshi = com.squareup.moshi.Moshi.Builder().build()
    private val sessionStateAdapter = moshi.adapter(com.ahmedyejam.mks.data.model.LearningSessionState::class.java)

    private var activeSessionId: Long? = null
    private var sessionTimeAccumulatedMs: Long = 0L
    private var sessionLastStartedTimestamp: Long = 0L
    private var isSessionTimerRunning: Boolean = false
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()
    private var timerJob: Job? = null

    private val reviewedSlideIds = mutableSetOf<Long>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadCourse(id: Long) {
        if (courseId == id && loadJob?.isActive == true) return
        courseId = id
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            combine(
                flowOf(knowledgeRepository.getSlideshowCourseById(id)), // getSlideshowCourseById is suspend in MksRepository. Wait, I should wrap it or adjust. Let me fix the loading block.
                knowledgeRepository.getSlidesByCourseId(id)
            ) { course, slides -> Pair(course, slides) }
            .flatMapLatest { pair ->
                val course = pair.first
                val slides = pair.second
                val coursesFlow = course?.bookId?.let { knowledgeRepository.getSlideshowCoursesByBookId(it) } ?: flowOf(emptyList())
                val quizzesFlow = course?.bookId?.let { quizRepository.getQuizzesByBookId(it, SortOption.TITLE) } ?: flowOf(emptyList())
                val categoriesFlow = quizRepository.getAllCategoriesWithMetadata()

                combine(coursesFlow, quizzesFlow, categoriesFlow) { allCourses, quizzes, cats ->
                    Triple(course, slides, Triple(allCourses, quizzes, cats))
                }
            }
            .collect { result ->
                val course = result.first
                val slides = result.second
                val extras = result.third
                val allCourses = extras.first
                val quizzes = extras.second
                val cats = extras.third
                val current = _uiState.value
                _uiState.value = current.copy(
                    course = course,
                    slides = slides,
                    availableCourses = allCourses.filter { it.id != id },
                    quizzes = quizzes,
                    categories = cats.map { it.name },
                    currentIndex = current.currentIndex.coerceIn(0, (slides.size - 1).coerceAtLeast(0)),
                    isLoading = false
                )
            }
        }
    }

    // A helper method since getSlideshowCourseById is a suspend function
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun fetchCourseAndObserve(id: Long) {
        val course = knowledgeRepository.getSlideshowCourseById(id)
        knowledgeRepository.getSlidesByCourseId(id).flatMapLatest { slides ->
            val coursesFlow = course?.bookId?.let { knowledgeRepository.getSlideshowCoursesByBookId(it) } ?: flowOf(emptyList())
            val quizzesFlow = course?.bookId?.let { quizRepository.getQuizzesByBookId(it, SortOption.TITLE) } ?: flowOf(emptyList())
            val categoriesFlow = quizRepository.getAllCategoriesWithMetadata()

            combine(coursesFlow, quizzesFlow, categoriesFlow) { allCourses, quizzes, cats ->
                Triple(course, slides, Triple(allCourses, quizzes, cats))
            }
        }.collect { result ->
            val loadedCourse = result.first
            val slides = result.second
            val extras = result.third
            val allCourses = extras.first
            val quizzes = extras.second
            val cats = extras.third
            val current = _uiState.value
            _uiState.value = current.copy(
                course = loadedCourse,
                slides = slides,
                availableCourses = allCourses.filter { it.id != id },
                quizzes = quizzes,
                categories = cats.map { it.name },
                currentIndex = current.currentIndex.coerceIn(0, (slides.size - 1).coerceAtLeast(0)),
                isLoading = false
            )
        }
    }

    fun loadCourseSafe(id: Long) {
        if (courseId == id && loadJob?.isActive == true) return
        courseId = id
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            fetchCourseAndObserve(id)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    fun startSessionTimer() {
        if (!isSessionTimerRunning) {
            sessionLastStartedTimestamp = System.currentTimeMillis()
            isSessionTimerRunning = true
            MksLogger.d("SlideshowCourseViewModel", "Stopwatch started/resumed")
            timerJob = viewModelScope.launch {
                while (isSessionTimerRunning) {
                    _elapsedSeconds.value = getSessionElapsedTimeMs() / 1000L
                    delay(1000)
                }
            }
        }
    }

    fun pauseSessionTimer() {
        if (isSessionTimerRunning) {
            val elapsed = System.currentTimeMillis() - sessionLastStartedTimestamp
            sessionTimeAccumulatedMs += elapsed
            isSessionTimerRunning = false
            MksLogger.d("SlideshowCourseViewModel", "Stopwatch paused. Accumulated: $sessionTimeAccumulatedMs ms")
            saveSessionStateIncremental()
            timerJob?.cancel()
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
        val courseId = courseId ?: return
        val current = _uiState.value
        val elapsed = getSessionElapsedTimeMs()
        
        val stateObj = com.ahmedyejam.mks.data.model.LearningSessionState(
            targetType = "COURSE",
            targetId = courseId,
            courseId = courseId,
            reviewedCardIds = reviewedSlideIds.toSet(),
            currentCardIndex = current.currentIndex,
            timersActive = mapOf(0L to elapsed),
            startedAt = System.currentTimeMillis() - elapsed,
            totalAttempts = current.slides.size
        )
        
        val json = try {
            sessionStateAdapter.toJson(stateObj)
        } catch (e: Exception) {
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
        
        val courseId = courseId ?: return
        val current = _uiState.value
        val elapsed = getSessionElapsedTimeMs()
        
        val stateObj = com.ahmedyejam.mks.data.model.LearningSessionState(
            targetType = "COURSE",
            targetId = courseId,
            courseId = courseId,
            reviewedCardIds = reviewedSlideIds.toSet(),
            currentCardIndex = current.currentIndex,
            timersActive = mapOf(0L to elapsed),
            startedAt = System.currentTimeMillis() - elapsed,
            completedAt = System.currentTimeMillis(),
            totalAttempts = current.slides.size
        )
        
        val json = try {
            sessionStateAdapter.toJson(stateObj)
        } catch (e: Exception) {
            ""
        }

        applicationScope.launch {
            studyRepository.getLearningSessionById(sessionId)?.let { session ->
                studyRepository.updateLearningSession(session.copy(stateJson = json, isCompleted = true))
                studyRepository.completeLearningSession(sessionId)
            }
        }
    }

    fun setPresentationMode(enabled: Boolean) {
        if (enabled) {
            _uiState.value = _uiState.value.copy(isPresentationMode = true, currentIndex = 0)
            reviewedSlideIds.clear()
            val currentSlide = _uiState.value.currentSlide
            if (currentSlide != null) {
                reviewedSlideIds.add(currentSlide.id)
            }
            sessionTimeAccumulatedMs = 0L
            isSessionTimerRunning = false
            
            viewModelScope.launch {
                val courseId = courseId ?: return@launch
                val sessionId = studyRepository.createLearningSession("COURSE", courseId, "")
                activeSessionId = sessionId
                startSessionTimer()
            }
        } else {
            endAndSaveSession()
            _uiState.value = _uiState.value.copy(isPresentationMode = false, currentIndex = 0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        endAndSaveSession()
    }

    fun nextSlide() {
        val current = _uiState.value
        val next = if (current.slides.isEmpty()) 0 else (current.currentIndex + 1).coerceAtMost(current.slides.lastIndex)
        _uiState.value = current.copy(currentIndex = next)
        val nextSlide = _uiState.value.currentSlide
        if (nextSlide != null) {
            reviewedSlideIds.add(nextSlide.id)
        }
        saveSessionStateIncremental()
    }

    fun toggleSlideStudied() {
        val current = _uiState.value
        val slide = current.currentSlide ?: return
        viewModelScope.launch {
            knowledgeRepository.updateCourseSlide(slide.copy(isCompleted = !slide.isCompleted))
        }
    }

    fun setCurrentIndex(index: Int) {
        val current = _uiState.value
        if (index in current.slides.indices && index != current.currentIndex) {
            _uiState.value = current.copy(currentIndex = index)
            val slide = _uiState.value.currentSlide
            if (slide != null) {
                reviewedSlideIds.add(slide.id)
            }
            saveSessionStateIncremental()
        }
    }

    fun previousSlide() {
        val current = _uiState.value
        val previous = (current.currentIndex - 1).coerceAtLeast(0)
        _uiState.value = current.copy(currentIndex = previous)
        val prevSlide = _uiState.value.currentSlide
        if (prevSlide != null) {
            reviewedSlideIds.add(prevSlide.id)
        }
        saveSessionStateIncremental()
    }

    fun updateCourse(title: String, description: String?, coverImage: String?) {
        val course = _uiState.value.course ?: return
        viewModelScope.launch {
            knowledgeRepository.updateSlideshowCourse(course.copy(title = title, description = description, coverImage = coverImage))
            _uiState.value = _uiState.value.copy(message = "Course updated")
            // refresh course object since it's not fully observed
            val refreshed = knowledgeRepository.getSlideshowCourseById(course.id)
            _uiState.value = _uiState.value.copy(course = refreshed)
        }
    }

    fun addSlide(title: String, body: String, notes: String?, imagePath: String?) {
        val course = _uiState.value.course ?: return
        viewModelScope.launch {
            val order = _uiState.value.slides.size
            val now = System.currentTimeMillis()
            knowledgeRepository.insertCourseSlide(
                CourseSlideEntity(
                    externalId = java.util.UUID.randomUUID().toString(),
                    courseId = course.id,
                    title = title,
                    body = body,
                    speakerNotes = notes,
                    imagePath = imagePath,
                    orderIndex = order,
                    createdAt = now,
                    updatedAt = now
                )
            )
            _uiState.value = _uiState.value.copy(message = "Slide added")
        }
    }

    fun updateSlide(slide: CourseSlideEntity, title: String, body: String, notes: String?, imagePath: String?) {
        viewModelScope.launch {
            knowledgeRepository.updateCourseSlide(
                slide.copy(
                    title = title,
                    body = body,
                    speakerNotes = notes,
                    imagePath = imagePath
                )
            )
            _uiState.value = _uiState.value.copy(message = "Slide updated")
        }
    }

    fun deleteSlide(slide: CourseSlideEntity) {
        viewModelScope.launch {
            knowledgeRepository.deleteCourseSlide(slide)
            _uiState.value = _uiState.value.copy(message = "Slide deleted")
        }
    }

    fun moveSlide(slide: CourseSlideEntity, direction: Int) {
        val slides = _uiState.value.slides.toMutableList()
        val index = slides.indexOfFirst { it.id == slide.id }
        val target = index + direction
        if (index !in slides.indices || target !in slides.indices) return
        val moved = slides.removeAt(index)
        slides.add(target, moved)
        viewModelScope.launch { knowledgeRepository.reorderCourseSlides(slides) }
    }

    fun toggleSlideSelection(id: Long) {
        val current = _uiState.value.selectedSlideIds
        val next = if (current.contains(id)) current - id else current + id
        _uiState.value = _uiState.value.copy(selectedSlideIds = next)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedSlideIds = emptySet())
    }

    fun selectAllSlides() {
        val allIds = _uiState.value.slides.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedSlideIds = allIds)
    }

    fun deleteSelectedSlides() {
        val ids = _uiState.value.selectedSlideIds
        if (ids.isEmpty()) return
        val slidesToDelete = _uiState.value.slides.filter { it.id in ids }
        viewModelScope.launch {
            slidesToDelete.forEach { knowledgeRepository.deleteCourseSlide(it) }
            _uiState.value = _uiState.value.copy(selectedSlideIds = emptySet(), message = "${ids.size} slides deleted")
        }
    }

    fun moveSelectedSlides(targetCourseId: Long) {
        val ids = _uiState.value.selectedSlideIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            knowledgeRepository.moveCourseSlides(ids.toList(), targetCourseId)
            _uiState.value = _uiState.value.copy(selectedSlideIds = emptySet(), message = "${ids.size} slides moved")
        }
    }

    fun copySelectedSlides(targetCourseId: Long) {
        val ids = _uiState.value.selectedSlideIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            knowledgeRepository.copyCourseSlides(ids.toList(), targetCourseId)
            _uiState.value = _uiState.value.copy(selectedSlideIds = emptySet(), message = "${ids.size} slides copied")
        }
    }

    fun importFromText(text: String, mode: TextParseMode) {
        val course = _uiState.value.course ?: return
        viewModelScope.launch {
            runCatching {
                val parser = TextSlideParser()
                val orderStart = _uiState.value.slides.size
                val slides = parser.parse(text, course.id, orderStart, mode)
                if (slides.isNotEmpty()) {
                    knowledgeRepository.insertCourseSlides(slides)
                }
                slides.size
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "$count slide(s) imported")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Failed to import text")
            }
        }
    }

    fun importFromPptx(uri: android.net.Uri) {
        val course = _uiState.value.course ?: return
        val context = appContext
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            runCatching {
                val parser = com.ahmedyejam.mks.data.importer.parser.PptxSlideParser()
                val orderStart = _uiState.value.slides.size
                val slides = parser.parse(context, uri, course.id, orderStart)
                if (slides.isNotEmpty()) {
                    knowledgeRepository.insertCourseSlides(slides)
                }
                slides.size
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "$count slide(s) imported from PPTX"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Failed to import PPTX"
                )
            }
        }
    }

    fun generateFromMarked(config: SlideGenerationConfig, clearMarksAfter: Boolean) {
        val course = _uiState.value.course ?: return
        viewModelScope.launch {
            runCatching {
                val markedQuestions = bookRepository.getBookStudyBundle(course.bookId)?.questions.orEmpty().filter { it.isMarked }
                val count = knowledgeRepository.addCourseSlidesFromQuestionsToCourse(course.id, markedQuestions.map { it.id }, config)
                if (clearMarksAfter) {
                    markedQuestions.forEach { question ->
                        assetRepository.updateQuestion(question.copy(isMarked = false, updatedAt = System.currentTimeMillis()))
                    }
                }
                count
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "$count marked-question slide(s) added")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Failed to generate slides")
            }
        }
    }

    fun generateFromMissed(config: SlideGenerationConfig) {
        val course = _uiState.value.course ?: return
        viewModelScope.launch {
            runCatching {
                val missedIds = bookRepository.getBookStudyBundle(course.bookId)?.questions.orEmpty()
                    .filter { it.attempts > 0 && (it.correctCount < it.attempts || it.lastAttemptResult == false) }
                    .map { it.id }
                knowledgeRepository.addCourseSlidesFromQuestionsToCourse(course.id, missedIds, config)
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "$count missed-question slide(s) added")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Failed to generate slides")
            }
        }
    }

    fun generateFromBook(config: SlideGenerationConfig, clearMarksAfter: Boolean) {
        val course = _uiState.value.course ?: return
        viewModelScope.launch {
            runCatching {
                val questions = bookRepository.getBookStudyBundle(course.bookId)?.questions.orEmpty()
                val count = knowledgeRepository.addCourseSlidesFromQuestionsToCourse(course.id, questions.map { it.id }, config)
                if (clearMarksAfter) {
                    questions.filter { it.isMarked }.forEach { question ->
                        assetRepository.updateQuestion(question.copy(isMarked = false, updatedAt = System.currentTimeMillis()))
                    }
                }
                count
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "$count slide(s) added from book")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Failed to generate slides")
            }
        }
    }

    fun generateFromQuiz(quizId: Long, config: SlideGenerationConfig, clearMarksAfter: Boolean) {
        val course = _uiState.value.course ?: return
        viewModelScope.launch {
            runCatching {
                val bundle = bookRepository.getBookStudyBundle(course.bookId)
                val questions = bundle?.questionsByQuiz?.get(quizId).orEmpty()
                val count = knowledgeRepository.addCourseSlidesFromQuestionsToCourse(course.id, questions.map { it.id }, config)
                if (clearMarksAfter) {
                    questions.filter { it.isMarked }.forEach { question ->
                        assetRepository.updateQuestion(question.copy(isMarked = false, updatedAt = System.currentTimeMillis()))
                    }
                }
                count
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "$count slide(s) added from quiz")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Failed to generate slides")
            }
        }
    }

    fun generateFromCategory(categoryName: String, config: SlideGenerationConfig, clearMarksAfter: Boolean) {
        val course = _uiState.value.course ?: return
        viewModelScope.launch {
            runCatching {
                val questions = bookRepository.getBookStudyBundle(course.bookId)?.questions.orEmpty()
                    .filter { it.categories.contains(categoryName) }
                val count = knowledgeRepository.addCourseSlidesFromQuestionsToCourse(course.id, questions.map { it.id }, config)
                if (clearMarksAfter) {
                    questions.filter { it.isMarked }.forEach { question ->
                        assetRepository.updateQuestion(question.copy(isMarked = false, updatedAt = System.currentTimeMillis()))
                    }
                }
                count
            }.onSuccess { count ->
                _uiState.value = _uiState.value.copy(message = "$count slide(s) added from category '$categoryName'")
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message ?: "Failed to generate slides")
            }
        }
    }
}
