# AI Context & Navigation Guide for MKS (Multiplatform)

Welcome, AI Agent. This document serves as your **Master Context Guide** for the MKS application — now a Compose Multiplatform project targeting Android and Linux Desktop.

Whenever you are initialized in this workspace, **read this file first**.

---

## 1. Project Overview

**MKS** is a quiz and knowledge-bank app that imports educational content from spreadsheets/documents and presents quizzes, flashcards, slideshows, notes, and AI prompts.

- **Language:** Kotlin (100% — Multiplatform)
- **UI:** Compose Multiplatform 1.10.1
- **DI:** Koin 4.0.3
- **Database:** SQLDelight 2.0.2 (26 tables, v30 schema)
- **Targets:** Android + JVM Desktop (Fedora Linux)
- **Build:** Gradle 9.4.1 | Kotlin 2.2.10 | AGP 9.2.1

---

## 2. Module Architecture

```
shared/              ← KMP shared code (ALL new development goes here)
├── commonMain/       Platform-agnostic: entities, repos, VMs, DI, SQLDelight
├── androidMain/      Android platform impls (Coil, SAF, Android TTS, Room driver)
└── desktopMain/      Desktop platform impls (FHS paths, espeak-ng, Skiko, JFileChooser)

app/                 ← Android application (legacy source in core/* + feature/*)
app-desktop/         ← Desktop JVM application (Main.kt — 4-tab Material3 app)
core/                5 legacy Android library modules (model, database, data, network, ui)
feature/             1 legacy Android feature module (ui — screens + ViewModels)
```

### Key Directories (shared/commonMain)

| Path | Contents |
|------|----------|
| `data/local/entity/` | 26 domain entities (@Serializable, Room-free) |
| `data/repository/` | 7 repositories with SQLDelight queries |
| `data/model/` | Domain models (MksResult, CategoryWithMetadata) |
| `sqldelight/.../db/` | 1.sqm migration + Schema.sq + 12 query files |
| `di/` | DataModule, ViewModelModule, UtilityModule (Koin) |
| `platform/` | 4 interfaces: FileManager, TtsManager, ImageLoader, FileDialog |
| `ui/` | 14 ViewModels (library, quiz, flashcard, slideshow, etc.) |
| `util/` | MksLogger (expect/actual), BoundedStreams, MksRoutes |

---

## 3. How to Find Code

### To find an entity:
```
shared/src/commonMain/kotlin/com/ahmedyejam/mks/data/local/entity/
```

### To find a repository:
```
shared/src/commonMain/kotlin/com/ahmedyejam/mks/data/repository/
```

### To find SQLDelight queries:
```
shared/src/commonMain/sqldelight/com/ahmedyejam/mks/db/*.sq
```

### To find platform implementations:
```
shared/src/androidMain/   (Android: AndroidFileManager, AndroidTtsManager, etc.)
shared/src/desktopMain/   (Desktop: DesktopFileManager, DesktopTtsManager, etc.)
```

### To find Koin modules:
```
shared/src/commonMain/kotlin/com/ahmedyejam/mks/di/
```

---

## 4. Architectural Patterns

### Dependency Injection (Koin)
- **Rule:** Use Koin `module { }` DSL in `di/` directory
- **Circular deps:** Use `KoinComponent` + `by inject()` (replaces Hilt's `Provider<T>`)
- **Platform bindings:** Registered in `androidPlatformModule` / `desktopPlatformModule`

### Database (SQLDelight)
- **Schema:** Defined in `Schema.sq` + `1.sqm` (CREATE TABLE IF NOT EXISTS)
- **Queries:** 12 `.sq` files with globally unique names (e.g., `bk_selectAll`, `qz_insert`)
- **Generated code:** `MksDatabase` interface with query classes (e.g., `BookQueriesQueries`)
- **Mapping:** SQLDelight rows → domain entities via private extension functions in repos

### Compose Multiplatform
- **UI code** should go in `shared/src/commonMain/` whenever possible
- **Platform-specific UI** uses `expect`/`actual` composables
- **`androidx.compose.*`** is available directly via Compose MP (no Jetpack Compose BOM)
- **ViewModel pattern:** Plain class with `KoinComponent` + `by inject()`, registered as `factory` in Koin

---

## 5. Common Tasks

### Add a new database column
1. Add column to `Schema.sq` and `1.sqm` (ALTER TABLE ADD COLUMN IF NOT EXISTS pattern)
2. Update the corresponding entity in `data/local/entity/`
3. Update the SQLDelight query files if needed
4. Regenerate: `./gradlew :shared:generateCommonMainMksDatabaseInterface`

### Add a new repository method
1. Add SQLDelight query to the appropriate `.sq` file (use entity prefix: `bk_`, `qz_`, etc.)
2. Implement method in repository class
3. Add mapper extension if returning domain entities

### Add a new screen
1. Create ViewModel in `shared/.../ui/{feature}/`
2. Create Composable screen in `shared/.../ui/{feature}/`
3. Register ViewModel in `ViewModelModule.kt`
4. Add screen to desktop `Main.kt` or Android `MksNavHost`

### Verify build
```bash
./gradlew projects --no-configuration-cache    # Project model
./gradlew :shared:compileKotlinDesktop         # Shared module
./gradlew :app-desktop:compileKotlin           # Full desktop app
```

---

## 6. Key Files Reference

| File | Purpose |
|------|---------|
| `shared/build.gradle.kts` | KMP configuration |
| `gradle/libs.versions.toml` | Centralized dependency versions |
| `shared/src/commonMain/sqldelight/.../1.sqm` | Database migration (v30) |
| `shared/src/commonMain/.../di/DataModule.kt` | Repository + DB Koin bindings |
| `shared/src/commonMain/.../di/ViewModelModule.kt` | ViewModel factory registrations |
| `app-desktop/src/.../desktop/Main.kt` | Desktop entry point |
| `docs/migration/multiplatform_desktop_build_plan.md` | Full migration plan |

---

**Last Updated:** June 16, 2026
