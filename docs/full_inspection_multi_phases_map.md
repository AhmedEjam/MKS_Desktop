# Full Inspection & Multi-Phases Map (Current State)

> **⚠️ REGENERATED:** 2026-06-15. This document is now the **authoritative current-state inspection** based on CLAUDE.md (v29, 6-module architecture). Previous pre-refactor version was severely outdated and has been replaced.

---

## Overview: Project Structure (6-Module Modern Architecture)

### Module Layout

```
/Users/ahmedy.ajam/Android MKS/
├── app/                          # Main application module (UI entry point, Activities, Hilt DI modules)
├── core/
│   ├── database/                 # Room database module (v29, 26 entities, 28 migrations, 26 DAOs)
│   ├── data/                     # Domain repository module (6 repos, import pipeline, preferences, preview services)
│   ├── model/                    # Shared domain models (entities, UI routes, search models)
│   ├── network/                  # Network module (OllamaRepository, RemoteAssetFetcher)
│   └── ui/                       # Shared UI components (theme, common composables)
└── feature/
    └── ui/                       # Feature UI screens (library, quiz, flashcard, dashboard, etc.)
```

### 6 Domain Repositories (All @Inject constructor)

| Repository | Location | Responsibility | Test Status |
|---|---|---|---|
| `BookRepository` | `core/data/repository/` | Book CRUD, cover images, stats, study bundles | 🔴 No tests |
| `QuizRepository` | `core/data/repository/` | Quiz/Question CRUD, answers, scoring, categories | 🔴 No tests |
| `KnowledgeRepository` | `core/data/repository/` | Flashcards, slideshows, notes, prompts, learning sessions, derived sync | 🔴 No tests |
| `StudyRepository` | `core/data/repository/` | Quiz sessions, knowledge study sessions, mistake logs | 🔴 No tests |
| `WorkspaceRepository` | `core/data/repository/` | Workspace CRUD, default workspace, soft deletes | 🔴 No tests |
| `AssetRepository` | `core/data/repository/` | Asset references, question assets, source documents, annotations | 🔴 No tests |

**Circular Dependencies (Broken via `javax.inject.Provider<T>`):**
- `BookRepository` ↔ `QuizRepository`
- `BookRepository` ↔ `AssetRepository`
- `QuizRepository` ↔ `AssetRepository`
- `KnowledgeRepository` ↔ `QuizRepository`
- `KnowledgeRepository` ↔ `BookRepository`
- `KnowledgeRepository` ↔ `AssetRepository`

---

## Database (Room v29)

### Current Schema

**26 Entities:**

**Workspace Tier (2):**
- `WorkspaceEntity` — Multi-workspace support with soft deletes
- `WorkspaceSettingsEntity` — Workspace-scoped preferences (language, theme, defaults)

**Core Quiz/Library Tier (7):**
- `BookEntity` — Top-level container; cover image; stats (total questions, answered, time); pin/system flags; `externalId` unique index for dedup
- `QuizEntity` — Quiz inside book; tags; cover image; cached stats; `externalId` unique index
- `QuestionEntity` — Single/Multiple/Boolean types; options (List<String>); correct answer indices; explanation, hint, reference; image URL; categories; attempts, weight
- `SessionEntity` — Quiz session; user answers (JSON); completion state; timer settings; streaks; score
- `CategoryMetadataEntity` — Category labels; emoji, color, pin state per category
- `QuestionCategoryEntity` — Normalized many-to-many question/category join
- `QuestionAssetEntity` — Generic assets (images, docs) linked to questions

**Knowledge Bank Tier (10):**
- `FlashcardDeckEntity` — Deck metadata; progress counters; pin flags; timestamps
- `FlashcardEntity` — Front/back; hint; tags; review metrics (count, ease, interval); optional `sourceQuestionId` link; `syncConfig` JSON
- `LearningSessionEntity` — Flashcard-deck session state stored as JSON
- `SlideshowCourseEntity` — Course metadata; progress; pin/system/derivation flags; optional `sourceQuizId`
- `CourseSlideEntity` — Slide body; notes; image; order; completion state; optional `sourceQuestionId`; sync config
- `NoteBlueprintEntity` — Note body (markdown); summary; bullet points (JSON); tags; review counters
- `NoteCollectionEntity` — Grouping for note blueprints
- `PromptDeckEntity` — AI prompt deck metadata; tags; timestamps
- `PromptCardEntity` — Individual prompt card; stem (template); variables; output type; timestamps
- `PromptRunEntity` — Prompt execution history; variables (JSON); output

**Knowledge Study Tracking (2):**
- `KnowledgeStudySessionEntity` — Generic progress tracker for non-quiz assets; timestamps; streak counters; completion %
- `StudySessionEntity` — Non-quiz study session progress tracker

