package com.ahmedyejam.mks.data.local.dao

import androidx.room.*
import com.ahmedyejam.mks.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE quizId = :quizId AND deletedAt IS NULL ORDER BY lastModifiedAt DESC")
    fun getSessionsByQuizId(quizId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId AND deletedAt IS NULL")
    suspend fun getSessionById(sessionId: Long): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("UPDATE sessions SET deletedAt = :deletedAt, updatedAt = :deletedAt, lastModifiedAt = :deletedAt WHERE id = :sessionId")
    suspend fun softDeleteSessionById(sessionId: Long, deletedAt: Long)

    @Query("UPDATE sessions SET deletedAt = :deletedAt, updatedAt = :deletedAt, lastModifiedAt = :deletedAt WHERE quizId = :quizId AND deletedAt IS NULL")
    suspend fun softDeleteSessionsByQuizId(quizId: Long, deletedAt: Long)

    @Query("UPDATE sessions SET deletedAt = NULL, updatedAt = :updatedAt, lastModifiedAt = :updatedAt WHERE id = :sessionId")
    suspend fun restoreSessionById(sessionId: Long, updatedAt: Long)

    @Query("UPDATE sessions SET deletedAt = NULL, updatedAt = :updatedAt, lastModifiedAt = :updatedAt WHERE quizId = :quizId AND deletedAt = :deletedAtFilter")
    suspend fun restoreSessionsByQuizId(quizId: Long, updatedAt: Long, deletedAtFilter: Long)

    @Delete
    suspend fun hardDeleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE quizId = :quizId")
    suspend fun hardDeleteSessionsByQuizId(quizId: Long)

    @Query("SELECT * FROM sessions WHERE quizId = :quizId AND deletedAt IS NULL ORDER BY lastModifiedAt DESC LIMIT 1")
    fun getLatestSessionForQuiz(quizId: Long): Flow<SessionEntity?>
}
