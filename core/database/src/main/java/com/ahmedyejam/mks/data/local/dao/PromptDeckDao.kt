package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmedyejam.mks.data.local.entity.PromptDeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDeckDao {
    @Query("SELECT * FROM prompt_decks WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY updatedAt DESC, title COLLATE NOCASE ASC")
    fun getDecksByBookId(bookId: Long): Flow<List<PromptDeckEntity>>

    @Query("SELECT * FROM prompt_decks WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY updatedAt DESC, title COLLATE NOCASE ASC")
    suspend fun getDecksByBookIdNow(bookId: Long): List<PromptDeckEntity>

    @Query("SELECT * FROM prompt_decks WHERE id = :id AND deletedAt IS NULL")
    suspend fun getDeckById(id: Long): PromptDeckEntity?

    @Query("SELECT * FROM prompt_decks WHERE id = :id LIMIT 1")
    suspend fun getDeckByIdIncludingDeleted(id: Long): PromptDeckEntity?

    @Query("SELECT COUNT(*) FROM prompt_decks WHERE deletedAt IS NULL")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM prompt_decks WHERE bookId = :bookId AND deletedAt IS NULL")
    suspend fun countByBookId(bookId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: PromptDeckEntity): Long

    @Update
    suspend fun updateDeck(deck: PromptDeckEntity)

    @Query("UPDATE prompt_decks SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :deckId")
    suspend fun softDeleteDeckById(deckId: Long, deletedAt: Long)

    @Query("UPDATE prompt_decks SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :deckId")
    suspend fun restoreDeckById(deckId: Long, updatedAt: Long)

    @Query("SELECT * FROM prompt_decks WHERE deletedAt IS NOT NULL AND bookId IN (SELECT id FROM books WHERE workspaceId = :workspaceId)")
    fun getDeletedDecksByWorkspaceFlow(workspaceId: Long): Flow<List<PromptDeckEntity>>

    @Delete
    suspend fun hardDeleteDeck(deck: PromptDeckEntity)
}
