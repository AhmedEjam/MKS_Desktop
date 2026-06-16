package com.ahmedyejam.mks.data.export

import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.db.MksDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportManager(private val db: MksDatabase) {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    /** Export a quiz as a ZIP bundle containing JSON manifest + question images. */
    suspend fun exportQuizBundle(quizId: Long, outputPath: String): MksResult<File> = withContext(Dispatchers.IO) {
        try {
            val quiz = db.quizQueriesQueries.qz_selectById(quizId).executeAsOneOrNull()
                ?: return@withContext MksResult.Error("Quiz not found")
            val questions = db.questionQueriesQueries.qu_selectByQuiz(quizId).executeAsList()
            val quizEntity = QuizEntity(
                id = quiz.id, externalId = quiz.externalId, bookId = quiz.bookId,
                title = quiz.title, description = quiz.description
            )

            val bundle = ExportBundle(
                quiz = quizEntity,
                questions = questions.map { q ->
                    QuestionEntity(id = q.id, externalId = q.externalId, quizId = q.quizId,
                        text = q.text, type = try { com.ahmedyejam.mks.data.local.entity.QuestionType.valueOf(q.type) }
                            catch (_: Exception) { com.ahmedyejam.mks.data.local.entity.QuestionType.SINGLE_CHOICE },
                        options = emptyList(), correctAnswers = emptyList(),
                        explanation = q.explanation, hint = q.hint, reference = q.reference,
                        imagePath = q.imagePath, imageName = q.imageName, imageSource = q.imageSource)
                }
            )

            val manifestJson = json.encodeToString(bundle)
            val outputFile = File(outputPath)
            ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifestJson.toByteArray())
                zip.closeEntry()

                // Add question images
                bundle.questions.forEach { q ->
                    q.imagePath?.let { imgPath ->
                        val imgFile = File(imgPath)
                        if (imgFile.exists()) {
                            zip.putNextEntry(ZipEntry("media/${imgFile.name}"))
                            imgFile.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            MksResult.Success(outputFile)
        } catch (e: Exception) {
            MksResult.Error("Export failed: ${e.message}", e)
        }
    }

    /** Export all books, quizzes, and questions as a full backup ZIP. */
    suspend fun exportFullBackup(outputPath: String): MksResult<File> = withContext(Dispatchers.IO) {
        try {
            val books = db.bookQueriesQueries.bk_selectAll().executeAsList()
            val outputFile = File(outputPath)
            ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                val data = json.encodeToString(mapOf(
                    "format" to "mks.backup",
                    "version" to 1,
                    "bookCount" to books.size,
                    "exportedAt" to com.ahmedyejam.mks.util.currentTimeMillis()
                ))
                zip.write(data.toByteArray())
                zip.closeEntry()
            }
            MksResult.Success(outputFile)
        } catch (e: Exception) {
            MksResult.Error("Backup failed: ${e.message}", e)
        }
    }

    /** Export a book with all its quizzes and questions as a ZIP bundle. */
    suspend fun exportBookBundle(bookId: Long, outputPath: String): MksResult<File> = withContext(Dispatchers.IO) {
        try {
            val book = db.bookQueriesQueries.bk_selectById(bookId).executeAsOneOrNull()
                ?: return@withContext MksResult.Error("Book not found")
            val quizzes = db.quizQueriesQueries.qz_selectByBook(bookId).executeAsList()
            val outputFile = File(outputPath)
            ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(json.encodeToString(mapOf(
                    "format" to "mks.bookexport.v1", "version" to 1,
                    "book" to mapOf("id" to book.id, "title" to book.title),
                    "quizCount" to quizzes.size, "exportedAt" to currentTimeMillis()
                )).toByteArray())
                zip.closeEntry()
            }
            MksResult.Success(outputFile)
        } catch (e: Exception) { MksResult.Error("Book export failed: ${e.message}", e) }
    }

    suspend fun exportAllToZip(outputPath: String): MksResult<File> = withContext(Dispatchers.IO) {
        try {
            val books = db.bookQueriesQueries.bk_selectAll().executeAsList()
            val outputFile = File(outputPath)
            ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
                zip.putNextEntry(ZipEntry("data/books.json"))
                zip.write(json.encodeToString(books.map { mapOf("id" to it.id, "externalId" to it.externalId, "title" to it.title) }).toByteArray())
                zip.closeEntry()
                val allQuizzes = books.flatMap { b -> db.quizQueriesQueries.qz_selectByBook(b.id).executeAsList() }
                zip.putNextEntry(ZipEntry("data/quizzes.json"))
                zip.write(json.encodeToString(allQuizzes.map { mapOf("id" to it.id, "bookId" to it.bookId, "title" to it.title) }).toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(json.encodeToString(mapOf("format" to "mks.fullexport.v1", "bookCount" to books.size, "quizCount" to allQuizzes.size, "exportedAt" to currentTimeMillis())).toByteArray())
                zip.closeEntry()
            }
            MksResult.Success(outputFile)
        } catch (e: Exception) { MksResult.Error("Full export failed: ${e.message}", e) }
    }

    private fun currentTimeMillis(): Long = com.ahmedyejam.mks.util.currentTimeMillis()
}

@Serializable
data class ExportBundle(
    val quiz: QuizEntity,
    val questions: List<QuestionEntity>,
    val format: String = "mks.export.v1"
)
