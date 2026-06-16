package com.ahmedyejam.mks.ui.quiz

import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.model.CategoryWithMetadata

/**
 * Represents the UI state for the Quiz screen.
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
