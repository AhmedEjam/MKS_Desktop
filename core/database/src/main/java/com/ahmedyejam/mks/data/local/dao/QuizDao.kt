package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getQuizzesByBookId(bookId: Long): Flow<List<QuizEntity>>

    @Query("""
        SELECT * FROM quizzes 
        WHERE bookId = :bookId AND deletedAt IS NULL
        ORDER BY 
        CASE WHEN :sortBy = 'TITLE' THEN title END ASC,
        CASE WHEN :sortBy = 'QUESTION_COUNT' THEN questionCount END DESC,
        CASE WHEN :sortBy = 'COMPLETION' OR :sortBy = 'PROGRESS' THEN completionPercentage END DESC,
        CASE WHEN :sortBy = 'ACCURACY' THEN accuracyPercentage END DESC,
        CASE WHEN :sortBy = 'LAST_STUDIED' THEN lastStudiedAt END DESC,
        CASE WHEN :sortBy = 'LAST_EDIT' THEN lastEditedAt END DESC
    """)
    fun getQuizzesByBookIdSorted(bookId: Long, sortBy: String): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE category = :category AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getQuizzesByCategory(category: String): Flow<List<QuizEntity>>

    @Query("""
        SELECT * FROM quizzes 
        WHERE category = :category AND deletedAt IS NULL
        ORDER BY 
        CASE WHEN :sortBy = 'TITLE' THEN title END ASC,
        CASE WHEN :sortBy = 'QUESTION_COUNT' THEN questionCount END DESC,
        CASE WHEN :sortBy = 'COMPLETION' OR :sortBy = 'PROGRESS' THEN completionPercentage END DESC,
        CASE WHEN :sortBy = 'ACCURACY' THEN accuracyPercentage END DESC,
        CASE WHEN :sortBy = 'LAST_STUDIED' THEN lastStudiedAt END DESC,
        CASE WHEN :sortBy = 'LAST_EDIT' THEN lastEditedAt END DESC
    """)
    fun getQuizzesByCategorySorted(category: String, sortBy: String): Flow<List<QuizEntity>>

    @Query("SELECT * FROM quizzes WHERE id = :id AND deletedAt IS NULL")
    suspend fun getQuizById(id: Long): QuizEntity?

    @Query("SELECT * FROM quizzes WHERE id = :id LIMIT 1")
    suspend fun getQuizByIdIncludingDeleted(id: Long): QuizEntity?

    @Query("SELECT * FROM quizzes WHERE externalId = :externalId AND deletedAt IS NULL LIMIT 1")
    suspend fun getQuizByExternalId(externalId: String): QuizEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity): Long

    @Update
    suspend fun updateQuiz(quiz: QuizEntity)

    @Query("UPDATE quizzes SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :quizId")
    suspend fun softDeleteQuizById(quizId: Long, deletedAt: Long)

    @Query("UPDATE quizzes SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :quizId")
    suspend fun restoreQuizById(quizId: Long, updatedAt: Long)

    @Delete
    suspend fun hardDeleteQuiz(quiz: QuizEntity)

    @Query("UPDATE quizzes SET questionCount = (SELECT COUNT(*) FROM questions WHERE quizId = :quizId AND deletedAt IS NULL) WHERE id = :quizId")
    suspend fun refreshQuestionCount(quizId: Long)

    @Query("UPDATE quizzes SET completionPercentage = :percentage WHERE id = :quizId")
    suspend fun updateCompletionPercentage(quizId: Long, percentage: Float)

    @Query("UPDATE quizzes SET answeredCount = :count WHERE id = :quizId")
    suspend fun updateAnsweredCount(quizId: Long, count: Int)

    @Query("UPDATE quizzes SET totalAttempts = :count WHERE id = :quizId")
    suspend fun updateTotalAttempts(quizId: Long, count: Int)

    @Query("UPDATE quizzes SET accuracyPercentage = :percentage WHERE id = :quizId")
    suspend fun updateAccuracyPercentage(quizId: Long, percentage: Float)

    @Query("SELECT * FROM quizzes WHERE deletedAt IS NULL")
    fun getAllQuizzesFlow(): Flow<List<QuizEntity>>

    @Query("SELECT COUNT(*) FROM quizzes WHERE deletedAt IS NULL")
    suspend fun countAll(): Int

    @Query("SELECT DISTINCT category FROM quizzes WHERE category IS NOT NULL AND deletedAt IS NULL")
    fun getAllCategories(): Flow<List<String>>

    @Query("UPDATE quizzes SET category = NULL, updatedAt = :updatedAt, lastEditedAt = :updatedAt WHERE category IS NOT NULL AND category != ''")
    suspend fun clearAllQuizCategories(updatedAt: Long)

    @Query("SELECT COUNT(*) FROM questions WHERE quizId = :quizId AND deletedAt IS NULL")
    fun getQuestionCount(quizId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM questions WHERE deletedAt IS NULL AND quizId IN (SELECT id FROM quizzes WHERE bookId = :bookId AND deletedAt IS NULL)")
    fun getBookQuestionCount(bookId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM questions WHERE quizId IN (SELECT id FROM quizzes WHERE bookId = :bookId AND deletedAt IS NULL) AND deletedAt IS NULL")
    suspend fun getBookQuestionCountNow(bookId: Long): Int

    @Query("SELECT * FROM quizzes WHERE bookId = :bookId AND deletedAt IS NULL")
    suspend fun getQuizzesByBookIdNow(bookId: Long): List<QuizEntity>

    @Query("SELECT * FROM quizzes WHERE deletedAt IS NOT NULL AND bookId IN (SELECT id FROM books WHERE workspaceId = :workspaceId)")
    fun getDeletedQuizzesByWorkspaceFlow(workspaceId: Long): Flow<List<QuizEntity>>
}
