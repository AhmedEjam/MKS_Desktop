package com.ahmedyejam.mks.ui.quiz

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompilerDialog(
    viewModel: CompilerViewModel,
    books: List<BookEntity>,
    quizzes: List<QuizEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMappingEditor by remember { mutableStateOf(false) }
    var showRangeDialog by remember { mutableStateOf(false) }
    var importTitle by remember { mutableStateOf("") }
    var importBookTitle by remember { mutableStateOf("") }
    var selectedBookId by remember { mutableStateOf<Long?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    var selectedQuestionIndexForAnswer by remember { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.advanced_compiler_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                    actions = {
                        if (uiState.questions.isNotEmpty()) {
                            IconButton(onClick = { showRangeDialog = true }) {
                                Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.range_selection))
                            }
                            Button(onClick = { showSaveDialog = true }) {
                                Text(stringResource(R.string.save_with_count, uiState.questions.count { it.isIncluded }))
                            }
                        }
                    }
                )

                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (uiState.sheetNames.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = uiState.sheetNames.indexOf(uiState.selectedSheet ?: ""),
                        edgePadding = 16.dp
                    ) {
                        uiState.sheetNames.forEach { sheet ->
                            Tab(
                                selected = uiState.selectedSheet == sheet,
                                onClick = { viewModel.onSheetSelected(sheet) },
                                text = { Text(sheet) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.questions.isEmpty() && !uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_questions_parsed))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                        ) {
                            itemsIndexed(
                                items = uiState.questions,
                                key = { index, question -> "${question.sourceLine}_$index" }
                            ) { index, question ->
                                QuestionPreviewItem(
                                    question = question,
                                    onClick = { viewModel.toggleQuestionInclusion(index) },
                                    onLongClick = { selectedQuestionIndexForAnswer = index }
                                )
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { showMappingEditor = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.mapping_label))
                    }
                }
            }
        }
    }

    if (showRangeDialog) {
        RangeSelectionDialog(
            maxQuestions = uiState.questions.size,
            onDismiss = { showRangeDialog = false },
            onApply = { from, to, include ->
                viewModel.toggleQuestionsRange(from, to, include)
                showRangeDialog = false
            }
        )
    }

    selectedQuestionIndexForAnswer?.let { index ->
        val question = uiState.questions[index]
        AnswerSelectionDialog(
            question = question,
            onDismiss = { selectedQuestionIndexForAnswer = null },
            onAnswerSelected = { answerId ->
                viewModel.updateQuestionCorrectAnswer(index, answerId)
                selectedQuestionIndexForAnswer = null
            }
        )
    }

    if (showMappingEditor) {
        MappingEditorDialog(
            uiState = uiState,
            onDismiss = { showMappingEditor = false },
            onUpdateHeaderRow = { viewModel.updateHeaderRow(it) },
            onUpdateMapping = { mapping, options -> 
                viewModel.updateMapping(mapping, options)
            }
        )
    }

    if (showSaveDialog) {
        if (uiState.targetQuizId != null) {
            val targetQuiz = quizzes.find { it.id == uiState.targetQuizId }
            val fallbackTitle = stringResource(R.string.imported_quiz_fallback)
            val noneSelection = stringResource(R.string.none_selection)
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text(stringResource(R.string.import_review_title)) },
                text = {
                    Text(stringResource(R.string.import_into_quiz_confirm, targetQuiz?.title ?: noneSelection))
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.saveParsedQuestions(
                            targetQuiz?.title ?: fallbackTitle,
                            targetQuiz?.bookId,
                            uiState.targetQuizId
                        )
                        showSaveDialog = false
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.import_label))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        } else {
            val newQuizLabel = stringResource(R.string.new_quiz)
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text(stringResource(R.string.save_imported_questions_title)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = importTitle,
                            onValueChange = { importTitle = it },
                            label = { Text(stringResource(R.string.quiz_title_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (selectedBookId == null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = importBookTitle,
                                onValueChange = { importBookTitle = it },
                                label = { Text(stringResource(R.string.book_title_label)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.add_to_book_optional), style = MaterialTheme.typography.labelLarge)
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBookId = null }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedBookId == null, onClick = { selectedBookId = null })
                                    Text(stringResource(R.string.new_book), modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                            items(books, key = { it.id }) { book ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBookId = book.id }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedBookId == book.id, onClick = { selectedBookId = book.id })
                                    Text(book.title, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveParsedQuestions(
                                title = if (importTitle.isBlank()) uiState.selectedSheet ?: newQuizLabel else importTitle,
                                bookId = selectedBookId,
                                newBookTitle = if (selectedBookId == null) importBookTitle else null
                            )
                            showSaveDialog = false
                            onDismiss()
                        },
                        enabled = importTitle.isNotBlank() || uiState.selectedSheet != null
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuestionPreviewItem(
    question: com.ahmedyejam.mks.data.importer.model.ParsedQuestion,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Memoize the card color and alpha to avoid re-calculating on every recomposition if possible
    val containerColor = if (question.isIncluded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    val alpha = if (question.isIncluded) 1f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .alpha(alpha)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${stringResource(R.string.line_prefix, question.sourceLine)}: ${question.stem}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = if (question.isIncluded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            question.options.forEach { option ->
                val isCorrect = question.correctAnswers.contains(option.id)
                Text(
                    text = "${option.id.removePrefix("opt_")}: ${option.text}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCorrect) Color(0xFF4CAF50) else if (question.isIncluded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (question.imageDataUrl != null) {
                Text(
                    text = stringResource(R.string.image_attached_indicator),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!question.isIncluded) {
                Text(
                    text = stringResource(R.string.excluded_from_import),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
        title = { Text(stringResource(R.string.select_range_title)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = fromText,
                        onValueChange = { fromText = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.from_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = toText,
                        onValueChange = { toText = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.to_label)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = include, onClick = { include = true })
                    Text(stringResource(R.string.include_label), modifier = Modifier.padding(start = 8.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !include, onClick = { include = false })
                    Text(stringResource(R.string.exclude_label), modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val from = fromText.toIntOrNull() ?: 1
                    val to = toText.toIntOrNull() ?: maxQuestions
                    onApply(from, to, include)
                }
            ) {
                Text(stringResource(R.string.apply_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun AnswerSelectionDialog(
    question: com.ahmedyejam.mks.data.importer.model.ParsedQuestion,
    onDismiss: () -> Unit,
    onAnswerSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_correct_answer_title)) },
        text = {
            LazyColumn {
                items(question.options, key = { it.id }) { option ->
                    val isSelected = question.correctAnswers.contains(option.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAnswerSelected(option.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = { onAnswerSelected(option.id) })
                        Text(
                            text = "${option.id.removePrefix("opt_")}: ${option.text}",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingEditorDialog(
    uiState: CompilerUiState,
    onDismiss: () -> Unit,
    onUpdateHeaderRow: (Int) -> Unit,
    onUpdateMapping: (Map<String, Int>, List<Int>) -> Unit
) {
    var currentMapping by remember { mutableStateOf(uiState.mapping) }
    var currentOptions by remember { mutableStateOf(uiState.optionColumns) }

    val stemLabel = stringResource(R.string.stem_label)
    val correctAnswerLabel = stringResource(R.string.correct_answer_label)
    val explanationLabel = stringResource(R.string.explanation_label)
    val hintLabel = stringResource(R.string.hint_label)
    val imageLabel = stringResource(R.string.image_label)
    val categoriesLabel = stringResource(R.string.categories_label)
    val referenceLabel = stringResource(R.string.reference_label)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mapping_settings_title)) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(stringResource(R.string.header_row_index_label), style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (uiState.headerRow > 0) onUpdateHeaderRow(uiState.headerRow - 1) }) {
                            Icon(Icons.Default.Remove, null)
                        }
                        Text(
                            text = uiState.headerRow.toString(),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(onClick = { onUpdateHeaderRow(uiState.headerRow + 1) }) {
                            Icon(Icons.Default.Add, null)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                val fields = listOf(
                    "question" to stemLabel,
                    "answer" to correctAnswerLabel,
                    "explanation" to explanationLabel,
                    "hint" to hintLabel,
                    "image" to imageLabel,
                    "categories" to categoriesLabel,
                    "reference" to referenceLabel
                )

                items(fields, key = { it.first }) { (key, label) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                        ColumnSelection(
                            availableColumns = uiState.availableColumns,
                            selectedIdx = currentMapping[key] ?: -1,
                            onSelect = { idx ->
                                currentMapping = currentMapping.toMutableMap().apply {
                                    if (idx == -1) remove(key) else put(key, idx)
                                }
                            }
                        )
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.option_columns_label), style = MaterialTheme.typography.labelLarge)
                    uiState.availableColumns.forEachIndexed { idx, name ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentOptions = if (currentOptions.contains(idx)) {
                                        currentOptions - idx
                                    } else {
                                        (currentOptions + idx).sorted()
                                    }
                                }
                        ) {
                            Checkbox(
                                checked = currentOptions.contains(idx),
                                onCheckedChange = {
                                    currentOptions = if (it) currentOptions + idx else currentOptions - idx
                                }
                            )
                            Text("$idx: $name", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onUpdateMapping(currentMapping, currentOptions)
                onDismiss()
            }) {
                Text(stringResource(R.string.apply_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ColumnSelection(
    availableColumns: List<String>,
    selectedIdx: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (selectedIdx == -1) stringResource(R.string.none_selection) else "$selectedIdx: ${availableColumns.getOrNull(selectedIdx) ?: ""}",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.none_selection)) },
                onClick = { onSelect(-1); expanded = false }
            )
            availableColumns.forEachIndexed { index, s ->
                DropdownMenuItem(
                    text = { Text("$index: $s") },
                    onClick = { onSelect(index); expanded = false }
                )
            }
        }
    }
}
