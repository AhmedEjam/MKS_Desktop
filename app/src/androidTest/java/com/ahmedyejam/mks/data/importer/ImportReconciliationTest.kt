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
class ImportReconciliationTest {
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
    fun reImportingSameQuestions_reconcilesInsteadOfDuplicating() =
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
                        stem = "Deterministic Q1",
                        options = listOf(ParsedOption("opt_A", "Choice A", true)),
                        correctAnswers = listOf("opt_A"),
                    ),
                    ParsedQuestion(
                        stem = "Deterministic Q2",
                        options = listOf(ParsedOption("opt_B", "Choice B", true)),
                        correctAnswers = listOf("opt_B"),
                    ),
                )

            // 3. First Import
            val result1 =
                importManager.importQuestions(
                    title = "Import 1",
                    questions = questions,
                    targetBookId = bookId,
                    targetQuizId = targetQuizId,
                )

            assertTrue("First import should be successful", result1.success)
            assertEquals("First import should have 2 imported questions", 2, result1.importedQuestionsCount)

            val questionsAfter1 = db.questionDao().getQuestionsByQuizId(targetQuizId).first()
            assertEquals("Should have 2 questions in DB", 2, questionsAfter1.size)

            // 4. Second Import (Same Questions)
            val result2 =
                importManager.importQuestions(
                    title = "Import 2",
                    questions = questions,
                    targetBookId = bookId,
                    targetQuizId = targetQuizId,
                )

            assertTrue("Second import should be successful", result2.success)
            assertEquals("Second import should have 0 NEW imported questions", 0, result2.importedQuestionsCount)
            assertEquals("Second import should have 2 UPDATED questions", 2, result2.updatedQuestionsCount)

            val questionsAfter2 = db.questionDao().getQuestionsByQuizId(targetQuizId).first()
            assertEquals("Should STILL have only 2 questions in DB", 2, questionsAfter2.size)
        }

    @Test
    fun reImportingModifiedQuestions_updatesExistingOnes() =
        runBlocking {
            // 1. Setup
            val bookId = db.bookDao().insertBook(BookEntity(externalId = "b1", title = "B1", description = ""))
            val targetQuizId = db.quizDao().insertQuiz(QuizEntity(externalId = "q1", bookId = bookId, title = "Q1", description = ""))

            val questions =
                listOf(
                    ParsedQuestion(
                        stem = "Original Text",
                        options = listOf(ParsedOption("opt_A", "A", true)),
                        correctAnswers = listOf("opt_A"),
                    ),
                )

            // 2. Initial import
            importManager.importQuestions("Title", questions, bookId, targetQuizId)

            val qBefore = db.questionDao().getQuestionsByQuizId(targetQuizId).first().first()
            assertEquals("Original Text", qBefore.text)

            // 3. Import modified question (same deterministic ID because of stable stem+options, or we use explicit ID)
            // Here, the deterministic ID will change if stem or options change.
            // Let's test explicit ID first.

            val modifiedQuestions =
                listOf(
                    ParsedQuestion(
                        stem = "Updated Text",
                        externalId = "explicit_id_1",
                        options = listOf(ParsedOption("opt_A", "A", true)),
                        correctAnswers = listOf("opt_A"),
                    ),
                )

            // Import with explicit ID
            importManager.importQuestions("Title", modifiedQuestions, bookId, targetQuizId)

            val questionsAfterExplicit = db.questionDao().getQuestionsByQuizId(targetQuizId).first()
            assertEquals(2, questionsAfterExplicit.size) // One original, one new with explicit_id_1

            // Now update the one with explicit_id_1
            val modifiedAgain =
                listOf(
                    ParsedQuestion(
                        stem = "Updated Text v2",
                        externalId = "explicit_id_1",
                        options = listOf(ParsedOption("opt_A", "A", true)),
                        correctAnswers = listOf("opt_A"),
                    ),
                )

            val result = importManager.importQuestions("Title", modifiedAgain, bookId, targetQuizId)
            assertEquals(1, result.updatedQuestionsCount)
            assertEquals(0, result.importedQuestionsCount)

            val qFinal = db.questionDao().getQuestionByExternalId(targetQuizId, "explicit_id_1")
            assertEquals("Updated Text v2", qFinal?.text)
        }
}
