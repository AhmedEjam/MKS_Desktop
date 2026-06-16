# MKS Application Architecture Map

> Last updated: 2026-06-16. Compose Multiplatform migration complete (Phase 1–5).  
> Targets: Android + Linux Desktop (Fedora KDE Plasma, JVM).

## 🏗️ Application Architecture Overview

```
MKS (My Knowledge Space)
├── Framework: Compose Multiplatform 1.10.1 + Material3
├── Architecture: MVVM with Koin DI
├── Database: SQLDelight 2.0.2 (26 tables, v30 schema)
├── Language: Kotlin (100% — Multiplatform)
├── Targets: Android (api 26+) + JVM Desktop (Java 17)
└── Build: Gradle 9.4.1 | Kotlin 2.2.10 | AGP 9.2.1
```

## 📦 Module Structure

```
MKS/
├── shared/                    KMP shared module
│   ├── commonMain/            53 files
│   │   ├── data/local/entity/    26 entity classes (@Serializable)
│   │   ├── data/repository/       7 repositories (SQLDelight-backed)
│   │   ├── data/model/            Domain models (MksResult, CategoryWithMetadata)
│   │   ├── sqldelight/.../db/     Schema + 12 .sq query files
│   │   ├── di/                    DataModule, ViewModelModule, UtilityModule
│   │   ├── platform/              4 interfaces (FileManager, TtsManager, ImageLoader, FileDialog)
│   │   ├── ui/                    14 ViewModels (Library, Quiz, Flashcard, etc.)
│   │   └── util/                  MksLogger, MksRoutes, BoundedStreams
│   ├── androidMain/            9 files (Android platform impls)
│   └── desktopMain/            9 files (Desktop platform impls)
│
├── app/                       Android application module
│   ├── src/main/                MksApplication.kt (Koin init) + MainActivity.kt
│   └── build.gradle.kts         Android app config
│
├── app-desktop/               Desktop JVM application
│   ├── src/main/                Main.kt (4-tab Material3 app)
│   └── build.gradle.kts         Desktop config with RPM/DEB packaging
│
└── gradle/                    Build configuration
    └── libs.versions.toml        Centralized version catalog
```

## 🗄️ Database Layer (SQLDelight)

```
Database Initialization
├── Platform Driver Factory (expect/actual)
│   ├── Android: AndroidSqliteDriver(context, Schema, "mks_database.db")
│   └── Desktop: JdbcSqliteDriver("jdbc:sqlite:~/.local/share/mks/mks_database.db")
│
├── Migration: 1.sqm (CREATE TABLE IF NOT EXISTS for all 26 tables)
├── Schema: Schema.sq (table definitions for code generation)
└── Queries: 12 .sq files (workspace, book, quiz, question, session,
              flashcard, slideshow, note, prompt, asset, mistake)
```

### Data Flow

```
Compose UI
  ↓ observe StateFlow / collectAsState
ViewModel (KoinComponent + by inject())
  ↓ suspend fun / Flow
Repository (MksDatabase)
  ↓ SQLDelight queries (bk_selectAll, qz_insert, etc.)
SQLDelight (generated MksDatabase + Query classes)
  ↓ SqlDriver
SQLite Database (~/.local/share/mks/mks_database.db on desktop)
```

## 🔌 Dependency Injection (Koin)

```
startKoin {
    modules(allModules + platformModule)
}

DataModule:
├── single<MksDatabase>              → DatabaseDriverFactory.createDriver()
├── single<AssetRepository>          → MksDatabase
├── single<StudyRepository>          → MksDatabase
├── single<BookRepository>           → MksDatabase + KoinComponent.inject()
├── single<QuizRepository>           → MksDatabase + KoinComponent.inject()
├── single<KnowledgeRepository>      → MksDatabase + KoinComponent.inject()
├── single<WorkspaceRepository>      → MksDatabase + Seeder
└── single<MksDatabaseSeeder>        → BookRepo + QuizRepo + KnowledgeRepo

ViewModelModule:
└── factory<LibraryViewModel>        → KoinComponent.inject() repos
    factory<QuizViewModel>           → (same pattern for all 14 VMs)
    ...

PlatformModule (androidMain / desktopMain):
├── single<FileManager>              → AndroidFileManager / DesktopFileManager
├── single<TtsManager>               → AndroidTtsManager / DesktopTtsManager
├── single<ImageLoader>              → AndroidImageLoader / DesktopImageLoader
└── factory<FileDialog>              → AndroidFileDialog / DesktopFileDialog
```

## 🖥️ Desktop Application

```
Main.kt
├── startKoin { modules(...) }
├── Window (Compose Multiplatform)
│   └── MaterialTheme
│       └── Scaffold (TopAppBar + NavigationBar + Content)
│           ├── LibraryTab      → LibraryViewModel (books CRUD)
│           ├── QuizListTab     → QuizViewModel (placeholder)
│           ├── FlashcardTab    → FlashcardDeckViewModel (placeholder)
│           └── SettingsTab     → Platform info + version
└── stopKoin() on close
```

## 📱 Android Application

```
MksApplication.kt
├── @HiltAndroidApp → removed, replaced by startKoin { ... }
├── ImageLoaderFactory (Coil) — retained
└── Crash handler setup

MainActivity.kt
├── @AndroidEntryPoint → removed
├── @Inject lateinit → replaced by by inject() (Koin)
├── Theme + Language management (DataStoreManager)
└── MksNavHost (22+ routes, legacy screens)
```

## 🔄 Platform Abstraction Layer

| Interface | Android | Desktop | Data Path |
|-----------|---------|---------|-----------|
| FileManager | Context.filesDir + ContentResolver | FHS ~/.local/share/mks/ | images/ subdir |
| TtsManager | android.speech.tts.TextToSpeech | espeak-ng subprocess (10s timeout) | WAV temp files |
| ImageLoader | Coil (25% RAM cache) | LRU (128 entries) + disk cache | ~/.local/share/mks/cache/ |
| FileDialog | SAF ActivityResultContracts | AWT JFileChooser | N/A |

## 🏷️ Entity Relationship Graph

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

## 🚀 Build Commands

```bash
./gradlew projects --no-configuration-cache         # Verify project model
./gradlew :shared:compileKotlinDesktop                # Compile shared module
./gradlew :app-desktop:compileKotlin                  # Compile desktop app
./gradlew :app-desktop:packageDistributionForCurrentOS # Package (macOS .app / Fedora .rpm)
./gradlew :app-desktop:packageRpm                     # Fedora RPM specifically
```

**Last Updated:** June 16, 2026
