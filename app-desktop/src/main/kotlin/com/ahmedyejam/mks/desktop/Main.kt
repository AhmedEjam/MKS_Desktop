package com.ahmedyejam.mks.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*

import com.ahmedyejam.mks.data.export.ExportManager
import com.ahmedyejam.mks.data.importer.model.ImportFormat
import com.ahmedyejam.mks.data.importer.model.ImportMode
import com.ahmedyejam.mks.data.importer.model.ImportResult
import com.ahmedyejam.mks.data.importer.model.ParsedQuestion
import com.ahmedyejam.mks.data.importer.model.ParsedOption
import com.ahmedyejam.mks.data.importer.parser.BundleFileParser
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetDataProviderFactory
import com.ahmedyejam.mks.data.importer.repository.ImportLibraryManager
import com.ahmedyejam.mks.data.local.entity.*
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.data.repository.*
import com.ahmedyejam.mks.di.*
import com.ahmedyejam.mks.platform.*
import com.ahmedyejam.mks.ui.components.EntityEditDialog
import com.ahmedyejam.mks.ui.flashcard.FlashcardDeckViewModel
import com.ahmedyejam.mks.ui.importer.CompilerViewModel
import com.ahmedyejam.mks.ui.importer.CompilerUiState
import com.ahmedyejam.mks.ui.quiz.QuizViewModel
import com.ahmedyejam.mks.ui.review.ReviewDashboardViewModel
import com.ahmedyejam.mks.ui.search.GlobalSearchViewModel
import com.ahmedyejam.mks.ui.slideshow.SlideshowCourseViewModel
import com.ahmedyejam.mks.ui.session.NoteCollectionViewModel
import com.ahmedyejam.mks.ui.session.PromptViewModel
import kotlinx.coroutines.*; import kotlinx.coroutines.flow.first
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.java.KoinJavaComponent.inject
import java.io.File

// ---- Entry Point ----

fun main() {
    startKoin {
        modules(allModules + desktopPlatformModule)
    }
    application {
        Window(
            onCloseRequest = { stopKoin(); exitApplication() },
            title = "MKS — My Knowledge Space",
            state = rememberWindowState(width = 1100.dp, height = 750.dp)
        ) {
            MaterialTheme(colorScheme = lightColorScheme()) {
                MksApp()
            }
        }
    }
}

// ---- App Shell ----

enum class Screen { LIBRARY, BOOK_DETAIL, QUIZ_PLAYER, FLASHCARD_STUDY, IMPORT, REVIEW_DASHBOARD, SEARCH, SLIDESHOW, NOTE_COLLECTION, PROMPT_DECK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MksApp() {
    var currentScreen by remember { mutableStateOf(Screen.LIBRARY) }
    var selectedBookId by remember { mutableStateOf(0L) }
    var selectedQuizId by remember { mutableStateOf(0L) }
    var selectedDeckId by remember { mutableStateOf(0L) }
    var selectedCourseId by remember { mutableStateOf(0L) }
    var selectedCollectionId by remember { mutableStateOf(0L) }
    var selectedPromptDeckId by remember { mutableStateOf(0L) }

