package com.ahmedyejam.mks.data.repository

import com.ahmedyejam.mks.data.local.entity.*

enum class SortOption { LAST_EDIT, LAST_STUDIED, TITLE, COMPLETION, QUESTION_COUNT, ACCURACY, PROGRESS }

data class KnowledgeSummary(
    val totalBooks: Int = 0,
    val totalQuizzes: Int = 0,
    val totalQuestions: Int = 0,
    val unansweredQuestions: Int = 0,
    val questionsWithNotes: Int = 0,
    val questionsWithAssets: Int = 0,
    val questionsWithSources: Int = 0,
    val markedQuestions: Int = 0,
    val droppedQuestions: Int = 0,
    val missedQuestions: Int = 0,
    val weakQuestions: Int = 0,
    val flashcardDecks: Int = 0,
    val totalFlashcards: Int = 0,
    val dueFlashcards: Int = 0,
    val weakFlashcards: Int = 0,
    val totalBlueprints: Int = 0,
    val blueprintsDueForReview: Int = 0,
    val linkedBlueprints: Int = 0,
    val promptDecks: Int = 0,
    val promptCards: Int = 0,
    val promptRuns: Int = 0,
    val savedPromptOutputs: Int = 0,
    val openMistakes: Int = 0,
    val fixedMistakes: Int = 0,
    val mistakesDueForReview: Int = 0,
    val pendingMistakesForReview: Int = 0,
    val reviewSchedulesDue: Int = 0,
)

data class BookKnowledgeSummary(
    val bookId: Long,
    val totalQuizzes: Int = 0,
    val totalQuestions: Int = 0,
    val unansweredQuestions: Int = 0,
    val questionsWithNotes: Int = 0,
    val questionsWithAssets: Int = 0,
    val questionsWithSources: Int = 0,
    val markedQuestions: Int = 0,
    val droppedQuestions: Int = 0,
    val missedQuestions: Int = 0,
    val weakQuestions: Int = 0,
    val flashcardDecks: Int = 0,
    val totalFlashcards: Int = 0,
    val totalBlueprints: Int = 0,
    val promptDecks: Int = 0,
    val promptCards: Int = 0,
    val promptRuns: Int = 0,
    val savedPromptOutputs: Int = 0,
    val openMistakes: Int = 0,
    val reviewSchedulesDue: Int = 0,
)

data class QuizKnowledgeSummary(
    val quizId: Long,
    val totalQuestions: Int = 0,
    val unansweredQuestions: Int = 0,
    val markedQuestions: Int = 0,
    val droppedQuestions: Int = 0,
    val missedQuestions: Int = 0,
    val questionsWithNotes: Int = 0,
    val questionsWithAssets: Int = 0,
    val questionsWithSources: Int = 0,
)

data class BookStudyBundle(
    val book: BookEntity,
    val quizzes: List<QuizEntity>,
    val questionsByQuiz: Map<Long, List<QuestionEntity>>,
    val flashcardDecks: List<FlashcardDeckEntity>,
    val slideshowCourses: List<SlideshowCourseEntity> = emptyList(),
    val noteBlueprints: List<NoteBlueprintEntity> = emptyList(),
    val prompts: List<PromptEntity> = emptyList(),
    val promptDecks: List<PromptDeckEntity> = emptyList(),
    val sourceDocuments: List<SourceDocumentEntity> = emptyList(),
    val mistakes: List<MistakeLogEntryEntity> = emptyList(),
    val questionAssets: List<QuestionAssetEntity> = emptyList(),
) {
    val questions: List<QuestionEntity>
        get() = quizzes.flatMap { quiz -> questionsByQuiz[quiz.id].orEmpty() }
}
