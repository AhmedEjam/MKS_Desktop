# MKS Stage 2B–2H Approved Patch Report

## Source bundle

Input bundle:

```text
MKS-GPT4-stage5-stage2A-approved-patched.zip
```

This patch applies the remaining approved master-plan systems onto the Stage 5 + Stage 2A bundle.

## Database result

```text
Previous Room DB version: 20
New Room DB version: 21
Migration added: MIGRATION_20_21
Migration file added: data/local/MksMigrations.kt
```

`AppModule` now registers migrations through `MksMigrations.ALL`, while preserving the previous migrations from `MksDatabase.kt`. This is a light architecture cleanup step rather than a risky full migration rewrite.

## Implemented Stage 2B — Marked / Dropped Workflows foundation

Added fields to `QuestionEntity`:

```text
droppedAt
droppedReason
markedAt
markReason
markReviewAt
```

Added DAO/repository support for:

```text
marked question lookup
marked questions by quiz/book
clear marks for quiz/book
weak question lookup
marked due-for-review lookup
update mark state with optional reason
update dropped state with optional reason
clear marks preview result
```

## Implemented Stage 2C — Mistake Log foundation

Added:

```text
MistakeLogEntryEntity
MistakeLogDao
mistake_log_entries table
```

Fields implemented:

```text
bookId
quizId
questionId
sessionId
selectedAnswer
correctAnswer
userReason
correctConcept
preventionNote
linkedFlashcardId
linkedBlueprintId
linkedAssetId
isFixed
reviewAt
createdAt
updatedAt
```

Quiz answer handling now auto-logs a minimal mistake entry when an answer is wrong. Details such as user reason, correct concept, and prevention note can be added later through richer mistake UI.

## Implemented Stage 2D — Simulation / Preview System

Added shared models:

```text
ChangeSimulationResult
SimulatedItem
```

Added reusable UI:

```text
ChangePreviewDialog
```

Added first integrated preview path:

```text
clear marks preview through repository
full import bundle preview through data tools screen
```

The shared model supports:

```text
created items
updated items
deleted items
skipped items
blocked items
warnings
affected books/quizzes/questions
```

## Implemented Stage 2E — Global Search

Added:

```text
GlobalSearchResultType
GlobalSearchResult
GlobalSearchDao
GlobalSearchRepository
GlobalSearchViewModel
GlobalSearchScreen
```

Search is SQL LIKE based first, as approved. It searches across:

```text
books
quizzes
questions
question explanations/hints/notes/options/categories
question assets
source documents
flashcards
blueprints
slides
prompt decks
prompt cards
prompt runs
mistake logs
```

Added navigation route:

```text
global_search
```

Entry point:

```text
Settings -> Global search
```

## Implemented Stage 2F — Full Import / Export foundation

Added:

```text
MksFullImportExportService
MksFullExportResult
DataToolsViewModel
DataToolsScreen
```

Export now creates a conservative full-library ZIP in cache with:

```text
manifest.json
data/books.json
data/quizzes.json
data/questions.json
data/sessions.json
data/question_categories.json
data/asset_references.json
data/question_assets.json
data/source_documents.json
data/flashcard_decks.json
data/flashcards.json
data/note_blueprints.json
data/slideshow_courses.json
data/course_slides.json
data/prompts.json
data/prompt_decks.json
data/prompt_cards.json
data/prompt_runs.json
data/knowledge_study_sessions.json
data/learning_sessions.json
data/mistake_log_entries.json
```

Import support added as a safe ZIP preview first. It reads `manifest.json` and `data/*.json` entries and displays a `ChangeSimulationResult` before any data is modified.

Important limitation: full graph import/apply is intentionally conservative in this patch. The legacy importer remains available for older quiz/book bundles. The new full graph bundle preview is ready for a future apply step with duplicate handling.

## Implemented Stage 2G — Review Scheduling and Dashboards

Added to flashcards:

```text
dueAt
reviewCount
```

Added review models/repository:

```text
ReviewDashboardSummary
ReviewQueueItem
ReviewQueueType
ReviewRepository
```

Added UI:

```text
ReviewDashboardViewModel
ReviewDashboardScreen
```

Review dashboard supports:

```text
due flashcards
due blueprints
due mistakes
marked questions
weak questions
unfinished slides count
mark reviewed
snooze 1 week
open parent route when available
```

Added navigation route:

```text
review_dashboard
```

Entry point:

```text
Settings -> Review dashboard
```

## Implemented Stage 2H — Architecture Cleanup

Added reusable components:

```text
ChangePreviewDialog
EmptyStateCard
SummaryCard
LoadingErrorState
```

Added route helper:

```text
MksRoutes.kt
```

Moved the new migration into:

```text
MksMigrations.kt
```

Connected prior migrations through:

```text
MksMigrations.ALL
```

This avoids a risky full rewrite while starting the approved migration split.

## Known limitations / deferred items

The following remain intentionally limited or deferred:

```text
No Room FTS search yet; LIKE search first only.
No cloud sync.
No collaboration import.
No automatic destructive import replace.
No advanced conflict resolver beyond preview.
No full media dedup dashboard.
No full spaced repetition algorithm.
No heavy analytics dashboard/charts.
No concept graph.
No total repository rewrite.
No universal KnowledgeAsset / AssetLink layer.
Full graph import apply is preview-first only in this patch.
Media files are not fully copied into the new full export ZIP yet; existing legacy export manager still handles older bundle/media export behavior.
Mistake detail editing UI is not complete yet; minimal auto-log and review queue are implemented.
```

## Build/test note

Attempted command:

```bash
./gradlew tasks --no-daemon
```

Result: failed because the sandbox cannot resolve/download Gradle from `services.gradle.org`.

Recommended local test command:

```bash
./gradlew clean assembleDebug --no-daemon
```

## Manual test checklist

After opening the project locally, test:

```text
1. Existing library opens.
2. Existing quizzes open.
3. Quiz answer submission still works.
4. Wrong answer creates a mistake entry.
5. Settings opens.
6. Settings -> Global search opens.
7. Search finds a book title, question text, flashcard text, blueprint title, prompt title, and source title.
8. Settings -> Review dashboard opens.
9. Review dashboard counts load.
10. Review item mark-reviewed and snooze buttons do not crash.
11. Settings -> Advanced import/export preview opens.
12. Full export creates a ZIP in cache.
13. Import preview reads a ZIP path and shows preview before modifying data.
14. Room migration from v20 to v21 validates on a device/emulator with existing data.
```