**Assets & Additional (5):**
- `AssetReferenceEntity` — Normalized local asset ownership index
- `SourceDocumentEntity` — Source materials (PDFs, URLs, textbooks) linked to books
- `MistakeLogEntryEntity` — Mistake tracking; user explanations; feedback flags
- `AnnotationEntity` — Highlights and notes on content (questions, slides, notes, etc.)
- **Dropped in v29:** `SourceDocumentAssetEntity` (v28 only)

### Migrations (28 total: v1→v29)

**Current Status:**
- v1→v15 — Not covered by test cases (likely in legacy deployments only)
- v15→v16 ✅ `Migration15To16Test` — Knowledge-bank tables + column safety checks
- v16→v17 ✅ `Migration16To17Test` — Category/asset tables
- v17→v22 — Not covered by test cases (4 steps missing)
- v22→v23 ✅ `Migration22To23Test` — Verified
- v23→v24 ✅ `Migration23To24Test` — Verified
- v24→v25 ✅ `Migration24To25Test` — Verified
- v25→v26 ✅ `Migration25To26Test` — Verified
- v26→v27 🔴 **No test** — Added spaced repetition fields, note collections, study sessions
- v27→v28 🔴 **No test** — Added source document assets
- v28→v29 🔴 **No test** — Dropped source document assets

**Action Required:** Create `Migration26To27Test`, `Migration27To28Test`, `Migration28To29Test` + one full `MigrateAll1To29Test` chain test.

---

## Hilt Dependency Injection (6 Modules)

All in `app/di/`:

### HiltDataModule
**Provides (singleton):**
- `AppModule` — Legacy container for startup settings
- `MksDatabase` — Room database with all migrations
- `FileManager` — File I/O
- `ExportManager` — ZIP export orchestrator
- `ImportLibraryManager` — Multi-format import pipeline
- `LibraryMapper` — DTO ↔ Entity bidirectional mapping
- `DataStoreManager` — Jetpack DataStore preferences
- `FocusManager` — Adaptive training logic

### HiltDaoModule
**Provides (singleton):**
- 6 Core Quiz DAOs: `BookDao`, `QuizDao`, `QuestionDao`, `SessionDao`, `CategoryMetadataDao`, `QuestionCategoryDao`

### HiltKnowledgeDaoModule
**Provides (singleton):**
- 10 Knowledge Bank DAOs: `SlideshowCourseDao`, `CourseSlideDao`, `NoteBlueprintDao`, `NoteCollectionDao`, `PromptDao`, `PromptDeckDao`, `PromptCardDao`, `PromptRunDao`, `KnowledgeStudySessionDao`, `StudySessionDao`

### HiltUtilityDaoModule
**Provides (singleton):**
- 10 Utility DAOs: `AssetReferenceDao`, `QuestionAssetDao`, `SourceDocumentDao`, `GlobalSearchDao`, `AnnotationDao`, `WorkspaceDao`, `FlashcardDeckDao`, `FlashcardDao`, `LearningSessionDao`, `MistakeLogDao`

### HiltRepositoryModule
**Provides (singleton):**
- `OllamaRepository` — Ollama local LLM integration (not `@Inject constructor`)
- `GlobalSearchRepository` — Full-text search orchestrator
- `ReviewRepository` — Unified review queue (7 types)

### HiltServiceModule
**Provides (singleton):**
- `DeletePreviewService` — Preview delete impacts
- `CategoryMergePreviewService` — Preview category merges
- `ClearMarksPreviewService` — Preview clear-marks impact
- `AssetReferenceAuditService` — Verify asset reference integrity

---

## Import Pipeline (12 Format-Specific Parsers)

### Multi-Format Orchestration

```
User selects file (URI)
  ↓
ImportLibraryManager.orchestrateImport()
  ├─ ImportFormatDetector.detectFormat() → ImportFormat enum
  │  └─ Order: extension → MIME → magic bytes → heuristic
  │
  ├─ Format-specific parser
  │  ├─ XLSX → XlsxLibraryCompiler.compile() (full library DTO)
  │  ├─ CSV/TSV → CsvParser.parse()
  │  ├─ JSON → JsonLibraryParser.parse()
  │  ├─ HTML → HtmlQuestionParser.parse()
  │  ├─ TEXT → TextQuestionParser.parse() + TextFlashcardParser, TextSlideParser, TextArticleParser
  │  ├─ PPTX → PptxSlideParser.parse() (Apache POI)
  │  ├─ ZIP → ZipLibraryParser.parse() (MKS bundle format)
  │  └─ UNKNOWN → fallback error handling
  │
  ├─ BundleNormalizer.normalize() — Post-parse cleanup (answer mode inference, trimming)
  ├─ ImportValidator.validate() — Pre-persist validation
  └─ Domain repos insert → database
```

