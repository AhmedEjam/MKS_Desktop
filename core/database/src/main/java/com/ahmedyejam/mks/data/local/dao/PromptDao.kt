package com.ahmedyejam.mks.data.local.dao

import androidx.room.*
import com.ahmedyejam.mks.data.local.entity.PromptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getPromptsByBookId(bookId: Long): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts WHERE id = :id AND deletedAt IS NULL")
    suspend fun getPromptById(id: Long): PromptEntity?

    @Query("SELECT * FROM prompts WHERE id = :id LIMIT 1")
    suspend fun getPromptByIdIncludingDeleted(id: Long): PromptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: PromptEntity): Long

    @Update
    suspend fun updatePrompt(prompt: PromptEntity)

    @Query("UPDATE prompts SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :promptId")
    suspend fun softDeletePromptById(promptId: Long, deletedAt: Long)

    @Query("UPDATE prompts SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :promptId")
    suspend fun restorePromptById(promptId: Long, updatedAt: Long)

    @Delete
    suspend fun hardDeletePrompt(prompt: PromptEntity)
}
