# AGENTS.md — MKS Multiplatform Project Guidance

**MKS** is a Compose Multiplatform quiz and knowledge-bank application targeting Android and Linux Desktop (Fedora KDE Plasma, JVM). It imports educational content from spreadsheets and documents, then presents interactive quizzes, flashcards, slideshows, and study materials with image support.

> Last updated: 2026-06-16. Migration Phase 1–5 complete.

## Project Overview

- **Language:** Kotlin (Multiplatform)
- **UI Framework:** Compose Multiplatform 1.10.1 + Material3
- **DI:** Koin 4.0.3 (replaced Dagger Hilt)
- **Database:** SQLDelight 2.0.2, 26 tables, v30 schema (replaced Room)
- **Targets:** Android (api 26+) + JVM Desktop (Java 17)
- **Desktop Env:** Fedora Linux (KDE Plasma), FHS paths (`~/.local/share/mks/`)
- **Image Loading:** Coil (Android) / LRU + disk cache (Desktop)
- **TTS:** Android TTS / espeak-ng subprocess with 10s timeout (Desktop)
- **File Import:** Multi-format (XLSX, CSV/TSV, JSON, HTML, TEXT, ZIP)
- **Serialization:** kotlinx.serialization (replaced Moshi)
- **Localization:** English + Arabic (RTL support)

---

## Module Architecture

```
shared/                    ← KMP shared code (ALL new development)
├── commonMain/            53 files
│   ├── data/local/entity/     26 entities (@Serializable, Room-free)
│   ├── data/repository/        7 repositories (SQLDelight-backed)
│   ├── data/model/             Domain models (MksResult, CategoryWithMetadata)
│   ├── data/local/             WorkspaceDefaults
│   ├── sqldelight/.../db/     1.sqm migration + Schema.sq + 12 query .sq files
│   ├── di/                    DataModule, ViewModelModule, UtilityModule
│   ├── platform/              4 interfaces (FileManager, TtsManager, ImageLoader, FileDialog)
│   ├── ui/                    14 ViewModels (Library, Quiz, Flashcard, etc.)
│   └── util/                  MksLogger (expect/actual), BoundedStreams, MksRoutes
├── androidMain/            9 files
│   ├── platform/              AndroidFileManager, AndroidTtsManager, AndroidImageLoader, AndroidFileDialog
│   ├── di/                    AndroidPlatformModule (Koin)
│   ├── db/                    DatabaseDriverFactory (AndroidSqliteDriver)
│   └── util/                  MksLogger.android (android.util.Log)
└── desktopMain/            9 files
    ├── platform/              DesktopFileManager (FHS), DesktopTtsManager (espeak-ng),
    │                          DesktopImageLoader (LRU), DesktopFileDialog (AWT)
    ├── di/                    DesktopPlatformModule (Koin)
    ├── db/                    DatabaseDriverFactory (JdbcSqliteDriver)
    └── util/                  MksLogger.desktop (println/System.err)

app/                       ← Android application (Koin-initialized)
app-desktop/               ← Desktop JVM application (Main.kt, 4-tab Material3 UI)
core/                      ← 5 legacy Android modules (model, database, data, network, ui)
feature/                   ← 1 legacy Android feature module (ui)
```

---

## Architecture Rules

### 1. New code goes in shared/commonMain
All entities, repositories, ViewModels, and UI code live in `shared/src/commonMain/`. Platform-specific code uses `expect`/`actual` or interfaces with platform-specific Koin bindings.