    val fileManager: FileManager by remember { inject(FileManager::class.java) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(when(currentScreen) {
                    Screen.LIBRARY -> "MKS Library"
                    Screen.BOOK_DETAIL -> "Book Details"
                    Screen.QUIZ_PLAYER -> "Quiz Player"
                    Screen.FLASHCARD_STUDY -> "Flashcard Study"
                    Screen.IMPORT -> "Import Questions"
                    Screen.REVIEW_DASHBOARD -> "Review Dashboard"
                    Screen.SEARCH -> "Global Search"
                    Screen.SLIDESHOW -> "Slideshow Course"
                    Screen.NOTE_COLLECTION -> "Notebook"
                    Screen.PROMPT_DECK -> "AI Prompt Deck"
                })},
                navigationIcon = {
                    if (currentScreen != Screen.LIBRARY) {
                        IconButton(onClick = { currentScreen = Screen.LIBRARY }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            if (currentScreen == Screen.LIBRARY || currentScreen == Screen.REVIEW_DASHBOARD || currentScreen == Screen.SEARCH) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, "Lib") },
                        label = { Text("Library") },
                        selected = currentScreen == Screen.LIBRARY,
                        onClick = { currentScreen = Screen.LIBRARY }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, "Review") },
                        label = { Text("Review") },
                        selected = currentScreen == Screen.REVIEW_DASHBOARD,
                        onClick = { currentScreen = Screen.REVIEW_DASHBOARD }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, "Search") },
                        label = { Text("Search") },
                        selected = currentScreen == Screen.SEARCH,
                        onClick = { currentScreen = Screen.SEARCH }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Add, "Import") },
                        label = { Text("Import") },
                        selected = currentScreen == Screen.IMPORT,
                        onClick = { currentScreen = Screen.IMPORT }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                Screen.LIBRARY -> LibraryScreen(
                    onBookClick = { id -> selectedBookId = id; currentScreen = Screen.BOOK_DETAIL }
                )
                Screen.BOOK_DETAIL -> BookDetailScreen(
                    bookId = selectedBookId,
                    onQuizClick = { id -> selectedQuizId = id; currentScreen = Screen.QUIZ_PLAYER },
                    onDeckClick = { id -> selectedDeckId = id; currentScreen = Screen.FLASHCARD_STUDY },
                    onCourseClick = { id -> selectedCourseId = id; currentScreen = Screen.SLIDESHOW },
                    onCollectionClick = { id -> selectedCollectionId = id; currentScreen = Screen.NOTE_COLLECTION },
                    onPromptDeckClick = { id -> selectedPromptDeckId = id; currentScreen = Screen.PROMPT_DECK }
                )
                Screen.QUIZ_PLAYER -> QuizPlayerScreen(
                    quizId = selectedQuizId,
                    onBack = { currentScreen = Screen.BOOK_DETAIL }
                )
                Screen.FLASHCARD_STUDY -> FlashcardStudyScreen(deckId = selectedDeckId)
                Screen.IMPORT -> ImportScreen(
                    fileManager = fileManager,
                    onDone = { currentScreen = Screen.LIBRARY }
                )
                Screen.REVIEW_DASHBOARD -> ReviewDashboardScreen(
                    onBack = { currentScreen = Screen.LIBRARY }
                )
                Screen.SEARCH -> SearchScreen(
                    onBack = { currentScreen = Screen.LIBRARY }
                )
                Screen.SLIDESHOW -> SlideshowScreen(
                    courseId = selectedCourseId,
                    onBack = { currentScreen = Screen.BOOK_DETAIL }
                )
                Screen.NOTE_COLLECTION -> NoteCollectionScreen(
                    collectionId = selectedCollectionId,
                    onBack = { currentScreen = Screen.BOOK_DETAIL }
                )
                Screen.PROMPT_DECK -> PromptDeckScreen(
                    deckId = selectedPromptDeckId,
                    onBack = { currentScreen = Screen.BOOK_DETAIL }
                )
            }
        }
    }
}

// ---- Library Screen ----

@Composable
fun LibraryScreen(onBookClick: (Long) -> Unit) {
    val repo: BookRepository by remember { inject(BookRepository::class.java) }
    val wsRepo: WorkspaceRepository by remember { inject(WorkspaceRepository::class.java) }
    var newTitle by remember { mutableStateOf("") }
    var books by remember { mutableStateOf<List<BookEntity>>(emptyList()) }
    var scope = rememberCoroutineScope()

    // Load books on composition and on demand
    fun loadBooks() { scope.launch { wsRepo.getOrCreateDefault(); books = repo.observeAllBooks().first() } }
    LaunchedEffect(Unit) { loadBooks() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTitle, onValueChange = { newTitle = it },
                label = { Text("New book") }, modifier = Modifier.weight(1f), singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (newTitle.isNotBlank()) {
                    scope.launch { val ws = wsRepo.getOrCreateDefault(); repo.createBook(BookEntity(workspaceId = ws.id, externalId = "", title = newTitle.trim())); newTitle = ""; loadBooks() }
                }
            }) { Text("Add") }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = { loadBooks() }) { Text("Refresh") }
        Spacer(Modifier.height(8.dp))
        if (books.isEmpty()) {
            Text("No books yet. Create one above.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(books, key = { it.id }) { book ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onBookClick(book.id) }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, style = MaterialTheme.typography.titleMedium)
                                Text("${book.questionCount} questions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { /* TODO: Edit Book */ }) { Icon(Icons.Rounded.Edit, "Edit") }
                            IconButton(onClick = { scope.launch { repo.deleteBook(book.id); loadBooks() } }) { Icon(Icons.Rounded.Delete, "Delete") }
                        }
                    }
                }
            }
        }
    }
}

