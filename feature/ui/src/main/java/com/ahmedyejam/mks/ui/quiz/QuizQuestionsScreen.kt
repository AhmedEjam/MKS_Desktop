package com.ahmedyejam.mks.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.repository.QuizKnowledgeSummary
import com.ahmedyejam.mks.ui.category.CreateFlashcardsFromSelectedDialog
import com.ahmedyejam.mks.ui.category.DefaultTopAppBar
import com.ahmedyejam.mks.ui.category.FilterControls
import com.ahmedyejam.mks.ui.category.QuestionAssetsDialog
import com.ahmedyejam.mks.ui.category.QuestionCard
import com.ahmedyejam.mks.ui.category.SelectionTopAppBar
import com.ahmedyejam.mks.ui.scanner.EditQuestionDialog
import com.ahmedyejam.mks.ui.theme.LocalMksDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizQuestionsScreen(
    viewModel: QuizQuestionsViewModel,
    focusedQuestionId: Long? = null,
    isEmbedded: Boolean = false,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<QuestionEntity?>(null) }
    var showAssetsDialog by remember { mutableStateOf<QuestionEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFlashcardDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberLazyListState()
    val isSelectionMode = uiState.selectedQuestionIds.isNotEmpty()

    LaunchedEffect(focusedQuestionId, uiState.filteredQuestions) {
        if (focusedQuestionId != null && uiState.filteredQuestions.isNotEmpty()) {
            val index = uiState.filteredQuestions.indexOfFirst { it.id == focusedQuestionId }
            if (index != -1) {
                scrollState.animateScrollToItem(index + 1)
            }
        }
    }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

    if (showMoveDialog) {
        QuizActionDialog(
            title = stringResource(R.string.move_to_quiz_title),
            onDismiss = { showMoveDialog = false },
            onConfirm = { targetQuizId ->
                viewModel.moveSelectedQuestionsToQuiz(targetQuizId)
                showMoveDialog = false
            },
            viewModel = viewModel
        )
    }

    if (showCopyDialog) {
        QuizActionDialog(
            title = stringResource(R.string.copy_to_quiz),
            onDismiss = { showCopyDialog = false },
            onConfirm = { targetQuizId ->
                viewModel.copySelectedQuestionsToQuiz(targetQuizId)
                showCopyDialog = false
            },
            viewModel = viewModel
        )
    }

    if (showExportDialog) {
        var newQuizTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.export_to_new_quiz)) },
            text = {
                OutlinedTextField(
                    value = newQuizTitle,
                    onValueChange = { newQuizTitle = it },
                    label = { Text(stringResource(R.string.quiz_title_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.exportSelectedQuestionsToNewQuiz(newQuizTitle)
                        showExportDialog = false
                    },
                    enabled = newQuizTitle.isNotBlank()
                ) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showFlashcardDialog) {
        CreateFlashcardsFromSelectedDialog(
            selectedCount = uiState.selectedQuestionIds.size,
            onDismiss = { showFlashcardDialog = false },
            onConfirm = { title, clearMarks ->
                viewModel.createFlashcardsFromSelected(title, clearMarks)
                showFlashcardDialog = false
            }
        )
    }

    if (showAddDialog) {
        EditQuestionDialog(
            question = QuestionEntity(
                quizId = uiState.quiz?.id ?: 0L,
                externalId = java.util.UUID.randomUUID().toString(),
                text = "",
                options = listOf("", "", "", ""),
                correctAnswers = listOf(0),
                type = com.ahmedyejam.mks.data.local.entity.QuestionType.SINGLE_CHOICE
            ),
            onDismiss = { showAddDialog = false },
            onConfirm = {
                viewModel.addQuestion(it)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { question ->
        EditQuestionDialog(
            question = question,
            onDismiss = { showEditDialog = null },
            onConfirm = {
                viewModel.updateQuestion(it)
                showEditDialog = null
            }
        )
    }

    showAssetsDialog?.let { question ->
        val assets by viewModel.assetsForQuestion(question.id).collectAsState(initial = emptyList())
        val bookId = uiState.quiz?.bookId ?: 0L
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
            onDismiss = { showAssetsDialog = null },
            onAddAsset = { viewModel.addQuestionAsset(it) },
            onUpdateAsset = { viewModel.updateQuestionAsset(it) },
            onDeleteAsset = { viewModel.deleteQuestionAsset(it) },
            onAddAnnotation = { selectedText, noteBody, colorLabel -> viewModel.addQuestionAnnotation(bookId, question.id, selectedText, noteBody, colorLabel) },
            onUpdateAnnotation = { viewModel.updateAnnotation(it) },
            onDeleteAnnotation = { viewModel.deleteAnnotation(it) },
            onCreateSourceAndAddAsset = { asset, source -> viewModel.createSourceAndAddQuestionAsset(asset, source) },
            onCreateBlueprintFromQuestion = { viewModel.createBlueprintFromQuestion(bookId, question.id) }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete selected questions?") },
            text = { Text("This will permanently delete ${uiState.selectedQuestionIds.size} selected question(s).") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedQuestions()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (isEmbedded) {
        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    SelectionTopAppBar(
                        selectedCount = uiState.selectedQuestionIds.size,
                        onClearSelection = { viewModel.clearSelection() },
                        onDelete = { showDeleteConfirm = true },
                        onMove = { showMoveDialog = true },
                        onSelectAll = { viewModel.toggleAllSelection() },
                        onCopy = { showCopyDialog = true },
                        onExport = { showExportDialog = true },
                        onCreateFlashcards = { showFlashcardDialog = true },
                        onMarkSelected = { viewModel.markSelectedQuestions(true) },
                        onUnmarkSelected = { viewModel.markSelectedQuestions(false) },
                        showMarkActions = true,
                        scrollBehavior = null
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_question))
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (showFilters) {
                    FilterControls(
                        visibility = uiState.visibility,
                        onToggle = { viewModel.toggleVisibility(it) }
                    )
                }
                QuizQuestionsList(
                    uiState = uiState,
                    scrollState = scrollState,
                    onToggleSelection = { viewModel.toggleSelection(it) },
                    onToggleMarked = { viewModel.toggleMarked(it) },
                    onImageClick = { fullscreenImageUrl = it },
                    onEditClick = { showEditDialog = it },
                    onAssetsClick = { showAssetsDialog = it },
                    onToggleDropped = { viewModel.toggleDropped(it) }
                )
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                if (isSelectionMode) {
                    SelectionTopAppBar(
                        selectedCount = uiState.selectedQuestionIds.size,
                        onClearSelection = { viewModel.clearSelection() },
                        onDelete = { showDeleteConfirm = true },
                        onMove = { showMoveDialog = true },
                        onSelectAll = { viewModel.toggleAllSelection() },
                        onCopy = { showCopyDialog = true },
                        onExport = { showExportDialog = true },
                        onCreateFlashcards = { showFlashcardDialog = true },
                        onMarkSelected = { viewModel.markSelectedQuestions(true) },
                        onUnmarkSelected = { viewModel.markSelectedQuestions(false) },
                        showMarkActions = true,
                        scrollBehavior = scrollBehavior
                    )
                } else {
                    DefaultTopAppBar(
                        categoryName = uiState.quiz?.title ?: stringResource(R.string.quizzes_title),
                        filteredCount = uiState.filteredQuestions.size,
                        totalCount = uiState.allQuestions.size,
                        isSearching = isSearching,
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onToggleSearch = { isSearching = !isSearching },
                        onToggleFilters = { showFilters = !showFilters },
                        onBack = onBack,
                        scrollBehavior = scrollBehavior
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_question))
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
                QuizQuestionsList(
                    uiState = uiState,
                    scrollState = scrollState,
                    onToggleSelection = { viewModel.toggleSelection(it) },
                    onToggleMarked = { viewModel.toggleMarked(it) },
                    onImageClick = { fullscreenImageUrl = it },
                    onEditClick = { showEditDialog = it },
                    onAssetsClick = { showAssetsDialog = it },
                    onToggleDropped = { viewModel.toggleDropped(it) }
                )
            }
        }
    }

    fullscreenImageUrl?.let { data ->
        ZoomableImageDialog(
            imageData = data,
            onDismiss = { fullscreenImageUrl = null }
        )
    }
}

@Composable
private fun QuizQuestionsList(
    uiState: QuizQuestionsUiState,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    onToggleSelection: (Long) -> Unit,
    onToggleMarked: (QuestionEntity) -> Unit,
    onImageClick: (String) -> Unit,
    onEditClick: (QuestionEntity) -> Unit,
    onAssetsClick: (QuestionEntity) -> Unit,
    onToggleDropped: (QuestionEntity) -> Unit
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.filteredQuestions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (uiState.searchQuery.isBlank()) stringResource(R.string.no_questions_found) else stringResource(R.string.no_questions_match_filter))
        }
    } else {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.quizSummary?.let { summary ->
                item { QuizKnowledgeSummaryCard(summary) }
            }
            items(uiState.filteredQuestions, key = { it.id }) { question ->
                QuestionCard(
                    question = question,
                    visibility = uiState.visibility,
                    isSelected = uiState.selectedQuestionIds.contains(question.id),
                    isSelectionMode = uiState.selectedQuestionIds.isNotEmpty(),
                    onToggleSelection = { onToggleSelection(question.id) },
                    onToggleMarked = { onToggleMarked(question) },
                    onImageClick = { onImageClick(it) },
                    onEditClick = { onEditClick(question) },
                    hasAssets = question.id in uiState.questionIdsWithAssets,
                    onAssetsClick = { onAssetsClick(question) },
                    onToggleDropped = { onToggleDropped(question) }
                )
            }
        }
    }
}

@Composable
private fun QuizKnowledgeSummaryCard(summary: QuizKnowledgeSummary) {
    val tokens = LocalMksDesignTokens.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Quiz summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuizSummaryChip("Questions", summary.totalQuestions, Modifier.weight(1f))
                QuizSummaryChip("Marked", summary.markedQuestions, Modifier.weight(1f))
                QuizSummaryChip("Missed", summary.missedQuestions, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuizSummaryChip("Notes", summary.questionsWithNotes, Modifier.weight(1f))
                QuizSummaryChip("Assets", summary.questionsWithAssets, Modifier.weight(1f))
                QuizSummaryChip("Sources", summary.questionsWithSources, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuizSummaryChip(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun QuizActionDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    viewModel: QuizQuestionsViewModel
) {
    val quizzes by viewModel.allQuizzes.collectAsState()
    var selectedQuizId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (quizzes.isEmpty()) {
                Text(stringResource(R.string.no_quizzes_available))
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
                                    quiz.description?.let { desc ->
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
                onClick = { selectedQuizId?.let { onConfirm(it) } },
                enabled = selectedQuizId != null
            ) {
                Text(stringResource(R.string.move))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
