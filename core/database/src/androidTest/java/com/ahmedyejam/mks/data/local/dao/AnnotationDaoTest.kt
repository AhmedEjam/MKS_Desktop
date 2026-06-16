package com.ahmedyejam.mks.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ahmedyejam.mks.data.local.MksDatabase
import com.ahmedyejam.mks.data.local.entity.AnnotationColorLabel
import com.ahmedyejam.mks.data.local.entity.AnnotationEntity
import com.ahmedyejam.mks.data.local.entity.AnnotationOwnerType
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.WorkspaceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AnnotationDaoTest {
    private lateinit var db: MksDatabase
    private lateinit var annotationDao: AnnotationDao
    private var workspaceId: Long = 0L
    private var bookId: Long = 0L

    @Before
    fun createDb() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            db = Room.inMemoryDatabaseBuilder(context, MksDatabase::class.java).build()
            annotationDao = db.annotationDao()
            workspaceId =
                db.workspaceDao().insertWorkspace(
                    WorkspaceEntity(
                        externalId = UUID.randomUUID().toString(),
                        name = "Default",
                        isDefault = true,
                    ),
                )
            bookId =
                db.bookDao().insertBook(
                    BookEntity(
                        workspaceId = workspaceId,
                        externalId = UUID.randomUUID().toString(),
                        title = "Book",
                        description = "",
                    ),
                )
        }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertSoftDeleteAndRestoreAnnotation() =
        runBlocking {
            val id =
                annotationDao.insertAnnotation(
                    AnnotationEntity(
                        workspaceId = workspaceId,
                        bookId = bookId,
                        ownerType = AnnotationOwnerType.QUESTION,
                        ownerId = 7L,
                        selectedText = "important line",
                        noteBody = "review this",
                        colorLabel = AnnotationColorLabel.YELLOW,
                    ),
                )

            assertEquals(1, annotationDao.getAnnotationsByOwner(AnnotationOwnerType.QUESTION, 7L).first().size)

            annotationDao.softDeleteAnnotationById(id, 3000L)
            assertEquals(0, annotationDao.getAnnotationsByOwner(AnnotationOwnerType.QUESTION, 7L).first().size)

            annotationDao.restoreAnnotationById(id, 4000L)
            assertEquals(1, annotationDao.getAnnotationsByOwner(AnnotationOwnerType.QUESTION, 7L).first().size)
        }

    @Test
    fun deleteByOwnerHidesAllOwnerAnnotations() =
        runBlocking {
            annotationDao.insertAnnotation(
                AnnotationEntity(
                    workspaceId = workspaceId,
                    bookId = bookId,
                    ownerType = AnnotationOwnerType.SLIDE,
                    ownerId = 42L,
                    noteBody = "first",
                ),
            )
            annotationDao.insertAnnotation(
                AnnotationEntity(
                    workspaceId = workspaceId,
                    bookId = bookId,
                    ownerType = AnnotationOwnerType.SLIDE,
                    ownerId = 42L,
                    noteBody = "second",
                ),
            )
            assertEquals(2, annotationDao.getAnnotationsByOwner(AnnotationOwnerType.SLIDE, 42L).first().size)

            annotationDao.softDeleteAnnotationsByOwner(AnnotationOwnerType.SLIDE, 42L, 5000L)
            assertEquals(0, annotationDao.getAnnotationsByOwner(AnnotationOwnerType.SLIDE, 42L).first().size)
        }

    @Test
    fun bookSoftDeleteRestoreAndPermanentDeleteControlAnnotationVisibility() =
        runBlocking {
            annotationDao.insertAnnotation(
                AnnotationEntity(
                    workspaceId = workspaceId,
                    bookId = bookId,
                    ownerType = AnnotationOwnerType.QUESTION,
                    ownerId = 11L,
                    noteBody = "book scoped",
                ),
            )
            assertEquals(1, annotationDao.getAnnotationsByBookId(bookId).first().size)

            annotationDao.softDeleteAnnotationsByBookId(bookId, 6000L)
            assertEquals(0, annotationDao.getAnnotationsByBookId(bookId).first().size)

            annotationDao.restoreAnnotationsByBookId(bookId, 7000L)
            assertEquals(1, annotationDao.getAnnotationsByBookId(bookId).first().size)

            annotationDao.permanentlyDeleteAnnotationsByBookId(bookId)
            assertEquals(0, annotationDao.getAnnotationsByBookId(bookId).first().size)
        }
}
