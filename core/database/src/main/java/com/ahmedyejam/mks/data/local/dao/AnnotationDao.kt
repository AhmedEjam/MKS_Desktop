package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmedyejam.mks.data.local.entity.AnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllAnnotations(): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE workspaceId = :workspaceId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAnnotationsByWorkspaceId(workspaceId: Long): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAnnotationsByBookId(bookId: Long): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE ownerType = :ownerType AND ownerId = :ownerId AND deletedAt IS NULL ORDER BY createdAt ASC")
    fun getAnnotationsByOwner(ownerType: String, ownerId: Long): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE ownerType = :ownerType AND ownerId = :ownerId AND deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getAnnotationsByOwnerNow(ownerType: String, ownerId: Long): List<AnnotationEntity>

    @Query("SELECT * FROM annotations WHERE id = :id AND deletedAt IS NULL")
    suspend fun getAnnotationById(id: Long): AnnotationEntity?

    @Query("SELECT * FROM annotations WHERE id = :id")
    suspend fun getAnnotationByIdIncludingDeleted(id: Long): AnnotationEntity?

    @Query("SELECT * FROM annotations ORDER BY updatedAt DESC, id ASC")
    suspend fun getAllAnnotationsIncludingDeleted(): List<AnnotationEntity>

    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY updatedAt DESC, id ASC")
    suspend fun getAnnotationsByBookIdIncludingDeleted(bookId: Long): List<AnnotationEntity>

    @Query("SELECT * FROM annotations WHERE deletedAt IS NULL AND (selectedText LIKE :likeQuery OR noteBody LIKE :likeQuery OR colorLabel LIKE :likeQuery) ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun searchAnnotations(likeQuery: String, limit: Int = 100): List<AnnotationEntity>

    @Query("SELECT COUNT(*) FROM annotations WHERE bookId = :bookId AND deletedAt IS NULL")
    suspend fun countAnnotationsByBookId(bookId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotations(annotations: List<AnnotationEntity>): List<Long>

    @Update
    suspend fun updateAnnotation(annotation: AnnotationEntity)

    @Query("UPDATE annotations SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :annotationId")
    suspend fun softDeleteAnnotationById(annotationId: Long, deletedAt: Long)

    @Query("UPDATE annotations SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE ownerType = :ownerType AND ownerId = :ownerId AND deletedAt IS NULL")
    suspend fun softDeleteAnnotationsByOwner(ownerType: String, ownerId: Long, deletedAt: Long)

    @Query("UPDATE annotations SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE bookId = :bookId AND deletedAt IS NULL")
    suspend fun softDeleteAnnotationsByBookId(bookId: Long, deletedAt: Long)

    @Query("UPDATE annotations SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :annotationId")
    suspend fun restoreAnnotationById(annotationId: Long, updatedAt: Long)

    @Query("UPDATE annotations SET deletedAt = NULL, updatedAt = :updatedAt WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun restoreAnnotationsByOwner(ownerType: String, ownerId: Long, updatedAt: Long)

    @Query("UPDATE annotations SET deletedAt = NULL, updatedAt = :updatedAt WHERE bookId = :bookId")
    suspend fun restoreAnnotationsByBookId(bookId: Long, updatedAt: Long)

    @Query("DELETE FROM annotations WHERE id = :annotationId")
    suspend fun permanentlyDeleteAnnotationById(annotationId: Long)

    @Query("DELETE FROM annotations WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun permanentlyDeleteAnnotationsByOwner(ownerType: String, ownerId: Long)

    @Query("DELETE FROM annotations WHERE bookId = :bookId")
    suspend fun permanentlyDeleteAnnotationsByBookId(bookId: Long)

    @Query("DELETE FROM annotations WHERE deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun cleanupDeletedAnnotationsOlderThan(olderThan: Long): Int

    @Delete
    suspend fun hardDeleteAnnotation(annotation: AnnotationEntity)
}
