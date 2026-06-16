package com.ahmedyejam.mks.data.importer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ahmedyejam.mks.data.importer.model.ParsedOption
import com.ahmedyejam.mks.data.importer.model.ParsedQuestion
import com.ahmedyejam.mks.data.importer.repository.ImportLibraryManager
import com.ahmedyejam.mks.data.local.FileManager
import com.ahmedyejam.mks.data.local.MksDatabase
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ImportTargetQuizTest {
    private lateinit var db: MksDatabase
    private lateinit var importManager: ImportLibraryManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MksDatabase::class.java).build()
        val fileManager = FileManager(context)
        importManager = ImportLibraryManager(context, db, fileManager)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun importQuestions_withTargetQuizId_mapsQuestionsToThatQuiz() =
        runBlocking {
            // 1. Create a book and a quiz
            val bookId =
                db.bookDao().insertBook(
                    BookEntity(
                        externalId = "target_book",
                        title = "Target Book",
                        description = "",
                    ),
                )
            val targetQuizId =
                db.quizDao().insertQuiz(
                    QuizEntity(
                        externalId = "target_quiz",
                        bookId = bookId,
                        title = "Target Quiz",
                        description = "",
                    ),
                )

            // 2. Prepare some questions to import
            val questions =
                listOf(
                    ParsedQuestion(
                        stem = "Q1",
                        options = listOf(ParsedOption("opt_A", "A", true)),
                        correctAnswers = listOf("opt_A"),
                        sourceLine = 1,
                    ),
                    ParsedQuestion(
                        stem = "Q2",
                        options = listOf(ParsedOption("opt_B", "B", true)),
                        correctAnswers = listOf("opt_B"),
                        sourceLine = 2,
                    ),
                )

            // 3. Import with targetQuizId
            val result =
                importManager.importQuestions(
                    title = "Imported Questions",
                    questions = questions,
                    targetBookId = bookId,
                    targetQuizId = targetQuizId,
                )

            // 4. Verify results
            assertTrue("Import should be successful", result.success)
            assertEquals("Should import 2 questions", 2, result.importedQuestionsCount)
            assertEquals("Should NOT import new quizzes", 0, result.importedQuizzesCount)

            val importedQuestions = db.questionDao().getQuestionsByQuizId(targetQuizId).first()
            assertEquals("Quiz should have 2 questions", 2, importedQuestions.size)
            assertTrue("All questions should belong to targetQuizId", importedQuestions.all { it.quizId == targetQuizId })
        }
}
