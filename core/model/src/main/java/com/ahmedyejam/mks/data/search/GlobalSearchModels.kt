package com.ahmedyejam.mks.data.search

enum class GlobalSearchResultType {
    BOOK, QUIZ, QUESTION, ANSWER, EXPLANATION, HINT, NOTE, ASSET, SOURCE,
    FLASHCARD, BLUEPRINT, SLIDE, PROMPT_DECK, PROMPT_CARD, PROMPT_RUN,
    MISTAKE, ANNOTATION, CATEGORY, TAG
}

data class GlobalSearchResultRow(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String? = null,
    val snippet: String? = null,
    val bookId: Long? = null,
    val quizId: Long? = null,
    val questionId: Long? = null,
    val parentId: Long? = null,
    val updatedAt: Long? = null
)
