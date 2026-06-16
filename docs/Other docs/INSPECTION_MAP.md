> Finalization status correction (2026-05-25): this older inspection map contains stale v15 wording. The active implementation is Room v17 with 16 migration steps and the v16/v17 knowledge-bank/category/asset additions. Use `AGENTS.md`, `MksDatabase.kt`, and `FINALIZATION_PATCH_REPORT.md` as the current references before applying future patches.

# 🔍 MKS PROJECT - COMPONENT REVIEW & INSPECTION BLUEPRINT

## Executive Summary

MKS is a multi-layered Android quiz/study application with **8 major functional blocks**, each responsible for specific domains. This blueprint organizes the project for systematic inspection, testing, and maintenance.

---

## 📊 COMPONENT ARCHITECTURE MAP

```
┌─────────────────────────────────────────────────────────────┐
│                      APPLICATION LAYER                      │
│  (MainActivity, MksApplication, Navigation, Theme, I18N)    │
├─────────────────────────────────────────────────────────────┤
│                       UI LAYER (MVVM)                       │
│  Screen + ViewModel pairs across 10 routes                  │
├─────────────────────────────────────────────────────────────┤
│           DATA LAYER (Repositories + Managers)              │
│  Repository, Import/Export, File Management                 │
├─────────────────────────────────────────────────────────────┤
│             PERSISTENCE LAYER (Room + DataStore)            │
│  Database (v17), DAOs, Entities, Migrations                 │
├─────────────────────────────────────────────────────────────┤
│          INFRASTRUCTURE LAYER (DI, Utilities)               │
│  AppModule, FileManager, Image Caching, Localization        │
└─────────────────────────────────────────────────────────────┘
```

---

## 🧩 FUNCTIONAL BLOCKS (8 MAJOR COMPONENTS)

### **BLOCK 1: APPLICATION INITIALIZATION & FRAMEWORK**

**Purpose:** Bootstrap the app, setup global dependencies, configure UI framework

**Key Files:**
- `MksApplication.kt` - App class, Coil image loader setup
- `MainActivity.kt` - Single activity entry point
- `AppModule.kt` (838 lines) - Manual DI container with lazy-initialized singletons
- `Theme.kt`, `Color.kt`, `Type.kt` - Design system
- `MksDesignTokens.kt` - Theme constants

**Responsibilities:**
1. Initialize `AppModule` (database, repository, managers)
2. Setup Coil image loader with caching (25% RAM, disk cache enabled)
3. Configure Material3 theme (dynamic/static)
4. Initialize DataStore for user preferences
5. Seed sample data on first launch

**Dependencies Provided:**
```
appModule = {
  database: MksDatabase,
  fileManager: FileManager,
  repository: MksRepository,
  exportManager: ExportManager,
  importManager: ImportLibraryManager,
  dataStoreManager: DataStoreManager,
  focusManager: FocusManager,
  applicationScope: CoroutineScope
}
```

**Inspection Checklist:**
- [ ] Verify `AppModule` initialization order (singletons lazy-loaded)
- [ ] Confirm Coil cache settings match performance requirements
- [ ] Test theme/language switching persistence
- [ ] Validate database migrations execute successfully
- [ ] Check seed data loads correctly on fresh install

**Related Test Files:**
- N/A (primarily integration concerns)

---

### **BLOCK 2: NAVIGATION & ROUTING**

**Purpose:** Manage screen transitions, routing logic, and navigation state

**Key Files:**
- `MksNavHost.kt` (328 lines) - Navigation graph definition
- `MainActivity.kt` - NavHost container
- All `*Screen.kt` Composables (12 screens)
- All `*ViewModel.kt` factory patterns

**Routes (10 main destinations + conditionals):**
```
library (START)
├─ CompilerDialog (embedded)
├─ LibraryScreen (books/quizzes grid)
├─ Settings route
└─ Scanner setup

quiz_questions/{quizId}
└─ QuizQuestionsScreen

category/{category}
└─ CategoryQuestionsScreen

quiz/{quizId}?sessionId={sessionId}
└─ QuizPlayerScreen

sessions/{quizId}
└─ SessionManagementScreen

scanner/{quizId}
└─ ScannerScreen

settings
└─ SettingsScreen

welcome (conditional)
└─ WelcomeScreen

booktools, flashcard, adaptive training
└─ Specialized screens
```

