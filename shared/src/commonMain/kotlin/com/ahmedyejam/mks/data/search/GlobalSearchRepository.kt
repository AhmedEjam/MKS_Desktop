package com.ahmedyejam.mks.data.search

import com.ahmedyejam.mks.db.MksDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GlobalSearchRepository(private val db: MksDatabase) {
    suspend fun search(query: String, limit: Long = 120): List<GlobalSearchResult> =
        withContext(Dispatchers.IO) {
            val cleaned = query.trim()
            if (cleaned.length < 2) return@withContext emptyList()
            
            db.globalSearchQueriesQueries.search("%$cleaned%", limit)
                .executeAsList()
                .map { row ->
                    GlobalSearchResult(
                        id = row.id ?: "",
                        type = try { GlobalSearchResultType.valueOf(row.type) } catch (e: Exception) { GlobalSearchResultType.QUESTION },
                        title = row.title ?: "",
                        subtitle = row.subtitle,
                        snippet = row.snippet,
                        bookId = row.bookId,
                        quizId = row.quizId,
                        questionId = row.questionId,
                        parentId = row.parentId,
                        updatedAt = row.updatedAt
                    )
                }
                .sortedWith(compareByDescending<GlobalSearchResult> { it.updatedAt ?: 0L }.thenBy { it.type.name })
        }
}
