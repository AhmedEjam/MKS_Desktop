# 📚 MKS — My Knowledge Space

**Multiplatform quiz & knowledge-bank application** targeting Android and Linux Desktop (Fedora KDE Plasma, JVM).

MKS imports educational content from multiple file formats (XLSX, CSV, JSON, HTML, etc.) and presents interactive learning experiences through quizzes, flashcards, slideshows, notes, and AI prompt decks.

**Language:** Kotlin (100%)  
**UI:** Compose Multiplatform 1.10.1  
**DI:** Koin 4.0.3  
**Database:** SQLDelight 2.0.2 (26 tables)  
**Image Loading:** Coil (Android) / Skiko + Disk Cache (Desktop)  
**TTS:** Android TTS / espeak-ng subprocess (Desktop)  
**Build:** Gradle 9.4.1 | Kotlin 2.2.10 | AGP 9.2.1 | Java 17  

---

## 🎯 Current State (June 2026)

### Migration Status: **Phase 1–5 Complete**

MKS has been migrated from Android-only (Hilt + Room) to Compose Multiplatform (Koin + SQLDelight). The desktop app compiles, packages, and runs with a working Library screen backed by real SQL queries.

```
shared/          ← KMP common code (53 files)
├── Entities (26) + Enums (8)
├── SQLDelight schema (1.sqm) + queries (12 .sq files)
├── Repositories (7, with SQLDelight-backed implementations)
├── ViewModels (14, KoinComponent + by inject())
├── Platform abstractions (4 interfaces: FileManager, TtsManager, ImageLoader, FileDialog)
├── Koin DI modules (3: DataModule, ViewModelModule, UtilityModule)
├── Utility types (MksLogger expect/actual, MksRoutes, BoundedStreams, MksResult)

app-android/      ← Android application (legacy source in core/* + feature/*)
app-desktop/      ← Desktop JVM application (Main.kt with 4-tab Material3 UI)
```

---

## 🏗️ Architecture Overview

### Technology Stack

```
✅ Language:           Kotlin (Multiplatform)
✅ UI Framework:       Compose Multiplatform (Material3)
✅ DI:                 Koin (replaced Dagger Hilt)
✅ Database:           SQLDelight (replaced Room)
✅ Image Loading:      Coil (Android) / LRU + disk cache (Desktop)
✅ TTS:                Android TTS / espeak-ng subprocess (Desktop)
✅ Serialization:      kotlinx.serialization
✅ Async:              kotlinx.coroutines
✅ Build:              Gradle 9.4.1 with version catalog
```

### Module Structure

```
MKS/
├── shared/              KMP shared module (commonMain + androidMain + desktopMain)
│   ├── commonMain/      53 files: entities, repos, VMs, DI, platform interfaces, SQLDelight
│   ├── androidMain/      9 files: platform impls (Coil, SAF, Android TTS, Room DB driver)
│   └── desktopMain/      9 files: platform impls (FHS paths, espeak-ng, Skiko, JFileChooser)
│
├── app/                 Android application (legacy modules still in core/*, feature/*)
├── app-desktop/         Desktop JVM application (1 file: Main.kt — full 4-tab app)
├── core/                5 legacy Android library modules (model, database, data, network, ui)
├── feature/             1 legacy Android feature module (ui — screens + ViewModels)
└── gradle/              Version catalog (libs.versions.toml), wrapper, properties
```

---

## 📁 Key Files

| File | Purpose |
|------|---------|
| `shared/build.gradle.kts` | KMP configuration (androidTarget + jvm("desktop")) |
| `shared/src/commonMain/sqldelight/.../1.sqm` | Full v30 schema migration (26 tables, IF NOT EXISTS) |
| `shared/src/commonMain/sqldelight/.../Schema.sq` | Schema definitions for code generation |
| `shared/src/commonMain/sqldelight/.../*Queries.sq` | 12 query files (CRUD for all tables) |
| `shared/src/commonMain/.../di/DataModule.kt` | Koin module: database + 7 repositories |
| `shared/src/commonMain/.../di/ViewModelModule.kt` | Koin module: 14 ViewModels (factory scope) |
| `shared/src/commonMain/.../platform/FileManager.kt` | Platform file I/O interface |
| `shared/src/commonMain/.../platform/TtsManager.kt` | TTS interface (play/stop/shutdown) |
| `shared/src/commonMain/.../platform/ImageLoader.kt` | Image loading interface |
| `shared/src/commonMain/.../platform/FileDialog.kt` | File dialog interface |
| `app-desktop/src/.../desktop/Main.kt` | Desktop entry point — 4-tab Material3 app |
| `app/src/.../MksApplication.kt` | Android Koin initialization |
| `gradle/libs.versions.toml` | Centralized version catalog |

