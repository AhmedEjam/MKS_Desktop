package com.ahmedyejam.mks.data.local.dao

import androidx.room.*
import com.ahmedyejam.mks.data.local.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND deletedAt IS NULL ORDER BY orderIndex ASC")
    fun getFlashcardsByDeckId(deckId: Long): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND deletedAt IS NULL ORDER BY orderIndex ASC")
    suspend fun getFlashcardsByDeckIdNow(deckId: Long): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE id = :id AND deletedAt IS NULL")
    suspend fun getFlashcardById(id: Long): FlashcardEntity?

    @Query("SELECT * FROM flashcards WHERE sourceQuestionId = :questionId AND deletedAt IS NULL")
    suspend fun getFlashcardsBySourceQuestionId(questionId: Long): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE id IN (:ids) AND deletedAt IS NULL ORDER BY orderIndex ASC")
    suspend fun getFlashcardsByIds(ids: List<Long>): List<FlashcardEntity>

    @Query("SELECT COUNT(*) FROM flashcards WHERE deletedAt IS NULL")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId AND deletedAt IS NULL")
    suspend fun countCardsInDeck(deckId: Long): Int

    @Query("SELECT COUNT(*) FROM flashcards WHERE deletedAt IS NULL AND attempts >= 2 AND correctCount * 2 < attempts")
    suspend fun countWeakFlashcards(): Int

    @Query("""
        SELECT * FROM flashcards
        WHERE deletedAt IS NULL AND ((dueAt > 0 AND dueAt <= :now)
           OR (dueAt = 0 AND lastReviewedAt = 0))
        ORDER BY dueAt ASC, createdAt ASC
        LIMIT :limit
    """)
    suspend fun getDueFlashcards(now: Long, limit: Int = 50): List<FlashcardEntity>

    @Query("""
        SELECT COUNT(*) FROM flashcards
        WHERE deletedAt IS NULL AND ((dueAt > 0 AND dueAt <= :now)
           OR (dueAt = 0 AND lastReviewedAt = 0))
    """)
    suspend fun countDueFlashcards(now: Long): Int

    @Query("UPDATE flashcards SET lastReviewedAt = :reviewedAt, dueAt = :nextDueAt, reviewCount = reviewCount + 1, updatedAt = :reviewedAt WHERE id = :id")
    suspend fun markReviewed(id: Long, reviewedAt: Long, nextDueAt: Long)

    @Query("UPDATE flashcards SET dueAt = :dueAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun snooze(id: Long, dueAt: Long, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(card: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(cards: List<FlashcardEntity>): List<Long>

    @Update
    suspend fun updateFlashcard(card: FlashcardEntity)

    @Query("UPDATE flashcards SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :cardId")
    suspend fun softDeleteFlashcardById(cardId: Long, deletedAt: Long)

    @Query("UPDATE flashcards SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE deckId = :deckId AND deletedAt IS NULL")
    suspend fun softDeleteAllCardsInDeck(deckId: Long, deletedAt: Long)

    @Query("UPDATE flashcards SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun restoreFlashcardById(cardId: Long, updatedAt: Long)

    @Query("UPDATE flashcards SET deletedAt = NULL, updatedAt = :updatedAt WHERE deckId = :deckId AND deletedAt = :deletedAtFilter")
    suspend fun restoreAllCardsInDeck(deckId: Long, updatedAt: Long, deletedAtFilter: Long)

    @Delete
    suspend fun hardDeleteFlashcard(card: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun hardDeleteAllCardsInDeck(deckId: Long)

    @Query("UPDATE flashcards SET orderIndex = :orderIndex, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun updateCardOrder(cardId: Long, orderIndex: Int, updatedAt: Long)

    @Query("UPDATE flashcards SET deckId = :deckId, updatedAt = :updatedAt WHERE id IN (:cardIds)")
    suspend fun moveCardsToDeck(cardIds: List<Long>, deckId: Long, updatedAt: Long)
}
