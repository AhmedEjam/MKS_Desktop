package com.ahmedyejam.mks.ui.flashcard

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmedyejam.mks.data.local.entity.FlashcardDeckEntity
import com.ahmedyejam.mks.data.local.entity.FlashcardEntity
import com.ahmedyejam.mks.data.model.FlashcardGenerationConfig
import com.ahmedyejam.mks.ui.quiz.CompilerDialog
import com.ahmedyejam.mks.ui.quiz.CompilerViewModel
import com.ahmedyejam.mks.ui.theme.LocalMksDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardDeckScreen(
    deckId: Long,
    focusedCardId: Long? = null,
    onBack: () -> Unit
) {
    val tokens = LocalMksDesignTokens.current
    val vm: FlashcardDeckViewModel = hiltViewModel()

    val compilerViewModel: CompilerViewModel = hiltViewModel()

    val state by vm.uiState.collectAsStateWithLifecycle()
    val elapsed by vm.elapsedSeconds.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    BackHandler(enabled = state.isStudyMode) {
        vm.setStudyMode(false)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state.isStudyMode) {
        if (state.isStudyMode) {
            vm.startSessionTimer()
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> vm.startSessionTimer()
                    Lifecycle.Event.ON_PAUSE -> vm.pauseSessionTimer()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                vm.pauseSessionTimer()
            }
        } else {
            onDispose {}
        }
    }

    LaunchedEffect(focusedCardId, state.cards) {
        if (focusedCardId != null && state.cards.isNotEmpty()) {
            val index = state.cards.indexOfFirst { it.id == focusedCardId }
            if (index != -1) {
                if (tokens.isPlain) {
                    listState.scrollToItem(index + 1)
                } else {
                    listState.animateScrollToItem(index + 1)
                }
            }
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var showCardEditor by remember { mutableStateOf<FlashcardEntity?>(null) }
    var showAddCard by rememberSaveable { mutableStateOf(false) }
    var showDeckEditor by rememberSaveable { mutableStateOf(false) }
    var showDeckSelectionForMove by rememberSaveable { mutableStateOf(false) }
    var showDeckSelectionForCopy by rememberSaveable { mutableStateOf(false) }
    var showPasteTextDialog by rememberSaveable { mutableStateOf(false) }
    var showCompilerDialog by rememberSaveable { mutableStateOf(false) }
    var showGeneratorDialog by rememberSaveable { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            compilerViewModel.onFileSelected(uri = uri, targetDeckId = deckId)
            showCompilerDialog = true
        }
    }

    LaunchedEffect(deckId) { vm.loadDeck(deckId) }
    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            vm.clearMessage()
        }
    }

    var showAttemptsChip by rememberSaveable { mutableStateOf(true) }
    var showCorrectChip by rememberSaveable { mutableStateOf(true) }
    var showSourceChip by rememberSaveable { mutableStateOf(true) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showExplanation by rememberSaveable { mutableStateOf(false) }
    var showReference by rememberSaveable { mutableStateOf(false) }
    var showHint by rememberSaveable { mutableStateOf(false) }
    var showAdditionalInfo by rememberSaveable { mutableStateOf(false) }

    state.deck?.let { deck ->
        if (showDeckEditor) {
            com.ahmedyejam.mks.ui.components.EntityEditDialog(
                title = "Edit deck",
                initialName = deck.title,
                initialDescription = deck.description ?: "",
                initialImage = deck.coverImage ?: "",
                showImage = true,
                onDismiss = { showDeckEditor = false },
                onSave = { title, desc, image ->
                    vm.updateDeck(title, desc.ifBlank { null }, image.ifBlank { null })
                    showDeckEditor = false
                }
            )
        }
    }

    if (showAddCard || showCardEditor != null) {
        FlashcardEditorDialog(
            card = showCardEditor,
            onDismiss = {
                showAddCard = false
                showCardEditor = null
            },
            onSave = { front, back, hint, tags ->
                val card = showCardEditor
                if (card == null) vm.addCard(front, back, hint, tags) else vm.updateCard(card, front, back, hint, tags)
                showAddCard = false
                showCardEditor = null
            }
        )
    }

    if (showGeneratorDialog) {
        FlashcardGeneratorDialog(
            quizzes = state.quizzes,
            categories = state.categories,
            onDismiss = { showGeneratorDialog = false },
            onConfirm = { source, sourceId, config, clearMarks ->
                when (source) {
                    "ALL" -> vm.generateFromBook(clearMarks, config)
                    "MARKED" -> vm.generateFromMarked("Marked question flashcards", clearMarks, config)
                    "MISSED" -> vm.generateFromMissed(config)
                    "QUIZ" -> sourceId?.toLongOrNull()?.let { vm.generateFromQuiz(it, clearMarks, config) }
                    "CATEGORY" -> sourceId?.let { vm.generateFromCategory(it, clearMarks, config) }
                }
                showGeneratorDialog = false
            }
        )
    }

    if (showDeckSelectionForMove) {
        DeckSelectionDialog(
            title = "Move cards to deck",
            decks = state.availableDecks,
            onDismiss = { showDeckSelectionForMove = false },
            onDeckSelected = { targetDeckId ->
                vm.moveSelectedCards(targetDeckId)
                showDeckSelectionForMove = false
            }
        )
    }

    if (showDeckSelectionForCopy) {
        DeckSelectionDialog(
            title = "Copy cards to deck",
            decks = state.availableDecks,
            onDismiss = { showDeckSelectionForCopy = false },
            onDeckSelected = { targetDeckId ->
                vm.copySelectedCards(targetDeckId)
                showDeckSelectionForCopy = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (state.selectedCardIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${state.selectedCardIds.size} selected") },
                    navigationIcon = { IconButton(onClick = { vm.clearSelection() }) { Icon(Icons.Default.Close, contentDescription = "Clear selection") } },
                    actions = {
                        IconButton(onClick = { vm.selectAllCards() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { showDeckSelectionForMove = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.DriveFileMove,
                                contentDescription = "Move"
                            )
                        }
                        IconButton(onClick = { showDeckSelectionForCopy = true }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        IconButton(onClick = { vm.deleteSelectedCards() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            } else {
                var subtitle: String? = null
                if (state.isStudyMode) {
                    val elapsed by vm.elapsedSeconds.collectAsState()
                    val minutes = elapsed / 60
                    val seconds = elapsed % 60
                    subtitle =
                        String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
                }

                com.ahmedyejam.mks.ui.components.StudyTopAppBar(
                    title = state.deck?.title ?: "Flashcards",
                    subtitle = subtitle,
                    onNavigateBack = onBack,
                    actions = {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showOptionsMenu, onDismissRequest = { showOptionsMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Show Attempts") },
                                onClick = { showAttemptsChip = !showAttemptsChip },
                                trailingIcon = { if (showAttemptsChip) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Show Correct") },
                                onClick = { showCorrectChip = !showCorrectChip },
                                trailingIcon = { if (showCorrectChip) Icon(Icons.Default.Check, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Show Source") },
                                onClick = { showSourceChip = !showSourceChip },
                                trailingIcon = { if (showSourceChip) Icon(Icons.Default.Check, null) }
                            )
                            if (state.isStudyMode) {
                                DropdownMenuItem(
                                    text = { Text("Show Explanation") },
                                    onClick = { showExplanation = !showExplanation },
                                    trailingIcon = { if (showExplanation) Icon(Icons.Default.Check, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Show Reference") },
                                    onClick = { showReference = !showReference },
                                    trailingIcon = { if (showReference) Icon(Icons.Default.Check, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Show Hint") },
                                    onClick = { showHint = !showHint },
                                    trailingIcon = { if (showHint) Icon(Icons.Default.Check, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Show Additional Info") },
                                    onClick = { showAdditionalInfo = !showAdditionalInfo },
                                    trailingIcon = { if (showAdditionalInfo) Icon(Icons.Default.Check, null) }
                                )
                            }
                        }
                        IconButton(onClick = { vm.setStudyMode(!state.isStudyMode) }) {
                            Icon(
                                if (state.isStudyMode) Icons.AutoMirrored.Filled.ViewList else Icons.Default.PlayArrow,
                                contentDescription = "Toggle study mode"
                            )
                        }
                        IconButton(onClick = { showDeckEditor = true }, enabled = state.deck != null) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit deck")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.deck == null -> Text("Deck not found", Modifier.align(Alignment.Center))
                state.isStudyMode -> FlashcardStudyContent(
                    state = state, 
                    viewModel = vm,
                    showExplanation = showExplanation,
                    showReference = showReference,
                    showHint = showHint,
                    showAdditionalInfo = showAdditionalInfo
                )
                else -> FlashcardDeckDetailContent(
                    state = state,
                    showAttemptsChip = showAttemptsChip,
                    showCorrectChip = showCorrectChip,
                    showSourceChip = showSourceChip,
                    onAddCard = { showAddCard = true },
                    onEditCard = { showCardEditor = it },
                    onDeleteCard = { vm.deleteCard(it) },
                    onMoveCard = { card, direction -> vm.moveCard(card, direction) },
                    onGenerateCards = { showGeneratorDialog = true },
                    onStartStudy = { vm.setStudyMode(true) },
                    onToggleSelect = { vm.toggleCardSelection(it) },
                    onImportFile = { filePickerLauncher.launch("*/*") },
                    onPasteText = { showPasteTextDialog = true }
                )
            }
        }
    }

    if (showPasteTextDialog) {
        PasteTextDialog(
            onDismiss = { showPasteTextDialog = false },
            onImport = { text, mode ->
                vm.importFromText(text, mode)
                showPasteTextDialog = false
            }
        )
    }

    if (showCompilerDialog) {
        CompilerDialog(
            viewModel = compilerViewModel,
            books = emptyList(),
            quizzes = emptyList(),
            onDismiss = { showCompilerDialog = false }
        )
    }
}

@Composable
private fun FlashcardDeckDetailContent(
    state: FlashcardDeckUiState,
    showAttemptsChip: Boolean,
    showCorrectChip: Boolean,
    showSourceChip: Boolean,
    onAddCard: () -> Unit,
    onEditCard: (FlashcardEntity) -> Unit,
    onDeleteCard: (FlashcardEntity) -> Unit,
    onMoveCard: (FlashcardEntity, Int) -> Unit,
    onGenerateCards: () -> Unit,
    onStartStudy: () -> Unit,
    onToggleSelect: (Long) -> Unit,
    onImportFile: () -> Unit,
    onPasteText: () -> Unit
) {
    val deck = state.deck ?: return
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DeckStatsCard(deck = deck, cardCount = state.cards.size)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onAddCard, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add card")
                }
                OutlinedButton(onClick = onStartStudy, enabled = state.cards.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Study")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onImportFile, modifier = Modifier.weight(1f)) {
                    Text("Import from file")
                }
                OutlinedButton(onClick = onPasteText, modifier = Modifier.weight(1f)) {
                    Text("Paste text")
                }
            }
        }
        item {
            val tokens = LocalMksDesignTokens.current
            Button(
                onClick = onGenerateCards,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(tokens.cardRadius)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Generate from questions", fontWeight = FontWeight.Bold)
            }
        }
        if (state.cards.isEmpty()) {
            item {
                val tokens = LocalMksDesignTokens.current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(tokens.cardRadius),
                    elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No flashcards yet", fontWeight = FontWeight.SemiBold)
                        Text("Create cards manually or generate them from marked/missed questions.")
                    }
                }
            }
        } else {
            items(state.cards, key = { it.id }) { card ->
                FlashcardListItem(
                    card = card,
                    showAttemptsChip = showAttemptsChip,
                    showCorrectChip = showCorrectChip,
                    showSourceChip = showSourceChip,
                    isSelected = state.selectedCardIds.contains(card.id),
                    isSelectionMode = state.selectedCardIds.isNotEmpty(),
                    onToggleSelect = { onToggleSelect(card.id) },
                    onEdit = { onEditCard(card) },
                    onDelete = { onDeleteCard(card) },
                    onMoveUp = { onMoveCard(card, -1) },
                    onMoveDown = { onMoveCard(card, 1) }
                )
            }
        }
    }
}

@Composable
private fun DeckStatsCard(deck: FlashcardDeckEntity, cardCount: Int) {
    val tokens = LocalMksDesignTokens.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(deck.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            deck.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("$cardCount cards") })
                AssistChip(onClick = {}, label = { Text("${deck.studiedCount} studied") })
                AssistChip(onClick = {}, label = { Text("${(deck.masteryPercentage * 100).toInt()}% mastery") })
            }
            LinearProgressIndicator(progress = { deck.masteryPercentage.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FlashcardListItem(
    card: FlashcardEntity,
    showAttemptsChip: Boolean = true,
    showCorrectChip: Boolean = true,
    showSourceChip: Boolean = true,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val tokens = LocalMksDesignTokens.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect() },
                onLongClick = onToggleSelect
            ),
        shape = RoundedCornerShape(tokens.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                } else {
                    Icon(Icons.Default.Style, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(card.frontText, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text(card.backText, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                if (!isSelectionMode) {
                    IconButton(onClick = onMoveUp) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up") }
                    IconButton(onClick = onMoveDown) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down") }
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showAttemptsChip) AssistChip(onClick = {}, label = { Text("Attempts ${card.attempts}") })
                if (showCorrectChip) AssistChip(onClick = {}, label = { Text("Correct ${card.correctCount}") })
                if (showSourceChip) card.sourceQuestionId?.let { AssistChip(onClick = {}, label = { Text("Source #$it") }) }
            }
        }
    }
}

@Composable
private fun FlashcardStudyContent(
    state: FlashcardDeckUiState,
    viewModel: FlashcardDeckViewModel,
    showExplanation: Boolean,
    showReference: Boolean,
    showHint: Boolean,
    showAdditionalInfo: Boolean
) {
    val tokens = LocalMksDesignTokens.current
    val card = state.currentCard
    if (card == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cards to study")
        }
        return
    }
    
    val fullBackText = card.backText
    var displayText = if (state.isFlipped) fullBackText else card.frontText
    if (state.isFlipped) {
        val sections = fullBackText.split("\n\n")
        displayText = sections.filter { section ->
            when {
                section.startsWith("Explanation\n") -> showExplanation
                section.startsWith("Reference\n") -> showReference
                section.startsWith("Hint\n") -> showHint
                section.startsWith("Additional Info\n") -> showAdditionalInfo
                else -> true
            }
        }.joinToString("\n\n")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("${state.currentIndex + 1} / ${state.cards.size}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 8.dp))
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable { viewModel.flipCard() },
            shape = RoundedCornerShape(tokens.cardRadius),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
        if (!state.isFlipped) {
            Button(onClick = { viewModel.flipCard() }, modifier = Modifier.fillMaxWidth()) { Text("Flip") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { viewModel.rateCurrentCard(FLASHCARD_RATING_AGAIN) }, modifier = Modifier.weight(1f)) { Text("Again") }
                Button(onClick = { viewModel.rateCurrentCard(FLASHCARD_RATING_GOOD) }, modifier = Modifier.weight(1f)) { Text("Good") }
                Button(onClick = { viewModel.rateCurrentCard(FLASHCARD_RATING_EASY) }, modifier = Modifier.weight(1f)) { Text("Easy") }
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { viewModel.previousCard() }) { Text("Previous") }
            TextButton(onClick = { viewModel.nextCard() }) { Text("Next") }
        }
    }
}

@Composable
private fun FlashcardEditorDialog(
    card: FlashcardEntity?,
    onDismiss: () -> Unit,
    onSave: (front: String, back: String, hint: String?, tags: List<String>) -> Unit
) {
    var front by rememberSaveable(card?.id) { mutableStateOf(card?.frontText ?: "") }
    var back by rememberSaveable(card?.id) { mutableStateOf(card?.backText ?: "") }
    var hint by rememberSaveable(card?.id) { mutableStateOf(card?.hint ?: "") }
    var tags by rememberSaveable(card?.id) { mutableStateOf(card?.tags?.joinToString(", ") ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (card == null) "Add flashcard" else "Edit flashcard") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(front, { front = it }, label = { Text("Front") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(back, { back = it }, label = { Text("Back") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                OutlinedTextField(hint, { hint = it }, label = { Text("Hint") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(tags, { tags = it }, label = { Text("Tags, comma separated") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(enabled = front.isNotBlank() && back.isNotBlank(), onClick = {
                onSave(
                    front.trim(),
                    back.trim(),
                    hint.trim().ifBlank { null },
                    tags.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct()
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashcardGeneratorDialog(
    quizzes: List<com.ahmedyejam.mks.data.local.entity.QuizEntity>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (source: String, sourceId: String?, config: FlashcardGenerationConfig, clearMarks: Boolean) -> Unit
) {
    var source by rememberSaveable { mutableStateOf("ALL") } // ALL, MARKED, MISSED, QUIZ, CATEGORY
    var sourceId by rememberSaveable { mutableStateOf<String?>(null) }
    
    var clearMarks by rememberSaveable { mutableStateOf(false) }
    
    var includeStemInFront by rememberSaveable { mutableStateOf(true) }
    var includeOptionsInFront by rememberSaveable { mutableStateOf(false) }
    var includeImageInFront by rememberSaveable { mutableStateOf(false) }
    
    var includeAnswerInBack by rememberSaveable { mutableStateOf(true) }
    var includeExplanationInBack by rememberSaveable { mutableStateOf(true) }
    var includeHintInBack by rememberSaveable { mutableStateOf(false) }
    var includeReferenceInBack by rememberSaveable { mutableStateOf(false) }
    var includeAdditionalInfoInBack by rememberSaveable { mutableStateOf(false) }
    var includeImageInBack by rememberSaveable { mutableStateOf(false) }

    var expandedSource by remember { mutableStateOf(false) }
    var expandedSubSource by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate from questions") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                
                ExposedDropdownMenuBox(
                    expanded = expandedSource,
                    onExpandedChange = { expandedSource = it }
                ) {
                    OutlinedTextField(
                        value = when (source) {
                            "ALL" -> "All Book Questions"
                            "MARKED" -> "Marked Questions"
                            "MISSED" -> "Missed Questions"
                            "QUIZ" -> "Specific Quiz"
                            "CATEGORY" -> "Category"
                            else -> ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSource) },
                        modifier = Modifier
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSource,
                        onDismissRequest = { expandedSource = false }
                    ) {
                        DropdownMenuItem(text = { Text("All Book Questions") }, onClick = { source = "ALL"; sourceId = null; expandedSource = false })
                        DropdownMenuItem(text = { Text("Marked Questions") }, onClick = { source = "MARKED"; sourceId = null; expandedSource = false })
                        DropdownMenuItem(text = { Text("Missed Questions") }, onClick = { source = "MISSED"; sourceId = null; expandedSource = false })
                        if (quizzes.isNotEmpty()) {
                            DropdownMenuItem(text = { Text("Specific Quiz") }, onClick = { source = "QUIZ"; sourceId = quizzes.firstOrNull()?.id?.toString(); expandedSource = false })
                        }
                        if (categories.isNotEmpty()) {
                            DropdownMenuItem(text = { Text("Category") }, onClick = { source = "CATEGORY"; sourceId = categories.firstOrNull(); expandedSource = false })
                        }
                    }
                }

                if (source == "QUIZ") {
                    ExposedDropdownMenuBox(
                        expanded = expandedSubSource,
                        onExpandedChange = { expandedSubSource = it }
                    ) {
                        OutlinedTextField(
                            value = quizzes.find { it.id.toString() == sourceId }?.title ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Quiz") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubSource) },
                            modifier = Modifier
                                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSubSource,
                            onDismissRequest = { expandedSubSource = false }
                        ) {
                            quizzes.forEach { quiz ->
                                DropdownMenuItem(text = { Text(quiz.title) }, onClick = { sourceId = quiz.id.toString(); expandedSubSource = false })
                            }
                        }
                    }
                } else if (source == "CATEGORY") {
                    ExposedDropdownMenuBox(
                        expanded = expandedSubSource,
                        onExpandedChange = { expandedSubSource = it }
                    ) {
                        OutlinedTextField(
                            value = sourceId ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubSource) },
                            modifier = Modifier
                                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSubSource,
                            onDismissRequest = { expandedSubSource = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = { sourceId = cat; expandedSubSource = false })
                            }
                        }
                    }
                }

                if (source != "MISSED") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearMarks, onCheckedChange = { clearMarks = it })
                        Text("Clear marks after conversion")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                
                Text("Front of card", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(includeStemInFront, { includeStemInFront = it }); Text("Question Stem") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(includeOptionsInFront, { includeOptionsInFront = it }); Text("Options") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(includeImageInFront, { includeImageInFront = it }); Text("Image") }
                
                Spacer(Modifier.height(8.dp))
                Text("Back of card", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(includeAnswerInBack, { includeAnswerInBack = it }); Text("Answer") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(includeExplanationInBack, { includeExplanationInBack = it }); Text("Explanation") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(includeHintInBack, { includeHintInBack = it }); Text("Hint") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(includeReferenceInBack, { includeReferenceInBack = it }); Text("Reference") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(includeAdditionalInfoInBack, { includeAdditionalInfoInBack = it }); Text("Additional Info") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(includeImageInBack, { includeImageInBack = it }); Text("Image") }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    source,
                    sourceId,
                    FlashcardGenerationConfig(
                        includeStemInFront, includeOptionsInFront, includeImageInFront,
                        includeAnswerInBack, includeExplanationInBack, includeHintInBack, includeReferenceInBack, includeAdditionalInfoInBack, includeImageInBack
                    ),
                    clearMarks
                )
            }) { Text("Generate") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeckSelectionDialog(
    title: String,
    decks: List<FlashcardDeckEntity>,
    onDismiss: () -> Unit,
    onDeckSelected: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (decks.isEmpty()) {
                Text("No other decks available in this book.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(decks, key = { it.id }) { deck ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeckSelected(deck.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(deck.title, fontWeight = FontWeight.SemiBold)
                                if (!deck.description.isNullOrBlank()) {
                                    Text(deck.description!!, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PasteTextDialog(
    onDismiss: () -> Unit,
    onImport: (String, com.ahmedyejam.mks.data.importer.parser.TextParseMode) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(com.ahmedyejam.mks.data.importer.parser.TextParseMode.ALTERNATING_PARAGRAPHS) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste text to import") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            mode =
                                com.ahmedyejam.mks.data.importer.parser.TextParseMode.ALTERNATING_PARAGRAPHS
                        }) {
                    androidx.compose.material3.RadioButton(
                        selected = mode == com.ahmedyejam.mks.data.importer.parser.TextParseMode.ALTERNATING_PARAGRAPHS,
                        onClick = { mode = com.ahmedyejam.mks.data.importer.parser.TextParseMode.ALTERNATING_PARAGRAPHS }
                    )
                    Text("Alternating Paragraphs (odd front, even back)", style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            mode =
                                com.ahmedyejam.mks.data.importer.parser.TextParseMode.EXPLICIT_LABELS
                        }) {
                    androidx.compose.material3.RadioButton(
                        selected = mode == com.ahmedyejam.mks.data.importer.parser.TextParseMode.EXPLICIT_LABELS,
                        onClick = { mode = com.ahmedyejam.mks.data.importer.parser.TextParseMode.EXPLICIT_LABELS }
                    )
                    Text("Explicit Labels (Front: ..., Back: ...)", style = MaterialTheme.typography.bodySmall)
                }
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Flashcards text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(text, mode) },
                enabled = text.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
