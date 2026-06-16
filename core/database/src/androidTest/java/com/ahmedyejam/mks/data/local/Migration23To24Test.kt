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
class Migration23To24Test {
    private val testDb = "migration-23-24-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MksDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate23To24_stabilizesDefaultWorkspaceAndRepairsBookOwnership() {
        var db = helper.createDatabase(testDb, 23)

        db.execSQL(
            """
            INSERT INTO workspaces (externalId, name, description, isDefault, createdAt, updatedAt, deletedAt)
            VALUES ('legacy-generated-id', 'Default Workspace', 'Auto-generated default workspace', 1, 1000, 1000, NULL)
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO books (workspaceId, externalId, title, description, iconName, coverImage, createdAt, updatedAt, contentUpdatedAt, lastStudiedAt, lastEditedAt, isPinned, isSystem, fields, questionCount, answeredCount, totalAttempts, completionPercentage, accuracyPercentage)
            VALUES (999, 'book_legacy', 'Legacy Book', '', NULL, NULL, 1000, 1000, 1000, 0, 1000, 0, 0, '[]', 0, 0, 0, 0.0, 0.0)
            """.trimIndent(),
        )
        db.close()

        db = helper.runMigrationsAndValidate(testDb, 24, true, MksMigrations.MIGRATION_23_24)

        val workspaceId =
            db.query(
                "SELECT id FROM workspaces WHERE externalId = '${WorkspaceDefaults.DEFAULT_EXTERNAL_ID}' " +
                    "AND isDefault = 1 AND deletedAt IS NULL",
            ).use { cursor ->
                assertTrue("Stable default workspace should exist", cursor.moveToFirst())
                cursor.getLong(0)
            }

        db.query("SELECT COUNT(*) FROM workspaces WHERE isDefault = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        db.query("SELECT workspaceId FROM books WHERE externalId = 'book_legacy'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(workspaceId, cursor.getLong(0))
        }

        db.query("SELECT COUNT(*) FROM workspace_settings WHERE workspaceId = $workspaceId").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }
}