### 12 Parsers

| Parser | Input | Output | Notes |
|--------|-------|--------|-------|
| `SpreadsheetQuestionParser` | XLSX/CSV rows | `List<ParsedQuestion>` | Header row detection + column mapping |
| `CsvParser` | CSV/TSV text | `List<List<String>>` | RFC 4180 + delimiter inference |
| `JsonQuestionParser` | JSON objects | `List<ParsedQuestion>` | Nested question array |
| `JsonLibraryParser` | JSON bundle | `LibraryBundleDto` | Full library structure |
| `HtmlQuestionParser` | HTML tables/divs | `List<ParsedQuestion>` | DOM extraction |
| `TextQuestionParser` | Plain text lines | `List<ParsedQuestion>` | Line-by-line Q&A parsing |
| `TextFlashcardParser` | Plain text | `List<FlashcardEntity>` | 3 parse modes (front/back per line, blank-line sep, Q&A format) |
| `TextSlideParser` | Plain text | `List<CourseSlideEntity>` | 3 parse modes (slides from text) |
| `TextArticleParser` | Plain text | `List<NoteBlueprintEntity>` | 2 parse modes (articles from text) |
| `PptxSlideParser` | PPTX file | `List<CourseSlideEntity>` | Apache POI extraction |
| `ZipLibraryParser` | MKS ZIP bundle | `LibraryBundleDto` | Manifest + asset bundling |
| `XlsxLibraryCompiler` | XLSX file | `LibraryBundleDto` | Full XLSX → library compilation |

### Image Resolution (Multi-Source Priority)

1. **XLSX embedded images** (via `XlsxImageResolver`)
2. **Image column** (URL or Base64)
3. **Question/answer cells** (embedded URL or data URI)
4. **Option cells** (fallback)
5. **Row-level images** (XLSX only)
6. **Merged cell regions** (if cell empty)

All images downloaded to local file storage or saved as Base64.

---

## ViewModel Injection Map

| ViewModel | Injected Repos | Additional | Notes |
|-----------|---|---|---|
| `LibraryViewModel` | Book, Quiz, Knowledge, Workspace, Asset | ExportManager, DataStoreManager | Main hub |
| `BookToolsViewModel` | Book, Quiz, Knowledge, Study, Asset | Ollama, DataStoreManager, FileManager | Knowledge dashboard |
| `QuizViewModel` | Quiz, Knowledge, Study, Asset | DataStoreManager, FocusManager | Quiz player |
| `FlashcardDeckViewModel` | Knowledge, Study | **AppModule** (applicationScope) | Flashcard study |
| `SlideshowCourseViewModel` | Knowledge | **AppModule** (applicationScope) | Slideshow player |
| `CompilerViewModel` | Knowledge, Quiz | @ApplicationContext | Import compiler |
| `CategoryQuestionsViewModel` | Quiz, Knowledge, Asset | — | Category browser |
| `QuizQuestionsViewModel` | Quiz, Knowledge, Asset | — | Question browser |
| `ReviewDashboardViewModel` | Study | ReviewRepository | Review queue |
| `GlobalSearchViewModel` | — | GlobalSearchRepository | Cross-entity search |
| `SummaryViewModel` | Study, Quiz | DataStoreManager | Post-quiz summary |
| `SessionViewModel` | Study | — | Session management |
| `DataToolsViewModel` | Book, Quiz | ExportManager, ImportLibraryManager | Data tools |
| `ScannerViewModel` | Quiz | @ApplicationContext | OCR scanner |
| `SettingsViewModel` | — | DataStoreManager | Settings (+ **AppModule** direct coupling) |
| `ImportViewModel` | Book, Quiz, Knowledge | ImportLibraryManager | Full-library import |

**Remaining AppModule Coupling:** `FlashcardDeckViewModel`, `SlideshowCourseViewModel`, `SettingsScreen` still reference `AppModule` directly. Target for Phase 2 cleanup.

---

## Testing (Current Coverage)

### Instrumented Tests (androidTest)
- ✅ `Migration15To16Test` — v15→v16 knowledge-bank tables
- ✅ `Migration16To17Test` — v16→v17 category/asset tables
- ✅ `Migration22To23Test`
- ✅ `Migration23To24Test`
- ✅ `Migration24To25Test`
- ✅ `Migration25To26Test`
- 🔴 `Migration26To27Test` — **MISSING**
- 🔴 `Migration27To28Test` — **MISSING**
- 🔴 `Migration28To29Test` — **MISSING**
- ✅ `AnnotationDaoTest`, `QuestionDaoTest`, `QuestionCategoryDaoTest`, `AssetReferenceDaoTest` — DAO tests
- ✅ `ImportReconciliationTest`, `XlsxImportTest`, `ImportTargetQuizTest` — Import tests