// ---- Book Detail ----

@Composable
fun BookDetailScreen(
    bookId: Long, 
    onQuizClick: (Long) -> Unit, 
    onDeckClick: (Long) -> Unit, 
    onCourseClick: (Long) -> Unit,
    onCollectionClick: (Long) -> Unit,
    onPromptDeckClick: (Long) -> Unit
) {
    val bookRepo: BookRepository by remember { inject(BookRepository::class.java) }
    val quizRepo: QuizRepository by remember { inject(QuizRepository::class.java) }
    val knowledgeRepo: KnowledgeRepository by remember { inject(KnowledgeRepository::class.java) }
    var book by remember { mutableStateOf<BookEntity?>(null) }
    var quizzes by remember { mutableStateOf<List<QuizEntity>>(emptyList()) }
    var decks by remember { mutableStateOf<List<FlashcardDeckEntity>>(emptyList()) }
    var courses by remember { mutableStateOf<List<SlideshowCourseEntity>>(emptyList()) }
    var collections by remember { mutableStateOf<List<NoteCollectionEntity>>(emptyList()) }
    var promptDecks by remember { mutableStateOf<List<PromptDeckEntity>>(emptyList()) }

    var newQuizTitle by remember { mutableStateOf("") }
    var newDeckTitle by remember { mutableStateOf("") }
    var newCourseTitle by remember { mutableStateOf("") }
    var newCollectionTitle by remember { mutableStateOf("") }
    var newPromptDeckTitle by remember { mutableStateOf("") }

    var scope = rememberCoroutineScope()
    var editingItem by remember { mutableStateOf<Any?>(null) }

    fun loadData() { scope.launch { 
        book = bookRepo.getBookById(bookId)
        quizzes = bookRepo.observeQuizzesByBook(bookId).first()
        decks = knowledgeRepo.observeDecksByBook(bookId).first()
        courses = knowledgeRepo.observeCoursesByBook(bookId).first()
        collections = knowledgeRepo.observeNoteCollectionsByBook(bookId).first()
        promptDecks = knowledgeRepo.observePromptDecksByBook(bookId).first()
    } }
    LaunchedEffect(bookId) { loadData() }

    book?.let { bk ->
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(bk.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                val exportMgr: ExportManager by remember { inject(ExportManager::class.java) }
                val fileDialog: FileDialog by remember { inject(FileDialog::class.java) }
                OutlinedButton(onClick = {
                    fileDialog.saveFile("${bk.title}_backup.zip") { result ->
                        if (result != null) scope.launch { exportMgr.exportFullBackup(result.path) }
                    }
                }) { Text("Export") }
            }
            Spacer(Modifier.height(16.dp))

            // Quizzes
            ManagementSection(
                title = "Quizzes",
                items = quizzes,
                newItemTitle = newQuizTitle,
                onNewItemTitleChange = { newQuizTitle = it },
                onAdd = { if (newQuizTitle.isNotBlank()) scope.launch { quizRepo.createQuiz(QuizEntity(externalId = "", bookId = bookId, title = newQuizTitle.trim())); newQuizTitle = ""; loadData() } },
                onItemClick = { onQuizClick(it.id) },
                onEdit = { editingItem = it },
                onDelete = { scope.launch { quizRepo.deleteQuiz(it.id); loadData() } },
                itemContent = { Text("${it.questionCount} questions", style = MaterialTheme.typography.bodySmall) }
            )

            // Decks
            ManagementSection(
                title = "Flashcard Decks",
                items = decks,
                newItemTitle = newDeckTitle,
                onNewItemTitleChange = { newDeckTitle = it },
                onAdd = { if (newDeckTitle.isNotBlank()) scope.launch { knowledgeRepo.createDeck(FlashcardDeckEntity(externalId = "", bookId = bookId, title = newDeckTitle.trim())); newDeckTitle = ""; loadData() } },
                onItemClick = { onDeckClick(it.id) },
                onEdit = { editingItem = it },
                onDelete = { scope.launch { knowledgeRepo.permanentlyDeleteFlashcardDeck(it.id); loadData() } },
                itemContent = { Text("${it.cardCount} cards", style = MaterialTheme.typography.bodySmall) }
            )

            // Courses
            ManagementSection(
                title = "Slideshow Courses",
                items = courses,
                newItemTitle = newCourseTitle,
                onNewItemTitleChange = { newCourseTitle = it },
                onAdd = { if (newCourseTitle.isNotBlank()) scope.launch { knowledgeRepo.createCourse(SlideshowCourseEntity(externalId = "", bookId = bookId, title = newCourseTitle.trim())); newCourseTitle = ""; loadData() } },
                onItemClick = { onCourseClick(it.id) },
                onEdit = { editingItem = it },
                onDelete = { scope.launch { knowledgeRepo.permanentlyDeleteSlideshowCourse(it.id); loadData() } },
                itemContent = { Text("${it.slideCount} slides", style = MaterialTheme.typography.bodySmall) }
            )

            // Collections
            ManagementSection(
                title = "Notebooks",
                items = collections,
                newItemTitle = newCollectionTitle,
                onNewItemTitleChange = { newCollectionTitle = it },
                onAdd = { if (newCollectionTitle.isNotBlank()) scope.launch { knowledgeRepo.createCollection(NoteCollectionEntity(externalId = "", bookId = bookId, title = newCollectionTitle.trim())); newCollectionTitle = ""; loadData() } },
                onItemClick = { onCollectionClick(it.id) },
                onEdit = { editingItem = it },
                onDelete = { /* scope.launch { knowledgeRepo.deleteCollection(it.id); loadData() } */ },
                itemContent = { Text("${it.noteCount} notes", style = MaterialTheme.typography.bodySmall) }
            )

            // Prompts
            ManagementSection(
                title = "AI Prompt Decks",
                items = promptDecks,
                newItemTitle = newPromptDeckTitle,
                onNewItemTitleChange = { newPromptDeckTitle = it },
                onAdd = { if (newPromptDeckTitle.isNotBlank()) scope.launch { knowledgeRepo.createPromptDeck(PromptDeckEntity(bookId = bookId, title = newPromptDeckTitle.trim())); newPromptDeckTitle = ""; loadData() } },
                onItemClick = { onPromptDeckClick(it.id) },
                onEdit = { editingItem = it },
                onDelete = { scope.launch { knowledgeRepo.permanentlyDeletePromptDeck(it.id); loadData() } },
                itemContent = { Text("AI Prompts", style = MaterialTheme.typography.bodySmall) }
            )
        }
    } ?: Text("Loading...")

    // Edit Dialog
    editingItem?.let { item ->
        EntityEditDialog(
            title = "Edit Item",
            initialName = when(item) { is QuizEntity -> item.title; is FlashcardDeckEntity -> item.title; is SlideshowCourseEntity -> item.title; is NoteCollectionEntity -> item.title; is PromptDeckEntity -> item.title; else -> "" },
            onDismiss = { editingItem = null },
            onSave = { name, desc ->
                scope.launch {
                    when(item) {
                        is QuizEntity -> quizRepo.updateQuiz(item.copy(title = name, description = desc))
                        is FlashcardDeckEntity -> knowledgeRepo.updateDeck(item.copy(title = name, description = desc))
                        is SlideshowCourseEntity -> knowledgeRepo.updateCourse(item.copy(title = name, description = desc))
                        is NoteCollectionEntity -> knowledgeRepo.updateCollection(item.copy(title = name, description = desc))
                        is PromptDeckEntity -> knowledgeRepo.updatePromptDeck(item.copy(title = name, description = desc))
                    }
                    loadData()
                }
            }
        )
    }
}

