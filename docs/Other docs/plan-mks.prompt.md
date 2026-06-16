# AGENTS.md Regeneration Plan for MKS
> Finalization status correction (2026-05-25): active source is Room v17 with migrations through 16→17; older v15/v16 text below is historical and should not drive new patches.


## Executive Summary

The MKS codebase is a fully functional Android quiz application with:
- **Manual Dependency Injection** (not Hilt) via `AppModule` with lazy-initialized singletons
- **Multi-format file import** supporting XLSX, CSV/TSV, JSON, HTML, and plain text
- **Complex spreadsheet parsing** with Apache POI, merged cells, formula evaluation, and automatic delimiter detection
- **Jetpack Compose** UI with custom Navigation routing (not Jetpack Navigation's typical setup)
- **Room Database** with 16 migration steps (currently v17)
- **Coil** for image caching with custom `ImageLoaderFactory`
- **OkHttp** integration for downloading remote images
- **Image extraction** from XLSX files via ZipFile and embedded cell images
- **DataStore** for user preferences
- **Multi-language support** (English + Arabic with RTL-aware layouts)

## Current State vs. Updated CLAUDE.md

**Problems with current document:**
1. References "Hilt" DI when implementation uses manual `AppModule`
2. Mentions hypothetical database features (KSP, Room v10) but actual implementation is Room v17 with 16 migration steps
3. Describes import pipeline at high level but misses critical details about:
   - `CompilerViewModel` (407 lines) orchestrating multi-format parsing
   - Spreadsheet parsing specifics (merged cells, delimiter inference, quoted CSV handling)
   - Image resolution strategy (embedded XLSX images, Base64, URLs, local files)
   - Header detection scoring algorithm
   - Multi-language header aliases (Arabic support)

4. Doesn't document Android-specific patterns:
   - Manual `ViewModelFactory` creation (not `@Inject` or `hiltViewModel()`)
   - Room migration strategy
   - Image downloading and caching flow
   - Compose Navigation route structure with query parameters

## Regeneration Approach

### 1. Core Architecture Section

**Current gaps:**
- No mention of manual DI pattern in `AppModule`
- Missing emphasis on `MksApplication.appModule` singleton lifecycle
- No documentation of lazy initialization pattern

**Updates needed:**
- Document `AppModule` as the DI container (not Hilt)
- Explain lazy-initialized singletons: `database`, `fileManager`, `exportManager`, `importManager`, `repository`, `dataStoreManager`, `focusManager`
- Show the `resetDatabase()` pattern for testing
- Clarify `ViewModelFactory` manual creation pattern used throughout navigation

### 2. Data Layer Deep Dive

**Document actual structure:**

```
data/
├── import/
│   ├── detector/
│   │   └── ImportFormatDetector.kt (detects: XLSX, CSV/TSV, JSON, HTML, ZIP, TEXT)
│   ├── parser/
│   │   ├── SpreadsheetHeaderMapper.kt (multi-language aliases, scoring algorithm)
│   │   ├── SpreadsheetQuestionParser.kt (row parsing, correct answer resolution, image extraction)
│   │   ├── GenericImageExtractor.kt (Base64, URLs, embedded references)
│   │   ├── JsonQuestionParser.kt
│   │   ├── HtmlQuestionParser.kt
│   │   ├── TextQuestionParser.kt
│   │   ├── XlsxImageResolver.kt (ZipFile extraction, cell image mapping)
│   ├── xlsx/
│   │   └── Spreadsheet-specific XLSX handling
│   ├── mapping/
│   │   └── LibraryMapper.kt
│   ├── normalization/
│   │   └── BundleNormalizer.kt
│   └── repository/
│       └── ImportLibraryManager.kt
├── local/
│   ├── FileManager.kt (image I/O, Base64 handling, HTTP downloads)
│   ├── MksDatabase.kt (Room v17, 16 migration steps)
│   ├── entity/ (BookEntity, QuizEntity, QuestionEntity, SessionEntity, CategoryMetadataEntity, etc.)
│   ├── dao/ (BookDao, QuizDao, QuestionDao, SessionDao, CategoryMetadataDao)
│   └── converter/ (TypeConverters for List<String>, etc.)
├── model/
│   ├── ParsedQuestion, ParsedOption (intermediate representation)
│   ├── ImportResult, ImportFormat, ImportMode, QuestionType
│   └── CategoryWithMetadata
├── preferences/
│   └── DataStoreManager.kt
├── focus/
│   └── FocusManager.kt
└── repository/
    ├── MksRepository.kt (single source of truth)
    ├── ExportManager.kt (quiz/book export to ZIP)
    └── SortOption enum
```

**Entity Structure:**
- `BookEntity` (id, externalId, title, description, fields[], coverImage, stats, system flag, timestamps)
- `QuizEntity` (id, externalId, bookId, title, description, category, coverImage, stats, system flag, timestamps)
- `QuestionEntity` (id, externalId, quizId, text, type, options[], correctAnswers[], explanation, hint, reference, additionalInfo, imagePath, categories[], metrics, timestamps)
- `SessionEntity` (id, quizId, answers[], isCompleted, timerSettings, timestamps)
- `CategoryMetadataEntity` (name, emoji, color, pinned)

### 3. Import Pipeline (Real Implementation)

**Actual data flow:**

```
User selects file (URI)
   ↓
CompilerViewModel.onFileSelected()
   ↓
ImportFormatDetector.detectFormat() → ImportFormat enum
   ↓
[Format-specific loading]
   ├─ XLSX: LoadWorkbookSpreadsheet() 
   │   - prepareTempFile() → cache dir
   │   - ZipFile extraction for embedded images
   │   - WorkbookFactory.create() → sheet discovery
   │   - Auto-detect header row (scoring first 25 rows)
   │
   ├─ CSV/TSV: LoadDelimitedSpreadsheet()
   │   - parseDelimited() → infer delimiter (`,`, `\t`, `;`)
   │   - parseDelimitedLine() with CSV quote handling
   │   - detectHeaderRow() via scoring
   │
   └─ JSON/HTML/TEXT: LoadNonSpreadsheet()
       - Content read via ContentResolver
       - Routed to JsonQuestionParser, HtmlQuestionParser, TextQuestionParser

[User sets header row and column mapping]
   ↓
SpreadsheetHeaderMapper.mapHeaders()
   - Multi-language aliases (English + Arabic)
   - Maps header → field ("question", "answer", "explanation", "hint", "reference", "additional", "categories", "image")
   - Scores header quality
   
SpreadsheetHeaderMapper.guessOptionColumns()
   - Detects "Option A", "Choice 1", "اختيار ب", etc.
   - Falls back to column range heuristic
   ↓
[Parse questions row-by-row]
   ↓
SpreadsheetQuestionParser.parseRow()
   - Stem extraction (strips DISPIMG() functions)
   - Option parsing with marked cell detection (`*✓☑✅`)
   - Correct answer resolution (supports "A", "1", "Option Text", "A, C", marked cells)
   - Image resolution priority:
     1. Image column cell
     2. Question column cell (embedded URL/Base64)
     3. Option cells
     4. Row-level images (XLSX)
     5. Cell-address images (XLSX merged regions)
   - Multi-value categories parsing (`,،;؛/|` delimiters)
   ↓
shouldSkipQuestion()
   - Empty stem + no options + no image + no metadata = skip
   ↓
Questions emitted to UI
   ↓
User saves via MksRepository.importParsedQuestions()
   ↓
ImportLibraryManager routes to database
```

### 4. CompilerViewModel Specifics

**UI State Model:**
```kotlin
data class CompilerUiState(
    val questions: List<ParsedQuestion>,        // Preview of parsed questions
    val isLoading: Boolean,
    val error: String?,
    val mode: ImportMode,                       // AUTO, SPREADSHEET, JSON, HTML, TEXT
    val detectedMode: ImportMode?,              // Format detection result
    val sheetNames: List<String>,               // XLSX sheets or single file name
    val selectedSheet: String?,
    val headerRow: Int,                         // User-selected or auto-detected
    val mapping: Map<String, Int>,              // Column index by field name
    val optionColumns: List<Int>,               // Indices of option columns
    val availableColumns: List<String>          // Header row values for UI display
)
```

**Key methods:**
- `onFileSelected(uri)` - Triggers format detection and initial loading
- `onSheetSelected(name)` - For XLSX multi-sheet workbooks
- `updateHeaderRow(index)` - Re-maps headers when user changes header row
- `updateMapping(mapping, optionCols)` - Re-parses when user adjusts column mapping
- `saveParsedQuestions(title, bookId)` - Persists parsed questions to database

**Parsing algorithms:**

1. **Header Row Detection** (scores first 25 rows):
   - +5 if "question" detected
   - +4 if "answer" detected
   - +2 if "explanation" detected
   - +1 if "hint" detected
   - +2 per option column
   - Highest score wins

2. **Delimiter Inference** (for CSV/TSV):
   ```
   Test `,`, `\t`, `;` on first 10 lines
   Calculate average column count per delimiter
   Choose delimiter with most consistent count
   ```

3. **CSV Parsing** with quote handling:
   ```
   Track inQuotes state
   When seeing `"` while inQuotes and next is also `"` → escaped quote
   When seeing delimiter outside quotes → cell boundary
   ```

4. **Merged Cell Resolution** (XLSX):
   ```
   Try reading direct cell first
   If empty, find merged region containing cell
   Read from merged region's first cell
   ```

5. **Correct Answer Resolution** (multiple formats):
   - Letter: "A", "B", "C" → match option index + 'A'
   - Sequence: "A, C" → split and match each
   - Number: "1", "2" → match option index (1-based)
   - Text: "Paris" → fuzzy match option text
   - Marked cells: "☑ Option" → auto-mark
   - Priority: explicit answer > marked cells

### 5. Image Handling Pipeline

**Images can come from:**
1. **XLSX embedded images** → extracted via ZipFile, mapped by cell address
2. **Image column** → URL or Base64 data URI
3. **Q&A cells** → embedded URLs or Base64
4. **Cell references** → `=IMAGE("url")`

**FileManager responsibilities:**
- `saveBase64AsImage(base64String)` → convert to file
- `downloadAndSaveImage(url)` → OkHttp download to cache
- `saveImage(uri)` → ContentResolver read to cache
- `getImagesDir()` → internal storage/images directory
- All images assigned UUID filenames to prevent conflicts

### 6. Navigation & UI Integration

**Route structure:**
```
library/                                    # LibraryScreen (start)
  │
  ├─ quiz/{quizId}?sessionId={sessionId}   # QuizPlayerScreen
  │  └─ summary/{sessionId}                 # SummaryScreen
  │
  ├─ quiz_questions/{quizId}               # QuizQuestionsScreen (browse)
  │
  ├─ category/{category}                   # CategoryQuestionsScreen
  │
  ├─ adaptive/{type}/{id}                  # AdaptiveTrainingScreen
  │  (type = "BOOK", "CATEGORY", "QUIZ", "QUESTION")
  │
  ├─ sessions/{quizId}                     # SessionManagementScreen
  │
  ├─ scanner/{quizId}                      # ScannerScreen (QR import)
  │
  ├─ settings/                              # SettingsScreen
```

**ViewModel Creation Pattern** (manual factories, no Hilt):
```kotlin
val viewModel: MyViewModel = viewModel(
    factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyViewModel(appModule.dependency1, appModule.dependency2) as T
        }
    }
)
```

### 7. Database & Migrations

**Current version:** Room v17 (16 migration steps total)

**Migrations to document:**
- v1→v2 through v14→v15 (show conceptual pattern, reference file for details)
- Adding columns: create new `@Migration` class with raw SQL
- Pattern: `migration_N_M.sql` or inline MapperMigration

**Key entities:**
- All have `id: Long` auto-increment, `externalId: String`, `createdAt`, `updatedAt`, `lastEditedAt`, `lastStudiedAt`
- `QuestionEntity` includes type-specific data (single/multiple choice, boolean)
- `SessionEntity` uses TypeConverter for `answers: List<(questionId, selectedOptionIds)>`

### 8. Android-Specific Patterns

**Coil Image Loading:**
- Custom `ImageLoaderFactory` in `MksApplication`
- 25% of RAM for memory cache
- Disk + memory cache policies enabled
- Crossfade animations enabled

**Jetpack Compose specifics:**
- Single-Activity architecture via `MainActivity`
- NavHostController with manual route management
- `viewModel()` composable with custom Factory
- No Hilt/dagger—all dependencies pass through ViewModels

**DataStore for preferences:**
- User-level settings managed by `DataStoreManager`
- Replaces SharedPreferences pattern

**Focus mode / Adaptive training:**
- `FocusManager` handles spaced repetition logic
- Routes to adaptive screens based on learning analytics

### 9. Testing & Development

**Build commands:**
- `./gradlew assembleDebug` - APK compilation
- `./gradlew test` - Unit tests
- `./gradlew connectedAndroidTest` - Instrumented tests
- `./gradlew build` - Full build + tests

**Sample data seeding:**
- `AppModule.seedDatabase()` on first run
- 13 sample questions with images (URLs + local file paths)
- Sample book/quiz/session structure for testing

## Key Decisions to Document

1. **Why manual DI instead of Hilt?**
   - Simpler for single-Activity app
   - Explicit dependency control
   - Easier to debug
   - Legacy compatibility

2. **Why multi-stage parsing?**
   - Format detection first (prevents wrong parsing)
   - Header mapping separate (user adjustable)
   - Question parsing separate (replayable)
   - Image resolution separate (dependencies on IDs)

3. **Why CompilerViewModel for import?**
   - Stateful parsing with user corrections
   - Live preview of imported questions
   - Cancel/retry capability
   - Separation from quiz player logic

4. **Why FileManager abstraction?**
   - Centralized I/O (caching, permissions, cleanup)
   - Easier to mock/test
   - Consistent image storage strategy
   - Future: remote storage providers

5. **Why multi-language header detection?**
   - Revenue opportunity: Arabic market
   - RTL layout support
   - Bilingual educational content common
   - Aliases for common field names

## Implementation Gaps / Aspirational Features

**Currently missing (per CLAUDE.md but not implemented):**
- KSP for Room/Moshi code generation (documented but may not be active)
- Hilt DI (architecture note only—manual DI is reality)
- Some import format features (LegacyBookDto mentioned but may be outdated)

**Should be excluded from AGENTS.md:**
- Any Hilt references
- Features not visible in codebase
- Hypothetical future patterns

## Outline for New AGENTS.md

1. **Project Overview** - Android quiz app, manual DI, multi-format import
2. **Architecture Fundamentals** - AppModule, Repository pattern, data flow
3. **Dependency Injection** - Manual AppModule pattern with lazy initialization
4. **Database & Entities** - Room v17, 16 migration steps, schema overview
5. **Import Pipeline** - Multi-stage parsing, CompilerViewModel orchestration
6. **File Format Support** - XLSX (merged cells, images), CSV (delimiter inference), JSON, HTML, TEXT
7. **Spreadsheet Parsing Deep Dive** - Header detection, column mapping, answer resolution, image extraction
8. **Image Management** - FileManager, embedded XLSX images, URL/Base64 handling, Coil caching
9. **Navigation & Routing** - Manual NavHostController, route structure, ViewModelFactory pattern
10. **UI Patterns** - Compose Screens, StateFlow patterns, manual injection pattern
11. **Localization & Multi-language** - Arabic support, RTL layouts, header aliases
12. **Development Workflow** - Build commands, sample data, testing patterns
13. **Architecture Decisions** - Why each major choice (DI, parsing stages, FileManager, etc.)
14. **Common Tasks** - Adding import format, new screen, new entity, database migration

## Estimated AGENTS.md Length

- Current: ~160 lines (outdated)
- Target: ~250-300 lines (accurate, detailed but concise)
- Deep-dive sections link to actual source files for specifics

## Next Steps

1. Write AGENTS.md with above structure
2. Include code examples from CompilerViewModel, AppModule, MksRepository
3. Reference specific files for algorithm details (don't duplicate code)
4. Validate against actual codebase for accuracy
5. Remove all Hilt/aspirational references
6. Keep language specific and actionable for AI agents

