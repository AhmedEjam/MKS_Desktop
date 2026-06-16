package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmedyejam.mks.data.local.entity.PromptRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptRunDao {
    @Query("SELECT * FROM prompt_runs WHERE promptCardId = :promptCardId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getRunsByCardId(promptCardId: Long): Flow<List<PromptRunEntity>>

    @Query("SELECT * FROM prompt_runs WHERE deletedAt IS NULL AND promptCardId IN (SELECT id FROM prompt_cards WHERE deckId = :deckId AND deletedAt IS NULL) ORDER BY createdAt DESC")
    fun getRunsByDeckId(deckId: Long): Flow<List<PromptRunEntity>>

    @Query("SELECT * FROM prompt_runs WHERE deletedAt IS NULL AND promptCardId IN (SELECT id FROM prompt_cards WHERE deckId = :deckId AND deletedAt IS NULL) ORDER BY createdAt DESC")
    suspend fun getRunsByDeckIdNow(deckId: Long): List<PromptRunEntity>

    @Query("SELECT COUNT(*) FROM prompt_runs WHERE deletedAt IS NULL")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM prompt_runs WHERE deletedAt IS NULL AND promptCardId IN (SELECT id FROM prompt_cards WHERE deletedAt IS NULL AND deckId IN (SELECT id FROM prompt_decks WHERE bookId = :bookId AND deletedAt IS NULL))")
    suspend fun countByBookId(bookId: Long): Int

    @Query("SELECT COUNT(*) FROM prompt_runs WHERE deletedAt IS NULL AND outputText IS NOT NULL AND outputText != ''")
    suspend fun countSavedOutputs(): Int

    @Query("SELECT COUNT(*) FROM prompt_runs WHERE deletedAt IS NULL AND outputText IS NOT NULL AND outputText != '' AND promptCardId IN (SELECT id FROM prompt_cards WHERE deletedAt IS NULL AND deckId IN (SELECT id FROM prompt_decks WHERE bookId = :bookId AND deletedAt IS NULL))")
    suspend fun countSavedOutputsByBookId(bookId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: PromptRunEntity): Long

    @Update
    suspend fun updateRun(run: PromptRunEntity)

    @Query("UPDATE prompt_runs SET deletedAt = :deletedAt WHERE id = :runId")
    suspend fun softDeleteRunById(runId: Long, deletedAt: Long)

    @Query("UPDATE prompt_runs SET deletedAt = NULL WHERE id = :runId")
    suspend fun restoreRunById(runId: Long)

    @Delete
    suspend fun hardDeleteRun(run: PromptRunEntity)
}