---

## 🗄️ Database (SQLDelight, 26 tables)

All 26 tables defined in `Schema.sq` and `1.sqm` with `CREATE TABLE IF NOT EXISTS`.

### Entity Relationship Graph

```
WorkspaceEntity
  └── BookEntity
        ├── QuizEntity
        │     ├── QuestionEntity
        │     │     ├── QuestionCategoryEntity
        │     │     ├── QuestionAssetEntity
        │     │     └── MistakeLogEntryEntity
        │     └── SessionEntity
        ├── FlashcardDeckEntity
        │     ├── FlashcardEntity
        │     └── LearningSessionEntity
        ├── SlideshowCourseEntity
        │     └── CourseSlideEntity
        ├── NoteCollectionEntity
        │     └── NoteBlueprintEntity
        ├── PromptDeckEntity
        │     ├── PromptCardEntity
        │     │     └── PromptRunEntity
        │     └── PromptEntity
        ├── SourceDocumentEntity
        └── AnnotationEntity
  └── WorkspaceSettingsEntity

Standalone: CategoryMetadataEntity, AssetReferenceEntity,
            KnowledgeStudySessionEntity, StudySessionEntity
```

---

## 🧭 Repositories

| Repository | Dependencies | Methods |
|-----------|-------------|---------|
| `AssetRepository` | MksDatabase | Question assets, source docs, asset refs |
| `WorkspaceRepository` | MksDatabase + Seeder | Workspace CRUD, auto-create default |
| `StudyRepository` | MksDatabase | Mistakes, flashcard/slide progress, sessions |
| `BookRepository` | MksDatabase + KoinComponent | Book CRUD, quiz observation |
| `QuizRepository` | MksDatabase + KoinComponent | Quiz/question/session CRUD, categories |
| `KnowledgeRepository` | MksDatabase + KoinComponent | Flashcard decks/cards, slides, notes, prompts |

**Circular deps resolved:** KoinComponent + `by inject()` (replaces Hilt's `Provider<T>`)

---

## 📦 Platform Abstractions

| Interface | Android | Desktop |
|-----------|---------|---------|
| `FileManager` | Context.filesDir + ContentResolver | FHS `~/.local/share/mks/` |
| `TtsManager` | android.speech.tts.TextToSpeech | espeak-ng subprocess (10s timeout) |
| `ImageLoader` | Coil (memory + disk cache) | LRU memory cache + disk cache |
| `FileDialog` | SAF ActivityResultContracts | AWT JFileChooser |

---

## 🚀 Build Commands

```bash
# Verify project model
./gradlew projects --no-configuration-cache

# Compile shared module (desktop target)
./gradlew :shared:compileKotlinDesktop

# Compile full desktop app
./gradlew :app-desktop:compileKotlin

# Build desktop distribution (macOS .app / Fedora .rpm)
./gradlew :app-desktop:packageDistributionForCurrentOS

# Fedora RPM
./gradlew :app-desktop:packageRpm
```

### Desktop Data Paths

```
~/.local/share/mks/               Data root
~/.local/share/mks/mks_database.db SQLite database
~/.local/share/mks/images/         Image storage
~/.local/share/mks/cache/          Image cache
```

---

## 📝 Migration History

| Date | Phase | Changes |
|------|-------|---------|
| June 2026 | 1 | Gradle KMP restructuring, version catalog, shared module scaffold |
| June 2026 | 2 | SQLDelight schema + 26 entities migrated to commonMain |
| June 2026 | 3 | Hilt → Koin migration, 7 repositories, circular dep resolution |
| June 2026 | 4 | Compose MP 1.10.1 enabled, 14 ViewModels migrated |
| June 2026 | 5 | Desktop app packaged, Library screen functional, repos implemented |

---

**Last Updated:** June 16, 2026  
**Database Version:** SQLDelight v30 schema (26 tables)  
**Kotlin:** 100% of shared codebase
