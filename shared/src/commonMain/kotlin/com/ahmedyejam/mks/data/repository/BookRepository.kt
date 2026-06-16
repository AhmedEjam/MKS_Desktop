package com.ahmedyejam.mks.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.db.MksDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BookRepository(private val db: MksDatabase) : KoinComponent {
    private val quizRepo: QuizRepository by inject()
    private val assetRepo: AssetRepository by inject()

    fun observeAllBooks(): Flow<List<BookEntity>> =
        db.bookQueriesQueries.bk_selectAll()
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toBookEntity() } }

    fun observeBooksByWorkspace(workspaceId: Long): Flow<List<BookEntity>> =
        db.bookQueriesQueries.bk_selectByWorkspace(workspaceId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toBookEntity() } }

    suspend fun getBookById(id: Long): BookEntity? = withContext(Dispatchers.IO) {
        db.bookQueriesQueries.bk_selectById(id).executeAsOneOrNull()?.toBookEntity()
    }

    suspend fun createBook(book: BookEntity): MksResult<Long> = withContext(Dispatchers.IO) {
        try {
            val now = currentTime()
            db.bookQueriesQueries.bk_insert(
                workspaceId = book.workspaceId,
                externalId = book.externalId.ifBlank { "book_${now}" },
                title = book.title,
                description = book.description,
                iconName = book.iconName,
                coverImage = book.coverImage,
                createdAt = now,
                updatedAt = now,
                contentUpdatedAt = now,
                lastStudiedAt = 0L,
                lastEditedAt = now,
                isPinned = if (book.isPinned) 1L else 0L,
                isSystem = if (book.isSystem) 1L else 0L,
                fields = "[]"
            )
            val externalId = book.externalId.ifBlank { "book_${now}" }
            val id = db.bookQueriesQueries.bk_selectByExternalId(externalId).executeAsOne().id
            MksResult.Success(id)
        } catch (e: Exception) {
            MksResult.Error("Failed to create book", e)
        }
    }

    suspend fun deleteBook(id: Long): MksResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            db.bookQueriesQueries.bk_softDelete(currentTime(), id)
            MksResult.Success(true)
        } catch (e: Exception) {
            MksResult.Error("Failed to delete book", e)
        }
    }

    fun observeQuizzesByBook(bookId: Long): Flow<List<QuizEntity>> =
        db.quizQueriesQueries.qz_selectByBook(bookId)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toQuizEntity() } }

    suspend fun getQuestionCount(bookId: Long): Int = withContext(Dispatchers.IO) {
        db.bookQueriesQueries.bk_countByWorkspace(bookId).executeAsOne().toInt()
    }

    private fun currentTime(): Long = com.ahmedyejam.mks.util.currentTimeMillis()
}

// Mappers: SQLDelight rows → domain entities

private fun com.ahmedyejam.mks.db.Books.toBookEntity() = BookEntity(
    id = id, workspaceId = workspaceId, externalId = externalId,
    title = title, description = description, iconName = iconName, coverImage = coverImage,
    createdAt = createdAt, updatedAt = updatedAt, contentUpdatedAt = contentUpdatedAt,
    lastStudiedAt = lastStudiedAt, lastEditedAt = lastEditedAt,
    isPinned = isPinned != 0L, isSystem = isSystem != 0L,
    fields = emptyList(),
    questionCount = questionCount.toInt(), answeredCount = answeredCount.toInt(),
    totalAttempts = totalAttempts.toInt(),
    completionPercentage = completionPercentage.toFloat(),
    accuracyPercentage = accuracyPercentage.toFloat(),
    deletedAt = deletedAt
)

private fun com.ahmedyejam.mks.db.Quizzes.toQuizEntity() = QuizEntity(
    id = id, externalId = externalId, bookId = bookId,
    title = title, description = description, category = category,
    tags = emptyList(), iconName = iconName, coverImage = coverImage,
    createdAt = createdAt, updatedAt = updatedAt, contentUpdatedAt = contentUpdatedAt,
    lastStudiedAt = lastStudiedAt, lastEditedAt = lastEditedAt,
    isPinned = isPinned != 0L, isSystem = isSystem != 0L,
    questionCount = questionCount.toInt(), answeredCount = answeredCount.toInt(),
    totalAttempts = totalAttempts.toInt(),
    completionPercentage = completionPercentage.toFloat(),
    accuracyPercentage = accuracyPercentage.toFloat(),
    deletedAt = deletedAt
)
