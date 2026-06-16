package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmedyejam.mks.data.local.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions WHERE targetId = :targetId AND targetType = :targetType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getSessionsForTarget(targetId: Long, targetType: String): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE id = :sessionId AND deletedAt IS NULL")
    suspend fun getSessionById(sessionId: Long): StudySessionEntity?

    @Query("SELECT * FROM study_sessions WHERE targetId = :targetId AND targetType = :targetType AND isCompleted = 0 AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveSessionForTarget(targetId: Long, targetType: String): StudySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Update
    suspend fun updateSession(session: StudySessionEntity)

    @Query("UPDATE study_sessions SET deletedAt = :deletedAt WHERE id = :sessionId")
    suspend fun softDeleteSession(sessionId: Long, deletedAt: Long = System.currentTimeMillis())
}
