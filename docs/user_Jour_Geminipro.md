# MKS Android App - User Journey & UI Map

> Last updated: 2026-06-08. Room v26, 25 migrations, 22+ navigation routes.

This document outlines every screen (page) in the MKS application, describing the interactable elements (buttons, sliders, gestures) and the outcomes of interacting with them. This serves as a dynamic map to understand functionality and features, and to guide the development of the iOS parity app.

## 1. App Initialization & Onboarding

### Welcome Screen (`/welcome`)
*Purpose*: First-run experience to select language, preview theme, and learn about the app's features.
- **Language Pill (EN / العربية)**
  - *Action*: Tap to select language.
  - *Loads/Outputs*: Updates `DataStoreManager` and changes the UI language instantly.
- **"Explore Features" Text Button**
  - *Action*: Tap.
  - *Loads/Outputs*: Shows an AlertDialog detailing the app's capabilities.
- **"Get Started" Button**
  - *Action*: Tap.
  - *Loads/Outputs*: Sets `showWelcomeOnStartup = false` in `DataStoreManager` and navigates to the **Library Screen**, popping the welcome screen off the backstack.

## 2. Main Navigation Hub

### Library Screen (`/library`)
*Purpose*: The central dashboard containing the user's books, quizzes, flashcard decks, and knowledge assets.
- **Top App Bar**
  - **Back Arrow** (if searching or inside a book dashboard)
    - *Action*: Tap.
    - *Loads/Outputs*: Resets to root library view or stops search.
  - **Search Icon**
    - *Action*: Tap.
    - *Loads/Outputs*: Reveals the inline search bar.
  - **Settings Icon**
    - *Action*: Tap.
    - *Loads/Outputs*: Navigates to **Settings Screen**.
  - **Sort Icon**
    - *Action*: Tap.
    - *Loads/Outputs*: Opens `SortDialog` to sort items by Name, Date, or Progress.
  - **View Mode Toggle**
    - *Action*: Tap.
    - *Loads/Outputs*: Toggles between Grid (2 columns) and List (1 column) view.
  - **Overflow Menu**
    - *Action*: Tap.
    - *Loads/Outputs*: Opens extra options (e.g., showing/hiding covers).
- **Floating Action Button (+)**
  - *Action*: Tap to expand the Fab Menu.
  - *Sub-options (context-dependent)*:
    - **Import Document**: Launches file picker for XLSX, CSV, TSV, JSON, HTML, TXT, ZIP files. Leads to `CompilerDialog` or direct import.
    - **Export**: Launches file picker to save a ZIP bundle of the selected book/quiz or entire library.
    - **Create Book/Quiz**: Opens `EditEntityDialog` to create a new Book or Quiz.
    - **Adaptive Training**: Starts adaptive training scoped to the current book (or all books at root level).
    - **Flashcard Deck**: (Inside a book) Opens dialog to create a new flashcard deck.
    - **Slideshow Course**: (Inside a book) Navigates to **Slideshow Course List Screen**.
    - **Review Blueprint**: (Inside a book) Navigates to **Review Blueprint List Screen**.
    - **Sources**: (Inside a book) Navigates to **Source Documents Screen**.
    - **Book Notes**: (Inside a book) Navigates to **Book Notes Screen**.
    - **AI Prompt Deck**: (Inside a book) Opens dialog to create a new AI prompt deck.
    - **Scanner**: (Quizzes) Navigates to **Scanner Screen** to OCR questions.
- **Content Grid / List Items**
  - **Book Card**
    - *Action*: Tap.
    - *Loads/Outputs*: Opens the specific Book (filters library to show items belonging to this book).
  - **Quiz Card**
    - *Action*: Tap.
    - *Loads/Outputs*: Navigates to **Session Management Screen**.
  - **Category Item**
    - *Action*: Tap.
    - *Loads/Outputs*: Navigates to **Category Questions Screen**.
  - **Knowledge Bank Assets** (Flashcards, Slideshows, Blueprints, Prompts)
    - *Action*: Tap.
    - *Loads/Outputs*: Navigates to their respective screens.
  - **Long Press on Book/Quiz/Category**
    - *Action*: Press and hold.
    - *Loads/Outputs*: Opens the `OptionsSheet` (bottom sheet) allowing Pin, Edit, Export, Import, Delete, Book Dashboard, or Scanner.

### Global Search Screen (`/global_search`)
*Purpose*: Comprehensive search across all app contents (books, quizzes, questions, flashcards, notes, blueprints, prompts, mistakes, assets).
- **Search Bar**
  - *Action*: Type at least 2 characters.
  - *Loads/Outputs*: Triggers full-text search across all entity types via `GlobalSearchDao`.
- **Result Item**
  - *Action*: Tap.
  - *Loads/Outputs*: Navigates to the corresponding screen (e.g., opening a specific flashcard deck or quiz question).

