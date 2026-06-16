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
class Migration22To23Test {
    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MksDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate22To23() {
        var db = helper.createDatabase(testDb, 22)

        // Insert a book in version 22
        db.execSQL(
            """
            INSERT INTO books (externalId, title, description, createdAt, updatedAt, contentUpdatedAt, lastStudiedAt, lastEditedAt, isPinned, isSystem, fields, questionCount, answeredCount, totalAttempts, completionPercentage, accuracyPercentage)
            VALUES ('book_22', 'Test Book 22', 'Desc 22', 1000, 2000, 3000, 0, 4000, 0, 0, '[]', 0, 0, 0, 0.0, 0.0)
        """,
        )

        db.close()

        // Re-open with migration to 23
        db = helper.runMigrationsAndValidate(testDb, 23, true, MksMigrations.MIGRATION_22_23)

        // Check if workspaces table exists and has a default entry
        db.query("SELECT * FROM workspaces").use { cursor ->
            assertTrue("Workspaces table should have at least one entry", cursor.moveToFirst())
            assertEquals("First workspace should be default", 1L, cursor.getLong(cursor.getColumnIndex("isDefault")))
            assertEquals("Default Workspace", cursor.getString(cursor.getColumnIndex("name")))
            assertEquals(WorkspaceDefaults.DEFAULT_EXTERNAL_ID, cursor.getString(cursor.getColumnIndex("externalId")))
        }

        // Check if books table has workspaceId and the existing book is migrated
        db.query("SELECT * FROM books").use { cursor ->
            assertTrue("Books table should have the migrated entry", cursor.moveToFirst())
            val workspaceId = cursor.getLong(cursor.getColumnIndex("workspaceId"))
            assertTrue("Migrated book should have a valid workspaceId", workspaceId > 0)
            assertEquals("book_22", cursor.getString(cursor.getColumnIndex("externalId")))
            assertEquals("Test Book 22", cursor.getString(cursor.getColumnIndex("title")))
        }

        // Check if workspace_settings table exists
        db.query("PRAGMA table_info(workspace_settings)").use { cursor ->
            assertTrue("workspace_settings table should exist", cursor.count > 0)
        }
    }
}
