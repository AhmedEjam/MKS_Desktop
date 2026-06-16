package com.ahmedyejam.mks.data.local.dao

import androidx.room.*
import com.ahmedyejam.mks.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE deletedAt IS NULL")
    fun getAllBooksFlow(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE workspaceId = :workspaceId AND deletedAt IS NULL")
    fun getBooksByWorkspaceFlow(workspaceId: Long): Flow<List<BookEntity>>

    @Query("SELECT COUNT(*) FROM books WHERE deletedAt IS NULL")
    suspend fun countAll(): Int

    @Query("""
        SELECT * FROM books 
        WHERE deletedAt IS NULL
        ORDER BY 
        CASE WHEN :sortBy = 'TITLE' THEN title END ASC,
        CASE WHEN :sortBy = 'QUESTION_COUNT' THEN questionCount END DESC,
        CASE WHEN :sortBy = 'COMPLETION' OR :sortBy = 'PROGRESS' THEN completionPercentage END DESC,
        CASE WHEN :sortBy = 'ACCURACY' THEN accuracyPercentage END DESC,
        CASE WHEN :sortBy = 'LAST_STUDIED' THEN lastStudiedAt END DESC,
        CASE WHEN :sortBy = 'LAST_EDIT' THEN lastEditedAt END DESC
    """)
    fun getAllBooksSortedFlow(sortBy: String): Flow<List<BookEntity>>

    @Query("""
        SELECT * FROM books
        WHERE workspaceId = :workspaceId AND deletedAt IS NULL
        ORDER BY
        CASE WHEN :sortBy = 'TITLE' THEN title END ASC,
        CASE WHEN :sortBy = 'QUESTION_COUNT' THEN questionCount END DESC,
        CASE WHEN :sortBy = 'COMPLETION' OR :sortBy = 'PROGRESS' THEN completionPercentage END DESC,
        CASE WHEN :sortBy = 'ACCURACY' THEN accuracyPercentage END DESC,
        CASE WHEN :sortBy = 'LAST_STUDIED' THEN lastStudiedAt END DESC,
        CASE WHEN :sortBy = 'LAST_EDIT' THEN lastEditedAt END DESC
    """)
    fun getBooksByWorkspaceSortedFlow(workspaceId: Long, sortBy: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id AND deletedAt IS NULL")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookByIdIncludingDeleted(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE externalId = :externalId AND deletedAt IS NULL LIMIT 1")
    suspend fun getBookByExternalId(externalId: String): BookEntity?

    @Query("SELECT * FROM books WHERE externalId = :externalId AND workspaceId = :workspaceId AND deletedAt IS NULL LIMIT 1")
    suspend fun getBookByExternalIdInWorkspace(externalId: String, workspaceId: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET questionCount = :count WHERE id = :bookId")
    suspend fun updateQuestionCount(bookId: Long, count: Int)

    @Query("UPDATE books SET completionPercentage = :percentage WHERE id = :bookId")
    suspend fun updateCompletionPercentage(bookId: Long, percentage: Float)

    @Query("UPDATE books SET answeredCount = :count WHERE id = :bookId")
    suspend fun updateAnsweredCount(bookId: Long, count: Int)

    @Query("UPDATE books SET totalAttempts = :count WHERE id = :bookId")
    suspend fun updateTotalAttempts(bookId: Long, count: Int)

    @Query("UPDATE books SET accuracyPercentage = :percentage WHERE id = :bookId")
    suspend fun updateAccuracyPercentage(bookId: Long, percentage: Float)

    @Query("UPDATE books SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :bookId")
    suspend fun softDeleteBookById(bookId: Long, deletedAt: Long)

    @Query("UPDATE books SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :bookId")
    suspend fun restoreBookById(bookId: Long, updatedAt: Long)

    @Query("UPDATE books SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE workspaceId = :workspaceId AND deletedAt IS NULL")
    suspend fun softDeleteBooksByWorkspaceId(workspaceId: Long, deletedAt: Long)

    @Query("UPDATE books SET deletedAt = NULL, updatedAt = :updatedAt WHERE workspaceId = :workspaceId AND deletedAt = :deletedAtFilter")
    suspend fun restoreBooksByWorkspaceId(workspaceId: Long, updatedAt: Long, deletedAtFilter: Long)

    @Query("SELECT * FROM books WHERE workspaceId = :workspaceId AND deletedAt IS NOT NULL")
    fun getDeletedBooksByWorkspaceFlow(workspaceId: Long): Flow<List<BookEntity>>

    @Delete
    suspend fun hardDeleteBook(book: BookEntity)
}
