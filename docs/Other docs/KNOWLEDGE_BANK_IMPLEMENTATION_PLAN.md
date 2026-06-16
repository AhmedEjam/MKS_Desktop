> Finalization status correction (2026-05-25): current active app database is Room v17. This plan still describes the v15→v16 knowledge-bank foundation, while v16→v17 category/asset indexing has since landed.

# MKS Knowledge Bank Full Implementation Plan

## Vision

Transform each `BookEntity` from a quiz container into a complete knowledge bank. A book should become the single home for every learning surface related to a topic:

- quizzes and question sessions
- flashcard decks and spaced-review sessions
- slideshow courses made of ordered slides
- note / blueprint files for compressed bullet-point reviews
- prompt decks and prompt cards for AI-agent interactions about the book
- study-progress records that track how app-level study workflows interact with book assets

The current implementation in this branch adds the first durable foundation for that model: Room entities, DAOs, a v15 -> v16 migration, repository APIs, bundle DTO placeholders, and sample seed objects.


## Clarified boundary: content vs management workflows

The book should own learning content only: quizzes, flashcard decks, slideshow courses, note blueprints, and prompt decks/cards. Study, display, edit, import, and export are app-level workflows used to manage those content types; they are not independent book content categories. The `knowledge_study_sessions` table is therefore a progress/state table linked to content, not a new content surface inside the book.

## Implemented foundation in this pass

### Database schema

Room database upgraded from v15 to v16 with these new tables:

1. `slideshow_courses`
   - A book-owned guided course.
   - Tracks objectives, slide count, studied slide count, progress, pin/system flags, and study timestamps.

2. `course_slides`
   - Ordered slides inside a slideshow course.
   - Supports markdown body, speaker notes, images, estimated duration, tags, completion state.

3. `note_blueprints`
   - Book-owned compressed review notes.
   - Supports summary, markdown body, bullet points, source file metadata, tags, review count.

4. `prompt_decks`
   - Book-owned group of AI interaction prompts.
   - Supports agent role, system context, preferred output format, card count, usage count.

5. `prompt_cards`
   - Ordered prompts inside a prompt deck.
   - Supports variables, examples, expected output, per-card agent role, usage count.

6. `knowledge_study_sessions`
   - Generic study state for non-quiz assets.
   - Can track progress for slideshow courses, individual slides, notes, prompt decks/cards, or future content types.

### Data access layer

Added DAOs:

- `SlideshowCourseDao`
- `CourseSlideDao`
- `NoteBlueprintDao`
- `PromptDeckDao`
- `PromptCardDao`
- `KnowledgeStudySessionDao`

### Repository layer

Added repository methods to create, edit, delete, display/query, and mark study usage for:

- slideshow courses
- course slides
- note blueprints
- prompt decks
- prompt cards
- generic knowledge study sessions

The repository now refreshes course and prompt stats and touches parent book timestamps when knowledge assets are edited or studied.

### Import/export contract placeholders

`LibraryBundleDto` schema was advanced to v6 with DTO placeholders for:

- `flashcardDecks`
- `slideshowCourses`
- `noteBlueprints`
- `promptDecks`
- `knowledgeStudySessions`

This does not yet fully import/export the new assets, but it establishes the public bundle contract for the next phase.

### Sample seed content

The sample book now seeds one basic slideshow course, one review blueprint, and one AI tutor prompt deck to exercise the new repository paths.

## Target information architecture

```text
Book content
  ├─ Quizzes
  │   └─ Questions
  ├─ Flashcard decks
  │   └─ Flashcards
  ├─ Slideshow courses
  │   └─ Course slides
  ├─ Note blueprints
  │   ├─ Bullet review notes
  │   ├─ Markdown summaries
  │   └─ Source file references / attachments
  └─ Prompt decks
      ├─ Prompt cards
      ├─ Prompt variables
      └─ AI-agent usage metadata

App-level management workflows
  ├─ Create / edit / delete
  ├─ Display / study
  ├─ Import / export
  └─ Study-progress records linked back to book assets
```

## Phase 1: schema and repository foundation

Status: partially complete in this pass.

Remaining work:

- Add migration tests for v15 -> v16.
- Add DAO tests for cascade delete and stat refresh.
- Add repository tests for course progress, prompt usage, and note review timestamps.
- Decide whether `BookEntity` should cache total knowledge-asset counts or whether UI should combine flows dynamically.
- Consider turning `exportSchema = true` so future Room migrations are safer.

## Phase 2: library UI integration

Goal: let a book display all knowledge surfaces from one place.

Recommended screens/components:

1. `BookKnowledgeScreen`
   - Replaces or augments the current book drill-down view.
   - Tabs or segmented sections:
     - Quizzes
     - Flashcards
     - Courses
     - Notes
     - AI Prompts

2. Cards/lists:
   - `SlideshowCourseCard`
   - `NoteBlueprintCard`
   - `PromptDeckCard`
   - `PromptCardRow`

3. Creation actions from the existing FAB:
   - New quiz
   - New flashcard deck
   - New slideshow course
   - New note blueprint
   - New prompt deck
   - Import asset bundle

