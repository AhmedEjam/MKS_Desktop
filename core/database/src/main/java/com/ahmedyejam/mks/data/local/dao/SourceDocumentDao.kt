package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmedyejam.mks.data.local.entity.SourceDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDocumentDao {
    @Query("SELECT * FROM source_documents WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY updatedAt DESC, title COLLATE NOCASE ASC")
    fun getSourcesByBookId(bookId: Long): Flow<List<SourceDocumentEntity>>

    @Query("SELECT * FROM source_documents WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY updatedAt DESC, title COLLATE NOCASE ASC")
    suspend fun getSourcesByBookIdNow(bookId: Long): List<SourceDocumentEntity>

    @Query("SELECT * FROM source_documents WHERE id = :id AND deletedAt IS NULL")
    suspend fun getSourceById(id: Long): SourceDocumentEntity?

    @Query("SELECT * FROM source_documents ORDER BY updatedAt DESC, title COLLATE NOCASE ASC")
    suspend fun getAllSourcesIncludingDeleted(): List<SourceDocumentEntity>

    @Query("SELECT * FROM source_documents WHERE bookId = :bookId ORDER BY updatedAt DESC, title COLLATE NOCASE ASC")
    suspend fun getSourcesByBookIdIncludingDeleted(bookId: Long): List<SourceDocumentEntity>

    @Query("SELECT * FROM source_documents WHERE deletedAt IS NULL AND (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    fun searchSources(query: String): Flow<List<SourceDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SourceDocumentEntity): Long

    @Update
    suspend fun updateSource(source: SourceDocumentEntity)

    @Query("UPDATE source_documents SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :sourceId")
    suspend fun softDeleteSourceById(sourceId: Long, deletedAt: Long)

    @Query("UPDATE source_documents SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :sourceId")
    suspend fun restoreSourceById(sourceId: Long, updatedAt: Long)

    @Delete
    suspend fun hardDeleteSource(source: SourceDocumentEntity)
}