**Navigation Patterns:**
1. Object-based route definitions
2. Manual `ViewModelProvider.Factory` for each screen
3. `LaunchedEffect` for data loading after recomposition
4. `NavController.navigate()` for transitions

**Inspection Checklist:**
- [ ] All routes have corresponding Composables
- [ ] Each screen has proper ViewModel factory
- [ ] Back stack behavior correct for all flows
- [ ] Deep links handled (if applicable)
- [ ] State preservation across recompositions
- [ ] No memory leaks in ViewModel scoping

**Related Test Files:**
- Navigation logic tested implicitly in UI tests

---

### **BLOCK 3: DATABASE & PERSISTENCE (Room v17)**

**Purpose:** Define entity schema, migrations, and database access layer

**Key Files:**
- `MksDatabase.kt` (v17) - Room database class
- `entity/` - 8+ entity classes:
  - `BookEntity` - Top-level container
  - `QuizEntity` - Quiz metadata
  - `QuestionEntity` - Question data + options
  - `SessionEntity` - User progress/answers
  - `CategoryMetadataEntity` - Category styling
  - `FlashcardEntity`, `FlashcardDeckEntity` - Flashcard system
  - `LearningSessionEntity` - Study sessions
  - `*SlideEntity`, `*PromptEntity`, `*NoteEntity` - Advanced features
- `dao/` - Data Access Objects (8+ DAOs)
- `Converters.kt` - Type converters for complex fields

**Schema Overview:**
```
BookEntity
├─ PK: id (Long)
├─ externalId: String
├─ title, description: String
├─ fields: List<String> (JSON)
├─ coverImage: String?
├─ Stats: questionCount, answeredCount, completionPercentage
├─ Metadata: isSystem, timestamps
└─ Relationships: 1→ many QuizEntities

QuizEntity
├─ PK: id (Long)
├─ FK: bookId
├─ title, description, category: String
├─ coverImage, metadata
└─ Relationships: 1→ many QuestionEntities

QuestionEntity
├─ PK: id (Long)
├─ FK: quizId
├─ text: String (stem)
├─ type: QuestionType (SINGLE, MULTIPLE, BOOLEAN)
├─ options: List<String> (JSON)
├─ correctAnswers: List<String> (option IDs, JSON)
├─ Metadata: explanation, hint, reference, imagePath
├─ Metrics: attempts, correctCount, weight
└─ Categories: List<String> (JSON)

SessionEntity
├─ PK: id (Long)
├─ FK: quizId
├─ answers: Map<Long, List<String>> (question→selected options)
├─ isCompleted: Boolean
├─ Metrics: timestamp, score
└─ Status: in-progress or archived
```

**Migrations (14 total: v1→v17):**
- Incremental ALTER TABLE statements
- Pattern: `MIGRATION_N_(N+1) = object : Migration(...)`
- Added columns include: new entity types, metrics, metadata

**Type Converters:**
- `@ProvidedTypeConverter` for List<String>, Map<T, T>, etc.
- JSON serialization via Moshi (KSP-generated adapters)

**Inspection Checklist:**
- [ ] All migrations tested (especially edge cases)
- [ ] Entity relationships properly defined (FKs)
- [ ] Indices on frequently queried columns
- [ ] DAO methods cover all CRUD operations
- [ ] Type converters handle null values safely
- [ ] Database constraints enforce data integrity
- [ ] Transactions used for multi-step operations
- [ ] Test migrations on schema upgrade path

**Related Test Files:**
- `app/src/androidTest/.../dao/QuestionDaoTest.kt`
- Migration testing (consider test migrations)

---

### **BLOCK 4: IMPORT PIPELINE (Multi-Format)**

**Purpose:** Detect file formats, parse various document types, validate, and normalize data

**Key Files:**

