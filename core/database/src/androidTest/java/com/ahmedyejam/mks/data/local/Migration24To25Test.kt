package com.ahmedyejam.mks.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration24To25Test {
    private val testDb = "migration-24-25-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MksDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate24To25_addsSoftDeleteColumnsAndKeepsRowsActive() {
        var db = helper.createDatabase(testDb, 24)
        db.execSQL(
            """
            INSERT INTO workspaces (externalId, name, description, isDefault, createdAt, updatedAt, deletedAt)
            VALUES ('workspace-test', 'Workspace', NULL, 1, 1000, 1000, NULL)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO books (workspaceId, externalId, title, description, iconName, coverImage, createdAt, updatedAt, contentUpdatedAt, lastStudiedAt, lastEditedAt, isPinned, isSystem, fields, questionCount, answeredCount, totalAttempts, completionPercentage, accuracyPercentage)
            VALUES (1, 'book-test', 'Book', '', NULL, NULL, 1000, 1000, 1000, 0, 1000, 0, 0, '[]', 0, 0, 0, 0.0, 0.0)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO quizzes (externalId, bookId, title, description, category, iconName, coverImage, createdAt, updatedAt, contentUpdatedAt, lastStudiedAt, lastEditedAt, isPinned, isSystem, questionCount, answeredCount, totalAttempts, completionPercentage, accuracyPercentage)
            VALUES ('quiz-test', 1, 'Quiz', '', NULL, NULL, NULL, 1000, 1000, 1000, 0, 1000, 0, 0, 0, 0, 0, 0.0, 0.0)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO questions (externalId, quizId, text, type, options, correctAnswers, explanation, hint, reference, weight, imagePath, imageName, imageSource, attempts, correctCount, isDropped, droppedAt, droppedReason, isMarked, markedAt, markReason, markReviewAt, notes, categories, additionalInfo, sourceBookId, sourceQuizId, sourceQuestionId, createdAt, updatedAt, lastStudiedAt, lastEditedAt, timeSpentMs, lastAttemptResult, consecutiveCorrect)
            VALUES ('question-test', 1, 'Question?', 'SINGLE_CHOICE', '["A","B"]', '[0]', NULL, NULL, NULL, 1, NULL, NULL, NULL, 0, 0, 0, NULL, NULL, 0, NULL, NULL, NULL, NULL, '[]', NULL, NULL, NULL, NULL, 1000, 1000, 0, 1000, 0, NULL, 0)
            """.trimIndent(),
        )
        db.close()

        db = helper.runMigrationsAndValidate(testDb, 25, true, MksMigrations.MIGRATION_24_25)

        db.query("SELECT COUNT(*) FROM books WHERE deletedAt IS NULL").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM quizzes WHERE deletedAt IS NULL").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM questions WHERE deletedAt IS NULL").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }
}
