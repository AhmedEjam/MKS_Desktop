# MKS Android App — User Journey & UI Map

> **Last updated:** 2026-06-08. Room v26, 25 migration steps, 22+ navigation routes.
>
> **What this is:** A plain-language, screen-by-screen map of every page, button, gesture, dialog, and interactable element in the MKS Android app. For each item, it says what happens when you interact with it — where it takes you, what it loads, and what it outputs.
>
> **How to use it:** Walk through each screen heading. Under each one you'll find every button and control listed with a short "what it does" description. This is meant to be updated whenever the app changes, and can be used side-by-side with an iOS project to check feature parity.

---

## Table of Contents

1. [App Launch & Onboarding](#1-app-launch--onboarding)
2. [Library Screen (Main Hub)](#2-library-screen-main-hub)
3. [Quiz Player Screen](#3-quiz-player-screen)
4. [Summary Screen (Post-Quiz)](#4-summary-screen-post-quiz)
5. [Session Management Screen](#5-session-management-screen)
6. [Quiz Questions Screen (Question Browser)](#6-quiz-questions-screen-question-browser)
7. [Category Questions Screen](#7-category-questions-screen)
8. [Adaptive Training](#8-adaptive-training)
9. [Book Knowledge Dashboard](#9-book-knowledge-dashboard)
10. [Flashcard Deck Screen](#10-flashcard-deck-screen)
11. [Slideshow Course Screens](#11-slideshow-course-screens)
12. [Review Blueprint Screens](#12-review-blueprint-screens)
13. [AI Prompt Deck Screens](#13-ai-prompt-deck-screens)
14. [Source Documents Screen](#14-source-documents-screen)
15. [Book Notes Screen](#15-book-notes-screen)
16. [Scanner Screen](#16-scanner-screen)
17. [Global Search Screen](#17-global-search-screen)
18. [Review Dashboard Screen](#18-review-dashboard-screen)
19. [Data Tools Screen](#19-data-tools-screen)
20. [Settings Screen](#20-settings-screen)
21. [Dialogs & Overlays Reference](#21-dialogs--overlays-reference)
22. [Architecture Notes for iOS Parity](#22-architecture-notes-for-ios-parity)

---

## 1. App Launch & Onboarding

### Welcome Screen

**Route:** `welcome`
**When shown:** Only on first launch (or if user re-enables it in Settings). Controlled by a DataStore flag.

| # | Element | Type | What It Does |
|---|---------|------|-------------|
| 1 | **Language Pill (EN / العربية)** | Toggle pill | Instantly switches the app language. Saves to DataStore. The entire UI re-renders in the chosen language. |
| 2 | **"Explore Features" button** | Text button | Opens an AlertDialog that lists the app's main features (quizzes, flashcards, import, etc.). Informational only. |
| 3 | **"Get Started" button** | Filled button | Sets the `showWelcomeOnStartup` flag to `false` so this screen never shows again. Navigates to the **Library Screen** and removes the welcome screen from the back stack. |

**After this screen:** The app always starts at the Library Screen on subsequent launches.

---

## 2. Library Screen (Main Hub)

**Route:** `library`
**What it is:** The central dashboard. Shows all Books, and when you tap into a book, shows its Quizzes and knowledge-bank assets. Also supports browsing by Category.

### Top App Bar

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **← Back arrow** | Appears only when you're inside a Book view or in search mode. Tapping it goes back to the root library (all books), or exits search. If you're already at root level, there's no arrow — just empty space. |
| 2 | **⋮ Overflow menu** | Opens a dropdown with: |
|   | → **Sort** | Opens the `SortDialog` — lets you sort items by Name (A-Z/Z-A), Date Created, Date Edited, or Progress. |
|   | → **Grid / List view toggle** | Switches between a 2-column grid layout and a single-column list layout. Persisted in DataStore. |
|   | → **Search** | Activates the inline search bar in the top bar. Type to filter books/quizzes/categories by name. Has a clear button (✕). |
|   | → **Settings** | Navigates to the **Settings Screen**. |

**Title behavior:** Shows "MKS Library" at root level, the Book title when inside a book, or "Category: [name]" when browsing a category.

### Content Area (Grid or List)

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Book Card** | **Tap:** Opens the book — filters the library to show only this book's quizzes and knowledge-bank items. Title bar updates to the book name. |
|   |  | **Long press:** Opens the `OptionsSheet` bottom sheet (see [Dialogs](#21-dialogs--overlays-reference)). |
| 2 | **Quiz Card** (inside a book) | **Tap:** Navigates to the **Session Management Screen** for this quiz, where you pick or create a session to play. |
|   |  | **Long press:** Opens the `OptionsSheet` with quiz-specific options. |
| 3 | **Category Chip/Card** | **Tap:** Navigates to the **Category Questions Screen** showing all questions tagged with this category across all quizzes in the book. |
| 4 | **Knowledge-bank items** (Flashcard decks, Slideshow courses, etc.) | **Tap:** Navigates to the appropriate screen (Flashcard Deck, Slideshow Course, etc.). |

### Floating Action Button (+) — The FAB Menu

Tapping the **+** button expands an animated menu of actions. The options change depending on context.

**Always visible:**

| # | Option | What It Does |
|---|--------|-------------|
| 1 | **Import** | Opens the system file picker. Accepts XLSX, CSV, TSV, JSON, HTML, TXT, ZIP. Detected format is auto-detected. Leads to the Compiler flow (header mapping, preview, save). |
| 2 | **Export** | Opens the system file saver. Creates a ZIP bundle of the currently selected book or entire library. |

**When inside a Book:**

| # | Option | What It Does |
|---|--------|-------------|
| 3 | **New Quiz** | Opens the `QuizSelectionDialog` to create a new quiz inside this book. |
| 4 | **Adaptive Training** | Starts an adaptive quiz session scoped to this book. Takes you to the **Quiz Player** with AI-selected weak/unanswered questions. |
| 5 | **Flashcard Deck** | Opens a dialog to create a new flashcard deck for this book. |
| 6 | **Slideshow Course** | Navigates to the **Slideshow Course List Screen** for this book. |
| 7 | **Review Blueprint** | Navigates to the **Review Blueprint List Screen** for this book. |
| 8 | **Sources** | Navigates to the **Source Documents Screen** for this book. |
| 9 | **Book Notes** | Navigates to the **Book Notes Screen** — shows all question-level notes across the book. |
| 10 | **AI Prompt Deck** | Opens a dialog to create a new AI prompt deck for this book. |

**When at root level (no book selected):**

| # | Option | What It Does |
|---|--------|-------------|
| 3 | **New Book** | Opens the `EditEntityDialog` to create a new book (title, description, optional cover image). |
| 4 | **Adaptive Training** | Starts adaptive training across ALL books. |

### OptionsSheet (Long-Press Bottom Sheet)

When you long-press a Book, Quiz, or Category, this bottom sheet appears:

| # | Option | What It Does |
|---|--------|-------------|
| 1 | **Pin / Unpin** | Toggles the `isPinned` flag. Pinned items float to the top of lists. |
| 2 | **Edit** | Opens the `EditEntityDialog` to rename, change description, or update cover image. |
| 3 | **Book Dashboard** | (Books only) Navigates to the **Book Knowledge Dashboard**. |
| 4 | **Export** | Exports this specific book/quiz as a ZIP file. |
| 5 | **Import Into** | Opens file picker to import questions directly into this quiz/book. |
| 6 | **Scanner** | (Quizzes only) Navigates to the **Scanner Screen** to photograph and OCR questions into this quiz. |
| 7 | **Delete** | Shows a confirmation dialog. Deleting a book deletes all its quizzes, flashcards, slideshows, and notes. Deleting a quiz deletes its questions and sessions. |

---

## 3. Quiz Player Screen

**Route:** `quiz/{quizId}?sessionId={sessionId}`
**What it is:** The core study interface. Shows one question at a time with multiple-choice options, images, explanations, and navigation controls.

### Top App Bar

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **← Back arrow** | Exits the quiz. Saves session progress automatically. Returns to the previous screen. |
| 2 | **Timer display** | Shows elapsed time in MM:SS format. If a quiz timer or per-question timer is set, it counts down. |
| 3 | **Progress indicator** | Shows "3/25" (current question / total). |
| 4 | **Score display** | Shows "★ 5/25" — score out of total initial question count. |
| 5 | **Streak counter** | Shows "🔥 3" when you have a streak of consecutive correct answers. Hidden when streak is 0. |
| 6 | **Progress bar** | A thin colored bar below the top bar showing your position (e.g., 40% through the quiz). |

### Question Content Area

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Question text** | Displays the question stem. Supports multi-line text. |
| 2 | **Question image** | If the question has an image, it's shown below the text. **Tap:** Opens the `ZoomableImageDialog` — fullscreen, pinch-to-zoom view. |
| 3 | **Assets button** (paperclip icon) | Only appears if the question has attached assets. **Tap:** Opens a read-only dialog listing the question's attachments (files, links, notes). |
| 4 | **Horizontal swipe** | **Swipe left:** Move to the next question. **Swipe right:** Move to the previous question. Animated slide transition. |
| 5 | **Explanation card** | After answering, shows the explanation text (if any) in an expandable card. Also shows Hint, High-Yield Info, and Reference if available. |

### Option Items

| # | Interaction | What It Does |
|---|-------------|-------------|
| 1 | **Tap an option** | Selects it. Single-choice questions show a radio-style indicator. Multiple-choice shows checkboxes. Colors change to indicate selection. |
| 2 | **Double-tap an option** | If "double-tap to submit" is enabled in settings, this selects AND submits in one action. |
| 3 | **Long-press an option** | "Drops" (eliminates) that option — strikes it through visually. Useful for process-of-elimination study. The option can't be selected after dropping. |
| 4 | **Elimination mode ✕ button** | When elimination mode is ON, each un-answered option shows a small ✕ button. Tapping it drops that option. |

**After submitting:** Options turn green (correct) or red (wrong). Correct answer(s) always highlighted green. The Submit button changes to "Next".

### Bottom Sheet (Drag Up to Expand)

The bottom of the screen has a drag handle. Pull it up to reveal the full control panel.

**Peek area (always visible):**

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Accuracy %** | Shows live accuracy (e.g., "Accuracy: 80%"). Color-coded: green ≥80%, orange ≥50%, red <50%. |
| 2 | **Streak count** | Shows current streak in the peek bar. |
| 3 | **Submit / Next / Reveal button** | The main action button. Text changes based on state: "Submit" (lock in answer), "Reveal" (show next option in one-by-one mode), "Next" (after answering). |

**Expanded area (scroll down for more):**

| # | Toggle Control | What It Does |
|---|----------------|-------------|
| 1 | **Categories** | ON/OFF. When ON, shows category tags on questions. |
| 2 | **One-by-One** | ON/OFF. Reveals answer options one at a time instead of all at once. |
| 3 | **Rapid Mode** | ON/OFF. Auto-advances to the next question after a short delay when you answer correctly. |
| 4 | **Eliminate** | ON/OFF. Shows ✕ buttons on options to "drop" them from consideration. |
| 5 | **Drop Question** | One-shot button. Opens a confirmation — permanently skips this question (it won't appear in future sessions). |
| 6 | **Focus Mode** | ON/OFF. Activates Do Not Disturb on the device (requires notification policy permission). |
| 7 | **Mark / Bookmark** | ON/OFF. Toggles the bookmark flag on the current question. Marked questions can be filtered/reviewed later. |
| 8 | **Finish Quiz** | One-shot button. Ends the session immediately and navigates to the **Summary Screen**. |

**Navigation Controls (below toggles):**

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Filter chips** | Row of filter chips: ALL, ANSWERED, UNANSWERED, MISSED, MARKED, DROPPED. Filters the question-number grid below. |
| 2 | **Question number grid** | A scrollable row of numbered squares. Each is color-coded: blue (current), green (correct), red (incorrect), gray (unanswered). Bookmark icon on marked questions. **Tap a number:** Jumps directly to that question. |
| 3 | **"Restore dropped question" chip** | Appears only when viewing a dropped question. **Tap:** Un-drops the question so it's included again. |

---

## 4. Summary Screen (Post-Quiz)

**Route:** `summary/{sessionId}`
**What it is:** Shows your results after completing or finishing a quiz session.

### Score Header Card

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Percentage display** | Large "85%" showing your score. |
| 2 | **Score fraction** | "17/20 correct" below the percentage. |
| 3 | **Best streak** | "🔥 Best streak: 8" — the longest consecutive correct streak in this session. |
| 4 | **Session label** | If you named your session, it shows here. |
| 5 | **Average time** | "Avg Time: 12.3s / question" — calculated from per-question timestamps. |

### Top Bar Actions

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Share button** | Opens the system share sheet with a text export of your session results (score, per-question details). |
| 2 | **✕ Close button** | Returns to the Library Screen. |

### Question Review Section

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Settings gear icon** | Toggles the "Visible Details" panel. |
| 2 | **Visible Details panel** | Filter chips to show/hide: Stem, Options, Hint, High Yield, Reference, Question Number, Explanation. Each chip toggles that field on all review cards below. |
| 3 | **Review filter chips** | Horizontal scrollable row: ALL, CORRECT, WRONG, UNANSWERED, DROPPED, WITH EXPLANATION. Filters the question list below. |
| 4 | **Question review cards** | Each card shows the question with color-coded options (green = correct, red = your wrong pick). Shows/hides details based on the visibility toggles above. |

### Category Performance Section

Shows a breakdown of your accuracy per category (e.g., "Anatomy: 90%", "Pharmacology: 60%").

### Bottom Action Buttons

| # | Button | What It Does |
|---|--------|-------------|
| 1 | **Retry** | Starts a brand new session for the same quiz. Navigates back to the Quiz Player. |
| 2 | **Library** | Returns to the Library Screen (home). |

---

## 5. Session Management Screen

**Route:** `sessions/{quizId}`
**What it is:** Shows all saved sessions for a specific quiz. Lets you resume, create, or delete sessions.

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **← Back button** | Returns to the previous screen. |
| 2 | **+ FAB button** | Opens the **Start Session Dialog** to create a new session. |
| 3 | **Session card** | Shows: session label, accuracy ring, score metrics (correct/incorrect/remaining), progress bar, last-active date. |
|   | → **Tap a session** | If incomplete: resumes it in the **Quiz Player**. If completed: opens the **Summary Screen**. |
|   | → **Delete button** | Shows a confirmation dialog. Permanently deletes this session record. |
| 4 | **Empty state card** | Shown when no sessions exist. Tap it to create a new session. |

### Start Session Dialog

The "new session" configuration dialog with many options:

| # | Field/Toggle | What It Does |
|---|--------------|-------------|
| 1 | **Session label** | Optional name for this session (e.g., "Morning review"). Auto-generated if left blank. |
| 2 | **Include filters** | Chip selectors: Unanswered, Missed, Marked, Categorized, Uncategorized. Only include questions matching these filters. |
| 3 | **Question range** | "From" and "To" number fields. Restricts the session to a specific range (e.g., questions 1-50). |
| 4 | **Shuffle Questions** | ON/OFF toggle. Randomizes question order. |
| 5 | **Shuffle Options** | ON/OFF toggle. Randomizes the order of answer options. |
| 6 | **Rapid Mode** | ON/OFF toggle. Auto-advance on correct answers. |
| 7 | **Repeat Wrong** | ON/OFF toggle. Re-queues incorrectly answered questions. |
| 8 | **Quiz Timer** | Checkbox + minutes field. Sets a total time limit for the session. |
| 9 | **Question Timer** | Checkbox + seconds field. Sets a per-question countdown. |
| 10 | **Remember Settings** | ON/OFF toggle. If ON, saves these choices as your new defaults. |
| 11 | **Start button** | Creates the session and immediately opens the **Quiz Player**. |

---

## 6. Quiz Questions Screen (Question Browser)

**Route:** `quiz_questions/{quizId}?questionId={questionId}`
**What it is:** A full list/browser of all questions in a quiz. Not for taking quizzes — for reviewing, editing, organizing, and bulk-managing questions.

### Top Bar

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **← Back** | Returns to previous screen. |
| 2 | **Search icon** | Toggles the search bar. Type to filter questions by text content. |
| 3 | **Filter icon** | Toggles the filter controls panel. |
| 4 | **Title** | Shows quiz name and "X / Y Questions" count. |

### Filter Controls (toggled panel)

A row of filter chips: **Stem, Options, Correct Answer, Explanation, Hint, Reference, Info, Marked Only, Has Attachments**. Each toggles that field's visibility on question cards.

### Question Cards

Each card shows a question with togglable sections.

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Long press** | Enters selection mode — adds this question to the multi-select set. |
| 2 | **Tap** (normal mode) | Opens the **Edit Question Dialog**. |
| 3 | **Tap** (selection mode) | Toggles selection on/off for this question. |
| 4 | **Bookmark icon** | Toggles the `isMarked` flag on this question. |
| 5 | **Edit (pencil) icon** | Opens the **Edit Question Dialog** (same as tap in normal mode). |
| 6 | **Attachments (paperclip) icon** | Opens the **Question Assets Dialog** — full asset management (add/edit/delete attachments, annotations, create blueprints from question, link source documents). |
| 7 | **Image** (if present) | **Tap:** Opens fullscreen zoomable image viewer. |

### Selection Mode Top Bar

When one or more questions are selected, the top bar changes to show:

| # | Action | What It Does |
|---|--------|-------------|
| 1 | **✕ Clear selection** | Exits selection mode. |
| 2 | **Select All** | Toggles all questions selected/deselected. |
| 3 | **Bookmark selected** | Marks all selected questions. |
| 4 | **Unbookmark selected** | Unmarks all selected questions. |
| 5 | **Move** | Opens a quiz picker dialog → moves selected questions to a different quiz. |
| 6 | **Copy** | Opens a quiz picker dialog → copies selected questions to another quiz. |
| 7 | **Export** | Opens a dialog to create a new quiz from the selected questions (enter a title). |
| 8 | **Create Flashcards** | Opens a dialog to create a new flashcard deck from selected questions. Options: deck title, clear marks after conversion. |
| 9 | **Delete** | Confirmation dialog → permanently deletes selected questions. |

### + FAB Button

Opens the **Edit Question Dialog** with blank fields to manually create a new question (stem, options, correct answers, explanation, hint, reference, info, weight).

### Quiz Summary Card

Shows a knowledge summary at the top: total questions, marked, missed, notes, assets, sources.

---

## 7. Category Questions Screen

**Route:** `category/{categoryName}`
**What it is:** Shows all questions tagged with a specific category, across all quizzes in the workspace. Very similar to the Quiz Questions Screen, but scoped to a category.

Same elements as Quiz Questions Screen (#6) with these additions:

| # | Extra Element | What It Does |
|---|---------------|-------------|
| 1 | **"Start Quiz" FAB** | Starts an adaptive quiz using only questions in this category. Navigates to the **Adaptive Training** (Quiz Player with category-scoped questions). |

---

## 8. Adaptive Training

**Route:** `adaptive/{type}/{id}`
**What it is:** Not a separate screen — it's the Quiz Player Screen, but fed with intelligently selected questions. The `type` can be:

| Type | What It Selects |
|------|----------------|
| `BOOK` | Weak, unanswered, and recently-missed questions from a specific book. |
| `CATEGORY` | Questions matching a specific category tag. |
| `QUIZ` | Focused review of a single quiz's weak spots. |
| `ALL` | Adaptive selection across all books and quizzes. |

The FocusManager algorithm prioritizes: unanswered > recently wrong > low-weight > marked. After finishing, navigates to the **Summary Screen**.

---

## 9. Book Knowledge Dashboard

**Route:** `book_dashboard/{bookId}`
**What it is:** An overview of a book's complete learning ecosystem.

### Study Progress Card

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Completion bar** | Progress bar showing % of questions answered. |
| 2 | **Due count** | Number of review-schedule items overdue. |
| 3 | **Weak count** | Number of "weak" questions (low accuracy). |
| 4 | **Marked count** | Number of bookmarked questions. |
| 5 | **Mistakes count** | Number of open (unresolved) mistake log entries. |

### Magic Actions

| # | Chip | What It Does |
|---|------|-------------|
| 1 | **"Draft Note from Marked"** | (Only visible if marked questions > 0) Creates a new Note Blueprint by compiling all marked questions' stems and explanations. |
| 2 | **"Note from Mistakes"** | (Only visible if open mistakes > 0) Creates a Note Blueprint from all mistake-log entries. |

### Learning Tools Grid

Six tappable cards, each navigating to a different sub-screen:

| # | Card | Tap → Navigates To |
|---|------|---------------------|
| 1 | **Quizzes** (count) | Back to Library, filtered to this book's quizzes. |
| 2 | **Flashcards** (count) | **Flashcard Deck List** for this book (shows all decks, lets you create new ones). |
| 3 | **Slideshows** (count) | **Slideshow Course List Screen**. |
| 4 | **Notes** (count) | **Review Blueprint List Screen**. |
| 5 | **AI Prompts** (count) | **AI Prompt Deck List Screen**. |
| 6 | **Sources** (count) | **Source Documents Screen**. |

---

## 10. Flashcard Deck Screen

**Route:** `flashcards/{deckId}?cardId={cardId}`
**What it is:** Two modes — **List Mode** (manage cards) and **Study Mode** (review cards).

### Top Bar

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **← Back** | Returns to previous screen. |
| 2 | **Toggle mode icon** | Switches between List mode (📋) and Study mode (▶). |
| 3 | **Edit deck icon** | Opens the **Deck Editor Dialog** (change deck title and description). |

### List Mode

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Stats card** | Shows deck title, description, card count, studied count, mastery %. Has a progress bar. |
| 2 | **"Add card" button** | Opens the **Flashcard Editor Dialog** to create a new card (front, back, hint, tags). |
| 3 | **"Study" button** | Switches to Study Mode. |
| 4 | **"Generate from questions" card** | Two buttons: |
|   | → **Marked** | Opens a dialog → generates flashcards from all marked questions in the parent book. Option to clear marks after conversion. |
|   | → **Missed** | Generates flashcards from all incorrectly-answered questions. |
| 5 | **Flashcard list item** | Shows front text, back text preview, attempt/correct counts, source question link. |
|   | → **↑ Move up** | Reorders the card up. |
|   | → **↓ Move down** | Reorders the card down. |
|   | → **Edit** | Opens the **Flashcard Editor Dialog** with existing data. |
|   | → **Delete** | Deletes this flashcard. |

### Study Mode

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Card counter** | Shows "3 / 15" (current position / total cards). |
| 2 | **Card surface** | Shows the front text. **Tap:** Flips to show the back text. |
| 3 | **"Flip" button** | Same as tapping the card — flips it. |
| 4 | **Rating buttons** (after flipping) | Three buttons: |
|   | → **Again** | Records a failed review. Card will come back sooner. |
|   | → **Good** | Records a successful review. Normal interval. |
|   | → **Easy** | Records an easy review. Longer interval before next review. |
| 5 | **Previous / Next** | Navigates between cards without rating. |

---

## 11. Slideshow Course Screens

### Slideshow Course List Screen

**Route:** `book_slideshows/{bookId}`

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **+ FAB** | Opens a dialog to create a new slideshow course (title + description). |
| 2 | **Course item** | Shows title, description, slide count. |
|   | → **Tap** | Navigates to the **Slideshow Course Screen** for this course. |
|   | → **Edit icon** | Opens edit dialog (change title/description). |
|   | → **Delete icon** | Confirmation dialog → deletes course and all its slides. |

### Slideshow Course Screen (Individual Course)

**Route:** `slideshow/{courseId}?slideId={slideId}`

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **+ FAB** | Opens a dialog to create a new slide (title + body text). |
| 2 | **Slide item** | Shows slide title and a body preview (first 160 chars). |
|   | → **Edit icon** | Opens edit dialog for this slide. |
|   | → **Delete icon** | Confirmation dialog → deletes this slide. |

---

## 12. Review Blueprint Screens

### Review Blueprint List Screen

**Route:** `book_blueprints/{bookId}`

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **+ FAB** | Opens the **Blueprint Create Dialog** with options: |
|   | → **Manual create** | Enter title, body (markdown supported), and mode (Concept Template, Disease, Drug, etc.). |
|   | → **"From Marked" button** | Auto-generates a blueprint from all marked questions. |
|   | → **"From Missed" button** | Auto-generates a blueprint from all missed questions. |
| 2 | **Note/Blueprint item** | Shows title, mode, review status, body preview. |
|   | → **Tap** | Navigates to the **Review Blueprint Screen**. |
|   | → **Edit icon** | Opens edit dialog. |
|   | → **Delete icon** | Confirmation → deletes. |

### Review Blueprint Screen (Individual)

**Route:** `blueprint/{noteId}`

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Blueprint body editor** | A multi-line text field showing the full blueprint body. Editable. |
| 2 | **"Save" button** | Saves changes to the blueprint body. |
| 3 | **"Mark reviewed" button** | Increments the review counter and updates the review status. |
| 4 | **"To flashcards" button** | Creates flashcards from this blueprint's content. |
| 5 | **"Append to question note" button** | Appends the blueprint content to a related question's notes field. |
| 6 | **Status info** | Shows blueprint mode (e.g., CONCEPT_TEMPLATE), review status, and review count. |

---

## 13. AI Prompt Deck Screens

### AI Prompt Deck List Screen

**Route:** `book_prompts/{bookId}`

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **+ FAB** | Opens a dialog to create a new prompt deck (title + description). |
| 2 | **Prompt deck item** | Shows title and description. |
|   | → **Tap** | Navigates to the **AI Prompt Deck Screen**. |
|   | → **Edit/Delete** | Same pattern as other list screens. |

### AI Prompt Deck Screen (Individual)

**Route:** `prompt_deck/{promptId}?cardId={cardId}&runId={runId}`

This is a complex screen with three sections:

#### Prompt Cards Section

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **+ FAB** | Opens the **Prompt Card Dialog** to create a new prompt card (title, prompt text with `{variable}` placeholders, output type). |
| 2 | **Prompt card** | Shows title, output type, usage count, prompt text preview. |
|   | → **"Use this card" button** | Selects this card for the "Run prompt" section below. |
|   | → **Edit icon** | Opens edit dialog. |
|   | → **Delete icon** | Deletes this prompt card. |

#### Run Prompt Section (appears after selecting a card)

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Variable input fields** | One text field per `{variable}` found in the prompt template. Fill these in to customize the prompt. |
| 2 | **Rendered prompt preview** | Shows the final prompt with variables replaced by your input. |
| 3 | **Copy icon** | Copies the rendered prompt to the clipboard. Also records a prompt run. |
| 4 | **AI output text field** | A large multi-line field where you can paste the AI's response (from ChatGPT, etc.). |
| 5 | **"Save run" button** | Records the prompt run with the variables used and the AI output. |
| 6 | **"To note" button** | Creates a new Question Note from the AI output. |
| 7 | **"To blueprint" button** | Creates a new Review Blueprint from the AI output. |
| 8 | **"To flashcards" button** | Creates a new Flashcard Deck from the AI output. |

#### Run History Section

Shows the last 10 prompt runs with their variables and outputs.

---

## 14. Source Documents Screen

**Route:** `book_sources/{bookId}?sourceId={sourceId}`

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **+ FAB** | Opens the **Source Document Dialog** to create a new source (title, type [Textbook/PDF/Lecture/Website/Guideline/Other], description/details). |
| 2 | **Source item** | Shows title, type, and description. |
|   | → **Edit icon** | Opens edit dialog. |
|   | → **Delete icon** | Confirmation dialog → deletes. Warns that citation assets may keep unresolved references. |

---

## 15. Book Notes Screen

**Route:** `book_notes/{bookId}`
**What it is:** Read-only view of all questions in this book that have notes attached.

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Question note card** | Shows the question stem (bold) and the notes content below it. Not editable from this screen — edit notes from the Quiz Questions Screen or Question Assets Dialog. |

---

## 16. Scanner Screen

**Route:** `scanner/{quizId}`
**What it is:** Camera-based question scanner. Photographs a page and uses OCR to extract questions.

### Camera View

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Camera viewfinder** | Full-screen camera preview. Point at a printed question paper. |
| 2 | **Capture button** (big circle) | Takes a photo. Image is sent to the OCR pipeline. Shows a loading spinner ("Processing image..."). |

### Review View (after OCR processing)

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Header** | "Review X recognized questions" showing how many questions were detected. |
| 2 | **Question card** | Shows detected question text, options (with correct answers highlighted green). |
|   | → **Edit icon** | Opens the **Edit Question Dialog** — lets you fix OCR errors before importing. |
|   | → **Delete icon** | Removes this detected question from the batch. |
| 3 | **"Discard" button** | Cancels the import, returns to camera view. |
| 4 | **"Import All" button** | Saves all reviewed questions into the quiz. Returns to previous screen. |

### Error View

Shown if OCR fails. Shows the error message and a "Try Again" button to return to camera view.

---

## 17. Global Search Screen

**Route:** `global_search`
**What it is:** Searches across the entire database — books, quizzes, questions, notes, flashcards, blueprints, prompts, mistakes, and assets.

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Search bar** | Type at least 2 characters to trigger search. Searches across all entity types. |
| 2 | **Result card** | Shows result type (BOOK, QUESTION, FLASHCARD, etc.), title, subtitle, and snippet. |
|   | → **Tap** | Navigates to the matching item's screen (e.g., tapping a FLASHCARD result opens that flashcard deck). |
| 3 | **Empty states** | "Type at least two characters…" or "No results found" messages. |

---

## 18. Review Dashboard Screen

**Route:** `review_dashboard?mistakeId={mistakeId}`
**What it is:** A unified review queue showing all items due for review across flashcards, blueprints, and mistake logs.

### Summary Cards

| # | Card | What It Shows |
|---|------|--------------|
| 1 | **Flashcards due** | Count of flashcard reviews scheduled for today. |
| 2 | **Blueprints due** | Count of blueprints due for manual review. |
| 3 | **Mistakes due** | Count of open mistake reviews that are overdue. |
| 4 | **Mistakes scheduled** | Count of open mistakes not yet due. |
| 5 | **Marked / weak** | Count of marked and weak questions across all books. |

### Review Queue

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Review item card** | Shows type (FLASHCARD / BLUEPRINT / MISTAKE), title, subtitle. |
|   | → **Tap** | Navigates to the relevant screen (flashcard deck, blueprint, etc.). |
|   | → **"Reviewed" button** | Marks this item as reviewed. Updates timestamps and counters. |
|   | → **"Snooze 1 week" button** | Pushes the review date one week into the future. |

---

## 19. Data Tools Screen

**Route:** `data_tools`
**What it is:** Advanced import/export with preview capabilities.

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **"Export full library" button** | Creates a ZIP file containing JSON dumps of all learning tables (books, quizzes, questions, sessions, knowledge-bank assets). Media files NOT included yet. |
| 2 | **"Save export file" button** | (Appears after export) Opens system file saver to save the ZIP to a location of your choice. |
| 3 | **"Choose ZIP and preview" button** | Opens file picker for ZIP files. Parses the ZIP and shows a preview dialog (what would be imported, conflicts, warnings) without actually importing. |
| 4 | **Local ZIP path field** | Text field for pasting a file path directly (for emulator/debug testing). |
| 5 | **"Preview pasted path" button** | Same as above but uses the manually entered path. |
| 6 | **Export warnings** | Shows any warnings from the export process. |

---

## 20. Settings Screen

**Route:** `settings`

### Library & Backup Group

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **"Export full library" button** | Creates and saves a timestamped ZIP backup of the entire library. Opens system file saver. |
| 2 | **"Advanced import/export preview" button** | Navigates to the **Data Tools Screen**. |
| 3 | **"Global search" button** | Navigates to the **Global Search Screen**. |
| 4 | **"Review dashboard" button** | Navigates to the **Review Dashboard Screen**. |

### Appearance Group

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **Theme chips** | Seven theme options: Dawn, Forest, Midnight, Lavender, Plain Light, Plain Dark, System. Tap to switch. Instant re-theme. |
| 2 | **Reset button** | Resets theme to Dawn, font scale to 1.0, and UI density to 1.0. |
| 3 | **Font Scale slider** | Range: 0.5× to 2.0×. Adjusts text size across the entire app. |
| 4 | **UI Density slider** | Range: 0.5× to 1.5×. Adjusts spacing and padding across the app. |
| 5 | **"Show cover images" toggle** | ON/OFF. Toggles whether book/quiz cover images are displayed in the Library. |
| 6 | **Language selector** | Two chips: English, العربية. Switches the app language (includes RTL layout for Arabic). |

### Global Configuration Group

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **"Show Welcome Screen" toggle** | ON/OFF. If ON, shows the welcome/onboarding screen on next launch. |
| 2 | **"Auto-hide knowledge summary" toggle** | ON/OFF. Controls whether the quiz knowledge summary card auto-collapses. |
| 3 | **"Focus Mode" toggle** | ON/OFF. Enables Do Not Disturb mode during quizzes. May prompt for notification-policy permission. |
| 4 | **"Grant permission" button** | (Appears if Focus Mode is on but permission not granted.) Opens system DND permission settings. |
| 5 | **"Skip Unanswered" toggle** | ON/OFF. When ON, the Submit button works even when no option is selected (treats it as a skip). |
| 6 | **"Double-tap to submit" toggle** | ON/OFF. When ON, double-tapping an option selects AND submits simultaneously. |
| 7 | **Rapid-mode advance delay slider** | Range: 500ms to 5000ms. Controls how long to wait before auto-advancing in Rapid Mode. |

### Danger Zone

| # | Element | What It Does |
|---|---------|-------------|
| 1 | **"Clear Categories" button** | Confirmation dialog → deletes ALL category tags and category metadata across the database. Questions are kept but their category links are removed. |
| 2 | **"Reset Database" button** | Confirmation dialog → **wipes the entire database** and re-seeds it with sample data. This is irreversible. All books, quizzes, questions, sessions, flashcards, notes, and prompts are deleted. |

---

## 21. Dialogs & Overlays Reference

### Edit Entity Dialog
Used for creating/editing Books and Quizzes. Fields: Title, Description, Cover Image (tap to pick from gallery). Save button creates/updates the entity.

### Edit Question Dialog
Full question editor. Fields: Question text (multi-line), Options (list with ✕ to remove, + to add), Correct answers (checkboxes), Explanation, Hint, Reference, Additional Info, Weight. Used in Scanner review, Quiz Questions Screen, and Category Questions Screen.

### Question Assets Dialog
Full asset management for a question. Sections:
- **Assets list** — add/edit/delete file attachments and links.
- **Annotations** — highlight text and add notes with color labels.
- **Source documents** — link existing sources or create new ones.
- **Linked blueprints** — shows any blueprints connected to this question.
- **"Create blueprint from question"** — generates a new review blueprint from this question.

### Import Review Dialog
Shown before importing a ZIP file. Shows: stats (books/quizzes/questions/categories/sessions to create/update), merge strategy options (Merge Only vs Merge & Update), HTTP image permission checkbox, errors, warnings, and book/quiz detail lists. Confirm button starts the import.

### Import Warnings Dialog
Shown after an import completes with warnings. Lists all warnings with line/row details.

### Zoomable Image Dialog
Fullscreen image viewer with pinch-to-zoom and pan gestures. Shown when tapping any question image.

### Sort Dialog
Lets you sort library items by: Name (A-Z), Name (Z-A), Date Created, Date Edited, Progress.

---

## 22. Architecture Notes for iOS Parity

| Android Concept | iOS Equivalent |
|----------------|---------------|
| **Jetpack Compose** (declarative UI) | **SwiftUI** |
| **StateFlow** + `collectAsState()` | `ObservableObject` + `@Published` + `@StateObject` |
| **ViewModel** (manual DI via `ViewModelProvider.Factory`) | `@StateObject` or `@ObservedObject` with manual init |
| **Room Database** (SQLite ORM, v26, 25 migrations) | **Core Data** or **GRDB** or **SQLite.swift** |
| **Jetpack Navigation** (string routes like `quiz/123`) | **NavigationStack** + `NavigationPath` (deep-linkable) |
| **DataStore** (key-value preferences) | **UserDefaults** or `@AppStorage` |
| **Coil** (async image loading + caching) | **AsyncImage** (built-in) or **Kingfisher** |
| **AnchoredDraggableState** (custom bottom sheet) | `.presentationDetents([.medium, .large])` |
| **Material 3 theming** (7 color themes) | Custom color sets in Asset Catalog or `Color` extensions |
| **RTL layout** (Arabic support) | SwiftUI handles RTL automatically with `layoutDirection` |
| **rememberLauncherForActivityResult** (file picker) | **FileImporter** / **FileExporter** modifiers |
| **CameraX** (camera preview + capture) | **AVCaptureSession** or **PhotosPicker** |
| **Haptic feedback** (`HapticFeedbackType.LongPress`) | `UIImpactFeedbackGenerator` |

### Navigation Route Map (for deep-linking)

```
welcome                                    → WelcomeScreen
library                                    → LibraryScreen
global_search                              → GlobalSearchScreen
review_dashboard?mistakeId={id}            → ReviewDashboardScreen
data_tools                                 → DataToolsScreen
settings                                   → SettingsScreen
quiz/{quizId}?sessionId={sessionId}        → QuizPlayerScreen
quiz_questions/{quizId}?questionId={id}    → QuizQuestionsScreen
sessions/{quizId}                          → SessionManagementScreen
summary/{sessionId}                        → SummaryScreen
scanner/{quizId}                           → ScannerScreen
category/{categoryName}                    → CategoryQuestionsScreen
adaptive/{type}/{id}                       → QuizPlayerScreen (adaptive mode)
book_dashboard/{bookId}                    → BookKnowledgeDashboardScreen
flashcards/{deckId}?cardId={id}            → FlashcardDeckScreen
slideshow/{courseId}?slideId={id}           → SlideshowCourseScreen
blueprint/{noteId}                         → ReviewBlueprintScreen
book_slideshows/{bookId}                   → SlideshowCourseListScreen
book_blueprints/{bookId}                   → ReviewBlueprintListScreen
book_sources/{bookId}?sourceId={id}        → SourceDocumentListScreen
book_notes/{bookId}                        → BookNotesScreen
book_prompts/{bookId}                      → AiPromptDeckListScreen
prompt_deck/{promptId}?cardId={id}         → AiPromptDeckScreen
```

---

*Last updated by Antigravity AI — 2026-06-08. This document should be refreshed with each major app update.*
