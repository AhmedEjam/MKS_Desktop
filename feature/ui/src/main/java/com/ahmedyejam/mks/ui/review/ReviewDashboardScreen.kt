package com.ahmedyejam.mks.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.data.local.entity.AnnotationEntity
import com.ahmedyejam.mks.data.local.entity.MistakeLogEntryEntity
import com.ahmedyejam.mks.data.review.ReviewQueueItem
import com.ahmedyejam.mks.ui.components.EmptyStateCard
import com.ahmedyejam.mks.ui.components.LoadingErrorState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReviewTab(val title: String) {
    QUEUE("Review Queue"),
    MISTAKES("Mistakes"),
    ANNOTATIONS("Annotations")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDashboardScreen(
    viewModel: ReviewDashboardViewModel,
    focusedMistakeId: Long? = null,
    onBack: () -> Unit,
    onOpenRoute: (Any) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val annotations by viewModel.annotations.collectAsState(initial = emptyList())
    val mistakes by viewModel.mistakes.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(ReviewTab.QUEUE) }
    var itemToSnooze by remember { mutableStateOf<Any?>(null) } // Can be ReviewQueueItem or MistakeLogEntryEntity
    var editingAnnotation by remember { mutableStateOf<AnnotationEntity?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                ReviewTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    ReviewTab.QUEUE -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item { LoadingErrorState(state.isLoading, state.error, viewModel::refresh) }
                            state.knowledgeSummary?.let { summary ->
                                item { KnowledgeSummaryCard(summary) }
                            }
                            if (!state.isLoading && state.queue.isEmpty()) {
                                item { EmptyStateCard("No due reviews", "There are no due review items right now.") }
                            } else {
                                items(state.queue, key = { it.type.name + it.id }) { item ->
                                    ReviewQueueCard(
                                        item = item,
                                        onMarkReviewed = { viewModel.markReviewed(item) },
                                        onSnoozeOneWeek = { viewModel.snoozeOneWeek(item) },
                                        onSnoozeCustom = { itemToSnooze = item },
                                        onOpenRoute = onOpenRoute
                                    )
                                }
                            }
                        }
                    }
                    ReviewTab.MISTAKES -> {
                        if (mistakes.isEmpty()) {
                            EmptyStateCard("No mistakes logged", "Mistakes made during quizzes will appear here.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(mistakes, key = { it.id }) { mistake ->
                                    MistakeCard(
                                        mistake = mistake,
                                        dateFormat = dateFormat,
                                        onToggleFixed = { viewModel.markMistakeFixed(mistake.id) },
                                        onSnooze = { itemToSnooze = mistake },
                                        onDelete = { viewModel.deleteMistake(mistake) }
                                    )
                                }
                            }
                        }
                    }
                    ReviewTab.ANNOTATIONS -> {
                        if (annotations.isEmpty()) {
                            EmptyStateCard("No annotations found", "Highlights and margin notes will show up here.")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(annotations, key = { it.id }) { annotation ->
                                    AnnotationCard(
                                        annotation = annotation,
                                        onEdit = { editingAnnotation = annotation },
                                        onDelete = { viewModel.deleteAnnotation(annotation.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Date Picker Dialog for custom snooze
    if (itemToSnooze != null) {
        CustomDatePickerDialog(
            onDismiss = { itemToSnooze = null },
            onDateSelected = { selectedDateMillis ->
                val delay = (selectedDateMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                when (val item = itemToSnooze) {
                    is ReviewQueueItem -> viewModel.snoozeCustom(item, delay)
                    is MistakeLogEntryEntity -> viewModel.snoozeMistake(item.id, selectedDateMillis)
                }
                itemToSnooze = null
            }
        )
    }

    // Annotation Edit Dialog
    editingAnnotation?.let { annotation ->
        var noteBody by remember { mutableStateOf(annotation.noteBody ?: "") }

        AlertDialog(
            onDismissRequest = { editingAnnotation = null },
            title = { Text("Edit Margin Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!annotation.selectedText.isNullOrBlank()) {
                        Text(
                            text = "Reference Text:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "\"${annotation.selectedText}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = noteBody,
                        onValueChange = { noteBody = it },
                        label = { Text("Note body") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAnnotation(annotation.copy(noteBody = noteBody.trim().takeIf { it.isNotBlank() }))
                        editingAnnotation = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAnnotation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReviewQueueCard(
    item: ReviewQueueItem,
    onMarkReviewed: () -> Unit,
    onSnoozeOneWeek: () -> Unit,
    onSnoozeCustom: () -> Unit,
    onOpenRoute: (Any) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.route != null) { item.route?.let(onOpenRoute) }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = item.type.name.replace('_', ' '),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            if (!item.subtitle.isNullOrBlank()) {
                Text(item.subtitle!!, style = MaterialTheme.typography.bodySmall)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onMarkReviewed) { Text("Reviewed") }
                TextButton(onClick = onSnoozeOneWeek) { Text("Snooze 1 week") }
                IconButton(onClick = onSnoozeCustom) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = "Snooze Custom",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun MistakeCard(
    mistake: MistakeLogEntryEntity,
    dateFormat: SimpleDateFormat,
    onToggleFixed: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mistake.isFixed) "FIXED" else "OPEN MISTAKE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mistake.isFixed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Mistake",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (!mistake.correctConcept.isNullOrBlank()) {
                Text(
                    text = mistake.correctConcept!!,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!mistake.userReason.isNullOrBlank()) {
                Text(
                    text = "User Reason: ${mistake.userReason}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (!mistake.preventionNote.isNullOrBlank()) {
                Text(
                    text = "Prevention Note: ${mistake.preventionNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (mistake.selectedAnswer != null || mistake.correctAnswer != null) {
                Text(
                    text = "Answer: ${mistake.selectedAnswer ?: "N/A"} (Correct: ${mistake.correctAnswer ?: "N/A"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (mistake.reviewAt != null) {
                Text(
                    text = "Review scheduled: ${dateFormat.format(Date(mistake.reviewAt!!))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onToggleFixed) {
                    Text(if (mistake.isFixed) "Reopen" else "Mark Fixed")
                }
                TextButton(onClick = onSnooze) {
                    Text("Snooze Custom")
                }
            }
        }
    }
}

@Composable
private fun AnnotationCard(
    annotation: AnnotationEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Margin Note (${annotation.ownerType})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Annotation",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (!annotation.selectedText.isNullOrBlank()) {
                Text(
                    text = "\"${annotation.selectedText}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!annotation.noteBody.isNullOrBlank()) {
                Text(
                    text = annotation.noteBody!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KnowledgeSummaryCard(summary: com.ahmedyejam.mks.data.repository.KnowledgeSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Knowledge summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryMiniStat("Books", summary.totalBooks, Modifier.weight(1f))
                SummaryMiniStat("Quizzes", summary.totalQuizzes, Modifier.weight(1f))
                SummaryMiniStat("Questions", summary.totalQuestions, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryMiniStat("Marked", summary.markedQuestions, Modifier.weight(1f))
                SummaryMiniStat("Assets", summary.questionsWithAssets, Modifier.weight(1f))
                SummaryMiniStat("Sources", summary.questionsWithSources, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryMiniStat("Flashcards", summary.totalFlashcards, Modifier.weight(1f))
                SummaryMiniStat("Blueprints", summary.totalBlueprints, Modifier.weight(1f))
                SummaryMiniStat("Prompts", summary.promptDecks, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryMiniStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // default to tomorrow
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(it)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
