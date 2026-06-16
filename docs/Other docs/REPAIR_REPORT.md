# MKS Repair Report

## Summary
This patch focuses on crash prevention and broken review/export flows. It repairs unsafe parsing, restored-data risks, startup summary load, flashcard due scheduling, review no-op actions, source dangling references, duplicate mistake logs, import parser null crashes, export retrievability, and UI recomposition null races.

## Changed files

### Build/config
- `gradle/libs.versions.toml`
- `gradle.properties`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

### Database/converters/DAOs
- `app/src/main/java/com/ahmedyejam/mks/data/local/MksDatabase.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/Converters.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/BookDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/QuizDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/QuestionDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/QuestionAssetDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/FlashcardDeckDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/FlashcardDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/NoteBlueprintDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/MistakeLogDao.kt`

### Repository/review/export/import
- `app/src/main/java/com/ahmedyejam/mks/data/repository/MksRepository.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/repository/ExportManager.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/review/ReviewModels.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/review/ReviewRepository.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/exportfull/MksFullImportExportService.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/import/parser/CsvParser.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/import/parser/JsonLibraryParser.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/import/parser/ZipLibraryParser.kt`

### UI
- `app/src/main/java/com/ahmedyejam/mks/ui/library/LibraryViewModel.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/library/LibraryScreen.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/data/DataToolsScreen.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/data/DataToolsViewModel.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/review/ReviewDashboardScreen.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/flashcard/FlashcardDeckScreen.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/quiz/CompilerDialog.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/quiz/QuizPlayerScreen.kt`

## Important implementation notes
- The app remains at DB version 21. `MksDatabase.DB_VERSION` now centralizes the number, but no schema migration was introduced.
- Blueprint snooze is intentionally migration-free. It uses a future `updatedAt` timestamp as the queue cutoff. A future v22 migration with a dedicated `nextReviewAt` column would be cleaner.
- Backup is disabled for crash-hardening. This prevents old Room/DataStore restores from crashing newer builds, but means users need explicit export/import for transfer.
- Export media support is still not implemented. The UI now states this clearly.

## Validation status
- XML parse check: passed.
- Unsafe `!!` grep in `app/src/main/java`: none remaining.
- Unsafe `valueOf` crash sites: wrapped with `runCatching`.
- Targeted Kotlin parser checks: no syntax errors in modified groups; unresolved Android/Compose/Room references are expected without the Gradle classpath.
- Full Gradle build: not completed in this sandbox because Gradle wrapper download failed with `UnknownHostException: services.gradle.org`.

## Recommended next run
After this repaired bundle builds in Android Studio, the next code run should focus on:
1. Full migration validation with old DB snapshots.
2. A dedicated `nextReviewAt` column for blueprints.
3. Media-inclusive full export.
4. Proper file picker for full import preview.
5. Optional main Library entry point for Global Search.
