package com.ahmedyejam.mks.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration27To28Test {
    private val testDb = "migration-27-28-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MksDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate27To28_verifiesSourceDocumentAssetsTable() {
        helper.createDatabase(testDb, 27).close()

        val db = helper.runMigrationsAndValidate(testDb, 28, true, MksMigrations.MIGRATION_27_28)

        db.query("PRAGMA table_info(source_document_assets)").use { cursor ->
            assertTrue("source_document_assets table should exist", cursor.count > 0)
            
            val columns = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            assertTrue(columns.contains("sourceDocumentId"))
            assertTrue(columns.contains("assetType"))
            assertTrue(columns.contains("localPath"))
        }
    }
}
