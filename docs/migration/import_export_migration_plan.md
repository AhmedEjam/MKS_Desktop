# Import/Export Migration Plan — Desktop Parity

> Last updated: 2026-06-16. Target: Full feature parity with Android import/export.

## Overview

The Android app supports importing 6 file formats and exporting quiz/book/knowledge-bank bundles as ZIP archives with embedded images. The desktop app currently has a minimal CSV-only importer. This plan covers migrating every import format, the export pipeline, and ensuring cross-platform data interchange.

---

## Current State

| Component | Android (legacy) | Desktop (current) |
|-----------|-----------------|-------------------|
| CSV/TSV import | ✅ Full | ✅ Basic (question + options + answer) |
| XLSX import | ✅ Full (POI + embedded images) | ❌ None |
| JSON import | ✅ Full | ❌ None |
| HTML import | ✅ Full | ❌ None |
| TEXT import | ✅ Full | ❌ None |
| ZIP import | ✅ Full (bundle re-import) | ❌ None |
| Quiz export (ZIP) | ✅ Full (questions + images) | ❌ None |
| Book export | ✅ Full (all quizzes + assets) | ❌ None |
| Knowledge bank export | ✅ Full | ❌ None |
| Format detection | ✅ Extension + MIME + magic bytes | ❌ None |

---

## Phase 1: Port Format Detection + CSV Parser (2 hrs)

### 1.1 — Port ImportFormatDetector

**Android deps to replace:**
- `Context` → not needed (file path, not URI)
- `ContentResolver.query()` → `File.name` for display name
- `ContentResolver.openInputStream()` → `File.inputStream()`

**New file:** `shared/src/commonMain/.../importer/ImportFormatDetector.kt`

```kotlin
class ImportFormatDetector {
    fun detectFormat(filePath: String): ImportFormat {
        val name = File(filePath).name
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "json" -> ImportFormat.JSON
            "zip" -> ImportFormat.ZIP
            "xlsx", "xls" -> ImportFormat.XLSX
            "csv", "tsv" -> ImportFormat.CSV_TSV
            "txt" -> ImportFormat.TEXT
            "html", "htm" -> ImportFormat.HTML
            else -> detectFromMagicBytes(filePath)
        }
    }
}
```

### 1.2 — Port ImportFormat enum + ParsedQuestion model
- `ImportFormat` enum → `shared/commonMain`
- `ParsedQuestion` data class → `shared/commonMain`

---

## Phase 2: Port CSV/TSV Parser (1 hr)

### 2.1 — Port CsvParser
Already partially done in `Main.kt`. Extract to `shared/src/commonMain/.../importer/parser/CsvParser.kt`:

- RFC 4180 quoted field handling (already implemented)
- Delimiter auto-detection (`,` vs `\t` vs `;`)
- Header row detection
- Multi-language header aliases from `SpreadsheetHeaderMapper`

---

## Phase 3: Port XLSX Parser (3 hrs)

### 3.1 — Dependencies
- Apache POI 5.2.5 — already in version catalog, JVM-compatible
- `java.util.zip.ZipFile` — JVM standard library
- **No Android dependencies** — POI works with `File` and `InputStream`

### 3.2 — Port XlsxLibraryCompiler
- Remove `Context`, `ContentResolver`, `Uri` references
- Replace `context.contentResolver.openInputStream(uri)` with `File(path).inputStream()`
- Replace `prepareTempFile()` (copied to app cache) with direct file reading
- Embedded image extraction: `ZipFile` → cell image map → same logic as Android

### 3.3 — Port XlsxImageResolver
- Already pure JVM — just move to `shared/commonMain`
- Uses `java.util.zip.ZipFile`, `org.apache.poi` — no Android APIs

### 3.4 — Port SpreadsheetHeaderMapper + SpreadsheetQuestionParser
- Pure Kotlin — move to `shared/commonMain`
- No Android dependencies

---

## Phase 4: Port JSON/HTML/TEXT Parsers (2 hrs)

### 4.1 — JsonQuestionParser, HtmlQuestionParser, TextQuestionParser
- Pure Kotlin text parsing — move to `shared/commonMain`
- No Android dependencies

### 4.2 — ZipLibraryParser
- Uses `java.util.zip.ZipInputStream` — JVM standard library
- Extracts JSON + image files from ZIP bundles
- Port to `shared/commonMain`

---

## Phase 5: Port ExportManager → SQLDelight (3 hrs)

### 5.1 — Dependency mapping

| Android (Room) | Desktop (SQLDelight) |
|---------------|---------------------|
| `bookDao.getBookById()` | `db.bookQueriesQueries.bk_selectById()` |
| `quizDao.getQuizById()` | `db.quizQueriesQueries.qz_selectById()` |
| `questionDao.getQuestionsByQuizId()` | `db.questionQueriesQueries.qu_selectByQuiz()` |
| `database.withTransaction {}` | `db.transaction {}` |
| `fileManager.getImagesDir()` | Platform FileManager.getImagesDir() |
| `OutputStream` (to file) | `File.outputStream()` |

### 5.2 — Port ExportManager

New file: `shared/src/commonMain/.../export/ExportManager.kt`

```kotlin
class ExportManager(
    private val db: MksDatabase,
    private val fileManager: FileManager
) {
    suspend fun exportQuizAsBundle(quizId: Long): File? {
        // Query quiz, book, questions via SQLDelight
        // Build LibraryBundleDto
        // Serialize to JSON via kotlinx.serialization
        // Collect image files from questions
        // Create ZIP with JSON + images
        // Return File handle
    }
}
```

### 5.3 — Exchange format (v7)
- `MksExchangeV7Models.kt` → port to `shared/commonMain` (pure kotlinx.serialization)
- `MksExchangeV7Archive.kt` → port to `shared/commonMain` (ZIP I/O)

