package com.ahmedyejam.mks.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ahmedyejam.mks.data.local.MksDatabase
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.QuestionCategoryEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestionCategoryDaoTest {
    private lateinit var db: MksDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MksDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun getQuestionsByCategoryUsesNormalizedJoinTable() =
        runBlocking {
            val bookId = db.bookDao().insertBook(BookEntity(externalId = "book", title = "Book"))
            val quizId = db.quizDao().insertQuiz(QuizEntity(externalId = "quiz", bookId = bookId, title = "Quiz"))
            val questionId =
                db.questionDao().insertQuestion(
                    QuestionEntity(
                        externalId = "q1",
                        quizId = quizId,
                        text = "Question",
                        type = QuestionType.SINGLE_CHOICE,
                        options = listOf("A", "B"),
                        correctAnswers = listOf(0),
                        categories = listOf("renal"),
                    ),
                )

            db.questionCategoryDao().insertCategories(listOf(QuestionCategoryEntity(questionId, "renal")))

            val questions = db.questionCategoryDao().getQuestionsByCategoryFlow("renal").first()

            assertEquals(1, questions.size)
            assertEquals(questionId, questions.single().id)
        }
}
