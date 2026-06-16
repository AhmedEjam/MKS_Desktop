package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmedyejam.mks.data.local.entity.KnowledgeStudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeStudySessionDao {
    @Query("SELECT * FROM knowledge_study_sessions WHERE targetType = :targetType AND targetId = :targetId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getSessionsByTarget(targetType: String, targetId: Long): Flow<List<KnowledgeStudySessionEntity>>

    @Query("SELECT * FROM knowledge_study_sessions WHERE targetType = :targetType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getSessionsByTargetType(targetType: String): Flow<List<KnowledgeStudySessionEntity>>

    @Query("SELECT * FROM knowledge_study_sessions WHERE targetId = :targetId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getSessionsByTargetId(targetId: Long): Flow<List<KnowledgeStudySessionEntity>>

    @Query("""
        SELECT * FROM knowledge_study_sessions 
        WHERE deletedAt IS NULL AND ((targetType = 'FLASHCARD_DECK' AND targetId IN (SELECT id FROM flashcard_decks WHERE bookId = :bookId AND deletedAt IS NULL))
           OR (targetType = 'SLIDESHOW' AND targetId IN (SELECT id FROM slideshow_courses WHERE bookId = :bookId AND deletedAt IS NULL))
           OR (targetType = 'NOTE' AND targetId IN (SELECT id FROM note_blueprints WHERE collectionId IN (SELECT id FROM note_collections WHERE bookId = :bookId AND deletedAt IS NULL) AND deletedAt IS NULL))
           OR (targetType = 'PROMPT' AND targetId IN (SELECT id FROM prompt_decks WHERE bookId = :bookId AND deletedAt IS NULL)))
        ORDER BY updatedAt DESC
    """)
    suspend fun getSessionsByBookIdNow(bookId: Long): List<KnowledgeStudySessionEntity>

    @Query("SELECT * FROM knowledge_study_sessions WHERE targetType = :targetType AND targetId = :targetId AND isCompleted = 0 AND deletedAt IS NULL ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveSessionByTarget(targetType: String, targetId: Long): KnowledgeStudySessionEntity?

    @Query("SELECT * FROM knowledge_study_sessions WHERE id = :id AND deletedAt IS NULL")
    suspend fun getSessionById(id: Long): KnowledgeStudySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: KnowledgeStudySessionEntity): Long

    @Update
    suspend fun updateSession(session: KnowledgeStudySessionEntity)

    @Query("UPDATE knowledge_study_sessions SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :sessionId")
    suspend fun softDeleteSessionById(sessionId: Long, deletedAt: Long)

    @Query("UPDATE knowledge_study_sessions SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun restoreSessionById(sessionId: Long, updatedAt: Long)

    @Delete
    suspend fun hardDeleteSession(session: KnowledgeStudySessionEntity)
}
