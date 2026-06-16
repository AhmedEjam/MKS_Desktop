package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmedyejam.mks.data.local.entity.NoteCollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteCollectionDao {
    @Query("SELECT * FROM note_collections WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getCollectionsByBookId(bookId: Long): Flow<List<NoteCollectionEntity>>

    @Query("SELECT * FROM note_collections WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getCollectionsByBookIdNow(bookId: Long): List<NoteCollectionEntity>

    @Query("SELECT * FROM note_collections WHERE id = :collectionId AND deletedAt IS NULL")
    suspend fun getCollectionById(collectionId: Long): NoteCollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: NoteCollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: NoteCollectionEntity)

    @Query("UPDATE note_collections SET deletedAt = :deletedAt WHERE id = :collectionId")
    suspend fun softDeleteCollection(collectionId: Long, deletedAt: Long = System.currentTimeMillis())
}
