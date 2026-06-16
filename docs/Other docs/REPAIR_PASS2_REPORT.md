# MKS Crash Repair Pass 2 Report

Date: 2026-05-26
Input bundle: `MKS-GPT4-crash-repair-pass1.zip`
Output bundle: `MKS-GPT4-crash-repair-pass2.zip`

## Goal

Pass 2 focused on build-risk and behavior-risk hardening after the main pass-1 startup crash repairs. It intentionally avoided broad product redesign and concentrated on safer migration behavior, safer import/export access paths, and remaining malformed-data crash guards.

## Repairs Applied

### 1. Room migration idempotency hardening

Changed files:
- `app/src/main/java/com/ahmedyejam/mks/data/local/MksDatabase.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/MksMigrations.kt`

Changes:
- Added `MksDatabase.addColumnIfMissing(...)` and `MksDatabase.columnExists(...)` helpers.
- Replaced direct `ALTER TABLE ... ADD COLUMN ...` calls across the migration chain with guarded column additions.

Why:
- A restored or partially migrated database can crash startup with `duplicate column name` before the app UI loads.
- This does not replace proper migration testing, but it reduces the most common restored-database startup failure mode.

### 2. Review queue ID parsing hardening

Changed file:
- `app/src/main/java/com/ahmedyejam/mks/data/review/ReviewRepository.kt`

Changes:
- Replaced direct `item.id.toLong()` use with `item.id.toLongOrNull()`.
- Malformed review queue IDs now safely no-op instead of crashing.

Why:
- Review queue IDs are generated internally, but defensive parsing prevents crashes if stale/corrupt UI state or future queue types provide unexpected IDs.

### 3. Adaptive BOOK route ID parsing hardening

Changed file:
- `app/src/main/java/com/ahmedyejam/mks/ui/quiz/QuizViewModel.kt`

Changes:
- Replaced direct adaptive route `id.toLong()` with `id?.toLongOrNull()`.

Why:
- A malformed route argument should produce an empty adaptive session rather than a crash.

### 4. Text import answer-number parsing hardening

Changed file:
- `app/src/main/java/com/ahmedyejam/mks/data/import/parser/TextQuestionParser.kt`

Changes:
- Replaced direct answer number `toInt()` with `toIntOrNull()`.

Why:
- Very large numeric prefixes or malformed imported text should be ignored rather than crashing import.

### 5. Import preview file picker

Changed files:
- `app/src/main/java/com/ahmedyejam/mks/data/exportfull/MksFullImportExportService.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/data/DataToolsViewModel.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/data/DataToolsScreen.kt`

Changes:
- Added `simulateImportBundle(uri: Uri)` that copies the selected file into a temporary cache preview file and reuses the safe preview parser.
- Added an Android `OpenDocument` launcher to Data Tools.
- Kept the local path text box as a debug/emulator fallback, but the primary UI path is now file picker based.

Why:
- Import preview should not require users to paste inaccessible app-internal or device-local paths.
- The preview-first safety model remains intact; this does not apply or merge data.

## Validation Performed

- XML parsing check: passed.
- DAO duplicate method scan: passed; no duplicate DAO method names found.
- High-risk pattern scan: passed for:
  - `!!`
  - direct `ALTER TABLE ... ADD COLUMN` migration calls
  - `fallbackToDestructiveMigration`
  - `allowBackup="true"`
  - remaining direct queue/route string ID parsing sites
- ZIP integrity check: passed after packaging.

## Build Status

A full Gradle build was attempted but could not run in this sandbox because the Gradle wrapper attempted to download Gradle from `services.gradle.org` and failed with `UnknownHostException`. This is an environment/network limitation, not a source-code validation result.

Recommended local validation command:

```bash
./gradlew clean assembleDebug
```

## Remaining Deferred Issues

- Full Room migration tests are still needed with real pre-v21 database fixtures.
- Full export/import apply/merge remains intentionally preview-only in this path.
- Media is still not included in the full structured export path.
- No destructive migration fallback was added, intentionally.
- No broad UI redesign or unrelated feature work was done.