**Format Detection:**
- `data/import/detector/ImportFormatDetector.kt`
  - Extension-based detection
  - MIME type checking
  - Magic byte inspection (ZIP, XLSX, OLE2, JSON, HTML)
  - Tabular heuristic (CSV/TSV)
  - Result: `ImportFormat` enum

**Spreadsheet Parsing:**
- `data/import/xlsx/XlsxLibraryCompiler.kt` - XLSX orchestrator
- `data/import/xlsx/XlsxImageResolver.kt` - Embedded image extraction
- `data/import/parser/SpreadsheetHeaderMapper.kt` - Header detection (multi-language)
- `data/import/parser/SpreadsheetQuestionParser.kt` - Row parsing
- `data/import/parser/GenericImageExtractor.kt` - Image cell detection

**Format-Specific Parsers:**
- `JsonQuestionParser.kt`
- `HtmlQuestionParser.kt`
- `TextQuestionParser.kt`

**Orchestration:**
- `ui/quiz/CompilerViewModel.kt` (407 lines)
  - User-adjustable flow
  - Live preview of questions
  - State management (header, mapping, options)

**Validation & Normalization:**
- `data/import/mapping/LibraryMapper.kt` - Map parsed→entities
- `data/import/normalization/BundleNormalizer.kt` - Data consistency

**Inspection Checklist:**
- [ ] All 6 formats detected correctly (XLSX, CSV, TSV, JSON, HTML, TEXT, ZIP)
- [ ] Header detection scoring algorithm tuned
- [ ] Multi-language field aliases comprehensive
- [ ] Image extraction from XLSX verified
- [ ] Quote handling in CSV/TSV RFC-4180 compliant
- [ ] Merged cell handling in XLSX
- [ ] Duplicate images not re-downloaded
- [ ] Base64 images decoded correctly
- [ ] Validation prevents invalid questions (blank stem + no image)
- [ ] Error messages user-friendly

**Related Test Files:**
- `app/src/test/.../data/import/parser/ParserBugFixTest.kt`
- `app/src/androidTest/.../data/import/ImportReconciliationTest.kt`
- `app/src/androidTest/.../data/import/ImportTargetQuizTest.kt`

---

### **BLOCK 5: REPOSITORY & DATA ACCESS (MksRepository)**

**Purpose:** Single source of truth for all data operations, combining database, files, and preferences

**Key Files:**
- `data/repository/MksRepository.kt` (480+ lines)
  - CRUD operations (Books, Quizzes, Questions, Sessions)
  - Image management (download, save, path resolution)
  - Statistics calculations (accuracy, completion)
  - Search functionality (full-text across entities)
  - Session management (create, resume, complete)
  - Export/import integration

**Core Operations:**

**Books:**
- `getBooks()`, `getBookById()`, `createBook()`, `updateBook()`, `deleteBook()`
- `getBookStats()` - Quiz/question counts, completion %

**Quizzes:**
- `getQuizzes()`, `getQuizById()`, `createQuiz()`, `updateQuiz()`, `deleteQuiz()`
- `getQuizzesByBook()`, `getQuizStats()`
- Multi-language category support

**Questions:**
- `getQuestions()`, `getQuestionById()`, `createQuestion()`, `updateQuestion()`
- `getQuestionsByQuiz()`, `getQuestionsByCategory()`
- Auto-set type (SINGLE/MULTIPLE based on correct answer count)
- Image download and caching

**Sessions:**
- `createSession()`, `getSession()`, `updateSession()`
- `submitAnswer()` - Answer recording
- `completeSession()` - Scoring logic
- `calculateScore()` - Algorithm implementation
- Session persistence (incomplete quizzes saved)

**Import/Export:**
- `importParsedQuestions()` - Saves parsed data to database
- `exportLibrary()` - JSON serialization
- `importLibraryFromJson()` - Reverse operation

**Statistics:**
- `updateQuestionStats()` - Track attempts, correct count
- `calculateAccuracy()` - Per-quiz/book performance

**Search:**
- `searchBooks()`, `searchQuizzes()`, `searchQuestions()`
- Full-text matching on titles, descriptions

