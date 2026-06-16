package com.ahmedyejam.mks.data.search

import com.ahmedyejam.mks.ui.*

data class GlobalSearchResult(
    val id: String,
    val type: GlobalSearchResultType,
    val title: String,
    val subtitle: String? = null,
    val snippet: String? = null,
    val bookId: Long? = null,
    val quizId: Long? = null,
    val questionId: Long? = null,
    val route: String? = null,
    val updatedAt: Long? = null,
)

fun GlobalSearchResultRow.toResult(): GlobalSearchResult {
    val resolvedType = runCatching { GlobalSearchResultType.valueOf(type) }.getOrDefault(GlobalSearchResultType.QUESTION)
    val route =
        when (resolvedType) {
            GlobalSearchResultType.BOOK -> MksRoutes.LIBRARY
            GlobalSearchResultType.QUIZ -> quizId?.let { MksRoutes.sessions(it) }
            GlobalSearchResultType.QUESTION,
            GlobalSearchResultType.ANSWER,
            GlobalSearchResultType.EXPLANATION,
            GlobalSearchResultType.HINT,
            GlobalSearchResultType.NOTE,
            -> quizId?.let { MksRoutes.quizQuestions(it) }
            GlobalSearchResultType.ASSET -> quizId?.let { MksRoutes.quizQuestions(it) }
            GlobalSearchResultType.SOURCE -> bookId?.let { MksRoutes.bookDashboard(it) }
            GlobalSearchResultType.FLASHCARD -> parentId?.let { MksRoutes.flashcards(it, id.toLongOrNull()) }
            GlobalSearchResultType.BLUEPRINT -> id.toLongOrNull()?.let { MksRoutes.blueprint(it) }
            GlobalSearchResultType.SLIDE -> parentId?.let { MksRoutes.slideshow(it, id.toLongOrNull()) }
            GlobalSearchResultType.PROMPT_DECK -> id.toLongOrNull()?.let { MksRoutes.promptDeck(it) }
            GlobalSearchResultType.PROMPT_CARD -> parentId?.let { MksRoutes.promptDeck(it, cardId = id.toLongOrNull()) }
            GlobalSearchResultType.PROMPT_RUN -> parentId?.let { MksRoutes.promptDeck(it, runId = id.toLongOrNull()) }
            GlobalSearchResultType.MISTAKE -> id.toLongOrNull()?.let { MksRoutes.reviewDashboard(it) }
            GlobalSearchResultType.ANNOTATION -> bookId?.let { MksRoutes.bookDashboard(it) }
            GlobalSearchResultType.CATEGORY, GlobalSearchResultType.TAG -> title.takeIf { it.isNotBlank() }?.let { MksRoutes.category(it) }
        }
    return GlobalSearchResult(id, resolvedType, title, subtitle, snippet, bookId, quizId, questionId, route, updatedAt)
}
