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
class Migration29To30Test {
    private val testDb = "migration-29-30-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MksDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate29To30_verifiesResultTaxonomyColumn() {
        helper.createDatabase(testDb, 29).close()

        val db = helper.runMigrationsAndValidate(testDb, 30, true, MksMigrations.MIGRATION_29_30)

        db.query("PRAGMA table_info(sessions)").use { cursor ->
            var hasResultTaxonomy = false
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "resultTaxonomy") {
                    hasResultTaxonomy = true
                }
            }
            assertTrue("sessions should have resultTaxonomy column", hasResultTaxonomy)
        }
    }
}