**Inspection Checklist:**
- [ ] All CRUD operations atomic (transactions)
- [ ] Image downloads cached properly
- [ ] Statistics calculations accurate
- [ ] Session state correctly persisted
- [ ] Score calculation logic verified
- [ ] Search performance optimized (indices)
- [ ] Concurrent operations safe (coroutines)
- [ ] Null handling consistent
- [ ] Relationships maintained (cascade deletes if applicable)

**Related Test Files:**
- `app/src/test/.../data/repository/ExportManagerTest.kt`

---

### **BLOCK 6: UI LAYER (Screen + ViewModel Pairs)**

**Purpose:** Present data, handle user input, coordinate business logic

**Core Screens:**

**1. Library Hub**
- `LibraryScreen.kt` + `LibraryViewModel.kt`
- Books/quizzes grid, search, sorting
- FAB menu for actions (new book, import file, settings)
- Long-press for context menus (edit, delete, export)

**2. Quiz Player (Main Interface)**
- `QuizPlayerScreen.kt` + `QuizViewModel.kt`
- Question display with options
- Timer management
- Progress tracking
- Session state management
- Image display (zoomable via `ZoomableImageDialog.kt`)

**3. Quiz Questions Browser**
- `QuizQuestionsScreen.kt` + `QuizQuestionsViewModel.kt`
- Question list view
- Filter/sort options
- Preview questions

**4. Category Management**
- `CategoryQuestionsScreen.kt` + `CategoryQuestionsViewModel.kt`
- Questions grouped by category
- Category metadata editing (emoji, color, pin state)
- `CategoryEditDialog.kt`, `CategoryComponents.kt`

**5. Session History**
- `SessionManagementScreen.kt` + `SessionViewModel.kt`
- List of completed/in-progress sessions
- Resume incomplete quizzes
- View session results

**6. Import Workflow**
- `CompilerDialog.kt` + `CompilerViewModel.kt` (407 lines)
- Multi-stage UI:
  1. File selection
  2. Format detection
  3. Header mapping adjustment
  4. Column assignment
  5. Question preview
  6. Confirmation
- `ImportViewModel.kt` - File picker logic

**7. QR Scanner**
- `ScannerScreen.kt` + `ScannerViewModel.kt`
- Barcode detection
- Quiz import via QR codes

**8. Summary/Analytics**
- `SummaryScreen.kt` + `SummaryViewModel.kt`
- Post-quiz score display
- Performance chart
- Next steps recommendations

**9. Settings**
- `SettingsScreen.kt`
- Theme selection (dynamic/static colors)
- Language selection (English/Arabic RTL)
- Preference toggles

**10. Welcome**
- `WelcomeScreen.kt`
- Conditional startup (theme/language selection)

**11. Advanced Features**
- `FlashcardDeckScreen.kt` / `FlashcardDeckViewModel.kt` - Spaced repetition
- `BookToolsViewModel.kt` - Additional book tools
- Adaptive training (TBD)

**UI Patterns:**
- `StateFlow` for state management
- `collectAsState()` for recomposition
- `LaunchedEffect` for side effects
- Material3 design components
- RTL support for Arabic

**Inspection Checklist:**
- [ ] All screens responsive to state changes
- [ ] ViewModels properly scoped (not leaked)
- [ ] Loading/error states handled
- [ ] Coroutines cancelled on composable disposal
- [ ] Navigation state preserved on recomposition
- [ ] Accessibility annotations present
- [ ] Images render without janky scrolling
- [ ] InputField validation correct
- [ ] RTL layout flips automatically for Arabic
- [ ] Theme transitions smooth
- [ ] Search results update in real-time

**Related Test Files:**
- UI tests as part of instrumented tests

---

### **BLOCK 7: FILE I/O & IMAGE MANAGEMENT**

**Purpose:** Handle disk I/O, image downloads, caching, and resource management

**Key Files:**
- `data/local/FileManager.kt`
  - Image I/O operations
  - Base64 encoding/decoding
  - HTTP downloads via OkHttp
  - File path management

**Image Operations:**

