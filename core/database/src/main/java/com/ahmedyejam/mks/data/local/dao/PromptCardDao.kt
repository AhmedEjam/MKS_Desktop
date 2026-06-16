package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmedyejam.mks.data.local.entity.PromptCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptCardDao {
    @Query("SELECT * FROM prompt_cards WHERE deckId = :deckId AND deletedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    fun getCardsByDeckId(deckId: Long): Flow<List<PromptCardEntity>>

    @Query("SELECT * FROM prompt_cards WHERE deckId = :deckId AND deletedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getCardsByDeckIdNow(deckId: Long): List<PromptCardEntity>

    @Query("SELECT * FROM prompt_cards WHERE id = :id AND deletedAt IS NULL")
    suspend fun getCardById(id: Long): PromptCardEntity?

    @Query("SELECT COUNT(*) FROM prompt_cards WHERE deletedAt IS NULL")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM prompt_cards WHERE deletedAt IS NULL AND deckId IN (SELECT id FROM prompt_decks WHERE bookId = :bookId AND deletedAt IS NULL)")
    suspend fun countByBookId(bookId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: PromptCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<PromptCardEntity>): List<Long>

    @Update
    suspend fun updateCard(card: PromptCardEntity)

    @Update
    suspend fun updateCards(cards: List<PromptCardEntity>)

    @Query("UPDATE prompt_cards SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :cardId")
    suspend fun softDeleteCardById(cardId: Long, deletedAt: Long)

    @Query("UPDATE prompt_cards SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun restoreCardById(cardId: Long, updatedAt: Long)

    @Delete
    suspend fun hardDeleteCard(card: PromptCardEntity)

    @Query("UPDATE prompt_cards SET usageCount = usageCount + 1, lastUsedAt = :lastUsedAt, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun recordUse(cardId: Long, lastUsedAt: Long, updatedAt: Long)
}
