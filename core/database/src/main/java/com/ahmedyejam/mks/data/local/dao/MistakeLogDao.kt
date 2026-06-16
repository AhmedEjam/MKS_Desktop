package com.ahmedyejam.mks.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ahmedyejam.mks.data.local.entity.MistakeLogEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MistakeLogDao {
    @Query("SELECT * FROM mistake_log_entries WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllMistakes(): Flow<List<MistakeLogEntryEntity>>

    @Query("SELECT * FROM mistake_log_entries WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getMistakesByBookId(bookId: Long): Flow<List<MistakeLogEntryEntity>>

    @Query("SELECT * FROM mistake_log_entries WHERE quizId = :quizId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getMistakesByQuizId(quizId: Long): Flow<List<MistakeLogEntryEntity>>

    @Query("SELECT * FROM mistake_log_entries WHERE questionId = :questionId AND deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getMistakesByQuestionId(questionId: Long): List<MistakeLogEntryEntity>

    @Query("SELECT * FROM mistake_log_entries WHERE id = :id AND deletedAt IS NULL")
    suspend fun getMistakeById(id: Long): MistakeLogEntryEntity?

    @Query("SELECT * FROM mistake_log_entries WHERE questionId = :questionId AND deletedAt IS NULL AND ((:sessionId IS NULL AND sessionId IS NULL) OR sessionId = :sessionId) LIMIT 1")
    suspend fun findByQuestionAndSession(questionId: Long, sessionId: Long?): MistakeLogEntryEntity?

    @Query("SELECT * FROM mistake_log_entries WHERE deletedAt IS NULL AND isFixed = 0 AND reviewAt IS NOT NULL AND reviewAt <= :now ORDER BY reviewAt ASC, createdAt DESC LIMIT :limit")
    suspend fun getDueMistakes(now: Long, limit: Int = 50): List<MistakeLogEntryEntity>

    @Query("SELECT COUNT(*) FROM mistake_log_entries WHERE deletedAt IS NULL AND isFixed = 0")
    suspend fun countOpenMistakes(): Int

    @Query("SELECT COUNT(*) FROM mistake_log_entries WHERE deletedAt IS NULL AND isFixed = 1")
    suspend fun countFixedMistakes(): Int

    @Query("SELECT COUNT(*) FROM mistake_log_entries WHERE deletedAt IS NULL AND isFixed = 0 AND reviewAt IS NOT NULL AND reviewAt <= :now")
    suspend fun countDueMistakes(now: Long): Int

    @Query("SELECT COUNT(*) FROM mistake_log_entries WHERE deletedAt IS NULL AND isFixed = 0 AND reviewAt IS NOT NULL AND reviewAt > :now")
    suspend fun countPendingMistakes(now: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistake(entry: MistakeLogEntryEntity): Long

    @Update
    suspend fun updateMistake(entry: MistakeLogEntryEntity)

    @Query("UPDATE mistake_log_entries SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :entryId")
    suspend fun softDeleteMistakeById(entryId: Long, deletedAt: Long)

    @Query("UPDATE mistake_log_entries SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :entryId")
    suspend fun restoreMistakeById(entryId: Long, updatedAt: Long)

    @Delete
    suspend fun hardDeleteMistake(entry: MistakeLogEntryEntity)

    @Query("UPDATE mistake_log_entries SET isFixed = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markFixed(id: Long, updatedAt: Long)

    @Query("UPDATE mistake_log_entries SET reviewAt = :reviewAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun snooze(id: Long, reviewAt: Long, updatedAt: Long)
}
