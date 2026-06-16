package com.ahmedyejam.mks.ui

import android.net.Uri

object MksRoutes {
    const val WELCOME = "welcome"
    const val LIBRARY = "library"
    const val GLOBAL_SEARCH = "global_search"
    const val REVIEW_DASHBOARD = "review_dashboard"
    const val DATA_TOOLS = "data_tools"
    const val SETTINGS = "settings"

    fun reviewDashboard(mistakeId: Long? = null): String {
        return if (mistakeId != null) "review_dashboard?mistakeId=$mistakeId" else "review_dashboard"
    }

    // Quiz & Sessions
    fun quizQuestions(quizId: Long, questionId: Long? = null): String {
        return if (questionId != null) "quiz_questions/$quizId?questionId=$questionId" else "quiz_questions/$quizId"
    }
    fun sessions(quizId: Long) = "sessions/$quizId"
    fun quiz(quizId: Long, sessionId: Long? = null): String {
        return if (sessionId != null) "quiz/$quizId?sessionId=$sessionId" else "quiz/$quizId"
    }
    fun summary(sessionId: Long) = "summary/$sessionId"
    fun scanner(quizId: Long) = "scanner/$quizId"

    // Categories
    fun category(category: String) = "category/${Uri.encode(category)}"
    fun adaptive(type: String, id: String) = "adaptive/${Uri.encode(type)}/${Uri.encode(id)}"

    // Knowledge Bank
    fun flashcards(deckId: Long, cardId: Long? = null): String {
        return if (cardId != null) "flashcards/$deckId?cardId=$cardId" else "flashcards/$deckId"
    }
    fun slideshow(courseId: Long, slideId: Long? = null): String {
        return if (slideId != null) "slideshow/$courseId?slideId=$slideId" else "slideshow/$courseId"
    }
    fun blueprint(noteId: Long) = "blueprint/$noteId"
    
    // Book Tools
    fun bookDashboard(bookId: Long) = "book_dashboard/$bookId"
    fun bookSlideshows(bookId: Long) = "book_slideshows/$bookId"
    fun bookBlueprints(bookId: Long) = "book_blueprints/$bookId"

    fun bookNotes(bookId: Long) = "book_notes/$bookId"
    fun bookPrompts(bookId: Long) = "book_prompts/$bookId"
    fun promptDeck(promptId: Long, cardId: Long? = null, runId: Long? = null): String {
        return when {
            cardId != null -> "prompt_deck/$promptId?cardId=$cardId"
            runId != null -> "prompt_deck/$promptId?runId=$runId"
            else -> "prompt_deck/$promptId"
        }
    }
}
