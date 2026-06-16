# MKS Stage 1 + Stage 2 Approved Patch Report

## Scope applied

This patch applies the approved first two stages of the master implementation plan to the provided Android/Kotlin Compose project bundle.

Approved Stage 1:
- Full flashcard management.
- Again / Good / Easy rating style.
- Detached flashcard copies generated from questions.
- Optional mark clearing after flashcard conversion.
- Generation from marked, missed, selected, and current-question contexts where reachable from current UI structure.
- No blueprint-to-flashcards, prompt-to-flashcards, full spaced repetition algorithm, or full Knowledge Hub UI in this patch.

Approved Stage 2:
- Question asset/media system.
- First asset types: image, PDF, text note, web link.
- Hybrid storage: local content/file URIs are copied into app storage; external URLs remain URLs.
- Question editor/browser attachment entry points.
- Quiz player `Assets (N)` button with bottom-sheet style read-only dialog.
- Paperclip indicator and `Has attachments` filter.
- No audio/video player, thumbnail generation, full asset manager screen, or universal KnowledgeAsset/AssetLink layer in this patch.

## Database and persistence

- Added `QuestionAssetEntity` and `QuestionAssetType`.
- Added `QuestionAssetDao`.
- Bumped Room database version from 17 to 18.
- Added additive `MIGRATION_17_18` creating `question_assets` with indices for book, quiz, question, asset type, and creation time.
- Registered `QuestionAssetDao` in `MksDatabase` and `AppModule`.
- Kept `AssetReferenceEntity` separate as low-level path/reference tracking.
- Added question-asset local file ownership into asset-reference rebuild/release paths.

## File handling

- Added generic asset-copy support in `FileManager`.
- Local `content://` and `file://` asset references are copied into internal `question_assets` storage.
- HTTP/HTTPS values are treated as external URLs, not copied.
- Local copied asset files are reference-tracked for cleanup safety.

## Flashcards

- Expanded flashcard DAO methods for card counting, ID lookup, card-order updates, and deck observation.
- Added repository methods for creating decks from selected question IDs, marked questions, and missed questions.
- Added mapping from question to flashcard:
  - Front = question stem.
  - Back = correct answer plus explanation/reference when present.
  - Hint = question hint.
  - Tags = question categories.
  - Source = `sourceQuestionId`.
  - Sync = detached copy.
- Added flashcard card reordering and rating with Again / Good / Easy.
- Expanded `FlashcardDeckViewModel` to support edit deck, add/edit/delete/reorder cards, study mode, ratings, and generation from marked/missed questions.
- Rebuilt `FlashcardDeckScreen` into a full management/study surface with stats, card editor, deck editor, generation controls, empty states, and source chips.

## Question assets UI

- Added `QuestionAssetsDialog` for add/edit/delete attachment management.
- Added `QuestionAssetsReadOnlyDialog` for quiz-player read-only viewing.
- Added attachment CRUD methods to category and quiz question ViewModels.
- Added `Has attachments` filter support to category and quiz question lists.
- Added paperclip button/indicator to question cards.
- Added selected-question flashcard generation dialog from question list selection mode.
- Added quiz-player `Assets (N)` button for the current question.

## Files most directly changed or added

- `app/src/main/java/com/ahmedyejam/mks/data/local/entity/QuestionAssetEntity.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/QuestionAssetDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/MksDatabase.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/FileManager.kt`
- `app/src/main/java/com/ahmedyejam/mks/di/AppModule.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/repository/MksRepository.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/FlashcardDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/FlashcardDeckDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/flashcard/FlashcardDeckViewModel.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/flashcard/FlashcardDeckScreen.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/category/QuestionAssetsDialog.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/category/CategoryQuestionsViewModel.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/category/CategoryQuestionsScreen.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/quiz/QuizQuestionsViewModel.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/quiz/QuizQuestionsScreen.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/quiz/QuizViewModel.kt`
- `app/src/main/java/com/ahmedyejam/mks/ui/quiz/QuizPlayerScreen.kt`

## Verification

Attempted command:

```bash
./gradlew assembleDebug
```

Result:

The sandbox could not download the Gradle distribution from `services.gradle.org` because internet access is unavailable in this environment. Therefore, a full Gradle compile/build could not be completed here.

Observed failure:

```text
java.net.UnknownHostException: services.gradle.org
```

## Remaining implementation notes

- The patch keeps the approved Stage 2 first-pass asset input simple: the user can enter a local path/content URI or external URL. A polished Android file picker can be added later without changing the database model.
- Full media thumbnailing, audio/video playback, full asset manager, full SRS algorithm, blueprint/prompt conversion, and Knowledge Hub UI remain intentionally excluded according to the approved scope.
- Export/import DTO expansion for question assets should be handled in the later approved import/export phase so media bundle behavior can be designed consistently across all knowledge objects.