@Composable
fun <T> ManagementSection(
    title: String,
    items: List<T>,
    newItemTitle: String,
    onNewItemTitleChange: (String) -> Unit,
    onAdd: () -> Unit,
    onItemClick: (T) -> Unit,
    onEdit: (T) -> Unit,
    onDelete: (T) -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = newItemTitle, onValueChange = onNewItemTitleChange, label = { Text("New $title") }, modifier = Modifier.weight(1f), singleLine = true)
        Spacer(Modifier.width(8.dp))
        Button(onClick = onAdd) { Text("Create") }
    }
    Spacer(Modifier.height(8.dp))
    if (items.isEmpty()) Text("No $title yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    else items.forEach { item ->
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).clickable { onItemClick(item) }) { 
                    Text((item as? Any)?.let { when(it) { is QuizEntity -> it.title; is FlashcardDeckEntity -> it.title; is SlideshowCourseEntity -> it.title; is NoteCollectionEntity -> it.title; is PromptDeckEntity -> it.title; else -> "" } } ?: "", style = MaterialTheme.typography.bodyLarge)
                    itemContent(item)
                }
                IconButton(onClick = { onEdit(item) }) { Icon(Icons.Rounded.Edit, "Edit") }
                IconButton(onClick = { onDelete(item) }) { Icon(Icons.Rounded.Delete, "Delete") }
                Icon(Icons.Default.ChevronRight, "Open")
            }
        }
    }
    Spacer(Modifier.height(20.dp))
}