**Saving:**
- `saveBase64AsImage(base64: String): String?`
  - Decodes Base64 → bytes
  - Saves to `filesDir/images/`
  - Returns local path

- `saveImage(inputStream: InputStream): String?`
  - Streams file to disk
  - Handles large files efficiently

- `saveImage(uri: Uri): String?`
  - ContentResolver integration
  - Works with system file picker

**Downloading:**
- `downloadAndSaveImage(url: String): String?`
  - OkHttp client for HTTPS URLs
  - Saves to local storage
  - Return path stored in DB

**Storage:**
- `getImagesDir(): File`
  - Returns `filesDir/images/`
  - Creates directory if missing
  - No external storage required

**Caching Strategy:**
```
Coil (Image Loader) - 2-tier cache
├─ Memory Cache: 25% of available RAM
├─ Disk Cache: Unlimited (Coil default)
│  Location: app-specific directory
│  Management: Auto-expires old entries
└─ Network: HTTPS URLs downloaded once, then local path stored

Never Overwrite: Images named img_<UUID>.<ext>
```

**Inspection Checklist:**
- [ ] No memory leaks in image loading
- [ ] Large files streamed (not fully buffered)
- [ ] OkHttp client configured correctly
- [ ] Cache hit rate monitored
- [ ] Disk usage within limits
- [ ] FileManager thread-safe
- [ ] Base64 images decoded without OutOfMemory
- [ ] Stale images cleaned up (if policy exists)
- [ ] Permission handling correct (Android 10+)
- [ ] Image URI sanitization prevents directory traversal

**Related Test Files:**
- `app/src/test/.../data/local/FileManagerTest.kt`

---

### **BLOCK 8: EXPORT & ADVANCED FEATURES**

**Purpose:** Library export, data serialization, adaptive training, and specialized modules

**Key Files:**

**Export Manager:**
- `data/repository/ExportManager.kt`
  - Serialize books/quizzes/questions to JSON
  - Create ZIP archives
  - Legacy format support (`LegacyBookDto`)
  - Handle circular references

**Data Models:**
- `data/model/ExportResult.kt` - Export operation result
- `data/model/CategoryWithMetadata.kt` - Category grouping
- `data/model/LearningSessionState.kt` - Study session state (Moshi serializable)

**Preferences:**
- `data/preferences/DataStoreManager.kt`
  - Theme setting (enum)
  - Language setting (enum)
  - User preferences (typed, serialized)
  - Reactive updates via Flow

**Advanced Features (In Development):**
- **Flashcards:** `FlashcardEntity`, `FlashcardDeckEntity`
  - Spaced repetition algorithm
  - Deck management
  - Study sessions

- **Adaptive Training:**
  - Question difficulty adjustment
  - Performance-based routing
  - Focus Manager (difficulty level tracking)

- **Slideshow Courses:**
  - `SlideshowCourseEntity`, `CourseSlideEntity`
  - Sequential learning flow

- **Knowledge Notes:**
  - `NoteBlueprintEntity` - Note templates
  - `PromptEntity` - Prompt data

- **Learning Sessions:**
  - `LearningSessionEntity` - Structured study sessions
  - `KnowledgeStudySessionEntity` - Knowledge-domain sessions

**Inspection Checklist:**
- [ ] Export produces valid JSON
- [ ] ZIP archives created correctly
- [ ] Legacy format parseable
- [ ] DataStore migrations safe
- [ ] Preference changes reactive
- [ ] Flashcard algorithm tested
- [ ] Spaced repetition intervals correct
- [ ] Adaptive difficulty scaling works
- [ ] No data loss on export/import round-trip

**Related Test Files:**
- `app/src/test/.../data/repository/ExportManagerTest.kt`

---

## 🔄 DATA FLOW ARCHITECTURE