## 3. Core Study Flows

### Quiz Player Screen (`/quiz/{quizId}?sessionId={sessionId}`)
*Purpose*: The main interactive testing environment.
- **Top App Bar**
  - **Back Arrow**: Exits quiz, saves session progress automatically.
  - **Timer display**: MM:SS format (countdown if quiz/question timer set).
  - **Progress**: "3/25" showing current question / total.
  - **Score**: "★ 5/25" — score out of total.
  - **Streak counter**: "🔥 3" for consecutive correct answers.
  - **Progress bar**: Thin colored bar showing position in quiz.
- **Question Content Area**
  - **Horizontal Swipe Gesture**: Swipe left/right to move between questions.
  - **Image Thumbnail**: Tap to open `ZoomableImageDialog` fullscreen.
  - **Assets Button** (paperclip icon): Opens read-only attachments dialog.
- **Option Items** (Multiple Choice / Single Choice)
  - **Tap Option**: Selects it (radio for single, checkbox for multiple choice).
  - **Double Tap Option**: If enabled in settings, selects AND submits.
  - **Long Press / Elimination**: Strikes through option visually.
- **Bottom Sheet (Settings & Navigation)**
  - Drag up/down on handle to expand/collapse.
  - **Peek area**: Accuracy %, Streak, Submit/Next/Reveal button.
  - **Expanded controls**: Categories, One-by-One, Rapid Mode, Eliminate, Drop Question, Focus Mode, Mark/Bookmark, Finish Quiz toggles.
  - **Navigation**: Filter chips (ALL, ANSWERED, UNANSWERED, MISSED, MARKED, DROPPED), question number grid.

### Summary Screen (`/summary/{sessionId}`)
*Purpose*: Post-quiz analytics and score breakdown.
- **Score Header Card**: Percentage, fraction, best streak, session label, average time.
- **Share button**: System share sheet with text export.
- **Visible Details panel**: Toggle chips for Stem, Options, Hint, High Yield, Reference, Explanation.
- **Review filter chips**: ALL, CORRECT, WRONG, UNANSWERED, DROPPED, WITH EXPLANATION.
- **Question review cards**: Color-coded options, visibility toggleable.
- **Category Performance**: Per-category accuracy breakdown.
- **Retry Button**: Starts new session for same quiz.
- **Library Button**: Returns to Library Screen.

### Session Management Screen (`/sessions/{quizId}`)
*Purpose*: List of previous sessions for a specific quiz.
- **Session Card**: Shows label, accuracy ring, score metrics, progress bar, last-active date.
  - **Tap**: Resumes incomplete or views completed session.
  - **Delete button**: Confirmation dialog → permanently deletes session.
- **+ FAB**: Opens Start Session Dialog (label, include filters, question range, shuffle, rapid mode, repeat wrong, quiz/question timers, remember settings).

## 4. Knowledge Bank & Book Tools

### Book Knowledge Dashboard (`/book_dashboard/{bookId}`)
*Purpose*: Overview of a book's complete learning ecosystem.
- **Study Progress Card**: Completion bar, due count, weak count, marked count, mistakes count.
- **Magic Actions**: "Draft Note from Marked", "Note from Mistakes" (conditional visibility).
- **Learning Tools Grid** (6 tappable cards):
  - **Quizzes** → Library filtered to book's quizzes
  - **Flashcards** → Flashcard Deck List
  - **Slideshows** → Slideshow Course List Screen
  - **Notes** → Review Blueprint List Screen
  - **AI Prompts** → AI Prompt Deck List Screen
  - **Sources** → Source Documents Screen

### Flashcard Deck Screen (`/flashcards/{deckId}?cardId={cardId}`)
*Purpose*: Two modes — **List Mode** (manage cards) and **Study Mode** (review cards).
- **List Mode**: Stats card, add card, study button, generate from marked/missed questions, card list with reorder/edit/delete.
- **Study Mode**: Card counter, tap-to-flip card, rating buttons (Again/Good/Easy), previous/next navigation.

### Slideshow Course Screens
- **Slideshow Course List Screen** (`/book_slideshows/{bookId}`): Create, edit, delete courses. Tap to open individual course.
- **Slideshow Course Screen** (`/slideshow/{courseId}?slideId={slideId}`): Create/edit/delete slides, view slide content.

### Review Blueprint Screens
- **Review Blueprint List Screen** (`/book_blueprints/{bookId}`): Create (manual, from marked, from missed), edit, delete blueprints.
- **Review Blueprint Screen** (`/blueprint/{noteId}`): Edit body, save, mark reviewed, convert to flashcards, append to question note.

### AI Prompt Deck Screens
- **AI Prompt Deck List Screen** (`/book_prompts/{bookId}`): Create, edit, delete prompt decks.
- **AI Prompt Deck Screen** (`/prompt_deck/{promptId}?cardId={cardId}&runId={runId}`): Create/edit/delete prompt cards with `{variable}` placeholders, run prompts, copy rendered output, save AI responses, convert to notes/blueprints/flashcards, view run history.