// ---- Quiz Player ----

@Composable
fun QuizPlayerScreen(quizId: Long, onBack: () -> Unit) {
    val viewModel: QuizViewModel by remember { inject(QuizViewModel::class.java) }
    LaunchedEffect(quizId) { viewModel.startQuiz(quizId) }
    com.ahmedyejam.mks.ui.quiz.SharedQuizPlayerScreen(viewModel = viewModel, onBack = onBack)
}

// ---- Flashcard Study ----

@Composable
fun FlashcardStudyScreen(deckId: Long) {
    val viewModel: FlashcardDeckViewModel by remember { inject(FlashcardDeckViewModel::class.java) }
    LaunchedEffect(deckId) { viewModel.loadDeck(deckId) }
    com.ahmedyejam.mks.ui.flashcard.SharedFlashcardStudyScreen(viewModel = viewModel, onBack = { /* Back */ })
}

// ---- Slideshow ----

@Composable
fun SlideshowScreen(courseId: Long, onBack: () -> Unit) {
    val viewModel: SlideshowCourseViewModel by remember { inject(SlideshowCourseViewModel::class.java) }
    LaunchedEffect(courseId) { viewModel.loadCourse(courseId) }
    com.ahmedyejam.mks.ui.slideshow.SharedSlideshowCourseScreen(viewModel = viewModel, onBack = onBack)
}

// ---- Other Screens ----

@Composable
fun NoteCollectionScreen(collectionId: Long, onBack: () -> Unit) {
    val viewModel: NoteCollectionViewModel by remember { inject(NoteCollectionViewModel::class.java) }
    LaunchedEffect(collectionId) { viewModel.loadCollection(collectionId) }
    com.ahmedyejam.mks.ui.session.SharedNoteCollectionScreen(viewModel = viewModel, onBack = onBack)
}

@Composable
fun PromptDeckScreen(deckId: Long, onBack: () -> Unit) {
    val viewModel: PromptViewModel by remember { inject(PromptViewModel::class.java) }
    LaunchedEffect(deckId) { viewModel.loadDeck(deckId) }
    com.ahmedyejam.mks.ui.session.SharedPromptDeckScreen(viewModel = viewModel, onBack = onBack)
}

