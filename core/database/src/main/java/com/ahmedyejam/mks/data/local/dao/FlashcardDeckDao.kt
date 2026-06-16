package com.ahmedyejam.mks.data.local.dao

import androidx.room.*
import com.ahmedyejam.mks.data.local.entity.FlashcardDeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDeckDao {
    @Query("SELECT * FROM flashcard_decks WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getFlashcardDecksByBookId(bookId: Long): Flow<List<FlashcardDeckEntity>>

    @Query("SELECT * FROM flashcard_decks WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getFlashcardDecksByBookIdNow(bookId: Long): List<FlashcardDeckEntity>

    @Query("SELECT * FROM flashcard_decks WHERE id = :id AND deletedAt IS NULL")
    suspend fun getFlashcardDeckById(id: Long): FlashcardDeckEntity?

    @Query("SELECT * FROM flashcard_decks WHERE id = :id LIMIT 1")
    suspend fun getFlashcardDeckByIdIncludingDeleted(id: Long): FlashcardDeckEntity?

    @Query("SELECT * FROM flashcard_decks WHERE id = :id AND deletedAt IS NULL")
    fun observeFlashcardDeckById(id: Long): Flow<FlashcardDeckEntity?>

    @Query("SELECT COUNT(*) FROM flashcard_decks WHERE deletedAt IS NULL")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcardDeck(deck: FlashcardDeckEntity): Long

    @Update
    suspend fun updateFlashcardDeck(deck: FlashcardDeckEntity)

    @Query("UPDATE flashcard_decks SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :deckId")
    suspend fun softDeleteFlashcardDeckById(deckId: Long, deletedAt: Long)

    @Query("UPDATE flashcard_decks SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :deckId")
    suspend fun restoreFlashcardDeckById(deckId: Long, updatedAt: Long)

    @Query("SELECT * FROM flashcard_decks WHERE deletedAt IS NOT NULL AND bookId IN (SELECT id FROM books WHERE workspaceId = :workspaceId)")
    fun getDeletedDecksByWorkspaceFlow(workspaceId: Long): Flow<List<FlashcardDeckEntity>>

    @Delete
    suspend fun hardDeleteFlashcardDeck(deck: FlashcardDeckEntity)
}

