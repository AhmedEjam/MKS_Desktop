# MKS Repair Pass 3 Report

Date: 2026-05-26

Input baseline: `MKS-GPT4-crash-repair-pass2.zip`

Output package: `MKS-GPT4-crash-repair-pass3.zip`

## Goal

Repair pass 3 focused on deeper runtime/data-integrity issues after the startup crash hardening in passes 1 and 2. The emphasis was not new features, but reducing destructive update behavior, review queue loops, and silent startup failures.

## Repairs applied

### 1. Category edit/delete/merge now updates questions in place

Changed file:

- `app/src/main/java/com/ahmedyejam/mks/data/repository/MksRepository.kt`

Problem:

Category delete/rename/merge operations previously rebuilt affected `QuestionEntity` rows and called `insertQuestions(...)`. Because the DAO insert uses `OnConflictStrategy.REPLACE`, this can behave like delete-then-insert at SQLite level and risks damaging child-linked data such as category links, assets, sessions, and other question-owned records.

Fix:

- Added `updateQuestionsInPlace(...)` helper.
- Category delete now calls in-place update.
- Category rename now calls in-place update.
- Category merge now calls in-place update.
- Each updated question still syncs its categories and refreshes affected quiz stats.

### 2. Import merge now updates existing rows instead of replacing them

Changed file:

- `app/src/main/java/com/ahmedyejam/mks/data/import/repository/ImportLibraryManager.kt`

Problem:

When an imported book, quiz, or question matched an existing external ID, the merge path used `insertBook(...)`, `insertQuiz(...)`, or `insertQuestion(...)` with an existing primary key. These DAO methods are REPLACE-style inserts and can behave destructively with related rows.

Fix:

- Existing books now use `bookDao.updateBook(...)`.
- Existing quizzes now use `quizDao.updateQuiz(...)`.
- Existing questions now use `questionDao.updateQuestion(...)`.
- Existing question progress/review fields are preserved during import update, including attempts, correctness, mark/drop state, notes, study timestamps, time spent, last result, and consecutive-correct count.

### 3. Library knowledge-summary failures are no longer silent

Changed file:

- `app/src/main/java/com/ahmedyejam/mks/ui/library/LibraryViewModel.kt`

Problem:

The library summary loader swallowed database errors and silently set the summary to null. This made startup DB/migration problems harder to diagnose because the UI showed no error signal.

Fix:

- Summary failure still keeps the main library usable.
- A snackbar event is now emitted with a concise message: the knowledge summary is unavailable but the main library can still be used.
- Used non-suspending `trySend(...)` to avoid startup coroutine blocking if the snackbar collector is not active yet.

### 4. Marked and weak review queues now respect review timing

Changed files:

- `app/src/main/java/com/ahmedyejam/mks/data/local/dao/QuestionDao.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/review/ReviewRepository.kt`

Problem:

Marked/weak question review items could reappear immediately after review/snooze because queue queries did not consistently respect review timestamps. In practice, `MARKED_QUESTION` and `WEAK_QUESTION` queues could feel like no-op review actions.

Fix:

- Added `getMarkedQuestionsForReview(now, limit)` so marked questions only appear when unscheduled or due.
- Added `snoozeMarkedQuestion(questionId, reviewAt, updatedAt)` for real marked-question snooze behavior.
- Added `getWeakQuestionsDue(cutoff, limit)` so recently reviewed weak questions are hidden for a cooldown window.
- Review summary and review queue now use due-aware methods.
- Weak-question snooze marks the question reviewed now, making it leave the queue until the weak cooldown expires.

## Validation performed

Static/source validation completed in the sandbox:

- XML parse check: passed.
- Duplicate DAO method scan: passed.
- No Kotlin `!!` usages in `app/src/main/java`.
- No `fallbackToDestructiveMigration` in app source.
- No `allowBackup="true"` in app source.
- Category rename/delete/merge no longer call `insertQuestions(updatedQuestions)`.
- Import merge no longer uses REPLACE-style `insertBook(bookEntity.copy(id...))`, `insertQuiz(quizEntity.copy(id...))`, or `insertQuestion(qEntity.copy(id...))` patterns for existing records.
- Pass-3 zip integrity test: passed.

## Build status

A full Gradle build could not be completed inside this sandbox. The Gradle wrapper still attempted to download Gradle from `services.gradle.org` and failed with `UnknownHostException`, even when invoked with `--offline`.

This is an environment/network limitation, not a confirmed source-code build failure. The project still needs a local build on a machine with Android SDK and Gradle distribution/cache available.

Suggested command locally:

```bash
./gradlew clean assembleDebug
```

## Deferred checks

These should be completed in the next local or network-enabled run:

1. Full local Gradle build and install.
2. Room migration test using old/restored database fixtures.
3. Import merge integration test proving existing records are updated without losing linked rows.
4. Category rename/delete/merge test proving question assets, categories, and stats survive.
5. Review-dashboard test proving marked/weak snooze and mark-reviewed actions remove items from the immediate queue.
6. Export/import smoke test with media caveat verified in UI.
