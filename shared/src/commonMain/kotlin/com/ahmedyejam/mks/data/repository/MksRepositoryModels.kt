package com.ahmedyejam.mks.data.repository

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
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

@Serializable
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
