package com.ahmedyejam.mks.data.local.dao

import androidx.room.*
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE quizId = :quizId AND deletedAt IS NULL")
    fun getQuestionsByQuizId(quizId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE quizId = :quizId AND deletedAt IS NULL")
    suspend fun getQuestionsByQuizIdNow(quizId: Long): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE quizId = :quizId")
    suspend fun getQuestionsByQuizIdIncludingDeleted(quizId: Long): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id AND deletedAt IS NULL")
    suspend fun getQuestionById(id: Long): QuestionEntity?

    @Query("SELECT * FROM questions WHERE quizId = :quizId AND externalId = :externalId AND deletedAt IS NULL")
    suspend fun getQuestionByExternalId(quizId: Long, externalId: String): QuestionEntity?

    @Query("SELECT * FROM questions WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun getQuestionsByIds(ids: List<Long>): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>): List<Long>

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Update
    suspend fun updateQuestions(questions: List<QuestionEntity>)

    @Query("UPDATE questions SET deletedAt = :deletedAt, updatedAt = :deletedAt, lastEditedAt = :deletedAt WHERE id = :questionId")
    suspend fun softDeleteQuestionById(questionId: Long, deletedAt: Long)

    @Query("UPDATE questions SET deletedAt = :deletedAt, updatedAt = :deletedAt, lastEditedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun softDeleteQuestionsByIds(ids: List<Long>, deletedAt: Long)

    @Query("UPDATE questions SET deletedAt = :deletedAt, updatedAt = :deletedAt, lastEditedAt = :deletedAt WHERE quizId = :quizId AND deletedAt IS NULL")
    suspend fun softDeleteQuestionsByQuizId(quizId: Long, deletedAt: Long)

    @Query("UPDATE questions SET deletedAt = NULL, updatedAt = :updatedAt, lastEditedAt = :updatedAt WHERE id = :questionId")
    suspend fun restoreQuestionById(questionId: Long, updatedAt: Long)

    @Query("UPDATE questions SET deletedAt = NULL, updatedAt = :updatedAt, lastEditedAt = :updatedAt WHERE quizId = :quizId AND deletedAt = :deletedAtFilter")
    suspend fun restoreQuestionsByQuizId(quizId: Long, updatedAt: Long, deletedAtFilter: Long)

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionByIdIncludingDeleted(id: Long): QuestionEntity?

    @Delete
    suspend fun hardDeleteQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id IN (:ids)")
    suspend fun hardDeleteQuestionsByIds(ids: List<Long>)
    
    @Query("DELETE FROM questions WHERE quizId = :quizId")
    suspend fun hardDeleteQuestionsByQuizId(quizId: Long)

    @Query("""
        UPDATE questions 
        SET attempts = attempts + 1, 
            correctCount = correctCount + :isCorrectInt,
            lastStudiedAt = :now,
            timeSpentMs = timeSpentMs + :timeSpentMs,
            lastAttemptResult = :isCorrect,
            consecutiveCorrect = CASE WHEN :isCorrectInt = 1 THEN consecutiveCorrect + 1 ELSE 0 END,
            updatedAt = :now
        WHERE id = :id
    """)
    suspend fun updatePerformanceMetrics(
        id: Long, 
        isCorrect: Boolean, 
        isCorrectInt: Int,
        timeSpentMs: Long, 
        now: Long
    )

    @Query("""
        SELECT * FROM questions 
        WHERE deletedAt IS NULL AND (:bookId = -1 OR quizId IN (SELECT id FROM quizzes WHERE bookId = :bookId AND deletedAt IS NULL))
        ORDER BY 
            attempts ASC, 
            (CAST(correctCount AS FLOAT) / CASE WHEN attempts = 0 THEN 0.1 ELSE attempts END) ASC, 
            lastStudiedAt ASC 
        LIMIT :limit
    """)
    suspend fun getAdaptiveQuestionsByBook(bookId: Long, limit: Int): List<QuestionEntity>

    @Query("""
        SELECT * FROM questions
        WHERE deletedAt IS NULL AND (quizId IN (SELECT id FROM quizzes WHERE category = :category AND deletedAt IS NULL)
        OR id IN (SELECT questionId FROM question_categories WHERE category = :category))
        ORDER BY
            attempts ASC,
            (CAST(correctCount AS FLOAT) / CASE WHEN attempts = 0 THEN 0.1 ELSE attempts END) ASC,
            lastStudiedAt ASC
        LIMIT :limit
    """)
    suspend fun getAdaptiveQuestionsByCategory(category: String, limit: Int): List<QuestionEntity>

    @Query("""
        SELECT * FROM questions
        WHERE deletedAt IS NULL AND (quizId IN (SELECT id FROM quizzes WHERE category = :category AND deletedAt IS NULL)
        OR id IN (SELECT questionId FROM question_categories WHERE category = :category))
        ORDER BY id
    """)
    fun getQuestionsByCategoryFlow(category: String): Flow<List<QuestionEntity>>

    @Query("""
        SELECT * FROM questions
        WHERE deletedAt IS NULL AND (quizId IN (SELECT id FROM quizzes WHERE category = :category AND deletedAt IS NULL)
        OR id IN (SELECT questionId FROM question_categories WHERE category = :category))
        ORDER BY id
    """)
    suspend fun getQuestionsByCategory(category: String): List<QuestionEntity>

    @Query("SELECT DISTINCT category FROM quizzes WHERE category IS NOT NULL AND deletedAt IS NULL")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM questions WHERE deletedAt IS NULL")
    fun getAllQuestionsFlow(): Flow<List<QuestionEntity>>

    @Query("SELECT COUNT(*) FROM questions WHERE deletedAt IS NULL")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE deletedAt IS NULL AND attempts = 0")
    suspend fun countUnanswered(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE deletedAt IS NULL AND notes IS NOT NULL AND TRIM(notes) != ''")
    suspend fun countWithNotes(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE deletedAt IS NULL AND isMarked = 1")
    suspend fun countMarked(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE deletedAt IS NULL AND isDropped = 1")
    suspend fun countDropped(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE deletedAt IS NULL AND attempts > 0 AND (correctCount < attempts OR lastAttemptResult = 0)")
    suspend fun countMissed(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE deletedAt IS NULL AND attempts >= 2 AND correctCount * 2 < attempts")
    suspend fun countWeak(): Int


    @Query("UPDATE questions SET categories = '[]', updatedAt = :updatedAt, lastEditedAt = :updatedAt WHERE categories IS NOT NULL AND categories != '[]'")
    suspend fun clearAllQuestionCategories(updatedAt: Long)


    @Query("SELECT * FROM questions WHERE deletedAt IS NULL AND isMarked = 1 ORDER BY markedAt DESC, updatedAt DESC")
    suspend fun getMarkedQuestions(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE deletedAt IS NULL AND isMarked = 1 AND (markReviewAt IS NULL OR markReviewAt <= :now) ORDER BY COALESCE(markReviewAt, markedAt, updatedAt) ASC LIMIT :limit")
    suspend fun getMarkedQuestionsForReview(now: Long, limit: Int = 50): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE quizId = :quizId AND deletedAt IS NULL AND isMarked = 1 ORDER BY markedAt DESC, updatedAt DESC")
    suspend fun getMarkedQuestionsByQuiz(quizId: Long): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE deletedAt IS NULL AND quizId IN (SELECT id FROM quizzes WHERE bookId = :bookId AND deletedAt IS NULL) AND isMarked = 1 ORDER BY markedAt DESC, updatedAt DESC")
    suspend fun getMarkedQuestionsByBook(bookId: Long): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE deletedAt IS NULL AND isDropped = 1 ORDER BY droppedAt DESC, updatedAt DESC")
    suspend fun getDroppedQuestions(): List<QuestionEntity>

    @Query("UPDATE questions SET isMarked = 0, markedAt = NULL, markReason = NULL, markReviewAt = NULL, updatedAt = :updatedAt, lastEditedAt = :updatedAt WHERE quizId = :quizId AND isMarked = 1")
    suspend fun clearMarksForQuiz(quizId: Long, updatedAt: Long)

    @Query("UPDATE questions SET isMarked = 0, markedAt = NULL, markReason = NULL, markReviewAt = NULL, updatedAt = :updatedAt, lastEditedAt = :updatedAt WHERE deletedAt IS NULL AND quizId IN (SELECT id FROM quizzes WHERE bookId = :bookId AND deletedAt IS NULL) AND isMarked = 1")
    suspend fun clearMarksForBook(bookId: Long, updatedAt: Long)

    @Query("UPDATE questions SET isMarked = 0, markedAt = NULL, markReason = NULL, markReviewAt = NULL, updatedAt = :updatedAt, lastEditedAt = :updatedAt WHERE id = :questionId")
    suspend fun clearQuestionMark(questionId: Long, updatedAt: Long)

    @Query("UPDATE questions SET lastStudiedAt = :reviewedAt, updatedAt = :reviewedAt WHERE id = :questionId")
    suspend fun markQuestionReviewed(questionId: Long, reviewedAt: Long)

    @Query("UPDATE questions SET markReviewAt = :reviewAt, updatedAt = :updatedAt WHERE id = :questionId AND isMarked = 1")
    suspend fun snoozeMarkedQuestion(questionId: Long, reviewAt: Long, updatedAt: Long)

    @Query("SELECT * FROM questions WHERE deletedAt IS NULL AND attempts > 0 AND correctCount < attempts ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getWeakQuestions(limit: Int = 50): List<QuestionEntity>

    @Query("""
        SELECT * FROM questions
        WHERE deletedAt IS NULL AND attempts > 0
          AND correctCount < attempts
          AND lastStudiedAt <= :cutoff
        ORDER BY lastStudiedAt ASC, updatedAt DESC
        LIMIT :limit
    """)
    suspend fun getWeakQuestionsDue(cutoff: Long, limit: Int = 50): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE deletedAt IS NULL AND markReviewAt IS NOT NULL AND markReviewAt <= :now ORDER BY markReviewAt ASC LIMIT :limit")
    suspend fun getMarkedQuestionsDueForReview(now: Long, limit: Int = 50): List<QuestionEntity>

    @Query("""
        SELECT * FROM questions 
        WHERE deletedAt IS NULL AND (text LIKE '%' || :searchQuery || '%' 
        OR explanation LIKE '%' || :searchQuery || '%'
        OR options LIKE '%' || :searchQuery || '%')
    """)
    fun searchAllQuestionsFlow(searchQuery: String): Flow<List<QuestionEntity>>
}
