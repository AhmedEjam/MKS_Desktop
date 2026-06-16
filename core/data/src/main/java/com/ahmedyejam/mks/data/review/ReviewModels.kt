package com.ahmedyejam.mks.data.review

enum class ReviewQueueType { FLASHCARD, BLUEPRINT, MISTAKE, MARKED_QUESTION, WEAK_QUESTION, UNFINISHED_SLIDE, ANNOTATION }

data class ReviewDashboardSummary(
    val dueFlashcards: Int = 0,
    val dueBlueprints: Int = 0,
    val dueMistakes: Int = 0,
    val pendingMistakes: Int = 0,
    val markedQuestions: Int = 0,
    val weakQuestions: Int = 0,
    val unfinishedSlides: Int = 0,
)

data class ReviewQueueItem(
    val id: String,
    val type: ReviewQueueType,
    val title: String,
    val subtitle: String? = null,
    val dueAt: Long? = null,
    val route: String? = null,
)
