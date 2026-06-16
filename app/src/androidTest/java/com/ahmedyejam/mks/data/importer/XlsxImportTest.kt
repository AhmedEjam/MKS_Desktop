package com.ahmedyejam.mks.data.importer

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ahmedyejam.mks.data.importer.model.ImportFormat
import com.ahmedyejam.mks.data.importer.repository.ImportLibraryManager
import com.ahmedyejam.mks.data.local.FileManager
import com.ahmedyejam.mks.data.local.MksDatabase
import com.ahmedyejam.mks.data.local.entity.QuestionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class XlsxImportTest {
    private lateinit var db: MksDatabase
    private lateinit var importManager: ImportLibraryManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MksDatabase::class.java).build()
        importManager = ImportLibraryManager(context, db, FileManager(context))
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun import_xlsxFixture_createsExpectedQuizAndQuestions() =
        runBlocking {
            val uri = copyFixtureToCache("sample_import.xlsx")

            val result = importManager.import(uri)

            assertTrue("XLSX import should succeed", result.success)
            assertEquals(ImportFormat.XLSX, result.detectedFormat)
            assertEquals(1, result.importedBooksCount)
            assertEquals(1, result.importedQuizzesCount)
            assertEquals(2, result.importedQuestionsCount)

            val books = db.bookDao().getAllBooksFlow().first()
            assertEquals(1, books.size)
            assertEquals("sample_import.xlsx", books.first().title)

            val quizzes = db.quizDao().getAllQuizzesFlow().first()
            assertEquals(1, quizzes.size)
            assertEquals("sample_import.xlsx", quizzes.first().title)

            val questions =
                db.questionDao().getQuestionsByQuizId(quizzes.first().id).first()
                    .sortedBy { it.text }
            assertEquals(2, questions.size)

            val capitalQuestion = questions.first { it.text == "Capital of France?" }
            assertEquals(QuestionType.SINGLE_CHOICE, capitalQuestion.type)
            assertEquals(listOf("Paris", "London", "Rome"), capitalQuestion.options)
            assertEquals(listOf(0), capitalQuestion.correctAnswers)
            assertEquals(listOf("geo", "europe"), capitalQuestion.categories)

            val primesQuestion = questions.first { it.text == "Select prime numbers" }
            assertEquals(QuestionType.MULTIPLE_CHOICE, primesQuestion.type)
            assertEquals(listOf("2", "4", "5"), primesQuestion.options)
            assertEquals(listOf(0, 2), primesQuestion.correctAnswers)
            assertEquals(listOf("math"), primesQuestion.categories)
        }

    private fun copyFixtureToCache(assetName: String): Uri {
        val fixture = File(context.cacheDir, assetName)
        InstrumentationRegistry.getInstrumentation().context.assets.open(assetName).use { input ->
            fixture.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return Uri.fromFile(fixture)
    }
}
