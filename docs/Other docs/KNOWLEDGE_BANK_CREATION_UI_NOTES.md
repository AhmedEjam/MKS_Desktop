# Knowledge Bank Creation/Display UI Pass

## Goal

Add a first integrated interface for creating and viewing every book-owned knowledge-bank component so the new entities can be tested inside the app rather than only through repository calls.

## Added UI surfaces

### Library / selected book

When a book is open, the floating action button now exposes creation actions for:

- Flashcard deck
- Slideshow course
- Review blueprint / note
- AI prompt deck

The selected-book library view now displays separate sections for:

- Quizzes
- Flashcard decks
- Slideshow courses
- Review blueprints
- AI prompt decks

Each section uses the existing library card style so the first integration remains consistent with the current app shell.

### Flashcard deck detail

`FlashcardDeckScreen` now supports:

- Displaying all cards in the deck
- Creating flashcards
- Editing flashcards
- Deleting flashcards
- Study preview with flip / wrong / correct actions

### Slideshow course detail

New `SlideshowCourseScreen` supports:

- Displaying course metadata and progress
- Displaying slides
- Creating slides
- Editing slides
- Deleting slides
- Marking slides complete/incomplete

### Review blueprint detail

New `NoteBlueprintScreen` supports:

- Displaying summary, brief bullet points, and full markdown body
- Editing the blueprint
- Marking the blueprint reviewed

### AI prompt deck detail

New `PromptDeckScreen` supports:

- Displaying prompt deck metadata
- Displaying prompt cards
- Creating prompt cards
- Editing prompt cards
- Deleting prompt cards
- Marking a prompt as used

## Navigation routes

Added routes:

- `courses/{courseId}`
- `notes/{noteId}`
- `prompts/{deckId}`

Existing route kept and expanded:

- `flashcards/{deckId}`

## Implementation scope

This is intentionally a primary/basic implementation. It focuses on integrated create/display/edit/test surfaces rather than final product polish. Later passes should add localization, richer markdown rendering, asset attachment, search filters per knowledge type, import/export mapping, and UI tests.

## Verification

Gradle compilation could not be completed in the sandbox because the wrapper attempts to download `gradle-9.4.1-bin.zip` from `services.gradle.org`, and network access is unavailable. Run locally:

```bash
./gradlew clean :app:assembleDebug :app:testDebugUnitTest
```
