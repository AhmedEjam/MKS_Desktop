package com.ahmedyejam.mks.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ahmedyejam.mks.data.local.WorkspaceDefaults.DEFAULT_DESCRIPTION
import com.ahmedyejam.mks.data.local.WorkspaceDefaults.DEFAULT_EXTERNAL_ID
import com.ahmedyejam.mks.data.local.WorkspaceDefaults.DEFAULT_NAME

object MksMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "books", "externalId", "TEXT NOT NULL DEFAULT ''")
            MksDatabase.addColumnIfMissing(db, "books", "updatedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "books", "contentUpdatedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "books", "lastStudiedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "books", "coverImage", "TEXT")
            MksDatabase.addColumnIfMissing(db, "quizzes", "externalId", "TEXT NOT NULL DEFAULT ''")
            MksDatabase.addColumnIfMissing(db, "quizzes", "updatedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "quizzes", "contentUpdatedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "quizzes", "lastStudiedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "quizzes", "coverImage", "TEXT")
            MksDatabase.addColumnIfMissing(db, "questions", "externalId", "TEXT NOT NULL DEFAULT ''")
            MksDatabase.addColumnIfMissing(db, "questions", "reference", "TEXT")
            MksDatabase.addColumnIfMissing(db, "questions", "imageName", "TEXT")
            MksDatabase.addColumnIfMissing(db, "questions", "imageSource", "TEXT")
            MksDatabase.addColumnIfMissing(db, "questions", "additionalInfo", "TEXT")
            db.execSQL("UPDATE books SET externalId = 'book_' || id")
            db.execSQL("UPDATE quizzes SET externalId = 'quiz_' || id")
            db.execSQL("UPDATE questions SET externalId = 'q_' || id")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `category_metadata` (`name` TEXT NOT NULL, `emoji` TEXT, `color` INTEGER, `isPinned` INTEGER NOT NULL, PRIMARY KEY(`name`))")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "books", "isPinned", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "books", "fields", "TEXT NOT NULL DEFAULT '[]'")
            MksDatabase.addColumnIfMissing(db, "quizzes", "isPinned", "INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "sessions", "shuffleQuestions", "INTEGER NOT NULL DEFAULT 1")
            MksDatabase.addColumnIfMissing(db, "sessions", "shuffleOptions", "INTEGER NOT NULL DEFAULT 1")
            MksDatabase.addColumnIfMissing(db, "sessions", "rapidMode", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "sessions", "repeatWrong", "INTEGER NOT NULL DEFAULT 1")
            MksDatabase.addColumnIfMissing(db, "sessions", "quizTimerSeconds", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "sessions", "questionTimerSeconds", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "sessions", "rangeFrom", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "sessions", "rangeTo", "INTEGER NOT NULL DEFAULT -1")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "sessions", "includeFilters", "TEXT NOT NULL DEFAULT '[]'")
            MksDatabase.addColumnIfMissing(db, "sessions", "sessionNotes", "TEXT NOT NULL DEFAULT '{}'")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "sessions", "lastModifiedAt", "INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE sessions SET lastModifiedAt = updatedAt")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "books", "lastEditedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "books", "isSystem", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "books", "questionCount", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "books", "completionPercentage", "REAL NOT NULL DEFAULT 0.0")
            MksDatabase.addColumnIfMissing(db, "quizzes", "lastEditedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "quizzes", "isSystem", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "quizzes", "questionCount", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "quizzes", "completionPercentage", "REAL NOT NULL DEFAULT 0.0")
            MksDatabase.addColumnIfMissing(db, "questions", "imagePath", "TEXT")
            MksDatabase.addColumnIfMissing(db, "questions", "attempts", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "questions", "correctCount", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "questions", "isDropped", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "questions", "categories", "TEXT NOT NULL DEFAULT '[]'")
            MksDatabase.addColumnIfMissing(db, "questions", "createdAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "questions", "updatedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "questions", "lastStudiedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "questions", "lastEditedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "sessions", "lastStudiedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "sessions", "lastEditedAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "sessions", "droppedOptions", "TEXT NOT NULL DEFAULT '{}'")
            MksDatabase.addColumnIfMissing(db, "sessions", "visibleOptionsCount", "TEXT NOT NULL DEFAULT '{}'")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "questions", "sourceBookId", "TEXT")
            MksDatabase.addColumnIfMissing(db, "questions", "sourceQuizId", "TEXT")
            MksDatabase.addColumnIfMissing(db, "questions", "sourceQuestionId", "TEXT")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "sessions", "currentStreak", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "sessions", "maxStreak", "INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "sessions", "questionIds", "TEXT NOT NULL DEFAULT '[]'")
            MksDatabase.addColumnIfMissing(db, "sessions", "originalQuestionCount", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "sessions", "answersByIndex", "TEXT NOT NULL DEFAULT '{}'")
            MksDatabase.addColumnIfMissing(db, "sessions", "droppedOptionsByIndex", "TEXT NOT NULL DEFAULT '{}'")
            MksDatabase.addColumnIfMissing(db, "sessions", "visibleOptionsCountByIndex", "TEXT NOT NULL DEFAULT '{}'")
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "books", "answeredCount", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "books", "totalAttempts", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "books", "accuracyPercentage", "REAL NOT NULL DEFAULT 0.0")
            MksDatabase.addColumnIfMissing(db, "quizzes", "answeredCount", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "quizzes", "totalAttempts", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "quizzes", "accuracyPercentage", "REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "questions", "timeSpentMs", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "questions", "lastAttemptResult", "INTEGER")
            MksDatabase.addColumnIfMissing(db, "questions", "consecutiveCorrect", "INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try { MksDatabase.addColumnIfMissing(db, "questions", "isMarked", "INTEGER NOT NULL DEFAULT 0") } catch (e: Exception) {}
            try { MksDatabase.addColumnIfMissing(db, "questions", "notes", "TEXT") } catch (e: Exception) {}
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
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
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_flashcard_decks_bookId ON flashcard_decks(bookId)")
            db.execSQL("""
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
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_flashcards_deckId ON flashcards(deckId)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS learning_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    deckId INTEGER NOT NULL,
                    label TEXT,
                    stateJson TEXT NOT NULL,
                    isCompleted INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(deckId) REFERENCES flashcard_decks(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_sessions_deckId ON learning_sessions(deckId)")
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "flashcards", "sourceQuestionId", "INTEGER")
            MksDatabase.addColumnIfMissing(db, "flashcards", "syncConfig", "TEXT NOT NULL DEFAULT '{}'")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS slideshow_courses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    externalId TEXT NOT NULL,
                    bookId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    coverImage TEXT,
                    slideCount INTEGER NOT NULL DEFAULT 0,
                    studiedSlideCount INTEGER NOT NULL DEFAULT 0,
                    progress REAL NOT NULL DEFAULT 0.0,
                    isSystem INTEGER NOT NULL DEFAULT 0,
                    isPinned INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    lastStudiedAt INTEGER NOT NULL DEFAULT 0,
                    lastEditedAt INTEGER NOT NULL,
                    isDerived INTEGER NOT NULL DEFAULT 0,
                    sourceQuizId INTEGER,
                    FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_slideshow_courses_bookId ON slideshow_courses(bookId)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS course_slides (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    externalId TEXT NOT NULL,
                    courseId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    body TEXT NOT NULL,
                    speakerNotes TEXT,
                    imagePath TEXT,
                    orderIndex INTEGER NOT NULL DEFAULT 0,
                    isCompleted INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    sourceQuestionId INTEGER,
                    syncConfig TEXT NOT NULL DEFAULT '{}',
                    FOREIGN KEY(courseId) REFERENCES slideshow_courses(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_course_slides_courseId ON course_slides(courseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_course_slides_sourceQuestionId ON course_slides(sourceQuestionId)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS note_blueprints (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    externalId TEXT NOT NULL,
                    bookId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    summary TEXT,
                    body TEXT NOT NULL,
                    bulletPoints TEXT NOT NULL DEFAULT '[]',
                    tags TEXT NOT NULL DEFAULT '[]',
                    reviewCount INTEGER NOT NULL DEFAULT 0,
                    lastReviewedAt INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    sourceQuestionId INTEGER,
                    FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_note_blueprints_bookId ON note_blueprints(bookId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_note_blueprints_sourceQuestionId ON note_blueprints(sourceQuestionId)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS prompts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    externalId TEXT NOT NULL,
                    bookId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    stem TEXT NOT NULL,
                    conversationLinks TEXT NOT NULL DEFAULT '[]',
                    usageCount INTEGER NOT NULL DEFAULT 0,
                    lastUsedAt INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prompts_bookId ON prompts(bookId)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS knowledge_study_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    targetType TEXT NOT NULL,
                    targetId INTEGER NOT NULL,
                    stateJson TEXT NOT NULL,
                    isCompleted INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_flashcards_sourceQuestionId ON flashcards(sourceQuestionId)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS question_categories (
                    questionId INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    PRIMARY KEY(questionId, category),
                    FOREIGN KEY(questionId) REFERENCES questions(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_question_categories_questionId ON question_categories(questionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_question_categories_category ON question_categories(category)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS asset_references (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    path TEXT NOT NULL,
                    ownerType TEXT NOT NULL,
                    ownerId INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_asset_references_path ON asset_references(path)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_asset_references_ownerType_ownerId ON asset_references(ownerType, ownerId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_asset_references_ownerType_ownerId_path ON asset_references(ownerType, ownerId, path)")
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS question_assets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    bookId INTEGER NOT NULL,
                    quizId INTEGER NOT NULL,
                    questionId INTEGER NOT NULL,
                    assetType TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    localPath TEXT,
                    externalUrl TEXT,
                    mimeType TEXT,
                    fileName TEXT,
                    fileSizeBytes INTEGER,
                    textContent TEXT,
                    sourceDocumentId INTEGER,
                    sourcePage TEXT,
                    sourceQuote TEXT,
                    sortOrder INTEGER NOT NULL DEFAULT 0,
                    isPinned INTEGER NOT NULL DEFAULT 0,
                    isPrimary INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(questionId) REFERENCES questions(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_question_assets_bookId ON question_assets(bookId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_question_assets_quizId ON question_assets(quizId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_question_assets_questionId ON question_assets(questionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_question_assets_assetType ON question_assets(assetType)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_question_assets_createdAt ON question_assets(createdAt)")
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS source_documents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    bookId INTEGER,
                    title TEXT NOT NULL,
                    sourceType TEXT NOT NULL,
                    author TEXT,
                    edition TEXT,
                    year TEXT,
                    publisher TEXT,
                    localPath TEXT,
                    externalUrl TEXT,
                    description TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_source_documents_bookId ON source_documents(bookId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_source_documents_title ON source_documents(title)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_source_documents_sourceType ON source_documents(sourceType)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_question_assets_sourceDocumentId ON question_assets(sourceDocumentId)")
            MksDatabase.addColumnIfMissing(db, "note_blueprints", "blueprintMode", "TEXT NOT NULL DEFAULT 'SIMPLE_NOTE'")
            MksDatabase.addColumnIfMissing(db, "note_blueprints", "linkedQuestionsJson", "TEXT NOT NULL DEFAULT '[]'")
            MksDatabase.addColumnIfMissing(db, "note_blueprints", "linkedAssetsJson", "TEXT NOT NULL DEFAULT '[]'")
            MksDatabase.addColumnIfMissing(db, "note_blueprints", "reviewStatus", "TEXT NOT NULL DEFAULT 'NEW'")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_note_blueprints_blueprintMode ON note_blueprints(blueprintMode)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_note_blueprints_reviewStatus ON note_blueprints(reviewStatus)")
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS prompt_decks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    bookId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    tagsJson TEXT,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prompt_decks_bookId ON prompt_decks(bookId)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS prompt_cards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    deckId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    promptText TEXT NOT NULL,
                    variablesJson TEXT,
                    outputType TEXT NOT NULL,
                    usageCount INTEGER NOT NULL DEFAULT 0,
                    lastUsedAt INTEGER,
                    sortOrder INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(deckId) REFERENCES prompt_decks(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prompt_cards_deckId ON prompt_cards(deckId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prompt_cards_outputType ON prompt_cards(outputType)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS prompt_runs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    promptCardId INTEGER NOT NULL,
                    inputValuesJson TEXT NOT NULL,
                    renderedPrompt TEXT NOT NULL,
                    outputText TEXT,
                    linkedAssetType TEXT,
                    linkedAssetId INTEGER,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(promptCardId) REFERENCES prompt_cards(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prompt_runs_promptCardId ON prompt_runs(promptCardId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prompt_runs_createdAt ON prompt_runs(createdAt)")
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MksDatabase.addColumnIfMissing(db, "questions", "droppedAt", "INTEGER")
            MksDatabase.addColumnIfMissing(db, "questions", "droppedReason", "TEXT")
            MksDatabase.addColumnIfMissing(db, "questions", "markedAt", "INTEGER")
            MksDatabase.addColumnIfMissing(db, "questions", "markReason", "TEXT")
            MksDatabase.addColumnIfMissing(db, "questions", "markReviewAt", "INTEGER")
            MksDatabase.addColumnIfMissing(db, "flashcards", "dueAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(db, "flashcards", "reviewCount", "INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_questions_isMarked ON questions(isMarked)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_questions_isDropped ON questions(isDropped)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_questions_markReviewAt ON questions(markReviewAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_flashcards_dueAt ON flashcards(dueAt)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS mistake_log_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    bookId INTEGER NOT NULL,
                    quizId INTEGER NOT NULL,
                    questionId INTEGER NOT NULL,
                    sessionId INTEGER,
                    selectedAnswer TEXT,
                    correctAnswer TEXT,
                    userReason TEXT,
                    correctConcept TEXT,
                    preventionNote TEXT,
                    linkedFlashcardId INTEGER,
                    linkedBlueprintId INTEGER,
                    linkedAssetId INTEGER,
                    isFixed INTEGER NOT NULL DEFAULT 0,
                    reviewAt INTEGER,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(questionId) REFERENCES questions(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_mistake_log_entries_bookId ON mistake_log_entries(bookId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_mistake_log_entries_quizId ON mistake_log_entries(quizId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_mistake_log_entries_questionId ON mistake_log_entries(questionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_mistake_log_entries_sessionId ON mistake_log_entries(sessionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_mistake_log_entries_reviewAt ON mistake_log_entries(reviewAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_mistake_log_entries_isFixed ON mistake_log_entries(isFixed)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_mistake_log_entries_createdAt ON mistake_log_entries(createdAt)")
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `note_blueprints_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `externalId` TEXT NOT NULL,
                    `bookId` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `summary` TEXT,
                    `body` TEXT NOT NULL,
                    `bulletPoints` TEXT NOT NULL DEFAULT '[]',
                    `tags` TEXT NOT NULL DEFAULT '[]',
                    `blueprintMode` TEXT NOT NULL DEFAULT 'SIMPLE_NOTE',
                    `linkedQuestionsJson` TEXT NOT NULL DEFAULT '[]',
                    `linkedAssetsJson` TEXT NOT NULL DEFAULT '[]',
                    `reviewStatus` TEXT NOT NULL DEFAULT 'NEW',
                    `reviewCount` INTEGER NOT NULL DEFAULT 0,
                    `lastReviewedAt` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `sourceQuestionId` INTEGER,
                    FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO `note_blueprints_new` (id, externalId, bookId, title, summary, body, bulletPoints, tags, blueprintMode, linkedQuestionsJson, linkedAssetsJson, reviewStatus, reviewCount, lastReviewedAt, createdAt, updatedAt, sourceQuestionId)
                SELECT id, externalId, bookId, title, summary, body, bulletPoints, tags, blueprintMode, linkedQuestionsJson, linkedAssetsJson, reviewStatus, reviewCount, lastReviewedAt, createdAt, updatedAt, sourceQuestionId FROM `note_blueprints`
            """.trimIndent())
            db.execSQL("DROP TABLE `note_blueprints`")
            db.execSQL("ALTER TABLE `note_blueprints_new` RENAME TO `note_blueprints`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_blueprints_bookId` ON `note_blueprints` (`bookId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_blueprints_sourceQuestionId` ON `note_blueprints` (`sourceQuestionId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_blueprints_blueprintMode` ON `note_blueprints` (`blueprintMode`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_blueprints_reviewStatus` ON `note_blueprints` (`reviewStatus`)")
        }
    }

    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create workspaces table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `workspaces` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `externalId` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT,
                    `isDefault` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `deletedAt` INTEGER
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workspaces_externalId` ON `workspaces` (`externalId`)")

            // 2. Create workspace_settings table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `workspace_settings` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `workspaceId` INTEGER NOT NULL,
                    `language` TEXT,
                    `theme` TEXT,
                    `defaultSort` TEXT,
                    `quizDefaultsJson` TEXT,
                    `importDefaultsJson` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `deletedAt` INTEGER,
                    FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_settings_workspaceId` ON `workspace_settings` (`workspaceId`)")

            // 3. Create default workspace
            val now = System.currentTimeMillis()
            db.execSQL("""
                INSERT INTO workspaces (externalId, name, description, isDefault, createdAt, updatedAt)
                VALUES ('$DEFAULT_EXTERNAL_ID', '$DEFAULT_NAME', '$DEFAULT_DESCRIPTION', 1, $now, $now)
            """.trimIndent())
            
            val defaultWorkspaceId = db.query("SELECT id FROM workspaces WHERE isDefault = 1 LIMIT 1").use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 1L
            }

            // 4. Migrate books table to include workspaceId
            // Room doesn't support ALTER TABLE for adding FKs easily, so we recreate
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `books_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `workspaceId` INTEGER NOT NULL,
                    `externalId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `iconName` TEXT,
                    `coverImage` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `contentUpdatedAt` INTEGER NOT NULL,
                    `lastStudiedAt` INTEGER NOT NULL,
                    `lastEditedAt` INTEGER NOT NULL,
                    `isPinned` INTEGER NOT NULL,
                    `isSystem` INTEGER NOT NULL,
                    `fields` TEXT NOT NULL,
                    `questionCount` INTEGER NOT NULL,
                    `answeredCount` INTEGER NOT NULL,
                    `totalAttempts` INTEGER NOT NULL,
                    `completionPercentage` REAL NOT NULL,
                    `accuracyPercentage` REAL NOT NULL,
                    FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())

            db.execSQL("""
                INSERT INTO books_new (
                    id, workspaceId, externalId, title, description, iconName, coverImage, 
                    createdAt, updatedAt, contentUpdatedAt, lastStudiedAt, lastEditedAt, 
                    isPinned, isSystem, fields, questionCount, answeredCount, 
                    totalAttempts, completionPercentage, accuracyPercentage
                )
                SELECT 
                    id, $defaultWorkspaceId, externalId, title, description, iconName, coverImage, 
                    createdAt, updatedAt, contentUpdatedAt, lastStudiedAt, lastEditedAt, 
                    isPinned, isSystem, fields, questionCount, answeredCount, 
                    totalAttempts, completionPercentage, accuracyPercentage
                FROM books
            """.trimIndent())

            db.execSQL("DROP TABLE `books`")
            db.execSQL("ALTER TABLE `books_new` RENAME TO `books`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_workspaceId` ON `books` (`workspaceId`)")
        }
    }

    val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            var defaultWorkspaceId = db.query(
                "SELECT id FROM workspaces WHERE externalId = '$DEFAULT_EXTERNAL_ID' LIMIT 1"
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }

            if (defaultWorkspaceId == null) {
                defaultWorkspaceId = db.query(
                    "SELECT id FROM workspaces WHERE isDefault = 1 AND deletedAt IS NULL ORDER BY id LIMIT 1"
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else null
                }
            }

            if (defaultWorkspaceId == null) {
                db.execSQL(
                    """
                    INSERT INTO workspaces (externalId, name, description, isDefault, createdAt, updatedAt)
                    VALUES ('$DEFAULT_EXTERNAL_ID', '$DEFAULT_NAME', '$DEFAULT_DESCRIPTION', 1, $now, $now)
                    """.trimIndent()
                )
                defaultWorkspaceId = db.query(
                    "SELECT id FROM workspaces WHERE externalId = '$DEFAULT_EXTERNAL_ID' LIMIT 1"
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else 1L
                }
            } else {
                db.execSQL(
                    """
                    UPDATE workspaces
                    SET externalId = '$DEFAULT_EXTERNAL_ID',
                        name = '$DEFAULT_NAME',
                        description = '$DEFAULT_DESCRIPTION',
                        isDefault = 1,
                        deletedAt = NULL,
                        updatedAt = $now
                    WHERE id = $defaultWorkspaceId
                    """.trimIndent()
                )
            }

            db.execSQL("UPDATE workspaces SET isDefault = CASE WHEN id = $defaultWorkspaceId THEN 1 ELSE 0 END")
            db.execSQL(
                """
                UPDATE books
                SET workspaceId = $defaultWorkspaceId
                WHERE workspaceId NOT IN (SELECT id FROM workspaces)
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_workspaceId` ON `books` (`workspaceId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workspaces_externalId` ON `workspaces` (`externalId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workspace_settings_workspaceId` ON `workspace_settings` (`workspaceId`)")

            val hasSettings = db.query(
                "SELECT id FROM workspace_settings WHERE workspaceId = $defaultWorkspaceId LIMIT 1"
            ).use { cursor -> cursor.moveToFirst() }
            if (!hasSettings) {
                db.execSQL(
                    """
                    INSERT INTO workspace_settings (workspaceId, language, theme, defaultSort, quizDefaultsJson, importDefaultsJson, createdAt, updatedAt)
                    VALUES ($defaultWorkspaceId, NULL, NULL, NULL, NULL, NULL, $now, $now)
                    """.trimIndent()
                )
            }
        }
    }

    val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val softDeleteTables = listOf(
                "books",
                "quizzes",
                "questions",
                "sessions",
                "flashcard_decks",
                "flashcards",
                "learning_sessions",
                "slideshow_courses",
                "course_slides",
                "note_blueprints",
                "prompts",
                "prompt_decks",
                "prompt_cards",
                "prompt_runs",
                "knowledge_study_sessions",
                "question_assets",
                "source_documents",
                "mistake_log_entries",
                "asset_references",
                "question_categories",
                "category_metadata"
            )
            softDeleteTables.forEach { tableName ->
                MksDatabase.addColumnIfMissing(db, tableName, "deletedAt", "INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_${tableName}_deletedAt` ON `$tableName` (`deletedAt`)")
            }
        }
    }

    val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `annotations` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `workspaceId` INTEGER NOT NULL,
                    `bookId` INTEGER NOT NULL,
                    `ownerType` TEXT NOT NULL,
                    `ownerId` INTEGER NOT NULL,
                    `selectedText` TEXT,
                    `noteBody` TEXT,
                    `colorLabel` TEXT NOT NULL DEFAULT 'YELLOW',
                    `positionDataJson` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `deletedAt` INTEGER,
                    FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_workspaceId` ON `annotations` (`workspaceId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_bookId` ON `annotations` (`bookId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_ownerType_ownerId` ON `annotations` (`ownerType`, `ownerId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_colorLabel` ON `annotations` (`colorLabel`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_deletedAt` ON `annotations` (`deletedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_updatedAt` ON `annotations` (`updatedAt`)")
        }
    }


    val MIGRATION_26_27 = object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Add fields to Quizzes, FlashcardDecks, SlideshowCourses, PromptDecks, PromptCards
            MksDatabase.addColumnIfMissing(db, "quizzes", "tags", "TEXT NOT NULL DEFAULT '[]'")
            MksDatabase.addColumnIfMissing(db, "quizzes", "iconName", "TEXT")
            MksDatabase.addColumnIfMissing(
                db,
                "flashcard_decks",
                "tags",
                "TEXT NOT NULL DEFAULT '[]'"
            )
            MksDatabase.addColumnIfMissing(
                db,
                "slideshow_courses",
                "tags",
                "TEXT NOT NULL DEFAULT '[]'"
            )
            MksDatabase.addColumnIfMissing(db, "slideshow_courses", "iconName", "TEXT")
            MksDatabase.addColumnIfMissing(db, "prompt_decks", "tags", "TEXT NOT NULL DEFAULT '[]'")
            MksDatabase.addColumnIfMissing(db, "prompt_decks", "iconName", "TEXT")
            MksDatabase.addColumnIfMissing(db, "prompt_decks", "coverImage", "TEXT")
            MksDatabase.addColumnIfMissing(db, "prompt_cards", "tags", "TEXT NOT NULL DEFAULT '[]'")

            // 2. Add Spaced Repetition fields to Questions and CourseSlides
            MksDatabase.addColumnIfMissing(db, "questions", "tags", "TEXT NOT NULL DEFAULT '[]'")
            MksDatabase.addColumnIfMissing(db, "questions", "difficulty", "TEXT")
            MksDatabase.addColumnIfMissing(db, "questions", "dueAt", "INTEGER NOT NULL DEFAULT 0")
            MksDatabase.addColumnIfMissing(
                db,
                "questions",
                "reviewCount",
                "INTEGER NOT NULL DEFAULT 0"
            )
            MksDatabase.addColumnIfMissing(
                db,
                "questions",
                "lastReviewedAt",
                "INTEGER NOT NULL DEFAULT 0"
            )

            MksDatabase.addColumnIfMissing(
                db,
                "course_slides",
                "tags",
                "TEXT NOT NULL DEFAULT '[]'"
            )
            MksDatabase.addColumnIfMissing(db, "course_slides", "difficulty", "TEXT")
            MksDatabase.addColumnIfMissing(
                db,
                "course_slides",
                "dueAt",
                "INTEGER NOT NULL DEFAULT 0"
            )
            MksDatabase.addColumnIfMissing(
                db,
                "course_slides",
                "reviewCount",
                "INTEGER NOT NULL DEFAULT 0"
            )
            MksDatabase.addColumnIfMissing(
                db,
                "course_slides",
                "lastReviewedAt",
                "INTEGER NOT NULL DEFAULT 0"
            )

            // 3. Create NoteCollections table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `note_collections` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `externalId` TEXT NOT NULL,
                    `bookId` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `iconName` TEXT,
                    `coverImage` TEXT,
                    `tags` TEXT NOT NULL DEFAULT '[]',
                    `noteCount` INTEGER NOT NULL DEFAULT 0,
                    `isSystem` INTEGER NOT NULL DEFAULT 0,
                    `isPinned` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `lastStudiedAt` INTEGER NOT NULL DEFAULT 0,
                    `lastEditedAt` INTEGER NOT NULL,
                    `deletedAt` INTEGER,
                    FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_collections_bookId` ON `note_collections` (`bookId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_collections_deletedAt` ON `note_collections` (`deletedAt`)")

            // 4. Create StudySessions table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `study_sessions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `targetType` TEXT NOT NULL,
                    `targetId` INTEGER NOT NULL,
                    `label` TEXT,
                    `stateJson` TEXT NOT NULL,
                    `timeSpentMs` INTEGER NOT NULL DEFAULT 0,
                    `completionPercentage` REAL NOT NULL DEFAULT 0.0,
                    `isCompleted` INTEGER NOT NULL DEFAULT 0,
                    `correctCount` INTEGER NOT NULL DEFAULT 0,
                    `incorrectCount` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `deletedAt` INTEGER
                )
            """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_targetId_targetType` ON `study_sessions` (`targetId`, `targetType`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_deletedAt` ON `study_sessions` (`deletedAt`)")

            // 5. Migrate NoteBlueprints
            // Create default collections for existing blueprints
            val now = System.currentTimeMillis()
            db.query("SELECT DISTINCT bookId FROM note_blueprints").use { cursor ->
                while (cursor.moveToNext()) {
                    val bookId = cursor.getLong(0)
                    db.execSQL(
                        """
                        INSERT INTO note_collections (externalId, bookId, title, description, isSystem, isPinned, createdAt, updatedAt, lastStudiedAt, lastEditedAt)
                        VALUES ('nc_default_$bookId', $bookId, 'Default Collection', 'Auto-generated collection', 1, 0, $now, $now, 0, $now)
                    """.trimIndent()
                    )
                }
            }

            // Recreate note_blueprints to point to collectionId instead of bookId
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `note_blueprints_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `externalId` TEXT NOT NULL,
                    `collectionId` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `summary` TEXT,
                    `iconName` TEXT,
                    `coverImage` TEXT,
                    `body` TEXT NOT NULL,
                    `bulletPoints` TEXT NOT NULL DEFAULT '[]',
                    `tags` TEXT NOT NULL DEFAULT '[]',
                    `blueprintMode` TEXT NOT NULL DEFAULT 'SIMPLE_NOTE',
                    `linkedQuestionsJson` TEXT NOT NULL DEFAULT '[]',
                    `linkedAssetsJson` TEXT NOT NULL DEFAULT '[]',
                    `reviewStatus` TEXT NOT NULL DEFAULT 'NEW',
                    `reviewCount` INTEGER NOT NULL DEFAULT 0,
                    `lastReviewedAt` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `sourceQuestionId` INTEGER,
                    `deletedAt` INTEGER,
                    FOREIGN KEY(`collectionId`) REFERENCES `note_collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent()
            )

            // Migrate data
            db.execSQL(
                """
                INSERT INTO `note_blueprints_new` (
                    id, externalId, collectionId, title, summary, body, bulletPoints, tags, blueprintMode, 
                    linkedQuestionsJson, linkedAssetsJson, reviewStatus, reviewCount, lastReviewedAt, 
                    createdAt, updatedAt, sourceQuestionId, deletedAt
                )
                SELECT 
                    b.id, b.externalId, c.id, b.title, b.summary, b.body, b.bulletPoints, b.tags, b.blueprintMode, 
                    b.linkedQuestionsJson, b.linkedAssetsJson, b.reviewStatus, b.reviewCount, b.lastReviewedAt, 
                    b.createdAt, b.updatedAt, b.sourceQuestionId, b.deletedAt
                FROM `note_blueprints` b
                INNER JOIN `note_collections` c ON b.bookId = c.bookId AND c.externalId = 'nc_default_' || b.bookId
            """.trimIndent()
            )

            db.execSQL("DROP TABLE `note_blueprints`")
            db.execSQL("ALTER TABLE `note_blueprints_new` RENAME TO `note_blueprints`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_blueprints_collectionId` ON `note_blueprints` (`collectionId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_blueprints_sourceQuestionId` ON `note_blueprints` (`sourceQuestionId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_blueprints_blueprintMode` ON `note_blueprints` (`blueprintMode`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_blueprints_reviewStatus` ON `note_blueprints` (`reviewStatus`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_blueprints_deletedAt` ON `note_blueprints` (`deletedAt`)")
        }
    }


    val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `source_document_assets` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `sourceDocumentId` INTEGER NOT NULL,
                    `assetType` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `localPath` TEXT,
                    `externalUrl` TEXT,
                    `mimeType` TEXT,
                    `fileName` TEXT,
                    `fileSizeBytes` INTEGER,
                    `textContent` TEXT,
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    `isPinned` INTEGER NOT NULL DEFAULT 0,
                    `isPrimary` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `deletedAt` INTEGER,
                    FOREIGN KEY(`sourceDocumentId`) REFERENCES `source_documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_document_assets_sourceDocumentId` ON `source_document_assets` (`sourceDocumentId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_document_assets_assetType` ON `source_document_assets` (`assetType`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_document_assets_createdAt` ON `source_document_assets` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_source_document_assets_deletedAt` ON `source_document_assets` (`deletedAt`)")
        }
    }

    val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Drop source document assets table
            db.execSQL("DROP TABLE IF EXISTS `source_document_assets`")
            
            // Add unique index constraints on externalId for books and quizzes
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_books_externalId` ON `books` (`externalId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_quizzes_externalId` ON `quizzes` (`externalId`)")
        }
    }

    val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `sessions` ADD COLUMN `resultTaxonomy` TEXT NOT NULL DEFAULT '{}'")
        }
    }

    val ALL = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
        MIGRATION_15_16,
        MIGRATION_16_17,
        MIGRATION_17_18,
        MIGRATION_18_19,
        MIGRATION_19_20,
        MIGRATION_20_21,
        MIGRATION_21_22,
        MIGRATION_22_23,
        MIGRATION_23_24,
        MIGRATION_24_25,
        MIGRATION_25_26,
        MIGRATION_26_27,
        MIGRATION_27_28,
        MIGRATION_28_29,
        MIGRATION_29_30
    )
}
