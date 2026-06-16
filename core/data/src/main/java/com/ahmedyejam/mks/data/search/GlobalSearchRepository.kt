package com.ahmedyejam.mks.data.search

import com.ahmedyejam.mks.data.local.dao.GlobalSearchDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GlobalSearchRepository constructor(private val dao: GlobalSearchDao) {
    suspend fun search(query: String): List<GlobalSearchResult> =
        withContext(Dispatchers.IO) {
            val cleaned = query.trim()
            if (cleaned.length < 2) return@withContext emptyList()
            dao.search("%$cleaned%")
                .map { it.toResult() }
                .sortedWith(compareByDescending<GlobalSearchResult> { it.updatedAt ?: 0L }.thenBy { it.type.name })
        }
}