### 2. Dependency Injection (Koin)
- Modules in `shared/.../di/DataModule.kt`, `ViewModelModule.kt`, `UtilityModule.kt`
- ViewModels use `KoinComponent` + `by inject()` (NOT `@Inject constructor`)
- Circular deps resolved by `KoinComponent.inject()` (replaces Hilt's `Provider<T>`)
- Platform modules: `androidPlatformModule` / `desktopPlatformModule`

### 3. Database (SQLDelight)
- Schema: `Schema.sq` (26 CREATE TABLE statements for code generation)
- Migration: `1.sqm` (CREATE TABLE IF NOT EXISTS, idempotent)
- Queries: 12 `.sq` files with globally unique prefixed names (`bk_`, `qz_`, `fc_`, `sl_`, `nt_`, `pr_`, `as_`, `mk_`, `ws_`, `se_`, `qu_`, `cm_`)
- Generated code: `MksDatabase` interface accessed via `DatabaseDriverFactory.createDriver()`
- Android driver: `AndroidSqliteDriver`
- Desktop driver: `JdbcSqliteDriver("jdbc:sqlite:~/.local/share/mks/mks_database.db")`

### 4. ViewModels
```kotlin
// Pattern: plain class + KoinComponent + by inject()
class LibraryViewModel : KoinComponent {
    private val bookRepo: BookRepository by inject()
    // ...
}
// Registered as: factory { LibraryViewModel() }
// Resolved in Compose: remember { LibraryViewModel() } or koinInject()
```

### 5. Platform Abstractions
```kotlin
// commonMain: interface
interface FileManager {
    fun getImagesDir(): String
    fun saveImage(bytes: ByteArray): String?
    // ...
}

// androidMain: Koin single<FileManager> { AndroidFileManager(androidContext()) }
// desktopMain: Koin single<FileManager> { DesktopFileManager() }
```

---

## Build Commands

```bash
# Verify project model (always run after build file changes)
./gradlew projects --no-configuration-cache

# Compile shared (desktop)
./gradlew :shared:compileKotlinDesktop

# Compile desktop app
./gradlew :app-desktop:compileKotlin

# Run desktop app (development)
./gradlew :app-desktop:run

# Hot reload desktop app
./gradlew :app-desktop:hotRun --mainClass=com.ahmedyejam.mks.desktop.MainKt

# Package desktop distribution
./gradlew :app-desktop:packageDistributionForCurrentOS       # macOS .app / Linux .rpm
./gradlew :app-desktop:packageRpm                            # Fedora RPM
./gradlew :app-desktop:packageDeb                            # Debian DEB

# Generate SQLDelight code
./gradlew :shared:generateCommonMainMksDatabaseInterface

# Android assemble
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

---

## Database Schema (v30, 26 Tables)

### Entity Relationship

```
WorkspaceEntity (workspaces)
└── WorkspaceSettingsEntity (workspace_settings)
└── BookEntity (books)
    ├── QuizEntity (quizzes)
    │   ├── QuestionEntity (questions)
    │   │   ├── QuestionCategoryEntity (question_categories)
    │   │   ├── QuestionAssetEntity (question_assets)
    │   │   └── MistakeLogEntryEntity (mistake_log_entries)
    │   └── SessionEntity (sessions)
    ├── FlashcardDeckEntity (flashcard_decks)
    │   ├── FlashcardEntity (flashcards)
    │   └── LearningSessionEntity (learning_sessions)
    ├── SlideshowCourseEntity (slideshow_courses)
    │   └── CourseSlideEntity (course_slides)
    ├── NoteCollectionEntity (note_collections)
    │   └── NoteBlueprintEntity (note_blueprints)
    ├── PromptDeckEntity (prompt_decks)
    │   ├── PromptCardEntity (prompt_cards)
    │   │   └── PromptRunEntity (prompt_runs)
    │   └── PromptEntity (prompts)
    ├── SourceDocumentEntity (source_documents)
    └── AnnotationEntity (annotations)

Standalone: CategoryMetadataEntity, AssetReferenceEntity,
            KnowledgeStudySessionEntity, StudySessionEntity
```

### Adding a New Column

1. Add column to `Schema.sq` (following existing table)
2. Add ALTER TABLE to `1.sqm` (use `IF NOT EXISTS` pattern)
3. Update entity in `data/local/entity/`
4. Update relevant `.sq` query files
5. Regenerate: `./gradlew :shared:generateCommonMainMksDatabaseInterface`

### Adding a New Query

```sql
-- In the appropriate .sq file (e.g., BookQueries.sq)
bk_selectByTitlePrefix:
SELECT * FROM books WHERE title LIKE ? || '%' AND deletedAt IS NULL;
```

Query names must be globally unique across all `.sq` files. Use entity prefixes:
`bk_` (books), `qz_` (quizzes), `qu_` (questions), `se_` (sessions),
`ws_` (workspaces), `fc_` (flashcards), `sl_` (slideshows), `nt_` (notes),
`pr_` (prompts), `as_` (assets), `mk_` (mistakes), `cm_` (categories).

---

## Repository Pattern

```kotlin
class BookRepository(private val db: MksDatabase) : KoinComponent {
    // Cross-repo deps via by inject() (lazy, breaks cycles)
    private val quizRepo: QuizRepository by inject()

    fun observeAllBooks(): Flow<List<BookEntity>> =
        db.bookQueriesQueries.bk_selectAll()
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toBookEntity() } }

    suspend fun createBook(book: BookEntity): MksResult<Long> =
        withContext(Dispatchers.IO) {
            try {
                db.bookQueriesQueries.bk_insert(/* params */)
                MksResult.Success(db.bookQueriesQueries.bk_lastInsertId().executeAsOne())
            } catch (e: Exception) { MksResult.Error("Failed", e) }
        }
}
```

---

## Desktop Data Paths (Linux)

```
~/.local/share/mks/               Application data root
~/.local/share/mks/mks_database.db SQLite database
~/.local/share/mks/images/         Image storage
~/.local/share/mks/cache/          Image disk cache
```

### Inspect Database

```bash
sqlite3 ~/.local/share/mks/mks_database.db
.tables
.schema books
SELECT * FROM books;
```

---

## Common Tasks

### Add New Screen to Desktop
1. Create ViewModel in `shared/.../ui/{feature}/`
2. Create Composable in `app-desktop/.../Main.kt` or separate file
3. Register ViewModel in `ViewModelModule.kt` as `factory { MyViewModel() }`
4. Add tab/navigation in `Main.kt`

### Add New Repository Method
1. Add SQLDelight query to appropriate `.sq` file
2. Implement method in repository class
3. Add mapper extension function if returning domain entities
4. Regenerate SQLDelight code

### Wire New Platform Implementation
1. Implement interface in `androidMain` or `desktopMain`
2. Register in `AndroidPlatformModule.kt` or `DesktopPlatformModule.kt`
3. Interface is resolved via `KoinComponent.inject()` or `get()` in modules

### Resolve AGP/Kotlin/Compose MP Version Issues
- Version catalog: `gradle/libs.versions.toml`
- Compose MP versions tested: 1.8.1 (failed onVariant), 1.10.1 (works)
- Fallback: downgrade AGP to 8.9.x if Compose MP compatibility breaks

---

## Key Files Reference

| File | Purpose |
|------|---------|
| `gradle/libs.versions.toml` | All dependency versions |
| `shared/build.gradle.kts` | KMP targets + source sets |
| `shared/.../sqldelight/.../1.sqm` | DB migration (v30) |
| `shared/.../sqldelight/.../Schema.sq` | Schema for code gen |
| `shared/.../di/DataModule.kt` | Repository + DB Koin bindings |
| `shared/.../di/ViewModelModule.kt` | ViewModel factories |
| `shared/.../platform/FileManager.kt` | File I/O interface |
| `shared/.../platform/TtsManager.kt` | TTS interface |
| `shared/.../platform/ImageLoader.kt` | Image loading interface |
| `shared/.../platform/FileDialog.kt` | File dialog interface |
| `app-desktop/.../Main.kt` | Desktop entry point (4 tabs) |
| `app/.../MksApplication.kt` | Android Koin init |
| `gradle.properties` | JVM args, AGP flags |
| `HOW_TO_BUILD.md` | Full build/install/debug guide |
| `docs/migration/multiplatform_desktop_build_plan.md` | Migration roadmap |
