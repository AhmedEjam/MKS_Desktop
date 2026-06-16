package com.ahmedyejam.mks.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.ui.scanner.EditQuestionDialog
import com.ahmedyejam.mks.ui.theme.LocalMksDesignTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryQuestionsScreen(
    viewModel: CategoryQuestionsViewModel,
    onBack: () -> Unit,
    onStartQuiz: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val allQuizzes by viewModel.allQuizzes.collectAsState()
    val showFiltersState = rememberSaveable { mutableStateOf(value = false) }
    var showFilters by showFiltersState
    val isSearchingState = rememberSaveable { mutableStateOf(value = false) }
    var isSearching by isSearchingState
    val showMoveDialogState = remember { mutableStateOf(value = false) }
    var showMoveDialog by showMoveDialogState
    val showCopyDialogState = remember { mutableStateOf(value = false) }
    var showCopyDialog by showCopyDialogState
    val showExportDialogState = remember { mutableStateOf(value = false) }
    var showExportDialog by showExportDialogState
    val showDeleteConfirmState = remember { mutableStateOf(value = false) }
    var showDeleteConfirm by showDeleteConfirmState
    val showEditDialogState = remember { mutableStateOf<QuestionEntity?>(null) }
    var showEditDialog by showEditDialogState
    val showAssetsDialogState = remember { mutableStateOf<QuestionEntity?>(null) }
    var showAssetsDialog by showAssetsDialogState
    val showFlashcardDialogState = remember { mutableStateOf(value = false) }
    var showFlashcardDialog by showFlashcardDialogState

    val scrollState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val isSelectionMode = uiState.selectedQuestionIds.isNotEmpty()
    val fullscreenImageUrlState = remember { mutableStateOf<String?>(null) }
    var fullscreenImageUrl by fullscreenImageUrlState

    if (showMoveDialog) {
        MoveQuestionsDialog(
            title = "Move to Quiz",
            confirmText = "Move",
            onDismiss = { showMoveDialogState.value = false },
            onMove = { targetQuizId ->
                viewModel.moveSelectedQuestionsToQuiz(targetQuizId)
                showMoveDialogState.value = false
            },
            viewModel = viewModel,
        )
    }

    if (showCopyDialog) {
        MoveQuestionsDialog(
            title = "Copy to Quiz",
            confirmText = "Copy",
            onDismiss = { showCopyDialogState.value = false },
            onMove = { targetQuizId ->
                viewModel.copySelectedQuestionsToQuiz(targetQuizId)
                showCopyDialogState.value = false
            },
            viewModel = viewModel,
        )
    }

    if (showExportDialog) {
        var newQuizTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showExportDialogState.value = false },
            title = { Text("Export to New Quiz") },
            text = {
                OutlinedTextField(
                    value = newQuizTitle,
                    onValueChange = { newQuizTitle = it },
                    label = { Text("Quiz title") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.exportSelectedQuestionsToNewQuiz(newQuizTitle)
                        showExportDialogState.value = false
                    },
                    enabled = newQuizTitle.isNotBlank(),
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showExportDialogState.value = false }) { Text("Cancel") } },
        )
    }

    if (showFlashcardDialog) {
        CreateFlashcardsFromSelectedDialog(
            selectedCount = uiState.selectedQuestionIds.size,
            onDismiss = { showFlashcardDialogState.value = false },
        ) { title, clearMarks ->
            viewModel.createFlashcardsFromSelected(title, clearMarks)
            showFlashcardDialogState.value = false
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmState.value = false },
            title = { Text("Delete selected questions?") },
            text = { Text("This will permanently delete ${uiState.selectedQuestionIds.size} selected question(s).") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedQuestions()
                        showDeleteConfirmState.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmState.value = false }) { Text("Cancel") } },
        )
    }

    showEditDialog?.let { question ->
        EditQuestionDialog(
            question = question,
            onDismiss = { showEditDialogState.value = null },
        ) {
            viewModel.updateQuestion(it)
            showEditDialogState.value = null
        }
    }

    showAssetsDialog?.let { question ->
        val assets by viewModel.assetsForQuestion(question.id).collectAsState(initial = emptyList())
        val bookId = allQuizzes.firstOrNull { it.id == question.quizId }?.bookId ?: 0L
        val sources by viewModel.sourceDocumentsForBook(bookId).collectAsState(initial = emptyList())
        val linkedBlueprints by viewModel.linkedBlueprintsForQuestion(bookId, question.id).collectAsState(initial = emptyList())
        val annotations by viewModel.annotationsForQuestion(question.id).collectAsState(initial = emptyList())
        QuestionAssetsDialog(
            question = question,
            bookId = bookId,
            assets = assets,
            annotations = annotations,
            sourceDocuments = sources,
            linkedBlueprints = linkedBlueprints,
            onDismiss = { showAssetsDialogState.value = null },
            onAddAsset = { viewModel.addQuestionAsset(it) },
            onUpdateAsset = { viewModel.updateQuestionAsset(it) },
            onDeleteAsset = { viewModel.deleteQuestionAsset(it) },
            onAddAnnotation = { selectedText, noteBody, colorLabel -> viewModel.addQuestionAnnotation(bookId, question.id, selectedText, noteBody, colorLabel) },
            onUpdateAnnotation = { viewModel.updateAnnotation(it) },
            onDeleteAnnotation = { viewModel.deleteAnnotation(it) },
            onCreateSourceAndAddAsset = { asset, source -> viewModel.createSourceAndAddQuestionAsset(asset, source) },
            onCreateBlueprintFromQuestion = { viewModel.createBlueprintFromQuestion(bookId, question.id) },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (isSelectionMode) {
                SelectionTopAppBar(
                    selectedCount = uiState.selectedQuestionIds.size,
                    onClearSelection = { viewModel.clearSelection() },
                    onDelete = { showDeleteConfirmState.value = true },
                    onMove = { showMoveDialogState.value = true },
                    onSelectAll = { viewModel.toggleAllSelection() },
                    onCopy = { showCopyDialogState.value = true },
                    onExport = { showExportDialogState.value = true },
                    onCreateFlashcards = { showFlashcardDialogState.value = true },
                    onMarkSelected = { viewModel.markSelectedQuestions(marked = true) },
                    onUnmarkSelected = { viewModel.markSelectedQuestions(marked = false) },
                    showMarkActions = true,
                    scrollBehavior = scrollBehavior,
                )
            } else {
                DefaultTopAppBar(
                    categoryName = uiState.categoryName,
                    filteredCount = uiState.filteredQuestions.size,
                    totalCount = uiState.allQuestions.size,
                    isSearching = isSearching,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onToggleSearch = { isSearchingState.value = !isSearching },
                    onToggleFilters = { showFiltersState.value = !showFilters },
                    onBack = onBack,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        floatingActionButton = {
            if (uiState.filteredQuestions.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onStartQuiz(uiState.categoryName) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text("Start Quiz") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (showFilters) {
                FilterControls(
                    visibility = uiState.visibility,
                    onToggle = { viewModel.toggleVisibility(it) }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredQuestions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (uiState.searchQuery.isBlank()) "No questions found for this category." else "No questions match your search.")
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.filteredQuestions, key = { it.id }) { question ->
                        QuestionCard(
                            question = question,
                            visibility = uiState.visibility,
                            isSelected = uiState.selectedQuestionIds.contains(question.id),
                            isSelectionMode = uiState.selectedQuestionIds.isNotEmpty(),
                            onToggleSelection = { viewModel.toggleSelection(question.id) },
                            onToggleMarked = { viewModel.toggleMarked(question) },
                            onImageClick = { fullscreenImageUrlState.value = it },
                            onEditClick = { showEditDialogState.value = question },
                            hasAssets = question.id in uiState.questionIdsWithAssets,
                            onAssetsClick = { showAssetsDialogState.value = question },
                        )
                    }
                }
            }
        }
    }

    fullscreenImageUrl?.let { data ->
        com.ahmedyejam.mks.ui.quiz.ZoomableImageDialog(
            imageData = data,
        ) { fullscreenImageUrlState.value = null }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterControls(
    visibility: VisibilityState,
    onToggle: (VisibilityState.() -> VisibilityState) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        FlowRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = visibility.showStem,
                onClick = { onToggle { copy(showStem = !showStem) } },
                label = { Text("Stem") }
            )
            FilterChip(
                selected = visibility.showOptions,
                onClick = { onToggle { copy(showOptions = !showOptions) } },
                label = { Text("Options") }
            )
            FilterChip(
                selected = visibility.showCorrectAnswer,
                onClick = { onToggle { copy(showCorrectAnswer = !showCorrectAnswer) } },
                label = { Text("Correct Answer") }
            )
            FilterChip(
                selected = visibility.showExplanation,
                onClick = { onToggle { copy(showExplanation = !showExplanation) } },
                label = { Text("Explanation") }
            )
            FilterChip(
                selected = visibility.showHint,
                onClick = { onToggle { copy(showHint = !showHint) } },
                label = { Text("Hint") }
            )
            FilterChip(
                selected = visibility.showReference,
                onClick = { onToggle { copy(showReference = !showReference) } },
                label = { Text("Reference") }
            )
            FilterChip(
                selected = visibility.showAdditionalInfo,
                onClick = { onToggle { copy(showAdditionalInfo = !showAdditionalInfo) } },
                label = { Text("Info") }
            )
            FilterChip(
                selected = visibility.showOnlyMarked,
                onClick = { onToggle { copy(showOnlyMarked = !showOnlyMarked) } },
                label = { Text("Marked Only") },
                leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = visibility.showOnlyWithAssets,
                onClick = { onToggle { copy(showOnlyWithAssets = !showOnlyWithAssets) } },
                label = { Text("Has attachments") },
                leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    onCreateFlashcards: (() -> Unit)? = null,
    onMarkSelected: (() -> Unit)? = null,
    onUnmarkSelected: (() -> Unit)? = null,
    showMarkActions: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = { Text(stringResource(R.string.selected_count_title, selectedCount)) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
            }
            if (showMarkActions && (onMarkSelected != null) && (onUnmarkSelected != null)) {
                IconButton(onClick = onMarkSelected) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Mark selected")
                }
                IconButton(onClick = onUnmarkSelected) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = "Unmark selected")
                }
            }
            IconButton(onClick = onMove) {
                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move")
            }
            onCopy?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                }
            }
            onExport?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Export")
                }
            }
            onCreateFlashcards?.let {
            IconButton(onClick = it) {
                Icon(Icons.Default.Style, contentDescription = "Create flashcards")
            }
        }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultTopAppBar(
    categoryName: String,
    filteredCount: Int,
    totalCount: Int,
    isSearching: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onToggleFilters: () -> Unit,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            if (isSearching) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search questions...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            } else {
                Column {
                    Text(
                        categoryName, 
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (totalCount > 0) {
                        Text(
                            "$filteredCount / $totalCount Questions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    if (isSearching) {
                        onToggleSearch()
                        onSearchQueryChange("")
                    } else {
                        onBack()
                    }
                },
            ) {
                Icon(
                    if (isSearching) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(onClick = onToggleSearch) {
                Icon(if (isSearching) Icons.Default.SearchOff else Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = onToggleFilters) {
                Icon(Icons.Default.FilterList, contentDescription = "Toggle Filters")
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuestionCard(
    question: QuestionEntity,
    visibility: VisibilityState,
    isSelected: Boolean,
    isSelectionMode: Boolean = false,
    onToggleSelection: () -> Unit,
    onToggleMarked: () -> Unit,
    onImageClick: (String) -> Unit,
    onEditClick: () -> Unit = {},
    hasAssets: Boolean = false,
    onAssetsClick: () -> Unit = {},
    onToggleDropped: (() -> Unit)? = null
) {
    val tokens = LocalMksDesignTokens.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelection() else onEditClick() },
                onLongClick = onToggleSelection
            ),
        shape = RoundedCornerShape(tokens.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp)
                        )
                    }
                    if (question.isDropped) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "DROPPED",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onAssetsClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attachments",
                            tint = if (hasAssets) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onToggleMarked, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (question.isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Mark",
                            tint = if (question.isMarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    onToggleDropped?.let { toggleDrop ->
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = toggleDrop, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Drop",
                                tint = if (question.isDropped) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (visibility.showStem && question.text.isNotBlank()) {
                Text(
                    text = question.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            val displayImage = question.imagePath ?: question.imageSource
            if (!displayImage.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                    .data(displayImage)
                    .crossfade(enable = true)
                    .build(),
                    contentDescription = "Question Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onImageClick(displayImage) },
                    contentScale = ContentScale.FillWidth
                )
            }

            if (visibility.showOptions && question.options.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    question.options.forEachIndexed { index, option ->
                        val isCorrect = visibility.showCorrectAnswer && question.correctAnswers.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCorrect) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${'A' + index}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCorrect) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCorrect) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else if (visibility.showCorrectAnswer && question.correctAnswers.isNotEmpty() && !visibility.showOptions) {
                Text(
                    text = "Correct Answer: ${question.correctAnswers.joinToString { index -> ( 'A' + index).toString() }}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (visibility.showHint && !question.hint.isNullOrBlank()) {
                InfoSection(title = "Hint", content = question.hint!!, color = MaterialTheme.colorScheme.secondary)
            }

            if (visibility.showExplanation && !question.explanation.isNullOrBlank()) {
                InfoSection(title = "Explanation", content = question.explanation!!, color = MaterialTheme.colorScheme.tertiary)
            }

            if (visibility.showReference && !question.reference.isNullOrBlank()) {
                InfoSection(title = "Reference", content = question.reference!!, color = MaterialTheme.colorScheme.outline)
            }

            if (visibility.showAdditionalInfo && !question.additionalInfo.isNullOrBlank()) {
                InfoSection(title = "Additional Info", content = question.additionalInfo!!, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun InfoSection(title: String, content: String, color: Color) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun MoveQuestionsDialog(
    title: String = "Move to Quiz",
    confirmText: String = "Move",
    onDismiss: () -> Unit,
    onMove: (Long) -> Unit,
    viewModel: CategoryQuestionsViewModel
) {
    val quizzes by viewModel.allQuizzes.collectAsState()
    var selectedQuizId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (quizzes.isEmpty()) {
                Text("No quizzes available to move to.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quizzes, key = { it.id }) { quiz ->
                        Surface(
                            onClick = { selectedQuizId = quiz.id },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedQuizId == quiz.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedQuizId == quiz.id,
                                    onClick = { selectedQuizId = quiz.id }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(quiz.title, style = MaterialTheme.typography.bodyLarge)
                                    quiz.description.let { desc ->
                                        if (desc.isNotBlank()) {
                                            Text(
                                                desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedQuizId?.let { onMove(it) } },
                enabled = selectedQuizId != null
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


@Composable
fun CreateFlashcardsFromSelectedDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (title: String, clearMarksAfter: Boolean) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("Selected question flashcards") }
    var clearMarks by rememberSaveable { mutableStateOf(value = false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create flashcards") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Create a new detached flashcard deck from $selectedCount selected question(s).")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Deck title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = clearMarks, onCheckedChange = { clearMarks = it })
                    Text("Clear marks after conversion")
                }
            }
        },
        confirmButton = {
            Button(enabled = title.isNotBlank(), onClick = { onConfirm(title.trim(), clearMarks) }) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
