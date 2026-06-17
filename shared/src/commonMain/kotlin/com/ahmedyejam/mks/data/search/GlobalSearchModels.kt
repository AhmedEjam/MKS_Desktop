package com.ahmedyejam.mks.data.search

import kotlinx.serialization.Serializable

@Serializable
enum class GlobalSearchResultType {
    BOOK, QUIZ, QUESTION, ANSWER, EXPLANATION, HINT, NOTE, ASSET, SOURCE,
    FLASHCARD, BLUEPRINT, SLIDE, PROMPT_DECK, PROMPT_CARD, PROMPT_RUN,
    MISTAKE, ANNOTATION, CATEGORY, TAG
}

@Serializable
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

@Serializable
data class GlobalSearchResult(
    val id: String,
    val type: GlobalSearchResultType,
    val title: String,
    val subtitle: String? = null,
    val snippet: String? = null,
    val bookId: Long? = null,
    val quizId: Long? = null,
    val questionId: Long? = null,
    val parentId: Long? = null,
    val route: String? = null,
    val updatedAt: Long? = null,
)

fun GlobalSearchResultRow.toResult(): GlobalSearchResult {
    val resolvedType = try { GlobalSearchResultType.valueOf(type) } catch (e: Exception) { GlobalSearchResultType.QUESTION }
    return GlobalSearchResult(id, resolvedType, title, subtitle, snippet, bookId, quizId, questionId, parentId, null, updatedAt)
}
