# Android map/plan.md - V04 exchange-peer update

## Active Android base

`MKS-GPT4-v26-annotation-cleanup-ui-patched.zip` remains the Android canonical base.

## Current status

PASS WITH FIXES

Android has not been patched in V04. The iOS V04 contract now defines the next Android patch target for schema-7 exchange parity.

## Android role after iOS V04

Android remains the canonical Room v26 model source, but its exchange writer/reader must be patched to produce and consume the same schema-7 entries now present in iOS V04.

## Required Android Stage 4C patch paths

- Export/import service for `.mks.zip` or full-library ZIP.
- DTO/mapping layer for schema-7 JSON.
- Media/asset storage copy layer.
- Soft-delete mapping layer.
- Conflict-resolution layer.
- Validation fixtures for Android export -> iOS import and iOS export -> Android import.

## Required Android Stage 4C entries

- `manifest.json`
- `workspace.json`
- `data/books.json`
- `data/quizzes.json`
- `data/questions.json`
- `data/question_categories.json`
- `data/asset_references.json`
- `data/question_assets.json`
- `data/source_documents.json`
- `data/annotations.json`
- `data/media_manifest.json`
- `data/soft_deletes.json`

## Android patch rules

1. Do not rename Room v26 entities just to mirror iOS code names.
2. Preserve Android DAO/repository architecture.
3. Add exchange DTOs at the boundary.
4. Keep external IDs stable.
5. Export soft-deleted records intentionally; never silently hard-delete during exchange.
6. Treat missing media as a warning, not a crash.

---

# Android map/plan.md - V02 backend-finalization update

## Verdict

PASS WITH FIXES

Android remains the canonical backend reference for MKS v26. No Android source code was patched in V02. The next Android patch should be narrowly scoped to schema-7 `.mks.zip` exchange canonicalization after iOS V02 is used as the import/export peer.

## Initial bundle for Android progress

- Initial Android base bundle: `MKS-GPT4-v26-annotation-cleanup-ui-patched.zip`
- Backend reference level: Room schema v26
- Role: source of truth for entity semantics, relationships, cascade behavior, feature coverage, and UI path coverage
- Current iOS peer base: `iOS_MKS.V02.zip`
- Immediate Android target: Stage 4 exchange writer/reader parity, not a UI redesign

## Confirmed backend domains to preserve

- Workspace and workspace settings
- Books, quizzes, questions, sessions, summaries
- Categories and question-category links
- Flashcard decks/cards and learning sessions
- Notes, review blueprints, slideshows, prompt decks/cards/runs
- Question assets, asset references, source documents
- Mistake logs and annotations
- Soft delete, restore, hard-purge, and repair semantics

## Stage 1 - Android backend canonicalization lock

### Phase 1.1 - Schema/source lock

Status: PASS WITH FIXES

Steps:
1. Keep Room schema v26 as the canonical reference until a deliberate schema bump is produced.
2. Freeze entity meanings and parent-child ownership before exchange work.
3. Verify each exchangeable record has a stable `externalId` or a documented stable synthetic key.
4. Document every destructive operation as soft-delete, restore, cascade soft-delete, or hard-purge.
5. Produce a schema-diff note before any Android Room migration is added.

Patch paths:
- `app/src/main/java/.../data/local/entity/*`
- `app/src/main/java/.../data/local/dao/*`
- `app/src/main/java/.../data/local/AppDatabase*`
- Room schema export / migration files

Objects/elements:
- `WorkspaceEntity`, `WorkspaceSettingsEntity`, `BookEntity`, `QuizEntity`, `QuestionEntity`, `SessionEntity`
- `QuestionAssetEntity`, `SourceDocumentEntity`, `AnnotationEntity`, `MistakeLogEntryEntity`
- `FlashcardDeckEntity`, `FlashcardEntity`, `SlideshowCourseEntity`, `CourseSlideEntity`, `PromptDeckEntity`, `PromptCardEntity`, `PromptRunEntity`

### Phase 1.2 - Soft-delete query audit

Status: REQUIRED

Steps:
1. Audit list queries for active-only behavior.
2. Audit detail queries for missing or soft-deleted parents.
3. Add explicit restore queries where feature behavior requires restore.
4. Keep hard delete limited to purge, cleanup, or migration repair paths.
5. Add regression fixtures for restored records and old deleted rows.

