package com.ahmedyejam.mks.ui.slideshow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.local.entity.CourseSlideEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.local.entity.SlideshowCourseEntity
import com.ahmedyejam.mks.data.model.SlideGenerationConfig
import com.ahmedyejam.mks.data.repository.*
import com.ahmedyejam.mks.util.currentTimeMillis
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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
        get() = slides.getOrNull(currentIndex)
}

class SlideshowCourseViewModel(
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

    private var sessionTimeAccumulatedMs: Long = 0L
    private var sessionLastStartedTimestamp: Long = 0L
    private var isSessionTimerRunning: Boolean = false
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()
    private var timerJob: Job? = null

    private val reviewedSlideIds = mutableSetOf<Long>()

    fun loadCourse(id: Long) {
        if (courseId == id && loadJob?.isActive == true) return
        courseId = id
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val course = knowledgeRepository.getSlideshowCourseById(id)
            if (course == null) {
                _uiState.update { it.copy(isLoading = false, error = "Course not found") }
                return@launch
            }

            combine(
                knowledgeRepository.getSlidesByCourseId(id),
                knowledgeRepository.observeSlideshowCoursesByBook(course.bookId),
                quizRepository.getAllCategoriesWithMetadata()
            ) { slides, allCourses, cats ->
                val current = _uiState.value
                _uiState.update { it.copy(
                    course = course,
                    slides = slides,
                    availableCourses = allCourses.filter { it.id != id },
                    categories = cats.map { it.name },
                    currentIndex = current.currentIndex.coerceIn(0, (slides.size - 1).coerceAtLeast(0)),
                    isLoading = false
                )}
            }.collect()
        }
    }

    fun startSessionTimer() {
        if (!isSessionTimerRunning) {
            sessionLastStartedTimestamp = currentTimeMillis()
            isSessionTimerRunning = true
            timerJob = viewModelScope.launch {
                while (isSessionTimerRunning) {
                    _elapsedSeconds.value = (sessionTimeAccumulatedMs + (currentTimeMillis() - sessionLastStartedTimestamp)) / 1000L
                    delay(1000)
                }
            }
        }
    }

    fun pauseSessionTimer() {
        if (isSessionTimerRunning) {
            sessionTimeAccumulatedMs += (currentTimeMillis() - sessionLastStartedTimestamp)
            isSessionTimerRunning = false
            timerJob?.cancel()
        }
    }

    fun nextSlide() {
        _uiState.update { it.copy(currentIndex = (it.currentIndex + 1).coerceAtMost(it.slides.lastIndex.coerceAtLeast(0))) }
        _uiState.value.currentSlide?.let { reviewedSlideIds.add(it.id) }
    }

    fun previousSlide() {
        _uiState.update { it.copy(currentIndex = (it.currentIndex - 1).coerceAtLeast(0)) }
        _uiState.value.currentSlide?.let { reviewedSlideIds.add(it.id) }
    }

    fun toggleSlideStudied() {
        val slide = _uiState.value.currentSlide ?: return
        viewModelScope.launch {
            knowledgeRepository.updateCourseSlide(slide.copy(isCompleted = !slide.isCompleted))
        }
    }

    fun deleteSlide(slide: CourseSlideEntity) {
        viewModelScope.launch {
            knowledgeRepository.deleteCourseSlide(slide)
        }
    }
}
