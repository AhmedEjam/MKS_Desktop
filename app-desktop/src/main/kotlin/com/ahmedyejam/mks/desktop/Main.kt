package com.ahmedyejam.mks.desktop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home

import com.ahmedyejam.mks.data.export.ExportManager
import com.ahmedyejam.mks.data.importer.model.ImportFormat
import com.ahmedyejam.mks.data.importer.model.ImportMode
import com.ahmedyejam.mks.data.importer.model.ImportResult
import com.ahmedyejam.mks.data.importer.model.ParsedQuestion
import com.ahmedyejam.mks.data.importer.parser.BundleFileParser
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetDataProviderFactory
import com.ahmedyejam.mks.data.importer.repository.ImportLibraryManager
import com.ahmedyejam.mks.data.local.entity.*
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.data.repository.*
import com.ahmedyejam.mks.di.*
import com.ahmedyejam.mks.platform.*
import com.ahmedyejam.mks.ui.importer.CompilerViewModel
import com.ahmedyejam.mks.ui.importer.CompilerUiState
import com.ahmedyejam.mks.ui.quiz.QuizViewModel
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

enum class Screen { LIBRARY, BOOK_DETAIL, QUIZ_PLAYER, FLASHCARD_STUDY, IMPORT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MksApp() {
    var currentScreen by remember { mutableStateOf(Screen.LIBRARY) }
    var selectedBookId by remember { mutableStateOf(0L) }
    var selectedQuizId by remember { mutableStateOf(0L) }
    var selectedDeckId by remember { mutableStateOf(0L) }

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
            if (currentScreen == Screen.LIBRARY) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, "Lib") },
                        label = { Text("Library") },
                        selected = true,
                        onClick = { currentScreen = Screen.LIBRARY }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Add, "Import") },
                        label = { Text("Import") },
                        selected = false,
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
                    onDeckClick = { id -> selectedDeckId = id; currentScreen = Screen.FLASHCARD_STUDY }
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
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(book.title, style = MaterialTheme.typography.titleMedium)
                            Text("${book.questionCount} questions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ---- Book Detail ----

@Composable
fun BookDetailScreen(bookId: Long, onQuizClick: (Long) -> Unit, onDeckClick: (Long) -> Unit) {
    val bookRepo: BookRepository by remember { inject(BookRepository::class.java) }
    val quizRepo: QuizRepository by remember { inject(QuizRepository::class.java) }
    val knowledgeRepo: KnowledgeRepository by remember { inject(KnowledgeRepository::class.java) }
    var book by remember { mutableStateOf<BookEntity?>(null) }
    var quizzes by remember { mutableStateOf<List<QuizEntity>>(emptyList()) }
    var decks by remember { mutableStateOf<List<FlashcardDeckEntity>>(emptyList()) }
    var newQuizTitle by remember { mutableStateOf("") }
    var newDeckTitle by remember { mutableStateOf("") }
    var scope = rememberCoroutineScope()

    fun loadData() { scope.launch { book = bookRepo.getBookById(bookId); quizzes = bookRepo.observeQuizzesByBook(bookId).first(); decks = knowledgeRepo.observeDecksByBook(bookId).first() } }
    LaunchedEffect(bookId) { loadData() }

    book?.let { bk ->
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(bk.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                // Export button
                val exportMgr: ExportManager by remember { inject(ExportManager::class.java) }
                val fileDialog: FileDialog by remember { inject(FileDialog::class.java) }
                OutlinedButton(onClick = {
                    fileDialog.saveFile("${bk.title}_backup.zip") { result ->
                        if (result != null) {
                            scope.launch {
                                exportMgr.exportFullBackup(result.path)
                            }
                        }
                    }
                }) { Text("Export") }
            }
            Spacer(Modifier.height(16.dp))

            // Quizzes section
            Text("Quizzes", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newQuizTitle, onValueChange = { newQuizTitle = it }, label = { Text("Quiz name") }, modifier = Modifier.weight(1f), singleLine = true)
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (newQuizTitle.isNotBlank()) {
                        scope.launch { quizRepo.createQuiz(QuizEntity(externalId = "", bookId = bookId, title = newQuizTitle.trim())); newQuizTitle = ""; loadData() }
                    }
                }) { Text("Create") }
            }
            Spacer(Modifier.height(8.dp))
            if (quizzes.isEmpty()) Text("No quizzes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(quizzes, key = { it.id }) { quiz ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onQuizClick(quiz.id) }.padding(vertical = 2.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) { Text(quiz.title, style = MaterialTheme.typography.bodyLarge); Text("${quiz.questionCount} questions", style = MaterialTheme.typography.bodySmall) }
                            Icon(Icons.Default.ChevronRight, "Open")
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Flashcard decks section
            Text("Flashcard Decks", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = newDeckTitle, onValueChange = { newDeckTitle = it }, label = { Text("Deck name") }, modifier = Modifier.weight(1f), singleLine = true)
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (newDeckTitle.isNotBlank()) {
                        scope.launch {
                            knowledgeRepo.createDeck(FlashcardDeckEntity(externalId = "", bookId = bookId, title = newDeckTitle.trim()))
                            newDeckTitle = ""
                        }
                    }
                }) { Text("Create") }
            }
            Spacer(Modifier.height(8.dp))
            if (decks.isEmpty()) Text("No decks yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(decks, key = { it.id }) { deck ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onDeckClick(deck.id) }.padding(vertical = 2.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) { Text(deck.title, style = MaterialTheme.typography.bodyLarge); Text("${deck.cardCount} cards", style = MaterialTheme.typography.bodySmall) }
                            Icon(Icons.Default.ChevronRight, "Open")
                        }
                    }
                }
            }
        }
    } ?: Text("Loading...")
}