### **Import Pipeline Flow**
```
User selects file (URI)
    ↓
CompilerViewModel.onFileSelected(uri)
    ├─ Get display name from ContentResolver
    ├─ ImportFormatDetector.detectFormat() → ImportFormat
    └─ Route to format-specific loader
        ├─ XLSX → XlsxLibraryCompiler
        ├─ CSV/TSV → SpreadsheetQuestionParser with delimiter inference
        └─ JSON/HTML/TEXT → format-specific parser
    ↓
SpreadsheetHeaderMapper.detectHeaderRow()
    → Score first 25 rows for field presence
    → Return row index with highest score
    ↓
User adjusts (if needed):
    ├─ Header row number
    ├─ Column → field mapping
    └─ Option column indices
    ↓
SpreadsheetQuestionParser.parseRow()
    → Extract question, options, correct answers
    → Detect marked cells, images, categories
    → Create ParsedQuestion objects
    ↓
CompilerViewModel displays preview (5-10 questions)
    ↓
User confirms or cancels
    ↓
MksRepository.importParsedQuestions()
    ├─ Create/get target Book
    ├─ Create/get target Quiz
    └─ Insert Questions with image downloads
    ↓
Database transaction completes
```

### **Quiz Playthrough Flow**
```
User selects quiz
    ↓
QuizViewModel.loadQuiz(quizId)
    ├─ Fetch quiz metadata
    └─ Load all questions
    ↓
User starts session
    ↓
SessionEntity created, stored in DB
    ↓
QuizPlayerScreen displays questions
    ├─ Load image via Coil
    ├─ Display options (shuffled or not)
    └─ Timer countdown (if enabled)
    ↓
User selects answer(s)
    ↓
QuizViewModel.submitAnswer()
    ├─ Store in SessionEntity.answers map
    └─ Update DB
    ↓
Next question or mark completed
    ↓
QuizViewModel.completeSession()
    ├─ Call MksRepository.completeSession()
    ├─ Calculate score and accuracy
    ├─ Update question statistics
    └─ Mark session complete
    ↓
SummaryScreen displays results
```

### **Image Loading Flow**
```
Question displayed
    ↓
imagePath → Coil ImageLoader.load(path)
    ├─ Check Coil memory cache → HIT → render
    ├─ Check Coil disk cache → HIT → render
    ├─ Check if local file exists → YES → render
    ├─ Check if HTTPS URL → Download via FileManager
    │   ├─ OkHttp.newCall()
    │   ├─ Save to filesDir/images/
    │   └─ Update question.imagePath in DB
    └─ Render
```

---

## 🧪 TESTING STRATEGY BY BLOCK

| Block | Unit Tests | Integration | Instrumented | Notes |
|-------|-----------|-------------|--------------|-------|
| 1. Init | N/A | AppModule setup | Seed data | DI container mostly declarative |
| 2. Navigation | N/A | Route coverage | UI flow | Test via navigation graph |
| 3. Database | Entity validation | DAO CRUD | Migrations | Room generates DAO tests |
| 4. Import | Parser unit tests | Format detection | End-to-end import | Extensive file format coverage |
| 5. Repository | Mock DB tests | Real DB ops | With UI | Integration with DAOs |
| 6. UI | N/A | State flows | Screen transitions | Compose UI tests |
| 7. File I/O | Stream mocks | Real file ops | Disk usage | Mock OkHttp for failures |
| 8. Export | Serialization | JSON round-trip | Archive validation | Compare snapshots |

---

## 📋 INSPECTION CHECKLIST (Master)

### **Phase 1: Static Analysis**
- [ ] All Kotlin files compile without warnings
- [ ] Lint rules pass (Android Lint, Detekt if configured)
- [ ] No unused imports or variables
- [ ] Naming conventions consistent
- [ ] Code formatting uniform (ktfmt or similar)

### **Phase 2: Architecture Review**
- [ ] DI container initialized once per app lifetime
- [ ] No service locator anti-pattern (appModule accessed appropriately)
- [ ] One-way data flow (UI → ViewModel → Repository → DB)
- [ ] Separation of concerns respected
- [ ] Navigation graph acyclic and complete

### **Phase 3: Database Integrity**
- [ ] All migrations tested
- [ ] Schema version matches code version
- [ ] Foreign keys enforced
- [ ] No orphened data
- [ ] Transactions used for multi-step ops