---

## Phase 6: Desktop Import UI (2 hrs)

### 6.1 — Update ImportScreen in Main.kt

```
ImportScreen flow:
1. FileDialog.openFile() → get file path
2. ImportFormatDetector.detectFormat() → determine parser
3. Route to parser:
   ├── XLSX → XlsxLibraryCompiler.parse()
   ├── CSV/TSV → CsvParser.parse()
   ├── JSON → JsonQuestionParser.parse()
   ├── HTML → HtmlQuestionParser.parse()
   ├── TEXT → TextQuestionParser.parse()
   └── ZIP → ZipLibraryParser.parse()
4. Show parsed questions preview (scrollable list)
5. User selects target book/quiz (or create new)
6. Import questions via QuizRepository
7. Report: "Imported N questions, M skipped"
```

### 6.2 — Export button
- Add "Export" button to Book Detail (exports book bundle)
- Add "Export" button to Quiz Player (exports quiz bundle)
- Use FileDialog.saveFile() for save location
- Generate ZIP bundle with ExportManager

---

## Phase 7: Cross-Platform Data Interchange (1 hr)

### 7.1 — Schema compatibility

Desktop SQLDelight schema **is identical** to Android Room v30 schema:
- Same 26 tables
- Same column names and types
- Same foreign keys and indexes
- SQLDelight `1.sqm` uses `CREATE TABLE IF NOT EXISTS` — idempotent

### 7.2 — Bundle compatibility

Export format (v7) is platform-agnostic:
- JSON manifest (kotlinx.serialization)
- Image files (PNG/JPEG in ZIP)
- External IDs for deduplication

```bash
# Desktop → Android flow:
1. Export on desktop → mks_bundle.zip
2. Share/transfer to Android device
3. Android app receives via Intent.ACTION_SEND
4. ImportLibraryManager processes the ZIP

# Android → Desktop flow:
1. Export on Android → mks_bundle.zip
2. Transfer to desktop
3. Desktop: Import tab → select ZIP → import
```

### 7.3 — Testing the round-trip

1. Create book + quiz + questions on desktop
2. Export as bundle
3. Re-import bundle on desktop (verify identical)
4. Transfer bundle to Android device
5. Import on Android (verify data + images match)

---

## File Migration Map

| Source (core/data) | Destination (shared/commonMain) | Android deps |
|---------------------|-------------------------------|--------------|
| `importer/detector/ImportFormatDetector.kt` | `importer/ImportFormatDetector.kt` | Context, Uri, ContentResolver → File |
| `importer/model/ImportFormat.kt` | `importer/model/ImportFormat.kt` | None |
| `importer/model/ParsedQuestion.kt` | `importer/model/ParsedQuestion.kt` | None |
| `importer/parser/CsvParser.kt` | `importer/parser/CsvParser.kt` | None |
| `importer/parser/SpreadsheetHeaderMapper.kt` | `importer/parser/SpreadsheetHeaderMapper.kt` | None |
| `importer/parser/SpreadsheetQuestionParser.kt` | `importer/parser/SpreadsheetQuestionParser.kt` | None |
| `importer/xlsx/XlsxLibraryCompiler.kt` | `importer/xlsx/XlsxLibraryCompiler.kt` | Context, Uri → File |
| `importer/xlsx/XlsxImageResolver.kt` | `importer/xlsx/XlsxImageResolver.kt` | None (pure JVM) |
| `importer/parser/JsonQuestionParser.kt` | `importer/parser/JsonQuestionParser.kt` | None |
| `importer/parser/HtmlQuestionParser.kt` | `importer/parser/HtmlQuestionParser.kt` | None |
| `importer/parser/TextQuestionParser.kt` | `importer/parser/TextQuestionParser.kt` | None |
| `importer/parser/ZipLibraryParser.kt` | `importer/parser/ZipLibraryParser.kt` | None |
| `repository/ExportManager.kt` | `export/ExportManager.kt` | Room DAOs → SQLDelight |
| `exchange/v7/MksExchangeV7Models.kt` | `exchange/MksExchangeV7Models.kt` | None |
| `exchange/v7/MksExchangeV7Archive.kt` | `exchange/MksExchangeV7Archive.kt` | None |

---

## Effort Summary

| Phase | Description | Hours |
|-------|-------------|-------|
| 1 | Format detection + models | 2 |
| 2 | CSV parser port | 1 |
| 3 | XLSX parser port | 3 |
| 4 | JSON/HTML/TEXT/ZIP parsers | 2 |
| 5 | ExportManager port | 3 |
| 6 | Import/Export UI | 2 |
| 7 | Cross-platform testing | 1 |
| **Total** | | **14 hours** |

---

## Dependencies (already in version catalog)

| Library | Version | Notes |
|---------|---------|-------|
| Apache POI | 5.2.5 | XLSX parsing (JVM) |
| Apache POI OOXML | 5.2.5 | XLSX format support |
| kotlinx-serialization-json | 1.8.1 | Bundle JSON manifest |
| OkHttp | 4.12.0 | Remote image download (optional) |

---

## Success Criteria

1. ✅ Import XLSX file → desktop app shows parsed questions → creates quiz
2. ✅ Import CSV file → detects delimiter → shows parsed questions → creates quiz
3. ✅ Import JSON/HTML/TEXT files → same flow
4. ✅ Import desktop-exported ZIP bundle → recreates identical data
5. ✅ Export quiz → produces ZIP with JSON + images
6. ✅ Export book → produces ZIP with all quizzes + assets
7. ✅ Desktop-exported bundle imports correctly on Android
8. ✅ Android-exported bundle imports correctly on desktop
