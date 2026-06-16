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
class Migration25To26Test {
    private val testDb = "migration-25-26-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MksDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate25To26_createsAnnotationsTableAndPreservesExistingRows() {
        var db = helper.createDatabase(testDb, 25)
        db.execSQL(
            """
            INSERT INTO workspaces (externalId, name, description, isDefault, createdAt, updatedAt, deletedAt)
            VALUES ('workspace-test', 'Workspace', NULL, 1, 1000, 1000, NULL)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO books (workspaceId, externalId, title, description, iconName, coverImage, createdAt, updatedAt, contentUpdatedAt, lastStudiedAt, lastEditedAt, isPinned, isSystem, fields, questionCount, answeredCount, totalAttempts, completionPercentage, accuracyPercentage, deletedAt)
            VALUES (1, 'book-test', 'Book', '', NULL, NULL, 1000, 1000, 1000, 0, 1000, 0, 0, '[]', 0, 0, 0, 0.0, 0.0, NULL)
            """.trimIndent(),
        )
        db.close()

        db = helper.runMigrationsAndValidate(testDb, 26, true, MksMigrations.MIGRATION_25_26)

        db.execSQL(
            """
            INSERT INTO annotations (workspaceId, bookId, ownerType, ownerId, selectedText, noteBody, colorLabel, positionDataJson, createdAt, updatedAt, deletedAt)
            VALUES (1, 1, 'QUESTION', 99, 'selected', 'margin note', 'YELLOW', NULL, 2000, 2000, NULL)
            """.trimIndent(),
        )

        db.query("SELECT COUNT(*) FROM annotations WHERE deletedAt IS NULL").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM books WHERE deletedAt IS NULL").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }
}