4. Edit dialogs:
   - Metadata editor for course/deck/note/prompt deck.
   - Inline editor for slides and prompt cards.
   - Markdown-capable text editor for notes and slide bodies.

Implementation advice:

- Create dedicated ViewModels instead of expanding `LibraryViewModel` indefinitely.
- Keep the repository APIs as the boundary; UI should not access DAOs directly.
- Start with simple Material3 lists and dialogs, then iterate to richer editors.

## Phase 3: slideshow course display and study mode

Goal: make slideshow courses useful as guided lessons.

Features:

- Course overview with objectives and progress.
- Ordered slide player with previous/next navigation.
- Slide completion toggle.
- Markdown rendering for slide body.
- Optional image display and zoom.
- Speaker notes toggle.
- Course progress stored through slide completion and/or `KnowledgeStudySessionEntity`.

Future extension:

- Convert slides into flashcards or quiz questions.
- Export course as HTML/PDF/PPTX.
- Generate slides from notes or imported outlines.

## Phase 4: note/blueprint system

Goal: make brief bullet-point reviews a first-class book asset.

Features:

- Note list per book.
- Markdown editor.
- Bullet-point editor.
- Source file attachment metadata.
- Review mode with compact bullet-only display.
- `Mark reviewed` action to update `reviewCount` and `lastReviewedAt`.

Import targets:

- Markdown `.md`
- plain text `.txt`
- copied/pasted outlines
- simple JSON bundle notes
- future PDF/doc import through text extraction

Future extension:

- Turn note bullets into flashcards.
- Generate prompt cards from notes.
- Generate slideshow drafts from note headings.

## Phase 5: AI prompt cards

Goal: let each book carry reusable prompts for AI-agent interaction.

Features:

- Prompt deck list per book.
- Prompt card editor with variables.
- Render variables as fillable fields before copying/running.
- Usage count and last used tracking.
- Optional output format and agent role inherited from deck, overridable per card.

Prompt card examples:

- Explain this topic simply.
- Quiz me Socratically.
- Identify my gaps from wrong answers.
- Convert this section into flashcards.
- Compare two concepts.
- Create a revision schedule.

AI-agent integration plan:

- Phase 5A: Copy prompt to clipboard.
- Phase 5B: Android share sheet to ChatGPT or other target apps.
- Phase 5C: In-app AI provider abstraction if/when allowed.
- Phase 5D: Store prompt run history and outputs as notes.

## Phase 6: import/export

Goal: make the new knowledge-bank objects portable.

Bundle schema v6 should eventually support:

```json
{
  "schema": 6,
  "books": [],
  "quizzes": [],
  "flashcardDecks": [],
  "slideshowCourses": [],
  "noteBlueprints": [],
  "promptDecks": [],
  "knowledgeStudySessions": []
}
```

Required work:

- Extend `LibraryMapper` to map entities <-> DTOs.
- Extend `ExportManager` to collect new assets per book and all-library export.
- Extend `ImportLibraryManager` to upsert new assets.
- Extend manifest asset handling for course slide images and note attachments.
- Add conflict handling for asset external IDs.
- Add golden bundle tests for v5 and v6 compatibility.

Import sources:

- Existing MKS ZIP bundle v6.
- Markdown notes.
- CSV/TSV prompt decks.
- JSON prompt/course/note bundles.
- PPTX/HTML course import as a later phase.

## Phase 7: unified study dashboard

Goal: surface what to study next across all book assets.

Features:

- Book dashboard with progress summary:
  - quiz completion
  - flashcard mastery
  - course progress
  - notes reviewed
  - prompt card usage
- Continue last session.
- Suggested next action:
  - weak quiz questions
  - due flashcards
  - unfinished course slides
  - unreviewed blueprint
  - AI prompt for weak topic

Implementation options:

- Start with flow combination in ViewModels.
- Later add cached book-level counters for performance if needed.

## Phase 8: quality and migration hardening

Required tests:

- Room migration v15 -> v16.
- Cascade deletes from book -> courses/notes/prompts/sessions.
- Course stat refresh when slides are added, completed, deleted.
- Prompt deck stat refresh when prompt cards are added, used, deleted.
- DTO serialization/deserialization for bundle schema v6.
- Backward compatibility importing schema v5 bundles.

Safety checks:

- Enforce image/file path boundaries for new course slide images and note source files.
- Validate imported prompt variables.
- Sanitize or safely render markdown.
- Avoid executing prompt text as code or commands.

## Suggested implementation order from here

1. Compile and run migrations locally.
2. Add migration/DAO tests.
3. Create a simple `BookKnowledgeScreen` with read-only tabs.
4. Add create/edit dialogs for notes and prompt cards first, because they are text-only.
5. Add slideshow course player.
6. Extend export for new assets.
7. Extend import for v6 bundles.
8. Add prompt sharing/copy actions.
9. Add dashboard progress recommendations.

## Local verification commands

```bash
./gradlew clean :app:assembleDebug :app:testDebugUnitTest
```

If migration tests are added later:

```bash
./gradlew :app:connectedDebugAndroidTest
```
