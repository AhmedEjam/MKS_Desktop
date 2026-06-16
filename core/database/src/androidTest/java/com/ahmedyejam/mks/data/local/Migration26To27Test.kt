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
class Migration26To27Test {
    private val testDb = "migration-26-27-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MksDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate26To27_verifiesNewColumnsAndTables() {
        var db = helper.createDatabase(testDb, 26)
        
        // Seed data for migration
        db.execSQL("INSERT INTO workspaces (externalId, name, isDefault, createdAt, updatedAt) VALUES ('w1', 'W1', 1, 1000, 1000)")
        db.execSQL("INSERT INTO books (workspaceId, externalId, title, description, createdAt, updatedAt, contentUpdatedAt, lastStudiedAt, lastEditedAt, isPinned, isSystem, fields, questionCount, answeredCount, totalAttempts, completionPercentage, accuracyPercentage) VALUES (1, 'b1', 'B1', '', 1000, 1000, 1000, 0, 1000, 0, 0, '[]', 0, 0, 0, 0.0, 0.0)")
        db.execSQL("INSERT INTO note_blueprints (externalId, bookId, title, summary, body, bulletPoints, tags, blueprintMode, linkedQuestionsJson, linkedAssetsJson, reviewStatus, reviewCount, lastReviewedAt, createdAt, updatedAt, sourceQuestionId) VALUES ('nb1', 1, 'Note', 'Summary', 'Body', '[]', '[]', 'SIMPLE_NOTE', '[]', '[]', 'NEW', 0, 0, 1000, 1000, NULL)")
        
        db.close()

        db = helper.runMigrationsAndValidate(testDb, 27, true, MksMigrations.MIGRATION_26_27)

        // Verify new columns in quizzes
        db.query("PRAGMA table_info(quizzes)").use { cursor ->
            var hasTags = false
            var hasIconName = false
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (name == "tags") hasTags = true
                if (name == "iconName") hasIconName = true
            }
            assertTrue("quizzes should have tags column", hasTags)
            assertTrue("quizzes should have iconName column", hasIconName)
        }

        // Verify note_collections table
        db.query("SELECT COUNT(*) FROM note_collections").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Default collection should be created", 1, cursor.getInt(0))
        }

        // Verify note_blueprints migration
        db.query("SELECT collectionId FROM note_blueprints LIMIT 1").use { cursor ->
            assertTrue("note_blueprints should have collectionId", cursor.moveToFirst())
            assertEquals("Blueprint should be linked to the new collection", 1L, cursor.getLong(0))
        }

        // Verify study_sessions table
        db.query("PRAGMA table_info(study_sessions)").use { cursor ->
            assertTrue("study_sessions table should exist", cursor.count > 0)
        }
    }
}
