package com.ahmedyejam.mks.ui.review

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.data.repository.KnowledgeSummary
import com.ahmedyejam.mks.data.review.ReviewQueueItem
import com.ahmedyejam.mks.data.review.ReviewQueueType
import com.ahmedyejam.mks.ui.components.EmptyStateCard
import com.ahmedyejam.mks.ui.components.LoadingErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedReviewDashboardScreen(
    viewModel: ReviewDashboardViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LoadingErrorState(state.isLoading, state.error, viewModel::refresh)
                }

                state.knowledgeSummary?.let { summary ->
                    item {
                        KnowledgeSummaryCard(summary)
                    }
                }

                if (!state.isLoading && state.queue.isEmpty()) {
                    item {
                        EmptyStateCard("All caught up!", "No items due for review right now.")
                    }
                } else {
                    items(state.queue, key = { it.type.name + it.id }) { item ->
                        ReviewQueueCard(
                            item = item,
                            onMarkReviewed = { viewModel.markReviewed(item) },
                            onSnooze = { viewModel.snooze(item, 7L * 24 * 60 * 60 * 1000) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewQueueCard(
    item: ReviewQueueItem,
    onMarkReviewed: () -> Unit,
    onSnooze: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.type.name.replace('_', ' '),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!item.subtitle.isNullOrBlank()) {
                Text(item.subtitle!!, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onMarkReviewed) { Text("Reviewed") }
                OutlinedButton(onClick = onSnooze) { Text("Snooze 1 week") }
            }
        }
    }
}

@Composable
private fun KnowledgeSummaryCard(summary: KnowledgeSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Knowledge Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMiniStat("Books", summary.totalBooks)
                SummaryMiniStat("Quizzes", summary.totalQuizzes)
                SummaryMiniStat("Questions", summary.totalQuestions)
                SummaryMiniStat("Marked", summary.markedQuestions)
                SummaryMiniStat("Flashcards", summary.totalFlashcards)
                SummaryMiniStat("Blueprints", summary.totalBlueprints)
                SummaryMiniStat("Open Mistakes", summary.openMistakes)
            }
        }
    }
}

@Composable
private fun SummaryMiniStat(label: String, value: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.widthIn(min = 100.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
