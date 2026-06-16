# MKS Stage 3 + Stage 4 Approved Patch Report

## Scope applied

This patch applies the approved Stage 3 and Stage 4 decisions to the provided Stage 1 + Stage 2 patched Android/Kotlin Compose project bundle.

Approved Stage 3:
- Source Documents and Citations.
- Book-specific sources first; no full global source library yet.
- Source file + citation fields.
- `SourceDocumentEntity` and `SourceDocumentDao`.
- Source references continue through `QuestionAssetEntity` fields: `sourceDocumentId`, `sourcePage`, and `sourceQuote`.
- Question attachment/source entry points.
- Source picker inside the question asset editor.
- Simple book-level Sources screen.
- Quiz/review source display inside the existing Assets/Sources dialog.

Approved Stage 4:
- Richer Blueprint System.
- Hybrid blueprint model: existing body/bullets plus lightweight JSON links.
- Added blueprint mode and manual review status fields.
- First templates: Disease, Drug, Concept, and Mistake-review.
- Creation from one question, marked questions, missed questions, and blank/manual blueprint.
- Blueprint to flashcards.
- Blueprint to question-note append flow.
- Linked blueprint access from question attachment dialog and book blueprint list.

## Explicitly deferred / not included

Stage 3 deferrals:
- No full global source library.
- No complex many-to-many source-question junction table.
- No advanced citation formatter.
- No automatic PDF citation extraction.
- No source-based quiz generation.

Stage 4 deferrals:
- No blueprint-to-quiz conversion.
- No blueprint-to-slideshow conversion.
- No full scheduled review algorithm.
- No complex blueprint section table unless needed later.
- No full custom template builder.
- No quiz-player inline blueprint panel.

## Database and persistence

- Bumped Room database version from 18 to 19.
- Added `SourceDocumentEntity` with fields for book scope, source type, citation metadata, file/URL fields, description, and timestamps.
- Added `SourceDocumentDao` with book source list, lookup, search, insert, update, and delete methods.
- Added additive `MIGRATION_18_19`:
  - Creates `source_documents`.
  - Adds indices for `bookId`, `title`, and `sourceType`.
  - Adds index for `question_assets.sourceDocumentId`.
  - Adds `blueprintMode`, `linkedQuestionsJson`, `linkedAssetsJson`, and `reviewStatus` to `note_blueprints`.
  - Adds indices for blueprint mode and review status.
- Registered `SourceDocumentEntity` and `SourceDocumentDao` in `MksDatabase`.
- Registered `MIGRATION_18_19` in `AppModule`.
- Extended repository asset-reference cleanup/rebuild paths to include source document local files.

## Source documents and citations

- Added source document repository CRUD.
- Added hybrid source file handling:
  - External HTTP/HTTPS values remain external URLs.
  - Local/content/file paths are copied through the existing internal-storage helper.
  - Source document file paths are tracked through `AssetReferenceEntity` as `source_document` owners.
- Added `createSourceDocumentAndQuestionAsset` to create a new source and citation asset in one flow.
- Extended `QuestionAssetsDialog` into an Attachments & Sources surface.
- Added `SOURCE_REFERENCE` to the selectable attachment form for Stage 3.
- Added existing-source picker inside the asset form.
- Added simple new-source creation inside the source-reference form.
- Added page/location and quote/citation note fields for source references.
- Added source titles and citation metadata to the read-only quiz Assets/Sources dialog.
- Added source access from:
  - Quiz question browser attachment dialog.
  - Category question browser attachment dialog.
  - Quiz player read-only Assets/Sources dialog.
  - Book-level Sources screen.

## Book-level Sources screen

- Added `SourceDocumentListScreen` under book tools.
- Added source create/edit/delete dialogs.
- Added a Sources action in the book FAB menu.
- Added navigation route: `book_sources/{bookId}`.
- Kept this intentionally simple; it is not a full global source manager.

## Richer blueprint model

- Extended `NoteBlueprintEntity` with:
  - `blueprintMode`
  - `linkedQuestionsJson`
  - `linkedAssetsJson`
  - `reviewStatus`
  - existing `reviewCount` / `lastReviewedAt` retained