Patch paths:
- DAO files for books, quizzes, questions, sessions, assets, sources, annotations, and mistake logs
- Repository delete/restore functions
- Any import repair service that currently hard-deletes silently

### Phase 1.3 - Relationship/cascade audit

Status: REQUIRED

Steps:
1. Confirm book cascade touches quizzes, questions, sessions, decks, courses, notes, prompts, assets, sources, annotations, and mistakes.
2. Confirm quiz cascade touches questions, sessions, assets, categories, mistakes, and summaries.
3. Confirm question cascade touches assets, category links, annotations, mistake logs, and derived learning objects.
4. Keep normal user delete separate from import conflict repair.

Patch paths:
- Repository orchestration layer
- Delete/restore helpers
- Existing tests around broad destructive actions

## Stage 2 / Stage 4 - Cross-platform exchange canonicalization

### Phase 4A - Schema-7 writer

Status: NEXT ANDROID PATCH

Steps:
1. Emit deterministic `.mks.zip` packages.
2. Write `manifest.json` first.
3. Write table JSON files under `data/` for all v26 backend domains.
4. Write soft-deleted rows with `deletedAt`; do not silently drop them.
5. Preserve `externalId` values exactly.
6. Emit `media/index.json` for local assets.
7. Add writer tests using the iOS V02 field shape.

Patch paths:
- Android export manager / full-library export service
- ZIP writer
- Repository export queries
- Data tools import/export screen backend actions

Required elements:
- `manifest.json`
- `schemaVersion: 7`
- `androidRoomSchema: 26`
- `workspaceExternalId`
- `exportedAt`
- `data/*.json`
- `media/index.json`
- `media/**`

### Phase 4B - Schema-7 reader

Status: REQUIRED AFTER WRITER

Steps:
1. Validate manifest before reading tables.
2. Reject unsupported future schemas safely.
3. Match records by `externalId` before local numeric ID.
4. Preserve external IDs on import.
5. Stage JSON validation before media copy.
6. Copy media only after core graph validation passes.
7. Surface warnings for missing media, orphaned links, duplicates, and unsupported records.

Patch paths:
- Android import manager
- Conflict resolver
- Media copy service
- Import preview and final commit paths

Interactables to preserve:
- Data tools import button
- Android file picker result
- Import preview dialog
- Merge-only option
- Merge-and-update option
- Missing media warning
- Plain HTTP consent
- Final import confirmation

### Phase 4C - Conflict and soft-delete policy

Status: REQUIRED

Steps:
1. Implement merge-only, merge-and-update, and keep-local-safe behavior.
2. Treat `deletedAt` as a real record state, not as absence.
3. Preserve newer local changes unless the selected policy explicitly overwrites.
4. Make restore and purge explicit actions.
5. Record conflict warnings for UI display.

Objects affected:
- Workspace, book, quiz, question, session
- Assets/sources/annotations/mistakes
- Categories/question-category links
- Learning tools

### Phase 4D - Android exchange fixtures

Status: REQUIRED

Steps:
1. Export a one-book workspace fixture.
2. Export a fixture with soft-deleted rows.
3. Export a fixture with image/PDF/link/text/source assets.
4. Export a fixture with annotations and mistake logs.
5. Import iOS-generated fixtures and compare canonical signatures.

## Stage 5 - Android/iOS core study parity support

### Phase 5A - Preserve core study behavior during exchange

Steps:
1. Verify imported iOS books appear in the Android library.
2. Verify imported quizzes run in the Android quiz player.
3. Verify sessions resume safely when imported from iOS.
4. Verify summaries tolerate missing/deleted questions.

Interactables:
- Library card tap/long press
- Quiz card tap/long press
- Session start/resume/delete
- Quiz option tap/long press
- Finish quiz and summary review

## Stage 6 - Assets, sources, annotations

### Phase 6A - Android media parity

Steps:
1. Resolve imported `media/index.json` paths.
2. Rehydrate local files under Android app storage.
3. Keep broken or missing media as warnings, not crashes.
4. Preserve asset/source/annotation ownership.

Interactables:
- Question attachment sheet
- Asset add/edit/delete dialogs
- Source picker/creator
- Annotation cleanup/search paths

## Stage 7 - Learning tools parity

### Phase 7A - Learning-object exchange

Steps:
1. Export/import flashcards and learning sessions.
2. Export/import notes/blueprints.
3. Export/import slideshow courses/slides.
4. Export/import prompt decks/cards/runs.
5. Export/import mistake logs and review queues.

