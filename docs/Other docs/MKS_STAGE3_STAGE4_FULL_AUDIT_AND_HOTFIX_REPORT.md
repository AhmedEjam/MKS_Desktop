# MKS Stage 3 + Stage 4 Full Audit and Hotfix Report

## Bundle audited

Input bundle:

```text
MKS-GPT4-stage3-stage4-approved-patched.zip
```

Project type observed:

```text
Android / Kotlin / Jetpack Compose / Room
```

Current Room DB version in bundle:

```text
19
```

## Audit scope

The audit focused on the last Stage 3 + Stage 4 patched bundle, including regressions and runtime risks introduced or exposed by:

- Stage 1 flashcard management
- Stage 2 question assets
- Stage 3 source documents / citations
- Stage 4 richer blueprints
- Room migrations 17 -> 18 and 18 -> 19
- Compose screen imports and route wiring
- DAO/entity/migration consistency
- Repository lifecycle behavior for generated content and local assets

## Verification attempted

### Zip integrity

Result: passed.

```text
No errors detected in compressed data.
```

### Gradle build

Command attempted:

```bash
./gradlew assembleDebug --no-daemon
```

Result: not completed in this sandbox because the Gradle wrapper attempted to download Gradle from `services.gradle.org`, but internet/DNS access is blocked here.

Observed failure:

```text
java.net.UnknownHostException: services.gradle.org
```

This means the code could not be fully compiled in this environment. The rest of this report is based on direct static inspection of the project source.

---

# Critical findings fixed in this hotfix

## 1. Potential Compose compile failure: missing scroll imports

File:

```text
app/src/main/java/com/ahmedyejam/mks/ui/booktools/BookToolScreens.kt
```

Problem:

`BlueprintCreateDialog` used:

```kotlin
verticalScroll(rememberScrollState())
```

but the file did not import:

```kotlin
androidx.compose.foundation.verticalScroll
androidx.compose.foundation.rememberScrollState
```

Impact:

This could cause unresolved reference compile errors when building the book tools / blueprint screens.

Fix applied:

```kotlin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
```

Status:

```text
FIXED
```

## 2. High-risk Room migration validation mismatch: extra migration index not declared by entity

Files:

```text
app/src/main/java/com/ahmedyejam/mks/data/local/MksDatabase.kt
app/src/main/java/com/ahmedyejam/mks/data/local/entity/QuestionAssetEntity.kt
```

Problem:

`MIGRATION_18_19` creates:

```sql
CREATE INDEX IF NOT EXISTS index_question_assets_sourceDocumentId ON question_assets(sourceDocumentId)
```

but `QuestionAssetEntity` did not declare:

```kotlin
Index("sourceDocumentId")
```

Impact:

Room migration validation may fail on upgraded installs because the migrated table contains an index that the entity schema does not expect.

Fix applied:

```kotlin
indices = [
    Index("bookId"),
    Index("quizId"),
    Index("questionId"),
    Index("assetType"),
    Index("createdAt"),
    Index("sourceDocumentId")
]
```

Status:

```text
FIXED
```

## 3. Room default-value mismatch risk for added non-null blueprint columns

File:

```text
app/src/main/java/com/ahmedyejam/mks/data/local/entity/NoteBlueprintEntity.kt
```

Problem:

`MIGRATION_18_19` adds non-null columns with SQL defaults:

```sql
blueprintMode TEXT NOT NULL DEFAULT 'SIMPLE_NOTE'
linkedQuestionsJson TEXT NOT NULL DEFAULT '[]'
linkedAssetsJson TEXT NOT NULL DEFAULT '[]'
reviewStatus TEXT NOT NULL DEFAULT 'NEW'
```

but the entity only had Kotlin constructor defaults. Kotlin defaults are not always equivalent to Room SQL defaults during migration schema validation.

Impact:

Room may reject upgraded databases if expected schema defaults and actual migrated schema defaults differ.

Fix applied:

