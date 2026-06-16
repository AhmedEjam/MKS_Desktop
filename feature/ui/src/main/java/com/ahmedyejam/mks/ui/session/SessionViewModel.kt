package com.ahmedyejam.mks.ui.session


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.local.entity.SessionEntity
import com.ahmedyejam.mks.data.preferences.DataStoreManager
import com.ahmedyejam.mks.data.repository.QuizRepository
import com.ahmedyejam.mks.data.repository.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val studyRepository: StudyRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions = _sessions.asStateFlow()

    private val _quizQuestionCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val quizQuestionCounts = _quizQuestionCounts.asStateFlow()

    fun loadSessions(quizId: Long) {
        viewModelScope.launch {
            studyRepository.getSessionsByQuizId(quizId).collect {
                _sessions.value = it
            }
        }
        viewModelScope.launch {
            val count = quizRepository.getQuestionsByQuizId(quizId).first().count { !it.isDropped }
            _quizQuestionCounts.value += (quizId to count)
        }
    }

    fun createSession(
        quizId: Long,
        label: String,
        shuffleQuestions: Boolean = true,
        shuffleOptions: Boolean = true,
        rapidMode: Boolean = false,
        repeatWrong: Boolean = true,
        quizTimerSeconds: Int = 0,
        questionTimerSeconds: Int = 0,
        rangeFrom: Int = 0,
        rangeTo: Int = -1,
        includeFilters: List<String> = emptyList(),
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val session = SessionEntity(
                quizId = quizId,
                label = label,
                shuffleQuestions = shuffleQuestions,
                shuffleOptions = shuffleOptions,
                rapidMode = rapidMode,
                repeatWrong = repeatWrong,
                quizTimerSeconds = quizTimerSeconds,
                questionTimerSeconds = questionTimerSeconds,
                rangeFrom = rangeFrom,
                rangeTo = rangeTo,
                includeFilters = includeFilters
            )
            val id = quizRepository.insertSession(session)
            onCreated(id)
        }
    }

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            quizRepository.deleteSession(session)
        }
    }

    /**
     * Creates a custom session containing only questions from specified categories.
     */
    fun createCustomSessionFromCategories(
        quizId: Long,
        categories: List<String>,
        label: String,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            // Fetch all questions for this quiz and filter by categories
            val questions = quizRepository.getQuestionsByQuizId(quizId).first()
                .filter { q -> q.categories.any { it in categories } }
            
            val questionIds = questions.map { it.id }
            
            val session = SessionEntity(
                quizId = quizId,
                label = label,
                questionIds = questionIds,
                originalQuestionCount = questionIds.size,
                // Ensure default filters are set or cleared as this is a custom explicit list
                includeFilters = emptyList() 
            )
            val id = quizRepository.insertSession(session)
            onCreated(id)
        }
    }

    fun saveDefaultSessionSettings(
        filters: Set<String>,
        shuffleQ: Boolean,
        shuffleO: Boolean,
        rapid: Boolean,
        repeatWrong: Boolean,
        quizTimer: Int,
        qTimer: Int
    ) {
        viewModelScope.launch {
            dataStoreManager.saveDefaultSessionSettings(
                filters = filters,
                shuffleQ = shuffleQ,
                shuffleO = shuffleO,
                rapid = rapid,
                repeatWrong = repeatWrong,
                quizTimer = quizTimer,
                qTimer = qTimer
            )
        }
    }
}