@Composable
fun SearchScreen(onBack: () -> Unit) {
    val viewModel: GlobalSearchViewModel by remember { inject(GlobalSearchViewModel::class.java) }
    com.ahmedyejam.mks.ui.search.SharedGlobalSearchScreen(viewModel = viewModel, onBack = onBack, onResultClick = { })
}

@Composable
fun ReviewDashboardScreen(onBack: () -> Unit) {
    val viewModel: ReviewDashboardViewModel by remember { inject(ReviewDashboardViewModel::class.java) }
    com.ahmedyejam.mks.ui.review.SharedReviewDashboardScreen(viewModel = viewModel, onBack = onBack)
}

// ---- Import Screen ----

@Composable
fun ImportScreen(fileManager: FileManager, onDone: () -> Unit) {
    val importManager: ImportLibraryManager by remember { inject(ImportLibraryManager::class.java) }
    val spreadsheetFactory: SpreadsheetDataProviderFactory by remember { inject(SpreadsheetDataProviderFactory::class.java) }
    val bundleFileParser: BundleFileParser by remember { inject(BundleFileParser::class.java) }
    val fileDialog: FileDialog by remember { inject(FileDialog::class.java) }
    val compilerViewModel = remember { CompilerViewModel(importManager, spreadsheetFactory, bundleFileParser) }

    val uiState by compilerViewModel.uiState.collectAsState()

    var step by remember { mutableStateOf(0) }
    var bookTitle by remember { mutableStateOf("") }
    var importCount by remember { mutableStateOf(0) }

    LaunchedEffect(uiState.questions, uiState.sheetNames, uiState.error) {
        if (uiState.questions.isNotEmpty() && step == 0) {
            bookTitle = uiState.fileName?.removeSuffix(".csv")?.removeSuffix(".tsv")
                ?.removeSuffix(".xlsx")?.removeSuffix(".xls")?.removeSuffix(".txt") ?: "Imported"
            step = if (uiState.detectedMode == ImportMode.SPREADSHEET) 1 else 2
        }
        if (uiState.questions.isEmpty() && !uiState.isLoading && uiState.error == null && step > 0) {
            importCount = uiState.stats.successfullyParsed
            if (importCount > 0) step = 3
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Import Questions", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        when (step) {
            0 -> StepSelectFile(fileDialog, compilerViewModel, uiState)
            1 -> StepColumnMapping(
                uiState = uiState,
                onHeaderChange = { compilerViewModel.updateHeaderRow(it) },
                onMappingChange = { compilerViewModel.updateMapping(it, uiState.optionColumns) },
                onOptionColumnsChange = { compilerViewModel.updateOptionColumns(it) },
                onSheetSelected = { compilerViewModel.onSheetSelected(it) },
                onContinue = { step = 2 },
                onBack = { compilerViewModel.closeCurrentResources(); step = 0 },
            )
            2 -> StepPreview(
                uiState = uiState,
                bookTitle = bookTitle,
                onToggle = { compilerViewModel.toggleQuestionInclusion(it) },
                onSelectAll = { compilerViewModel.toggleQuestionsRange(1, uiState.questions.size, true) },
                onSelectNone = { compilerViewModel.toggleQuestionsRange(1, uiState.questions.size, false) },
                onRangeSelect = { from, to, include -> compilerViewModel.toggleQuestionsRange(from, to, include) },
                onCorrectAnswerChange = { idx, ansId -> compilerViewModel.updateQuestionCorrectAnswer(idx, ansId) },
                onTitleChange = { bookTitle = it },
                onImport = {
                    compilerViewModel.saveParsedQuestions(
                        title = "Imported Quiz",
                        bookId = null,
                        newBookTitle = bookTitle,
                    )
                },
                onBack = { step = if (uiState.detectedMode == ImportMode.SPREADSHEET) 1 else 0 },
            )
            3 -> StepDone(importCount, bookTitle, {
                compilerViewModel.closeCurrentResources(); step = 0; importCount = 0
            }, onDone)
        }
    }
}

