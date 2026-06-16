package com.ahmedyejam.mks.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration16To17Test {
    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(TEST_DB)
        helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(TEST_DB)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(16) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(
                                    """
                                    CREATE TABLE IF NOT EXISTS flashcards (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                        sourceQuestionId INTEGER
                                    )
                                    """.trimIndent(),
                                )
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) = Unit
                        },
                    )
                    .build(),
            )
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        helper.close()
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(TEST_DB)
    }

    @Test
    fun migration16To17CreatesCategoryAndAssetTablesAndFlashcardIndex() {
        MksDatabase.MIGRATION_16_17.migrate(db)

        assertTableExists("question_categories")
        assertTableExists("asset_references")
        assertIndexExists("index_flashcards_sourceQuestionId")
        assertIndexExists("index_question_categories_category")
        assertIndexExists("index_asset_references_ownerType_ownerId_path")
    }

    private fun assertTableExists(name: String) {
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(name)).use { cursor ->
            assertTrue("Expected table $name", cursor.moveToFirst())
        }
    }

    private fun assertIndexExists(name: String) {
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?", arrayOf(name)).use { cursor ->
            assertTrue("Expected index $name", cursor.moveToFirst())
        }
    }

    private companion object {
        const val TEST_DB = "migration-16-17-test.db"
    }
}
