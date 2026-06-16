# MKS Patch Implementation Plan

## Scope
Apply the first three next-step improvements to this Android bundle without changing the database schema:

1. Fix weak/no-op UI paths.
2. Make marked questions more useful.
3. Complete the first safe layer of non-quiz CRUD and management UX.

This patch intentionally avoids risky schema migrations and keeps changes localized to Compose UI/ViewModel/repository-facing methods that already exist.

## Current findings

### Weak UI / defects
- `CategoryQuestionsScreen` selection toolbar exposes Copy and Export buttons through default no-op callbacks.
- Category question cards show an edit affordance but do not open the question editor.
- Category question bulk workflows are weaker than quiz question bulk workflows.
- Session deletion happens immediately without confirmation.
- Quiz player navigation has All/Answered/Unanswered/Missed but no Marked or Dropped filter.
- Dropped questions can be created but are not easily restorable from the player.
- Prompt conversation link open button is a stub.

### Marked questions
- Marking exists in player/browser/category list and session filters.
- There is no strong follow-up loop for marked questions inside the player or summary.
- Bulk mark/unmark is missing from question selection bars.

### Non-quiz tools
- ViewModel already has create/update/delete methods for slideshow courses, slides, notes/blueprints, and prompts.
- List screens do not expose Create actions.
- Detail screens have limited edit/delete actions.
- Prompt usage tracking exists in fields but copy/open actions do not record use.

## Patch goals

### A. Fix weak/no-op UI
- Wire Category Questions selection Copy and Export actions.
- Wire Category Questions card edit action to `EditQuestionDialog`.
- Add category bulk Mark/Unmark actions.
- Add delete confirmation to session deletion.
- Replace prompt conversation link stub with open/copy behavior where possible.

### B. Improve marked/dropped question workflow
- Add `MARKED` and `DROPPED` to quiz-player navigation filters.
- Add a restore action for dropped questions from the player when viewing dropped questions.
- Add bulk mark/unmark actions in quiz and category question selection toolbars.

### C. Non-quiz CRUD usability layer
- Add Create FABs to slideshow course, blueprint, and AI prompt list screens.
- Add simple create dialogs for slideshow course, blueprint, prompt.
- Add edit/delete actions in list rows where safe.
- Add basic slide creation/edit/delete in slideshow detail.
- Add prompt usage tracking on copy/open actions.

## Implementation steps

1. Update `CategoryQuestionsViewModel.kt`
   - Add `copySelectedQuestionsToQuiz(targetQuizId)`.
   - Add `exportSelectedQuestionsToNewQuiz(title)`.
   - Add `updateQuestion(question)`.
   - Add `markSelectedQuestions(marked: Boolean)`.

2. Update `CategoryQuestionsScreen.kt`
   - Add copy/export dialogs.
   - Add edit dialog wiring.
   - Add delete confirmation dialog.
   - Pass new toolbar callbacks.

3. Update shared `SelectionTopAppBar`
   - Add optional mark/unmark selected actions.
   - Keep copy/export configurable and visible only when wired.

4. Update `QuizQuestionsViewModel.kt` and `QuizQuestionsScreen.kt`
   - Add bulk mark/unmark selected actions.
   - Add delete confirmation dialog.

5. Update `QuizViewModel.kt` and `QuizPlayerScreen.kt`
   - Extend navigation filter with MARKED and DROPPED.
   - Add `restoreCurrentDroppedQuestion()`.
   - Add filter logic and a visible restore button for dropped questions.

6. Update `SessionManagementScreen.kt`
   - Add confirmation before deleting a session.

7. Update `BookToolsViewModel.kt`
   - Add `recordPromptUse(prompt)`.
   - Ensure update methods refresh timestamps consistently.

8. Update `BookToolScreens.kt`
   - Add create FAB and dialogs for slideshow/blueprint/prompt lists.
   - Add edit/delete affordances for list items.
   - Add basic slide create/edit/delete actions.
   - Add prompt copy/open actions that record usage.

9. Validate
   - Run Gradle build if the wrapper/distribution is available locally.
   - If build cannot run because Gradle must be downloaded, perform static checks for changed files and package the patched bundle.

## Expected user-visible result
- Category bulk Copy/Export/Edit no longer feels broken.
- Deleting sessions requires confirmation.
- Marked questions become easier to filter and bulk-manage.
- Dropped questions can be found and restored from the player.
- Slideshow, blueprint, and prompt lists can create new content.
- Prompt links are usable and prompt usage can be tracked.

## Deliberately deferred
- Full KnowledgeAsset schema and migrations.
- True independent knowledge model.
- Full Knowledge Hub screen.
- Full import/export of non-quiz data.
- True PromptDeck/PromptCard schema split.

## Implementation status in this bundle

### Completed
- Added `implementation_plan.md` before code changes.
- Wired Category Questions selection Copy and Export actions.
- Wired Category Questions card edit action to `EditQuestionDialog`.
- Added Category Questions and Quiz Questions bulk Mark/Unmark selected actions.
- Added delete confirmation dialogs for selected question deletion and session deletion.
- Added `MARKED` and `DROPPED` quiz-player navigation filters.
- Added a restore action for the current dropped question.
- Added Create FABs and simple create/edit/delete dialogs to slideshow course, review blueprint, and AI prompt list screens.
- Added basic slide create/edit/delete actions in slideshow detail.
- Added blueprint detail actions: mark reviewed, edit, delete.
- Replaced prompt conversation link stub with open/copy actions.
- Added prompt usage tracking when copying rendered prompts or using prompt links.

### Validation status
- Static file and brace-balance checks completed for changed Kotlin files.
- Gradle build could not be executed in this sandbox because the Gradle wrapper requires downloading `gradle-9.4.1-bin.zip` from `services.gradle.org`, and external network access is unavailable.

### Still deferred
- Full Knowledge Hub screen.
- Independent knowledge schema.
- Full non-quiz import/export schema.
- True PromptDeck/PromptCard database split.
