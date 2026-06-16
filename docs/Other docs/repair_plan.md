# MKS Crash Repair Plan and Applied Patch

## Goal
Repair the highest-risk startup, import, review, and export crash paths without adding new product features or changing the database schema version.

## Applied repair sequence

### 1. Build configuration hardening
- Replaced mismatched KSP `2.3.5` with Kotlin-matched `2.2.10-2.0.2`.
- Added `ksp.useKSP2=true` for Kotlin 2.2.x compatibility.

### 2. Startup crash hardening
- Replaced unsafe `SortOption.valueOf(...)` parsing in `LibraryViewModel` with `runCatching` fallback to `SortOption.TITLE`.
- Hardened Room converters:
  - `toQuestionType` now falls back to `SINGLE_CHOICE`.
  - `toIntList` now returns `emptyList()` on malformed stored JSON.
  - `toStringMap` and `fromStringMap` now fail safely.
- Disabled Android auto backup restore for the app and excluded Room/DataStore from backup rule files to avoid restored old DB/DataStore values crashing newer builds.

### 3. Library summary startup load repair
- Replaced the first-frame `getLibraryKnowledgeSummary()` N+1 loop with aggregate DAO count queries.
- Added aggregate count methods to `BookDao`, `QuizDao`, `QuestionDao`, `QuestionAssetDao`, `FlashcardDeckDao`, `FlashcardDao`, and `NoteBlueprintDao`.
- Fixed due flashcard counting through a single DAO predicate:
  - due if `dueAt > 0 AND dueAt <= now`, or
  - new/unreviewed if `dueAt = 0 AND lastReviewedAt = 0`.

### 4. Review and scheduling repairs
- Removed duplicate due/review/snooze declarations from `FlashcardDao`.
- Updated `rateFlashcard()` to calculate `nextDueAt` and call `flashcardDao.markReviewed()`.
- Made blueprint snooze hide the item until the requested due time without adding a schema migration by storing the future due timestamp in `updatedAt` and filtering due blueprints by `updatedAt <= now`.
- Made `MARKED_QUESTION` review clear the mark.
- Made `WEAK_QUESTION` review update `lastStudiedAt`/`updatedAt` as a lightweight reviewed marker.
- Added a pending mistake count so the dashboard can show mistakes scheduled but not due yet.

### 5. Link integrity and duplicate mistake repair
- Added `QuestionAssetDao.clearSourceReference(...)`.
- Deleting a source document now nulls matching `question_assets.sourceDocumentId` values before deleting the source.
- Added `MistakeLogDao.findByQuestionAndSession(...)`.
- `autoLogWrongAnswer()` is now idempotent for the same `(questionId, sessionId)`.

### 6. Import/export safety
- Replaced hardcoded export manifest DB version with `MksDatabase.DB_VERSION`.
- Added a database version constant without bumping the schema.
- Added a Data Tools “Save export file” flow using `ACTION_CREATE_DOCUMENT` so the cache export can be copied to a user-selected file.
- Updated export UI text to clearly state that this repair build exports structured data only and does not include media.

### 7. Null-safety cleanup
- Removed remaining unsafe `!!` usages in app source.
- Hardened `CsvParser` empty delimiter scoring.
- Hardened `JsonLibraryParser` optional field remapping.
- Hardened `ZipLibraryParser` parent directory handling.
- Hardened race-prone UI access in `LibraryScreen`, `QuizPlayerScreen`, `CompilerDialog`, and `FlashcardDeckScreen`.
- Hardened `ExportManager` map lookups and repository asset-path cleanup.

## Deferred / intentionally not changed
- No Room schema version bump was made.
- No new entity/table was added.
- No `fallbackToDestructiveMigration` was added.
- Full media-inclusive export remains deferred.
- Full import-apply/merge remains preview-first and deferred.
- Full migration test suite for every old schema remains deferred because only the v21 schema JSON is present in this bundle.
- Compose BOM alignment was inspected as a risk, but not changed in this repair to avoid a wider dependency upgrade.

## Validation performed here
- XML files parse successfully.
- Grep checks confirm no remaining `!!` in `app/src/main/java`.
- Grep checks confirm unsafe `valueOf` calls are now wrapped in `runCatching`.
- Targeted Kotlin parser checks were run on modified UI, DAO, repository, import parser, export, database, and converter files. Only unresolved dependency errors appeared, which is expected outside a full Android/Gradle classpath.
- A full Gradle build was attempted but could not complete in this sandbox because the Gradle wrapper needed to download `gradle-9.4.1-bin.zip` from `services.gradle.org`, and outbound network resolution failed.

## Next verification step outside this sandbox
Run in Android Studio or a network-enabled terminal:

```bash
./gradlew --no-daemon :app:clean :app:assembleDebug
```

Then launch-test:
1. Fresh install.
2. Install over an older app data state if available.
3. Open Library.
4. Open Review Dashboard.
5. Rate flashcards with Again / Good / Easy and confirm `dueAt` changes.
6. Snooze a blueprint and confirm it leaves the due queue.
7. Wrong-answer the same question twice in one session and confirm only one mistake log row appears.
8. Create a Data Tools export and save it through the new save button.
