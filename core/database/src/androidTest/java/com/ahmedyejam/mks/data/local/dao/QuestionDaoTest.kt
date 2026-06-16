package com.ahmedyejam.mks.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ahmedyejam.mks.data.local.MksDatabase
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class QuestionDaoTest {
    private lateinit var questionDao: QuestionDao
    private lateinit var quizDao: QuizDao
    private lateinit var bookDao: BookDao
    private lateinit var db: MksDatabase
    private var workspaceId: Long = 0L

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MksDatabase::class.java).build()
        questionDao = db.questionDao()
        quizDao = db.quizDao()
        bookDao = db.bookDao()
        workspaceId =
            runBlocking {
                db.workspaceDao().insertWorkspace(
                    com.ahmedyejam.mks.data.local.entity.WorkspaceEntity(
                        externalId = UUID.randomUUID().toString(),
                        name = "Default",
                        isDefault = true,
                    ),
                )
            }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun updatePerformanceMetrics_incrementsCorrectly() =
        runBlocking {
            // Setup dependencies
            val bookId =
                bookDao.insertBook(
                    BookEntity(
                        workspaceId = workspaceId,
                        externalId = UUID.randomUUID().toString(),
                        title = "Test Book",
                        description = "Test Description",
                    ),
                )
            val quizId =
                quizDao.insertQuiz(
                    QuizEntity(
                        externalId = UUID.randomUUID().toString(),
                        bookId = bookId,
                        title = "Test Quiz",
                        description = "Test Description",
                    ),
                )

            // Insert initial question
            val question =
                QuestionEntity(
                    externalId = UUID.randomUUID().toString(),
                    quizId = quizId,
                    text = "Test Question",
                    type = QuestionType.SINGLE_CHOICE,
                    options = listOf("A", "B"),
                    correctAnswers = listOf(0),
                    attempts = 5,
                    correctCount = 3,
                    timeSpentMs = 1000,
                )
            val id = questionDao.insertQuestion(question)

            // Update metrics
            val now = 2000L
            val timeSpent = 500L
            questionDao.updatePerformanceMetrics(
                id = id,
                isCorrect = true,
                isCorrectInt = 1,
                timeSpentMs = timeSpent,
                now = now,
            )

            // Verify updates
            val updated = questionDao.getQuestionById(id)
            assertEquals(6, updated?.attempts)
            assertEquals(4, updated?.correctCount)
            assertEquals(1500L, updated?.timeSpentMs)
            assertEquals(now, updated?.lastStudiedAt)
            assertEquals(true, updated?.lastAttemptResult)
            assertEquals(now, updated?.updatedAt)
        }

    @Test
    @Throws(Exception::class)
    fun softDeleteQuestion_hidesAndRestoreShowsQuestion() =
        runBlocking {
            val bookId =
                bookDao.insertBook(
                    BookEntity(
                        workspaceId = workspaceId,
                        externalId = UUID.randomUUID().toString(),
                        title = "Soft Delete Book",
                        description = "",
                    ),
                )
            val quizId =
                quizDao.insertQuiz(
                    QuizEntity(
                        externalId = UUID.randomUUID().toString(),
                        bookId = bookId,
                        title = "Soft Delete Quiz",
                        description = "",
                    ),
                )
            val questionId =
                questionDao.insertQuestion(
                    QuestionEntity(
                        externalId = UUID.randomUUID().toString(),
                        quizId = quizId,
                        text = "Soft deleted?",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("A", "B"),
                        correctAnswers = listOf(0),
                    ),
                )

            assertEquals(1, questionDao.getQuestionsByIds(listOf(questionId)).size)

            questionDao.softDeleteQuestionById(questionId, 3000L)
            assertEquals(0, questionDao.getQuestionsByIds(listOf(questionId)).size)

            questionDao.restoreQuestionById(questionId, 4000L)
            assertEquals(1, questionDao.getQuestionsByIds(listOf(questionId)).size)
        }
}
