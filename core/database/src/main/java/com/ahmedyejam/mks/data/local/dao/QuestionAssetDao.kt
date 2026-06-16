package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmedyejam.mks.data.local.entity.QuestionAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionAssetDao {
    @Query("SELECT * FROM question_assets WHERE questionId = :questionId AND deletedAt IS NULL ORDER BY isPinned DESC, sortOrder ASC, createdAt ASC")
    fun getAssetsByQuestionId(questionId: Long): Flow<List<QuestionAssetEntity>>

    @Query("SELECT * FROM question_assets WHERE questionId = :questionId AND deletedAt IS NULL ORDER BY isPinned DESC, sortOrder ASC, createdAt ASC")
    suspend fun getAssetsByQuestionIdNow(questionId: Long): List<QuestionAssetEntity>

    @Query("SELECT * FROM question_assets WHERE questionId = :questionId ORDER BY isPinned DESC, sortOrder ASC, createdAt ASC")
    suspend fun getAssetsByQuestionIdIncludingDeleted(questionId: Long): List<QuestionAssetEntity>

    @Query("SELECT * FROM question_assets WHERE quizId = :quizId AND deletedAt IS NULL ORDER BY questionId ASC, isPinned DESC, sortOrder ASC, createdAt ASC")
    fun getAssetsByQuizId(quizId: Long): Flow<List<QuestionAssetEntity>>

    @Query("SELECT * FROM question_assets WHERE quizId = :quizId AND deletedAt IS NULL ORDER BY questionId ASC, isPinned DESC, sortOrder ASC, createdAt ASC")
    suspend fun getAssetsByQuizIdNow(quizId: Long): List<QuestionAssetEntity>

    @Query("SELECT * FROM question_assets WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAssetsByBookId(bookId: Long): Flow<List<QuestionAssetEntity>>

    @Query("SELECT * FROM question_assets WHERE id = :id AND deletedAt IS NULL")
    suspend fun getAssetById(id: Long): QuestionAssetEntity?

    @Query("SELECT * FROM question_assets ORDER BY updatedAt DESC, id ASC")
    suspend fun getAllAssetsIncludingDeleted(): List<QuestionAssetEntity>

    @Query("SELECT * FROM question_assets WHERE bookId = :bookId ORDER BY updatedAt DESC, id ASC")
    suspend fun getAssetsByBookIdIncludingDeleted(bookId: Long): List<QuestionAssetEntity>

    @Query("SELECT * FROM question_assets WHERE quizId = :quizId ORDER BY questionId ASC, isPinned DESC, sortOrder ASC, createdAt ASC")
    suspend fun getAssetsByQuizIdIncludingDeleted(quizId: Long): List<QuestionAssetEntity>

    @Query("SELECT DISTINCT questionId FROM question_assets WHERE deletedAt IS NULL")
    fun getQuestionIdsWithAssetsFlow(): Flow<List<Long>>

    @Query("SELECT DISTINCT questionId FROM question_assets WHERE quizId = :quizId AND deletedAt IS NULL")
    fun getQuestionIdsWithAssetsForQuizFlow(quizId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(DISTINCT questionId) FROM question_assets WHERE deletedAt IS NULL")
    suspend fun countDistinctQuestionsWithAssets(): Int

    @Query("SELECT COUNT(DISTINCT questionId) FROM question_assets WHERE deletedAt IS NULL AND sourceDocumentId IS NOT NULL")
    suspend fun countDistinctQuestionsWithSources(): Int

    @Query("SELECT COUNT(*) FROM question_assets WHERE questionId = :questionId AND deletedAt IS NULL")
    fun getAssetCountForQuestion(questionId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: QuestionAssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<QuestionAssetEntity>): List<Long>

    @Update
    suspend fun updateAsset(asset: QuestionAssetEntity)

    @Query("UPDATE question_assets SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :assetId")
    suspend fun softDeleteAssetById(assetId: Long, deletedAt: Long)

    @Query("UPDATE question_assets SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE questionId = :questionId AND deletedAt IS NULL")
    suspend fun softDeleteAssetsForQuestion(questionId: Long, deletedAt: Long)

    @Query("UPDATE question_assets SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :assetId")
    suspend fun restoreAssetById(assetId: Long, updatedAt: Long)

    @Delete
    suspend fun hardDeleteAsset(asset: QuestionAssetEntity)

    @Query("DELETE FROM question_assets WHERE questionId = :questionId")
    suspend fun hardDeleteAssetsForQuestion(questionId: Long)

    @Query("UPDATE question_assets SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :assetId")
    suspend fun updateAssetOrder(assetId: Long, sortOrder: Int, updatedAt: Long)

    @Query("UPDATE question_assets SET isPrimary = 0, updatedAt = :updatedAt WHERE questionId = :questionId AND id != :assetId")
    suspend fun clearOtherPrimaryAssets(questionId: Long, assetId: Long, updatedAt: Long)

    @Query("UPDATE question_assets SET sourceDocumentId = NULL, updatedAt = :updatedAt WHERE sourceDocumentId = :sourceId")
    suspend fun clearSourceReference(sourceId: Long, updatedAt: Long)
}