### Source Documents Screen (`/book_sources/{bookId}?sourceId={sourceId}`)
- Create source documents (title, type, description). Edit/delete with citation warnings.

### Book Notes Screen (`/book_notes/{bookId}`)
- Read-only view of all questions with notes attached in this book.

### Review Dashboard Screen (`/review_dashboard?mistakeId={mistakeId}`)
*Purpose*: Unified review queue for all items due across flashcards, blueprints, and mistake logs.
- **Summary Cards**: Flashcards due, Blueprints due, Mistakes due, Mistakes scheduled, Marked/weak questions.
- **Review item card**: Tap to navigate, "Reviewed" button, "Snooze 1 week" button.

## 5. Question Management

### Quiz Questions Screen (`/quiz_questions/{quizId}?questionId={questionId}`)
*Purpose*: Full list/browser of all questions in a quiz. For reviewing, editing, organizing, and bulk-managing.
- **Top Bar**: Back, Search, Filter, title with question count.
- **Filter Controls**: Toggle chips for Stem, Options, Correct Answer, Explanation, Hint, Reference, Info, Marked Only, Has Attachments.
- **Question Cards**: Long press for multi-select; tap to edit; bookmark, edit, attachments icons; tap image for fullscreen.
- **Selection Mode**: Clear, Select All, Bookmark/Unbookmark, Move, Copy, Export, Create Flashcards, Delete.
- **+ FAB**: Create new question manually.

### Category Questions Screen (`/category/{categoryName}`)
- Same as Quiz Questions Screen but scoped to a category across all quizzes.
- **"Start Quiz" FAB**: Starts adaptive quiz using category-scoped questions.

## 6. Adaptive Training (`/adaptive/{type}/{id}`)
- Not a separate screen — Quiz Player with intelligently selected questions.
- Types: `BOOK` (weak/unanswered from book), `CATEGORY` (category-scoped), `QUIZ` (quiz weak spots), `ALL` (all books).
- FocusManager algorithm: unanswered > recently wrong > low-weight > marked.

## 7. Utility & Configuration

### Settings Screen (`/settings`)
*Purpose*: App-wide configuration.
- **Library & Backup**: Export full library, Advanced import/export preview → Data Tools, Global search, Review dashboard.
- **Appearance**: 7 theme options (Dawn, Forest, Midnight, Lavender, Plain Light, Plain Dark, System), Reset, Font Scale (0.5×–2.0×), UI Density (0.5×–1.5×), Show cover images toggle, Language selector (English/Arabic).
- **Global Configuration**: Show Welcome Screen, Auto-hide knowledge summary, Focus Mode (DND), Skip Unanswered, Double-tap to submit, Rapid-mode advance delay (500ms–5000ms).
- **Danger Zone**: Clear Categories, Reset Database (wipes and re-seeds).

### Data Tools Screen (`/data_tools`)
*Purpose*: Advanced import/export with preview capabilities.
- **Export full library**: ZIP of all learning tables (JSON dumps).
- **Save export file**: System file saver.
- **Choose ZIP and preview**: Parse without importing, show conflicts/warnings.
- **Local ZIP path field**: For emulator/debug testing.

### Scanner Screen (`/scanner/{quizId}`)
*Purpose*: Camera-based question scanner using OCR.
- **Camera View**: Full-screen preview, capture button.
- **Review View**: Edit/delete detected questions, discard or import all.
- **Error View**: Retry button on OCR failure.

## Architectural Notes for iOS Parity

- **State Management**: `StateFlow` + `collectAsState()` → SwiftUI `ObservableObject` + `@Published` + `@StateObject`
- **Navigation**: Jetpack Navigation Compose with string routes → `NavigationStack` + `NavigationPath`
- **Bottom Sheets**: Custom `AnchoredDraggableState` → `.presentationDetents([.medium, .large])`
- **Themes**: Material 3 (7 themes) → Semantic Colors or custom Asset Catalog color sets
- **Database**: Room v26 (25 migrations) → Core Data, GRDB, or SQLite.swift
- **DI**: Manual `ViewModelProvider.Factory` → `@StateObject` with manual init
- **Preferences**: DataStore → UserDefaults / `@AppStorage`
- **Images**: Coil → AsyncImage (built-in) or Kingfisher
- **RTL**: Compose handles automatically → SwiftUI `layoutDirection`
- **File Picker**: `rememberLauncherForActivityResult` → FileImporter / FileExporter
- **Camera**: CameraX → AVCaptureSession / PhotosPicker
- **Haptics**: `HapticFeedbackType.LongPress` → `UIImpactFeedbackGenerator`

---
*Generated by Antigravity AI — last updated 2026-06-08*