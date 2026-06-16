package com.ahmedyejam.mks.ui.booktools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.repository.BookKnowledgeSummary
import com.ahmedyejam.mks.ui.components.EntityEditDialog
import com.ahmedyejam.mks.ui.library.components.CreateQuizDialog
import com.ahmedyejam.mks.ui.review.CustomDatePickerDialog
import com.ahmedyejam.mks.ui.theme.LocalMksDesignTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookKnowledgeDashboardScreen(
    bookId: Long,
    viewModel: BookToolsViewModel,
    onBack: () -> Unit,
    onOpenQuiz: (Long) -> Unit,
    onOpenFlashcard: (Long) -> Unit,
    onOpenSlideshow: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenPrompt: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val tabs = remember { BookTab.entries.toTypedArray() }
    val pagerState = rememberPagerState(initialPage = tabs.indexOf(BookTab.QUIZZES).takeIf { it >= 0 } ?: 0) { tabs.size }

    var showCreateQuiz by remember { mutableStateOf(false) }
    var showCreateSlideshow by remember { mutableStateOf(false) }
    var showCreateFlashcard by remember { mutableStateOf(false) }
    var showCreateNote by remember { mutableStateOf(false) }
    var showCreatePrompt by remember { mutableStateOf(false) }
    var showCreateSource by remember { mutableStateOf(false) }

    var editingSlideshow by remember { mutableStateOf<com.ahmedyejam.mks.data.local.entity.SlideshowCourseEntity?>(null) }
    var editingNote by remember { mutableStateOf<com.ahmedyejam.mks.data.local.entity.NoteBlueprintEntity?>(null) }
    var editingPrompt by remember { mutableStateOf<com.ahmedyejam.mks.data.local.entity.PromptDeckEntity?>(null) }
    var editingSource by remember { mutableStateOf<com.ahmedyejam.mks.data.local.entity.SourceDocumentEntity?>(null) }
    var itemToSnooze by remember { mutableStateOf<com.ahmedyejam.mks.data.local.entity.MistakeLogEntryEntity?>(null) }

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = { Text(uiState.book?.title ?: "Book") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    divider = {},
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(tab.title) },
                            icon = { Icon(tab.icon, null, Modifier.size(20.dp)) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val currentTab = tabs[pagerState.currentPage]
            if (currentTab != BookTab.DASHBOARD && currentTab != BookTab.MISTAKES) {
                FloatingActionButton(
                    onClick = {
                        when (currentTab) {
                            BookTab.QUIZZES -> showCreateQuiz = true
                            BookTab.SLIDES -> showCreateSlideshow = true
                            BookTab.FLASHCARDS -> showCreateFlashcard = true
                            BookTab.NOTES -> showCreateNote = true
                            BookTab.PROMPTS -> showCreatePrompt = true
                            BookTab.SOURCES -> showCreateSource = true
                            else -> {}
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create ${currentTab.title}")
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                beyondViewportPageCount = 1
            ) { page ->
                when (tabs[page]) {
                    BookTab.DASHBOARD -> DashboardTab(uiState.bookSummary, bookId, viewModel)
                    
                    BookTab.SLIDES -> SlidesTab(
                        uiState.allCourses,
                        onOpenSlideshow,
                        { editingSlideshow = it },
                        { viewModel.deleteSlideshowCourse(it) })

                    BookTab.QUIZZES -> QuizzesTab(
                        uiState.quizzes,
                        onOpenQuiz,
                        { viewModel.deleteQuiz(it) })

                    BookTab.NOTES -> NotesTab(
                        uiState.allNotes,
                        onOpenNote,
                        { editingNote = it },
                        { viewModel.deleteNote(it) })

                    BookTab.MISTAKES -> MistakesTab(
                        uiState.mistakes,
                        { viewModel.toggleMistakeFixed(it) },
                        { itemToSnooze = it },
                        { viewModel.deleteMistake(it) })

                    BookTab.FLASHCARDS -> FlashcardsTab(
                        uiState.flashcardDecks,
                        onOpenFlashcard,
                        { viewModel.deleteFlashcardDeck(it) })

                    BookTab.PROMPTS -> PromptsTab(
                        uiState.allPromptDecks,
                        onOpenPrompt,
                        { editingPrompt = it },
                        { viewModel.deletePromptDeck(it) })

                    BookTab.SOURCES -> SourcesTab(
                        uiState.allSources,
                        { editingSource = it },
                        { viewModel.deleteSource(it) })
                }
            }
        }
    }

    if (showCreateQuiz) {
        CreateQuizDialog(
            quizzes = uiState.quizzes,
            categories = uiState.questions.flatMap { it.categories }.distinct(),
            onDismiss = { showCreateQuiz = false },
            onConfirm = { title, description, coverImage, sourceQuizIds, sourceCategories, filters ->
                viewModel.createNewQuiz(
                    bookId,
                    title,
                    description,
                    coverImage,
                    sourceQuizIds,
                    sourceCategories,
                    filters
                )
                showCreateQuiz = false
            }
        )
    }

    if (showCreateSlideshow) {
        EntityEditDialog(
            title = stringResource(R.string.create_study_slides),
            titleLabel = "Course title",
            descriptionLabel = "Description",
            onDismiss = { showCreateSlideshow = false },
            onSave = { title, body, _ ->
                viewModel.createSlideshowCourse(bookId, title, body.takeIf { it.isNotBlank() })
                showCreateSlideshow = false
            }
        )
    }

    if (showCreateFlashcard) {
        EntityEditDialog(
            title = stringResource(R.string.flashcard_deck),
            initialName = "",
            initialDescription = "",
            initialImage = "",
            titleLabel = stringResource(R.string.name_label),
            descriptionLabel = stringResource(R.string.description_label),
            showImage = true,
            allowBlankName = true,
            onDismiss = { showCreateFlashcard = false },
            onSave = { title, desc, cover ->
                viewModel.createFlashcardDeckFromBook(
                    bookId = bookId,
                    title = title,
                    description = desc,
                    coverUri = cover.ifBlank { null },
                    onCreated = { onOpenFlashcard(it) }
                )
                showCreateFlashcard = false
            }
        )
    }

    if (showCreateNote) {
        ArticleCreateDialog(
            onDismiss = { showCreateNote = false },
            onCreate = { title, body, mode ->
                viewModel.createNote(bookId, title, body, mode = mode)
                showCreateNote = false
            },
            onMarked = {
                viewModel.createBlueprintFromMarked(bookId, "Marked questions article")
                showCreateNote = false
            },
            onMissed = {
                viewModel.createBlueprintFromMissed(bookId, "Missed questions article")
                showCreateNote = false
            }
        )
    }

    if (showCreatePrompt) {
        PromptDeckCreateDialog(
            onDismiss = { showCreatePrompt = false },
            onCreate = { title, description, fromTemplates ->
                viewModel.createPromptDeck(bookId, title, description.takeIf { it.isNotBlank() }, seedDefaultCards = fromTemplates)
                showCreatePrompt = false
            }
        )
    }

    if (showCreateSource) {
        SourceDocumentDialog(
            title = "Create source",
            confirmLabel = "Create",
            onDismiss = { showCreateSource = false },
        ) { title, type, details, url ->
            viewModel.createSource(bookId, title, type, details, url)
            showCreateSource = false
        }
    }

    if (itemToSnooze != null) {
        CustomDatePickerDialog(
            onDismiss = { itemToSnooze = null },
            onDateSelected = { selectedDateMillis ->
                itemToSnooze?.let { mistake ->
                    viewModel.snoozeMistake(mistake.id, selectedDateMillis)
                }
                itemToSnooze = null
            }
        )
    }

    editingSlideshow?.let { course ->
        EntityEditDialog(
            title = stringResource(R.string.edit),
            initialName = course.title,
            initialDescription = course.description ?: "",
            titleLabel = "Course title",
            descriptionLabel = "Description",
            onDismiss = { editingSlideshow = null },
            onSave = { title, body, _ ->
                viewModel.updateSlideshowCourse(course.copy(title = title, description = body.takeIf { it.isNotBlank() }))
                editingSlideshow = null
            }
        )
    }

    editingNote?.let { note ->
        EntityEditDialog(
            title = stringResource(R.string.edit),
            initialName = note.title,
            initialDescription = "",
            titleLabel = "Note title",
            descriptionLabel = "",
            onDismiss = { editingNote = null },
            onSave = { title, _, _ ->
                viewModel.updateNote(note.copy(title = title))
                editingNote = null
            }
        )
    }

    editingPrompt?.let { prompt ->
        EntityEditDialog(
            title = stringResource(R.string.edit),
            initialName = prompt.title,
            initialDescription = prompt.description ?: "",
            titleLabel = "Deck name",
            descriptionLabel = "Description",
            onDismiss = { editingPrompt = null },
            onSave = { title, body, _ ->
                viewModel.updatePromptDeck(prompt.copy(title = title, description = body.takeIf { it.isNotBlank() }))
                editingPrompt = null
            }
        )
    }

    editingSource?.let { source ->
        SourceDocumentDialog(
            title = stringResource(R.string.edit),
            confirmLabel = "Save",
            initialTitle = source.title,
            initialType = source.sourceType,
            initialDetails = source.description.orEmpty(),
            initialUrl = source.externalUrl ?: source.localPath ?: "",
            onDismiss = { editingSource = null },
        ) { title, type, details, url ->
            viewModel.updateSource(source.copy(
                title = title, 
                sourceType = type, 
                description = details.takeIf { it.isNotBlank() },
                externalUrl = url.takeIf { it.isNotBlank() && (it.startsWith("http") || it.startsWith("content://")) },
                localPath = url.takeIf { it.isNotBlank() && !(it.startsWith("http") || it.startsWith("content://")) }
            ))
            editingSource = null
        }
    }
}

@Composable
fun DashboardSummaryCard(summary: BookKnowledgeSummary?) {
    summary ?: return
    val tokens = LocalMksDesignTokens.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Assessment, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Study Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            val progress = if (summary.totalQuestions == 0) 0f else 
                (summary.totalQuestions - summary.unansweredQuestions).toFloat() / summary.totalQuestions
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Completion", style = MaterialTheme.typography.bodySmall)
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SmallStat("Due", summary.reviewSchedulesDue.toString(), MaterialTheme.colorScheme.error)
                SmallStat("Weak", summary.weakQuestions.toString(), MaterialTheme.colorScheme.secondary)
                SmallStat("Marked", summary.markedQuestions.toString(), MaterialTheme.colorScheme.tertiary)
                SmallStat("Mistakes", summary.openMistakes.toString(), MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun SmallStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MagicActionsSection(
    bookId: Long,
    summary: BookKnowledgeSummary?,
    viewModel: BookToolsViewModel
) {
    summary ?: return
    val uiState by viewModel.uiState.collectAsState()
    val questions = uiState.questions

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Magic Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (summary.markedQuestions > 0) {
                MagicActionChip(
                    label = "Draft Note from Marked",
                    icon = Icons.Rounded.AutoFixHigh,
                    onClick = { viewModel.createBlueprintFromMarked(bookId, "Marked Review - ${System.currentTimeMillis() % 10000}") }
                )
                MagicActionChip(
                    label = "Cards from Marked",
                    icon = Icons.Rounded.ViewCarousel,
                    onClick = { viewModel.createFlashcardDeckFromMarkedQuestions(bookId, "Marked Cards - ${System.currentTimeMillis() % 10000}") }
                )
            }
            if (summary.openMistakes > 0) {
                MagicActionChip(
                    label = "Note from Mistakes",
                    icon = Icons.Rounded.HistoryEdu,
                    onClick = { viewModel.createBlueprintFromMissed(bookId, "Mistakes Summary - ${System.currentTimeMillis() % 10000}") }
                )
                MagicActionChip(
                    label = "Cards from Mistakes",
                    icon = Icons.Rounded.ViewCarousel,
                    onClick = { viewModel.createFlashcardDeckFromMissedQuestions(bookId, "Mistakes Cards - ${System.currentTimeMillis() % 10000}") }
                )
            }
            if (questions.isNotEmpty()) {
                MagicActionChip(
                    label = "Note from Questions",
                    icon = Icons.Rounded.HistoryEdu,
                    onClick = { viewModel.createBlueprintFromQuestions(bookId, "Study Note - ${System.currentTimeMillis() % 10000}", questions.map { it.id }) }
                )
                MagicActionChip(
                    label = "Cards from Questions",
                    icon = Icons.Rounded.ViewCarousel,
                    onClick = { viewModel.createFlashcardDeckFromQuestions(bookId, "Study Cards - ${System.currentTimeMillis() % 10000}", questions.map { it.id }) }
                )
                MagicActionChip(
                    label = stringResource(R.string.generate_from_questions),
                    icon = Icons.Rounded.Slideshow,
                    onClick = { viewModel.createSlideshowCourseFromQuestions(bookId, "Study Slides - ${System.currentTimeMillis() % 10000}", questions.map { it.id }) }
                )
            }
        }
    }
}

@Composable
private fun MagicActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    FilterChip(
        selected = true,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Suppress("unused")
@Composable
private fun ToolCard(
    title: String,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val tokens = LocalMksDesignTokens.current
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(tokens.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("$count items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PromptDeckCreateDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, fromTemplates: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var fromTemplates by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create prompt deck") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Deck title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fromTemplates = !fromTemplates }
                        .padding(vertical = 4.dp)
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = fromTemplates, 
                        onCheckedChange = { fromTemplates = it }
                    )
                    Text("Seed with best-in-class templates")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title, description, fromTemplates) },
                enabled = title.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