### Unit Tests (test)
- ✅ `SpreadsheetHeaderMapperTest` — Header detection
- ✅ `ParserBugFixTest` — Parser regressions
- ✅ `BoundedStreamsTest` — Security limits
- ✅ `ImportValidatorTest` — Validation
- ✅ `FileManagerTest` — File I/O
- ✅ `SettingsSanitizerTest` — Preferences
- ✅ `QuestionValidatorTest`, `SessionStateValidatorTest` — Data validation
- ✅ `MksRouteBuildersTest` — Navigation

### Repository Tests
- 🔴 **ZERO repository test suites** — All 6 domain repos untested

---

## Infrastructure

### Gradle & Build
- **Version Catalog:** ✅ `gradle/libs.versions.toml` (in use)
- **CI Pipeline:** ✅ `.github/workflows/android-ci.yml` (runs lint→test→build)
- **KSP:** Room + Moshi code generation enabled
- **Target SDK:** 35, Min SDK: 30, Compile SDK: 35
- **JVM Target:** Java 11
- **Modules:** `:app`, `:core:model`, `:core:database`, `:core:data`, `:core:network`, `:core:ui`, `:feature:ui`

### Repository Hygiene
- 🔴 **~25 scripts in root:** `add_hilt*.py`, `fix_*.py`, `patch_*.py`, `update_*.py`, `test_*.py` (8+), `query_books.kt`, `test_parser.kt`, generated reports
- **Action:** Move to `scripts/legacy/` + document in `CONTRIBUTING.md`

---

## Known Issues & Debt

| Issue | Priority | Status | Notes |
|-------|----------|--------|-------|
| Missing migration tests (v26→v29) | 🔴 | OPEN | Add 3 tests + full chain test |
| Zero repository tests | 🔴 | OPEN | Add test harness + 6 repo suites |
| AppModule coupling (3 points) | 🟡 | BLOCKED | After phase 1, cleanup in phase 2 |
| Root script hygiene | 🔴 | OPEN | Move ~25 scripts to archive |
| CI coverage/linting gates | 🟡 | OPEN | Add detekt, ktlint, Kover, schema diff |
| Crash observability | 🔴 | OPEN | Global exception handler + Result<T> patterns |
| SourceDocumentAssetEntity dropped | ✅ | RESOLVED | v28→v29 migration verified |

---

## Quick Reference: Project Files

### Core Database
- **Schema:** `core/database/src/main/java/com/ahmedyejam/mks/data/local/MksDatabase.kt`
- **Migrations:** `core/database/src/main/java/com/ahmedyejam/mks/data/local/MksMigrations.kt`
- **DAOs:** `core/database/src/main/java/com/ahmedyejam/mks/data/local/dao/` (26 files)
- **Entities:** `core/model/src/main/java/com/ahmedyejam/mks/data/local/entity/` (26 files)

### Domain Layer
- **Repositories:** `core/data/src/main/java/com/ahmedyejam/mks/data/repository/` (6 files)
- **Import Pipeline:** `core/data/src/main/java/com/ahmedyejam/mks/data/importer/` (12+ parsers)
- **Preferences:** `core/data/src/main/java/com/ahmedyejam/mks/data/preferences/`
- **Preview Services:** `core/data/src/main/java/com/ahmedyejam/mks/data/preview/`

### UI Layer
- **Navigation:** `feature/ui/src/main/java/com/ahmedyejam/mks/ui/MksNavHost.kt` (22+ routes)
- **Screens:** `feature/ui/src/main/java/com/ahmedyejam/mks/ui/{quiz,flashcard,slideshow,booktools,search,review,data,importer,settings,trash,workspace,welcome}/`

### Hilt DI
- **Modules:** `app/src/main/java/com/ahmedyejam/mks/di/` (6 files: HiltDataModule, HiltDaoModule, HiltKnowledgeDaoModule, HiltUtilityDaoModule, HiltRepositoryModule, HiltServiceModule)

### Tests
- **Instrumented:** `app/src/androidTest/java/com/ahmedyejam/mks/` + `core/database/src/androidTest/`
- **Unit:** `core/data/src/test/java/com/ahmedyejam/mks/data/`

---

## Conclusion

MKS is a **mature 6-module Android application** with solid architecture (Hilt DI, modular repos, multi-format import). The codebase is current at v29 with 26 entities and 28 migration steps. Primary debt items are:

1. **Missing migration tests (v26→v29)** + zero repository test suites → add comprehensive test harness
2. **~25 legacy scripts cluttering root** → move to archive
3. **AppModule coupling (3 points)** → scheduled for phase 2
4. **CI gates** → extend existing pipeline with linting/coverage/schema checks

All other architectural components are sound and ready for feature development. See `mks_enhancement_plan.md` for prioritized next steps.