// ---- Quiz Player ----

@Composable
fun QuizPlayerScreen(quizId: Long, onBack: () -> Unit) {
    val viewModel: QuizViewModel by remember { inject(QuizViewModel::class.java) }
    
    LaunchedEffect(quizId) {
        viewModel.startQuiz(quizId)
    }

    com.ahmedyejam.mks.ui.quiz.SharedQuizPlayerScreen(
        viewModel = viewModel,
        onBack = onBack
    )
}

// ---- Flashcard Study ----

@Composable
fun FlashcardStudyScreen(deckId: Long) {
    val knowledgeRepo: KnowledgeRepository by remember { inject(KnowledgeRepository::class.java) }
    val studyRepo: StudyRepository by remember { inject(StudyRepository::class.java) }
    var cards by remember { mutableStateOf<List<FlashcardEntity>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var scope = rememberCoroutineScope()

    LaunchedEffect(deckId) {
        knowledgeRepo.observeCardsByDeck(deckId).collect { cards = it }
    }

    val card = cards.getOrNull(currentIndex)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Flashcard Study", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Card ${currentIndex + 1} of ${cards.size}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        card?.let { c ->
            Card(
                modifier = Modifier.fillMaxWidth().height(250.dp).clickable { isFlipped = !isFlipped },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isFlipped) c.backText else c.frontText,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
            Text("Tap card to flip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            if (isFlipped) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            scope.launch { studyRepo.updateFlashcardProgress(c.id, false) }
                            if (currentIndex < cards.lastIndex) { currentIndex++; isFlipped = false }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Wrong") }
                    Button(
                        onClick = {
                            scope.launch { studyRepo.updateFlashcardProgress(c.id, true) }
                            if (currentIndex < cards.lastIndex) { currentIndex++; isFlipped = false }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) { Text("Correct") }
                }
            }
        } ?: Text("No cards in this deck yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
    onSheetSelected: (String?) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val fieldNames = listOf("question", "options", "answer", "explanation", "hint", "reference", "categories")
    val mapping = uiState.mapping
    val availableColumns = uiState.availableColumns

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Step 2: Map Columns", style = MaterialTheme.typography.bodyLarge)
        Text("Select which row is the header, then assign columns to fields.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Sheet selector for multi-sheet XLSX
        if (uiState.sheetNames.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
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

        // Header row selector
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Header row: ", style = MaterialTheme.typography.bodyMedium)
            for (i in 0 until minOf(10, 20)) {
                FilterChip(selected = i == uiState.headerRow, onClick = { onHeaderChange(i) }, label = { Text("${i + 1}") })
                Spacer(Modifier.width(4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))

        // Show header preview
        if (availableColumns.isNotEmpty()) {
            Text("Header: ${availableColumns.joinToString(" | ")}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
        }

        // Column mapping
        Text("Assign columns:", style = MaterialTheme.typography.titleSmall)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(fieldNames) { field ->
                val colIdx = mapping[field] ?: -1
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(field.replaceFirstChar { it.uppercase() }, modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    for (i in availableColumns.indices) {
                        FilterChip(
                            selected = i == colIdx,
                            onClick = { onMappingChange(mapping + (field to if (i == colIdx) -1 else i)) },
                            label = { Text(availableColumns[i].take(10), style = MaterialTheme.typography.labelSmall) },
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
    onTitleChange: (String) -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    val questions = uiState.questions
    val includedCount = questions.count { it.isIncluded }

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
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(questions.size) { idx ->
                val q = questions[idx]
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                        Checkbox(checked = q.isIncluded, onCheckedChange = { onToggle(idx) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(q.stem.take(120), style = MaterialTheme.typography.bodySmall, maxLines = 3)
                            if (q.options.isNotEmpty())
                                Text(q.options.take(6).joinToString(" | ") { it.text.take(20) }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (q.correctAnswers.isNotEmpty())
                                Text("✓ ${q.correctAnswers.joinToString()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
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