@Composable
fun RangeSelectionDialog(
    maxQuestions: Int,
    onDismiss: () -> Unit,
    onApply: (Int, Int, Boolean) -> Unit
) {
    var fromText by remember { mutableStateOf("1") }
    var toText by remember { mutableStateOf(maxQuestions.toString()) }
    var include by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Range") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = fromText,
                        onValueChange = { fromText = it.filter { c -> c.isDigit() } },
                        label = { Text("From") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = toText,
                        onValueChange = { toText = it.filter { c -> c.isDigit() } },
                        label = { Text("To") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = include, onClick = { include = true })
                    Text("Include", modifier = Modifier.padding(start = 8.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !include, onClick = { include = false })
                    Text("Exclude", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val from = fromText.toIntOrNull() ?: 1
                val to = toText.toIntOrNull() ?: maxQuestions
                onApply(from, to, include)
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AnswerSelectionDialog(
    question: ParsedQuestion,
    onDismiss: () -> Unit,
    onAnswerSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Correct Answer") },
        text = {
            LazyColumn {
                items(question.options, key = { it.id }) { option ->
                    val isSelected = question.correctAnswers.contains(option.id)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onAnswerSelected(option.id) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = { onAnswerSelected(option.id) })
                        Text(text = "${option.id.removePrefix("opt_")}: ${option.text}", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ---- Import Steps ----

@Composable
fun StepSelectFile(fileDialog: FileDialog, compilerViewModel: CompilerViewModel, uiState: CompilerUiState) {
    Column {
        Text("Step 1: Select a file to import.", style = MaterialTheme.typography.bodyLarge)
        Text("Supported: CSV, TSV, XLSX, XLS, JSON, HTML, ZIP, TXT", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            fileDialog.openFile(listOf(FileFilter("All Supported", listOf("csv","tsv","xlsx","xls","json","html","zip","txt")))) { fileResult ->
                if (fileResult != null) compilerViewModel.onFileSelected(fileResult.path)
            }
        }, enabled = !uiState.isLoading) {
            Text(if (uiState.isLoading) "Parsing..." else "Select File")
        }
        if (uiState.isLoading) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator() }
        if (uiState.error != null) { Spacer(Modifier.height(8.dp)); Text(uiState.error!!, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun StepColumnMapping(
    uiState: CompilerUiState,
    onHeaderChange: (Int) -> Unit,
    onMappingChange: (Map<String, Int>) -> Unit,
    onOptionColumnsChange: (List<Int>) -> Unit,
    onSheetSelected: (String?) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val fieldNames = listOf("question", "answer", "explanation", "hint", "reference", "categories")
    val mapping = uiState.mapping
    val availableColumns = uiState.availableColumns

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Step 2: Map Columns", style = MaterialTheme.typography.bodyLarge)
        Text("Select which row is the header, then assign columns to fields.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (uiState.sheetNames.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Text("Sheet: ", style = MaterialTheme.typography.bodyMedium)
                uiState.sheetNames.forEach { sheet ->
                    FilterChip(
                        selected = sheet == uiState.selectedSheet,
                        onClick = { onSheetSelected(sheet) },
                        label = { Text(sheet.take(15)) }
                    )
                    Spacer(Modifier.width(4.dp))
                }
            }
        }

        if (uiState.error != null) {
            Spacer(Modifier.height(4.dp))
            Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Header row: ", style = MaterialTheme.typography.bodyMedium)
            for (i in 0 until minOf(10, 20)) {
                FilterChip(selected = i == uiState.headerRow, onClick = { onHeaderChange(i) }, label = { Text("${i + 1}") })
                Spacer(Modifier.width(4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))

        if (availableColumns.isNotEmpty()) {
            Text("Header: ${availableColumns.joinToString(" | ")}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
        }

        Text("Assign columns:", style = MaterialTheme.typography.titleSmall)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(fieldNames) { field ->
                val colIdx = mapping[field] ?: -1
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(field.replaceFirstChar { it.uppercase() }, modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    availableColumns.forEachIndexed { i, colName ->
                        FilterChip(
                            selected = i == colIdx,
                            onClick = { onMappingChange(mapping + (field to if (i == colIdx) -1 else i)) },
                            label = { Text(colName.take(10), style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(horizontal = 1.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                    FilterChip(
                        selected = colIdx == -1,
                        onClick = { onMappingChange(mapping + (field to -1)) },
                        label = { Text("skip", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text("Options (Multiple):", style = MaterialTheme.typography.titleSmall)
                FlowRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    availableColumns.forEachIndexed { i, colName ->
                        val isSelected = uiState.optionColumns.contains(i)
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                val newList = if (isSelected) uiState.optionColumns - i else (uiState.optionColumns + i).sorted()
                                onOptionColumnsChange(newList)
                            },
                            label = { Text(colName.take(15)) },
                            leadingIcon = { if (isSelected) Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = onContinue) { Text("Continue →") }
        }
    }
}

@Composable
fun StepPreview(
    uiState: CompilerUiState,
    bookTitle: String,
    onToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onRangeSelect: (Int, Int, Boolean) -> Unit,
    onCorrectAnswerChange: (Int, String) -> Unit,
    onTitleChange: (String) -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    val questions = uiState.questions
    val includedCount = questions.count { it.isIncluded }
    var showRangeDialog by remember { mutableStateOf(false) }
    var selectedQuestionIdxForAnswer by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Step 3: Preview & Import", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(value = bookTitle, onValueChange = onTitleChange, label = { Text("Book name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(4.dp))
        Text("${uiState.stats.successfullyParsed} parsed, ${uiState.stats.skippedEmptyStem} skipped, ${includedCount} selected for import", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$includedCount of ${questions.size} selected", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onSelectAll) { Text("All") }
            TextButton(onClick = onSelectNone) { Text("None") }
            TextButton(onClick = { showRangeDialog = true }) { Text("Range Select") }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(questions.size) { idx ->
                val q = questions[idx]
                Card(modifier = Modifier.fillMaxWidth().clickable { selectedQuestionIdxForAnswer = idx }) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                        Checkbox(checked = q.isIncluded, onCheckedChange = { onToggle(idx) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(q.stem.take(120), style = MaterialTheme.typography.bodySmall, maxLines = 3, fontWeight = FontWeight.Bold)
                            if (q.options.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                q.options.forEach { opt ->
                                    val isCorrect = q.correctAnswers.contains(opt.id)
                                    Text(
                                        text = "${opt.id.removePrefix("opt_")}: ${opt.text}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (uiState.error != null) Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        if (uiState.isLoading) { LinearProgressIndicator(); Spacer(Modifier.height(4.dp)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = onImport, enabled = !uiState.isLoading && includedCount > 0 && bookTitle.isNotBlank()) {
                Text(if (uiState.isLoading) "Importing..." else "Import $includedCount Selected")
            }
        }
    }

    if (showRangeDialog) {
        RangeSelectionDialog(
            maxQuestions = questions.size,
            onDismiss = { showRangeDialog = false },
            onApply = { from, to, include -> onRangeSelect(from, to, include); showRangeDialog = false }
        )
    }

    selectedQuestionIdxForAnswer?.let { idx ->
        AnswerSelectionDialog(
            question = questions[idx],
            onDismiss = { selectedQuestionIdxForAnswer = null },
            onAnswerSelected = { ansId -> onCorrectAnswerChange(idx, ansId); selectedQuestionIdxForAnswer = null }
        )
    }
}

@Composable
fun StepDone(count: Int, title: String, onRestart: () -> Unit, onDone: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("✓ Import Complete!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.height(16.dp))
        Text("Imported $count questions into \"$title\".", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRestart) { Text("Import Another") }
            OutlinedButton(onClick = onDone) { Text("Go to Library") }
        }
    }
}