- Added blueprint mode constants:
  - `SIMPLE_NOTE`
  - `OUTLINE`
  - `CHECKLIST`
  - `ALGORITHM`
  - `DISEASE_TEMPLATE`
  - `DRUG_TEMPLATE`
  - `CONCEPT_TEMPLATE`
  - `MISTAKE_REVIEW`
  - `CUSTOM`
- Added review status constants:
  - `NEW`
  - `REVIEWING`
  - `REVIEWED`
  - `NEEDS_UPDATE`
- Updated `NoteBlueprintDao` with linked-question lookup.

## Blueprint UI and flows

- Updated book blueprint list to show blueprint mode and review status.
- Added first-pass template creation dialog for Disease, Drug, Concept, and Mistake-review blueprints.
- Added quick actions to create a blueprint from marked questions and missed questions.
- Added question-level blueprint creation from the Attachments & Sources dialog.
- Added linked blueprint display in the question Attachments & Sources dialog.
- Updated blueprint detail screen to display:
  - summary
  - blueprint mode
  - review status and review count
  - linked questions/assets JSON
  - body
  - bullet points
- Updated review action so marking reviewed also sets `reviewStatus = REVIEWED`.

## Blueprint conversions

- Added repository method to generate a flashcard deck from a blueprint.
- The generated deck creates:
  - one main card with title/body
  - additional cards from blueprint bullet points
- Added blueprint detail action for Blueprint to Flashcards.
- Added repository and UI action to append blueprint content to the source/linked question note.
- This append flow intentionally does not silently overwrite existing notes; it appends below existing note text.

## Files most directly changed or added

- `app/src/main/java/com/ahmedyejam/mks/data/local/entity/SourceDocumentEntity.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/SourceDocumentDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/entity/NoteBlueprintEntity.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/NoteBlueprintDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/MksDatabase.kt`
- `app/src/main/java/com/ahmedyejam/mks/di/AppModule.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/repository/MksRepository.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/category/QuestionAssetsDialog.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/category/CategoryQuestionsViewModel.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/category/CategoryQuestionsScreen.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/quiz/QuizQuestionsViewModel.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/quiz/QuizQuestionsScreen.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/quiz/QuizViewModel.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/quiz/QuizPlayerScreen.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/booktools/BookToolsViewModel.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/booktools/BookToolScreens.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/library/LibraryScreen.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/library/components/LibraryFabMenu.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/MksNavHost.kt`

## Verification

Attempted command:

```bash
./gradlew assembleDebug --no-daemon
```

Result:

The sandbox could not download the Gradle distribution from `services.gradle.org` because internet access is unavailable in this environment. Therefore, a full Gradle compile/build could not be completed here.

Observed failure:

```text
java.net.UnknownHostException: services.gradle.org
```

## Notes for next local/Xcode/Android Studio verification

1. Open the patched project in Android Studio.
2. Let Gradle sync download the wrapper distribution and dependencies.
3. Run `./gradlew assembleDebug` or Android Studio Build.
4. Test migration from the previous Stage 1+2 DB version 18 to version 19.
5. Smoke-test:
   - Book FAB → Sources.
   - Add/edit/delete a source.
   - Question browser → paperclip/assets → source reference.
   - Quiz player → Assets/Sources read-only dialog.
   - Book blueprints → create template blueprint.
   - Create blueprint from marked/missed questions.
   - Question assets dialog → Make blueprint.
   - Blueprint detail → mark reviewed, create flashcards, append to question note.

## Remaining implementation notes

- Source document author/edition/year/publisher fields exist in the entity, but the first-pass UI keeps details in a simple text field to avoid overbuilding the source manager.
- The source picker supports attaching citations to question assets without introducing a complex many-to-many source-question junction table.
- `linkedQuestionsJson` and `linkedAssetsJson` are lightweight JSON string fields as approved; a normalized link table can be considered later only if the app needs complex graph queries.
- Export/import support for sources and richer blueprint fields should be completed in the later approved import/export stage so media bundle rules stay consistent across all knowledge objects.
