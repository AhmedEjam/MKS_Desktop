package com.ahmedyejam.mks.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration28To29Test {
    private val testDb = "migration-28-29-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MksDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate28To29_verifiesTableDropAndIndexes() {
        helper.createDatabase(testDb, 28).close()

        val db = helper.runMigrationsAndValidate(testDb, 29, true, MksMigrations.MIGRATION_28_29)

        // Verify table drop
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='source_document_assets'").use { cursor ->
            assertFalse("source_document_assets table should be dropped", cursor.moveToFirst())
        }

        // Verify indexes
        db.query("PRAGMA index_list(books)").use { cursor ->
            var hasUniqueExternalId = false
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val unique = cursor.getInt(cursor.getColumnIndexOrThrow("unique"))
                if (name == "index_books_externalId" && unique == 1) hasUniqueExternalId = true
            }
            assertTrue("books should have unique index on externalId", hasUniqueExternalId)
        }

        db.query("PRAGMA index_list(quizzes)").use { cursor ->
            var hasUniqueExternalId = false
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val unique = cursor.getInt(cursor.getColumnIndexOrThrow("unique"))
                if (name == "index_quizzes_externalId" && unique == 1) hasUniqueExternalId = true
            }
            assertTrue("quizzes should have unique index on externalId", hasUniqueExternalId)
        }
    }
}