### **Phase 4: Performance**
- [ ] Image loading doesn't block UI thread
- [ ] Large file imports streamed, not fully buffered
- [ ] Database queries use indices
- [ ] Coroutine scoping prevents leaks
- [ ] Memory usage stays < threshold

### **Phase 5: Security**
- [ ] File paths sanitized (no directory traversal)
- [ ] HTTPS enforced for remote images
- [ ] DataStore encryption enabled (if applicable)
- [ ] No hardcoded secrets
- [ ] Input validation on user-provided data

### **Phase 6: Functional Testing**
- [ ] Import all 6 file formats successfully
- [ ] Quiz playthrough scores correctly
- [ ] Sessions persist across app restarts
- [ ] Images render without corruption
- [ ] Theme/language toggle works seamlessly
- [ ] Export/import round-trip preserves data

---

## 🚀 QUICK INSPECTION WORKFLOW

**For a focused review:**

1. **10 Min: Overview**
   - Read AGENTS.md (this project's API doc)
   - Understand 8 functional blocks

2. **30 Min: Entry Point**
   - Review `MksApplication.kt` + `MainActivity.kt`
   - Verify `AppModule` initialization
   - Check theme/language setup

3. **30 Min: Critical Path**
   - Trace library → quiz → player → summary screens
   - Verify data flows through Repository
   - Check Session storage logic

4. **20 Min: Import Pipeline**
   - Test importing a sample CSV/XLSX
   - Verify header detection
   - Confirm question parsing

5. **20 Min: Database**
   - Run migrations test
   - Query a quiz + questions
   - Verify relationships intact

6. **20 Min: Performance**
   - Load library with 100+ quizzes
   - Play through 10-question quiz
   - Monitor memory/FPS via Android Profiler

---

## 📞 DEPENDENCY CROSS-REFERENCE

```
MksApplication
├─ AppModule (initializes all singletons)
│  ├─ MksDatabase
│  │  ├─ BookDao, QuizDao, QuestionDao, SessionDao, etc.
│  │  └─ Converters (Type adapters)
│  ├─ MksRepository
│  │  ├─ Uses: All DAOs, FileManager
│  │  └─ Provides: CRUD, statistics, search
│  ├─ FileManager
│  │  └─ Provides: Image I/O, downloads, caching
│  ├─ ExportManager
│  │  ├─ Uses: MksRepository, Moshi
│  │  └─ Provides: JSON export, ZIP creation
│  ├─ ImportLibraryManager
│  │  ├─ Uses: All parsers, validators, normalizers
│  │  └─ Provides: Multi-format import orchestration
│  ├─ DataStoreManager
│  │  └─ Provides: Theme, language, preference persistence
│  └─ FocusManager
│     └─ Provides: Adaptive training logic
└─ ImageLoader (Coil)
   ├─ Memory cache (25% RAM)
   ├─ Disk cache
   └─ Network fetching

MksNavHost
├─ All Screen Composables
├─ All ViewModel instances (via Factory)
└─ NavController

Each ViewModel
├─ Receives appModule dependencies
├─ Manages UI State (StateFlow)
└─ Coordinates with Repository/Managers
```

---

## 💡 KEY INSIGHTS FOR INSPECTION

1. **Manual DI simplifies scoping** - No Hilt magic, dependencies explicit
2. **Multi-format import is the most complex block** - Extensive validation needed
3. **Room v17 with 14 migrations** - Database upgrades must be tested thoroughly
4. **StateFlow-based UI** - State management is reactive and testable
5. **Image caching layered** - Coil + FileManager + DB paths create efficiency
6. **Session persistence key** - Unfinished quizzes must survive app crashes
7. **Localization baked in** - Arabic support affects UI and parsing
8. **Export format legacy-compatible** - Old quiz formats must import correctly

---

## 📝 Document Information

- **Created:** May 21, 2026
- **Project:** MKS Android Quiz Application
- **Purpose:** Comprehensive component inspection and management blueprint
- **Scope:** 8 functional blocks covering architecture, testing, and workflow
- **Maintained By:** AI Development Team

---