## Stage 8 - UI/path coverage audit

### Phase 8A - Android map as coverage oracle

Steps:
1. Preserve the existing Android path/interactable register as the coverage source.
2. Compare every Android Page/UI chunk against iOS screens once iOS UI catches up.
3. Track hidden/no-op interactions separately from missing features.
4. Require confirmation for destructive actions and import/export paths.

Primary coverage groups:
- P01-P20 page/UI chunks
- S01-S06 support/backend chunks
- Dialogs, sheets, pickers, permissions, gestures, and system-entry paths

## Stage 9 - Android release hardening

### Phase 9A - Exchange hardening

Steps:
1. Test corrupted ZIP files.
2. Test missing media.
3. Test old schema imports.
4. Test large libraries.
5. Test restored and soft-deleted records.
6. Test no-crash startup after partial import failure.

## Android V02 recommendation

Keep Android source frozen except for focused exchange-canonicalization patches. Use `iOS_MKS.V02.zip` as the iOS peer base, and start Android coding only when Stage 4A exchange fixture shape is locked.
## V03 Stage 4A update

Status: NOT PATCHED in Android.

Android remains the canonical v26 reference bundle. The next Android patch should implement the same schema-7 Stage 4A archive layout generated by iOS V03:

- `manifest.json`
- `workspace.json`
- `data/books.json`
- `data/quizzes.json`
- `data/questions.json`
- `data/question_categories.json`
- `data/soft_deletes.json`

Required Android phase next:

Stage 4C — Android exchange reader/writer patch after iOS Stage 4A output shape is accepted.

---

## Stage 4C update - Android schema-7 reader/writer parity

**Base loaded:** `MKS-GPT4-v26-annotation-cleanup-ui-patched.zip`

**Output base:** `MKS_ANDROID.V26_STAGE4C_SCHEMA7.zip`

**Status:** PASS WITH FIXES

### Implemented in Android

1. Added schema-7 exchange DTOs and constants under `data/exchange/v7`.
2. Added schema-7 archive bridge capable of converting:
   - schema-7 split JSON directory -> existing `LibraryBundleDto`
   - existing `LibraryBundleDto` -> schema-7 split JSON ZIP
3. Patched `ZipLibraryParser` to detect `manifest.json` with `format = mks.exchange` and `schemaVersion = 7`.
4. Added schema-7 export helper methods in `ExportManager`:
   - `exportQuizToSchema7Zip`
   - `exportBundleToSchema7Zip`
   - `exportAllToSchema7Zip`

### Remaining Android patches

1. Wire schema-7 export/import into Data Tools and existing import/export UI paths.
2. Add DAO-native import for question assets, source documents, annotations, media manifest, and soft-delete replay.
3. Add media byte copy into `media/`.
4. Add cross-platform fixtures shared with iOS.
5. Run Android Gradle build and instrumentation tests in Android Studio/CI.

### Safest next Android phase

Stage 4D-A: UI wiring for schema-7 import/export behind a guarded/export-format choice.

## Stage 4D — Schema-7 UI wiring and cross-platform fixture

Status: PASS WITH FIXES

Base used: `MKS_ANDROID.V26_STAGE4C_SCHEMA7.zip`
New Android base: `MKS_ANDROID.V26_STAGE4D_SCHEMA7_UI.zip`

Implemented:
- Library export UI path now writes schema-7 archives for quiz, book, and full-library export.
- Schema-7 Android writer now emits standard unencrypted ZIP files compatible with iOS V04.
- Minimal schema-7 fixture added under Android migration docs.

Deferred:
- Media byte copying into `media/`.
- DAO-native merge for assets/sources/annotations.
- Full Gradle test run.



## Stage 4E — schema-7 round-trip fixtures (implemented)
Status: PASS WITH FIXES
Base inputs: Android `MKS_ANDROID.V26_STAGE4D_SCHEMA7_UI.zip`; iOS `iOS_MKS.V04.zip`.
Outputs: Android Stage 4E bundle and iOS V05 bundle.
Scope: deterministic schema-7 fixture with workspace, settings, books, quizzes, questions, category links, asset metadata, source documents, annotations, and soft-delete markers.
Validation: fixture entry set and canonical SHA-256 signature must match on both platforms before media-byte exchange is added.
Next: Stage 4F media-byte copying and archive import/export picker hardening.
