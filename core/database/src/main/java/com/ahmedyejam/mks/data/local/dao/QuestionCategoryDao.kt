package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ahmedyejam.mks.data.local.entity.QuestionCategoryEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<QuestionCategoryEntity>)

    @Query("DELETE FROM question_categories WHERE questionId = :questionId")
    suspend fun deleteCategoriesForQuestion(questionId: Long)

    @Query("DELETE FROM question_categories WHERE category = :category")
    suspend fun deleteCategory(category: String)

    @Query("DELETE FROM question_categories")
    suspend fun clearAllCategories()

    @Query("SELECT DISTINCT category FROM question_categories ORDER BY category")
    fun getAllQuestionCategories(): Flow<List<String>>

    @Query(
        """
        SELECT q.* FROM questions q
        WHERE q.deletedAt IS NULL AND (q.quizId IN (SELECT id FROM quizzes WHERE category = :category AND deletedAt IS NULL)
        OR q.id IN (SELECT questionId FROM question_categories WHERE category = :category))
        ORDER BY q.id
        """
    )
    fun getQuestionsByCategoryFlow(category: String): Flow<List<QuestionEntity>>

    @Query(
        """
        SELECT q.* FROM questions q
        WHERE q.deletedAt IS NULL AND (q.quizId IN (SELECT id FROM quizzes WHERE category = :category AND deletedAt IS NULL)
        OR q.id IN (SELECT questionId FROM question_categories WHERE category = :category))
        ORDER BY q.id
        """
    )
    suspend fun getQuestionsByCategory(category: String): List<QuestionEntity>

    @Query(
        """
        SELECT q.* FROM questions q
        WHERE q.deletedAt IS NULL AND (q.quizId IN (SELECT id FROM quizzes WHERE category = :category AND deletedAt IS NULL)
        OR q.id IN (SELECT questionId FROM question_categories WHERE category = :category))
        ORDER BY
            q.attempts ASC,
            (CAST(q.correctCount AS FLOAT) / CASE WHEN q.attempts = 0 THEN 0.1 ELSE q.attempts END) ASC,
            q.lastStudiedAt ASC
        LIMIT :limit
        """
    )
    suspend fun getAdaptiveQuestionsByCategory(category: String, limit: Int): List<QuestionEntity>

    @Transaction
    suspend fun replaceCategories(questionId: Long, categories: List<String>) {
        deleteCategoriesForQuestion(questionId)
        val cleaned = categories
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .map { QuestionCategoryEntity(questionId = questionId, category = it) }
        if (cleaned.isNotEmpty()) {
            insertCategories(cleaned)
        }
    }
}
