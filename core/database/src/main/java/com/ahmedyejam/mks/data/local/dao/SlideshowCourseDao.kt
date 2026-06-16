package com.ahmedyejam.mks.data.local.dao

import androidx.room.*
import com.ahmedyejam.mks.data.local.entity.SlideshowCourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SlideshowCourseDao {
    @Query("SELECT * FROM slideshow_courses WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY lastEditedAt DESC")
    fun getCoursesByBookId(bookId: Long): Flow<List<SlideshowCourseEntity>>

    @Query("SELECT * FROM slideshow_courses WHERE bookId = :bookId AND deletedAt IS NULL ORDER BY lastEditedAt DESC")
    suspend fun getSlideshowCoursesByBookIdNow(bookId: Long): List<SlideshowCourseEntity>

    @Query("SELECT * FROM slideshow_courses WHERE id = :id AND deletedAt IS NULL")
    suspend fun getCourseById(id: Long): SlideshowCourseEntity?

    @Query("SELECT * FROM slideshow_courses WHERE id = :id LIMIT 1")
    suspend fun getCourseByIdIncludingDeleted(id: Long): SlideshowCourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: SlideshowCourseEntity): Long

    @Update
    suspend fun updateCourse(course: SlideshowCourseEntity)

    @Query("UPDATE slideshow_courses SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :courseId")
    suspend fun softDeleteCourseById(courseId: Long, deletedAt: Long)

    @Query("UPDATE slideshow_courses SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :courseId")
    suspend fun restoreCourseById(courseId: Long, updatedAt: Long)

    @Query("SELECT * FROM slideshow_courses WHERE deletedAt IS NOT NULL AND bookId IN (SELECT id FROM books WHERE workspaceId = :workspaceId)")
    fun getDeletedCoursesByWorkspaceFlow(workspaceId: Long): Flow<List<SlideshowCourseEntity>>

    @Delete
    suspend fun hardDeleteCourse(course: SlideshowCourseEntity)
}
