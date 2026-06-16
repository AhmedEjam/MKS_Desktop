package com.ahmedyejam.mks.data.local.dao

import androidx.room.*
import com.ahmedyejam.mks.data.local.entity.LearningSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningSessionDao {
    @Query("SELECT * FROM learning_sessions WHERE deckId = :deckId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getSessionsByDeckId(deckId: Long): Flow<List<LearningSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: LearningSessionEntity): Long

    @Update
    suspend fun updateSession(session: LearningSessionEntity)

    @Query("UPDATE learning_sessions SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :sessionId")
    suspend fun softDeleteSessionById(sessionId: Long, deletedAt: Long)

    @Query("UPDATE learning_sessions SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun restoreSessionById(sessionId: Long, updatedAt: Long)

    @Delete
    suspend fun hardDeleteSession(session: LearningSessionEntity)
}

