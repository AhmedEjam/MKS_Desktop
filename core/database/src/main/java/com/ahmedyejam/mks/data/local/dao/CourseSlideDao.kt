package com.ahmedyejam.mks.data.local.dao

import androidx.room.*
import com.ahmedyejam.mks.data.local.entity.CourseSlideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseSlideDao {
    @Query("SELECT * FROM course_slides WHERE courseId = :courseId AND deletedAt IS NULL ORDER BY orderIndex ASC")
    fun getSlidesByCourseId(courseId: Long): Flow<List<CourseSlideEntity>>

    @Query("SELECT * FROM course_slides WHERE courseId = :courseId AND deletedAt IS NULL ORDER BY orderIndex ASC")
    suspend fun getSlidesByCourseIdNow(courseId: Long): List<CourseSlideEntity>

    @Query("SELECT * FROM course_slides WHERE courseId = :courseId ORDER BY orderIndex ASC")
    suspend fun getSlidesByCourseIdIncludingDeleted(courseId: Long): List<CourseSlideEntity>

    @Query("SELECT * FROM course_slides WHERE id = :id AND deletedAt IS NULL")
    suspend fun getSlideById(id: Long): CourseSlideEntity?

    @Query("SELECT * FROM course_slides WHERE sourceQuestionId = :questionId AND deletedAt IS NULL")
    suspend fun getSlidesBySourceQuestionId(questionId: Long): List<CourseSlideEntity>

    @Query("SELECT COUNT(*) FROM course_slides WHERE deletedAt IS NULL AND isCompleted = 0")
    suspend fun countUnfinishedSlides(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlide(slide: CourseSlideEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlides(slides: List<CourseSlideEntity>)

    @Update
    suspend fun updateSlide(slide: CourseSlideEntity)

    @Update
    suspend fun updateSlides(slides: List<CourseSlideEntity>)

    @Query("UPDATE course_slides SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :slideId")
    suspend fun softDeleteSlideById(slideId: Long, deletedAt: Long)

    @Query("UPDATE course_slides SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE courseId = :courseId AND deletedAt IS NULL")
    suspend fun softDeleteSlidesByCourseId(courseId: Long, deletedAt: Long)

    @Query("UPDATE course_slides SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :slideId")
    suspend fun restoreSlideById(slideId: Long, updatedAt: Long)

    @Query("UPDATE course_slides SET deletedAt = NULL, updatedAt = :updatedAt WHERE courseId = :courseId AND deletedAt = :deletedAtFilter")
    suspend fun restoreSlidesByCourseId(courseId: Long, updatedAt: Long, deletedAtFilter: Long)

    @Delete
    suspend fun hardDeleteSlide(slide: CourseSlideEntity)

    @Query("SELECT COUNT(*) FROM course_slides WHERE courseId = :courseId AND deletedAt IS NULL")
    suspend fun countSlidesInCourse(courseId: Long): Int

    @Query("SELECT * FROM course_slides WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun getSlidesByIds(ids: List<Long>): List<CourseSlideEntity>

    @Query("UPDATE course_slides SET courseId = :targetCourseId, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun moveSlidesToCourse(ids: List<Long>, targetCourseId: Long, updatedAt: Long)
}