Added `@ColumnInfo(defaultValue = ...)` on the new blueprint fields, and aligned existing blueprint list/review defaults that are already created with SQL defaults in older migrations:

```kotlin
@ColumnInfo(defaultValue = "'[]'") val bulletPoints: List<String> = emptyList()
@ColumnInfo(defaultValue = "'[]'") val tags: List<String> = emptyList()
@ColumnInfo(defaultValue = "'SIMPLE_NOTE'") val blueprintMode: String = BlueprintMode.SIMPLE_NOTE
@ColumnInfo(defaultValue = "'[]'") val linkedQuestionsJson: String = "[]"
@ColumnInfo(defaultValue = "'[]'") val linkedAssetsJson: String = "[]"
@ColumnInfo(defaultValue = "'NEW'") val reviewStatus: String = BlueprintReviewStatus.NEW
@ColumnInfo(defaultValue = "0") val reviewCount: Int = 0
@ColumnInfo(defaultValue = "0") val lastReviewedAt: Long = 0
```

Status:

```text
FIXED
```

## 4. Room default-value mismatch risk for `question_assets` fields

File:

```text
app/src/main/java/com/ahmedyejam/mks/data/local/entity/QuestionAssetEntity.kt
```

Problem:

`MIGRATION_17_18` creates `question_assets` with SQL defaults for:

```sql
sortOrder INTEGER NOT NULL DEFAULT 0
isPinned INTEGER NOT NULL DEFAULT 0
isPrimary INTEGER NOT NULL DEFAULT 0
```

but the entity had Kotlin-only defaults.

Impact:

Room schema validation may fail on migrated installs, especially from DB 17 -> 18 -> 19.

Fix applied:

```kotlin
@ColumnInfo(defaultValue = "0") val sortOrder: Int = 0
@ColumnInfo(defaultValue = "0") val isPinned: Boolean = false
@ColumnInfo(defaultValue = "0") val isPrimary: Boolean = false
```

Status:

```text
FIXED
```

---

# Findings not changed in this hotfix, but important

## 1. Full Gradle build still must be run locally

Because this sandbox cannot download Gradle, local verification is still required.

Run locally:

```bash
./gradlew clean assembleDebug --no-daemon
```

Then run migration tests if possible:

```bash
./gradlew connectedAndroidTest
```

Priority:

```text
CRITICAL before treating this as final build-ready code
```

## 2. Export/import does not yet round-trip Stage 3/4 data

The current export/import pipeline still mainly exports books, quizzes, questions, sessions, categories, and legacy assets. It does not yet fully export/import:

- `source_documents`
- `question_assets`
- richer blueprint fields
- source files attached to sources
- blueprint links

This was partly deferred to the later Full Import/Export stage, but it is still a data-loss risk if the user relies on export as a backup after adding sources/assets/blueprints.

Recommendation:

Until Stage 2F is approved and implemented, show a warning in export UI/report that source documents, question assets, and advanced blueprint links may not be included in exported bundles.

Priority:

```text
HIGH for user data safety
```

## 3. Source picker is functional but first-pass only

The source picker supports selecting an existing source or creating a simple new source. However, the source manager UI currently stores detailed citation fields mostly in a generic description/details field rather than separate editable UI fields for:

- author
- edition
- year
- publisher
- URL
- local file

The entity supports these fields, but the first-pass UI does not fully expose them.

Priority:

```text
MEDIUM; acceptable for approved Stage 3 first pass
```

## 4. Source deletion leaves citation assets as text-only/orphan references

There is no foreign key from `question_assets.sourceDocumentId` to `source_documents.id`. This avoids complex coupling, but deleting a source document can leave existing question assets with a `sourceDocumentId` that no longer resolves.

The citation text/page/quote remains in the asset, but the reusable source title is lost.

Recommendation:

Before deleting a source, show an affected citation count and offer:

```text
Delete source only, keep citation text
Cancel
```

Priority:

```text
MEDIUM
```

## 5. Linked blueprint lookup uses string matching on JSON

