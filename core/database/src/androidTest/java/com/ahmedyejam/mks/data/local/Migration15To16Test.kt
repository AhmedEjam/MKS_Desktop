package com.ahmedyejam.mks.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration15To16Test {
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
                        object : SupportSQLiteOpenHelper.Callback(15) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                createMinimalVersion15Schema(db)
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
    fun migration15To16CreatesKnowledgeBankTablesAndColumnsOnce() {
        MksDatabase.MIGRATION_15_16.migrate(db)

        assertColumnExists("flashcards", "sourceQuestionId")
        assertColumnExists("flashcards", "syncConfig")

        assertTableExists("slideshow_courses")
        assertColumnExists("slideshow_courses", "isPinned")
        assertColumnCount("slideshow_courses", "isPinned", 1)

        assertTableExists("course_slides")
        assertTableExists("note_blueprints")
        assertTableExists("prompts")

        assertIndexExists("index_slideshow_courses_bookId")
        assertIndexExists("index_course_slides_sourceQuestionId")
        assertIndexExists("index_note_blueprints_sourceQuestionId")
        assertIndexExists("index_prompts_bookId")
    }

    @Test
    fun migration15To17PathCompletesWithV17TablesAndIndexes() {
        MksDatabase.MIGRATION_15_16.migrate(db)
        MksDatabase.MIGRATION_16_17.migrate(db)

        assertTableExists("question_categories")
        assertTableExists("asset_references")
        assertIndexExists("index_flashcards_sourceQuestionId")
        assertIndexExists("index_question_categories_questionId")
        assertIndexExists("index_asset_references_ownerType_ownerId_path")
    }

    private fun createMinimalVersion15Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS books (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                externalId TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                fields TEXT NOT NULL DEFAULT '[]',
                questionCount INTEGER NOT NULL DEFAULT 0,
                answeredCount INTEGER NOT NULL DEFAULT 0,
                completionPercentage REAL NOT NULL DEFAULT 0.0,
                accuracyPercentage REAL NOT NULL DEFAULT 0.0,
                totalAttempts INTEGER NOT NULL DEFAULT 0,
                isSystem INTEGER NOT NULL DEFAULT 0,
                isPinned INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                lastEditedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quizzes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                externalId TEXT NOT NULL,
                bookId INTEGER NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                category TEXT,
                questionCount INTEGER NOT NULL DEFAULT 0,
                answeredCount INTEGER NOT NULL DEFAULT 0,
                completionPercentage REAL NOT NULL DEFAULT 0.0,
                accuracyPercentage REAL NOT NULL DEFAULT 0.0,
                isSystem INTEGER NOT NULL DEFAULT 0,
                isPinned INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS questions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                externalId TEXT NOT NULL,
                quizId INTEGER NOT NULL,
                text TEXT NOT NULL,
                type TEXT NOT NULL,
                options TEXT NOT NULL,
                correctAnswers TEXT NOT NULL,
                explanation TEXT,
                hint TEXT,
                imagePath TEXT,
                categories TEXT NOT NULL DEFAULT '[]',
                attempts INTEGER NOT NULL DEFAULT 0,
                correctCount INTEGER NOT NULL DEFAULT 0,
                weight REAL NOT NULL DEFAULT 1.0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(quizId) REFERENCES quizzes(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS flashcard_decks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                externalId TEXT NOT NULL,
                bookId INTEGER NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                iconName TEXT,
                coverImage TEXT,
                cardCount INTEGER NOT NULL DEFAULT 0,
                studiedCount INTEGER NOT NULL DEFAULT 0,
                masteryPercentage REAL NOT NULL DEFAULT 0.0,
                isSystem INTEGER NOT NULL DEFAULT 0,
                isPinned INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                lastStudiedAt INTEGER NOT NULL DEFAULT 0,
                lastEditedAt INTEGER NOT NULL,
                FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS flashcards (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                externalId TEXT NOT NULL,
                deckId INTEGER NOT NULL,
                frontText TEXT NOT NULL,
                backText TEXT NOT NULL,
                hint TEXT,
                imagePath TEXT,
                tags TEXT NOT NULL DEFAULT '[]',
                orderIndex INTEGER NOT NULL DEFAULT 0,
                attempts INTEGER NOT NULL DEFAULT 0,
                correctCount INTEGER NOT NULL DEFAULT 0,
                difficulty TEXT,
                lastReviewedAt INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(deckId) REFERENCES flashcard_decks(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
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

    private fun assertColumnExists(
        tableName: String,
        columnName: String,
    ) {
        assertTrue("Expected column $tableName.$columnName", columnCount(tableName, columnName) >= 1)
    }

    private fun assertColumnCount(
        tableName: String,
        columnName: String,
        expectedCount: Int,
    ) {
        assertEquals("Unexpected duplicate count for $tableName.$columnName", expectedCount, columnCount(tableName, columnName))
    }

    private fun columnCount(
        tableName: String,
        columnName: String,
    ): Int {
        var count = 0
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) {
                    count++
                }
            }
        }
        return count
    }

    private companion object {
        const val TEST_DB = "migration-15-16-test.db"
    }
}