DAO query:

```sql
linkedQuestionsJson LIKE '%' || :questionId || '%'
```

This can produce false positives. For example, question id `12` can match `[112]`.

Current impact:

Usually minor, but it can show unrelated linked blueprints in some cases.

Recommendation:

For now, encode linked question ids with delimiters and query more safely, or parse links in Kotlin after a book-level query. Later, replace with a proper link table only if needed.

Priority:

```text
LOW-MEDIUM
```

## 6. Quiz player source list flow is slightly fragile

`QuizViewModel.sourceDocumentsForCurrentQuiz()` builds a Flow from the current UI state. It should work after quiz state has loaded, but it is not as clean as deriving sources directly from `quizId` or storing `bookId` in quiz UI state.

Recommendation:

In a future cleanup, add `bookId` to `QuizState` and expose:

```kotlin
fun sourceDocumentsForBook(bookId: Long): Flow<List<SourceDocumentEntity>>
```

or use a `flatMapLatest` based on state.

Priority:

```text
LOW-MEDIUM
```

## 7. Blueprint-to-question-note append has no preview/undo

The append flow avoids overwriting notes, which is good. But it does not preview the final note or offer undo.

Recommendation:

When the simulation/preview system is approved, route this operation through a reusable preview dialog.

Priority:

```text
LOW-MEDIUM
```

## 8. Blueprint-to-flashcards can create duplicate cards repeatedly

Running blueprint -> flashcards multiple times creates a new deck each time. This is safe and non-destructive, but it can create clutter.

Recommendation:

Later add a confirmation dialog showing how many cards will be created and the target deck name.

Priority:

```text
LOW
```

---

# Smoke-test checklist for Android Studio

After opening the hotfixed bundle locally, run this sequence:

## Build and migration

```text
[ ] Gradle sync succeeds
[ ] ./gradlew clean assembleDebug succeeds
[ ] Fresh install opens
[ ] Upgrade from DB 18 to DB 19 opens without Room migration crash
[ ] Upgrade from DB 17 to DB 19 opens without Room migration crash
```

## Stage 1 flashcards

```text
[ ] Open flashcard deck
[ ] Add card
[ ] Edit card
[ ] Delete card
[ ] Reorder cards
[ ] Study card
[ ] Rate Again / Good / Easy
[ ] Generate from marked questions
[ ] Generate from missed questions
[ ] Clear marks option behaves correctly
```

## Stage 2 question assets

```text
[ ] Open quiz question browser
[ ] Open question attachments dialog
[ ] Add image/path asset
[ ] Add PDF/path asset
[ ] Add text note asset
[ ] Add web-link asset
[ ] Edit asset title/description
[ ] Delete asset
[ ] Has attachments filter works
[ ] Paperclip indicator appears
[ ] Quiz player Assets button opens read-only dialog
```

## Stage 3 sources

```text
[ ] Book FAB/menu opens Sources
[ ] Create source
[ ] Edit source
[ ] Delete source
[ ] Add source reference from question attachment dialog
[ ] Select existing source
[ ] Create simple new source while attaching citation
[ ] Page/location and quote display in read-only Assets/Sources dialog
```

## Stage 4 blueprints

```text
[ ] Open book blueprints list
[ ] Create Disease template
[ ] Create Drug template
[ ] Create Concept template
[ ] Create Mistake-review template
[ ] Create blueprint from marked questions
[ ] Create blueprint from missed questions
[ ] Create blueprint from one question
[ ] Open blueprint detail
[ ] Mark reviewed
[ ] Convert blueprint to flashcards
[ ] Append blueprint to question note
```

---

# Final audit status

After the hotfixes in this report, the bundle is safer than the submitted Stage 3/4 patch. The most important issues found were compile/import risk and Room migration-validation risk. Both were patched.

However, because Gradle cannot run in this sandbox, this should be treated as:

```text
Static-audited and hotfixed, but still requiring local Android Studio/Gradle build verification.
```
