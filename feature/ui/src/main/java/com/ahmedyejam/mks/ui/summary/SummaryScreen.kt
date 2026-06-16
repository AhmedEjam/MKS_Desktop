package com.ahmedyejam.mks.ui.summary

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.SessionEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel,
    sessionId: Long,
    onHomeClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val showDetailSettingsState = remember { mutableStateOf(value = false) }
    var showDetailSettings by showDetailSettingsState
    var clearMarksPreviewText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionId) {
        viewModel.loadSummary(sessionId)
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val session = state.session ?: return
    val totalCount = session.originalQuestionCount.coerceAtLeast(state.questions.size).coerceAtLeast(1)
    val percentage = ((session.score.toFloat() / totalCount) * 100).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.session_summary_title)) },
                actions = {
                    IconButton(onClick = {
                        viewModel.previewClearMarks(session.quizId) { preview ->
                            clearMarksPreviewText = preview
                        }
                    }) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Clear Marks")
                    }
                    IconButton(onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, viewModel.getExportText())
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.export_session_review))
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export_label))
                    }
                    IconButton(onClick = onHomeClick) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.correct_summary, session.score, totalCount),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (state.maxStreak > 0) {
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Whatshot,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (MaterialTheme.colorScheme.primaryContainer == Color(0xFFFFB300)) Color.Black else Color(0xFFFF9800)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.best_streak, state.maxStreak),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (MaterialTheme.colorScheme.primaryContainer == Color(0xFFFFB300)) Color.Black else Color(0xFFFF9800),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (session.label.isNotBlank()) {
                            Text(
                                text = session.label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        
                        // Time per question metric
                        val totalTimeMs = state.questions.sumOf { it.timeSpentMs }
                        val avgTimeSec = if (state.questions.isNotEmpty()) (totalTimeMs / 1000.0 / state.questions.size) else 0.0
                        Text(
                            text = "Avg Time: ${String.format(java.util.Locale.US, "%.1f", avgTimeSec)}s / question",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.question_review_header), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showDetailSettings = !showDetailSettings }) {
                            Icon(
                                if (showDetailSettings) Icons.Default.Settings else Icons.Default.SettingsSuggest,
                                contentDescription = stringResource(R.string.detail_settings),
                                tint = if (showDetailSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showDetailSettings,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(stringResource(R.string.visible_details), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ReviewDetail.entries.forEach { detail ->
                                        val label = when (detail) {
                                            ReviewDetail.STEM -> stringResource(R.string.review_detail_stem)
                                            ReviewDetail.OPTIONS -> stringResource(R.string.review_detail_options)
                                            ReviewDetail.HINT -> stringResource(R.string.review_detail_hint)
                                            ReviewDetail.HIGH_YIELD -> stringResource(R.string.review_detail_high_yield)
                                            ReviewDetail.REFERENCE -> stringResource(R.string.review_detail_reference)
                                            ReviewDetail.QUESTION_NUMBER -> stringResource(R.string.review_detail_question_number)
                                            ReviewDetail.EXPLANATION -> stringResource(R.string.review_detail_explanation)
                                        }
                                        FilterChip(
                                            selected = state.visibleDetails.contains(detail),
                                            onClick = { viewModel.toggleReviewDetail(detail) },
                                            label = { Text(label) },
                                            leadingIcon = {
                                                if (state.visibleDetails.contains(detail)) {
                                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReviewFilter.entries.forEach { filter ->
                            val label = when (filter) {
                                ReviewFilter.ALL -> stringResource(R.string.review_filter_all)
                                ReviewFilter.CORRECT -> stringResource(R.string.review_filter_correct)
                                ReviewFilter.WRONG -> stringResource(R.string.review_filter_wrong)
                                ReviewFilter.UNANSWERED -> stringResource(R.string.review_filter_unanswered)
                                ReviewFilter.DROPPED -> stringResource(R.string.review_filter_dropped)
                                ReviewFilter.WITH_EXPLANATION -> stringResource(R.string.review_filter_with_explanation)
                            }
                            FilterChip(
                                selected = state.reviewFilter == filter,
                                onClick = { viewModel.setReviewFilter(filter) },
                                label = { Text(label) },
                                leadingIcon = {
                                    val icon = when (filter) {
                                        ReviewFilter.ALL -> Icons.Default.AllInclusive
                                        ReviewFilter.CORRECT -> Icons.Default.CheckCircle
                                        ReviewFilter.WRONG -> Icons.Default.Cancel
                                        ReviewFilter.UNANSWERED -> Icons.AutoMirrored.Filled.Help
                                        ReviewFilter.DROPPED -> Icons.Default.DeleteSweep
                                        ReviewFilter.WITH_EXPLANATION -> Icons.Default.Lightbulb
                                    }
                                    Icon(icon, null, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }
                }
            }

            if (state.filteredQuestions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.no_questions_match_filter),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(state.filteredQuestions) { index, question ->
                    ReviewQuestionItem(
                        question = question,
                        session = session,
                        visibleDetails = state.visibleDetails,
                        index = index
                    )
                }
            }

            if (state.categoryPerformance.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.category_performance), style = MaterialTheme.typography.titleMedium)
                }
                items(state.categoryPerformance.toList(), key = { it.first }) { (category, stats) ->
                    CategoryStatsRow(category, stats)
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetryClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.retry_full))
                    }

                    Button(
                        onClick = onHomeClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.library))
                    }
                }
            }
        }
    }

    clearMarksPreviewText?.let { preview ->
        AlertDialog(
            onDismissRequest = { clearMarksPreviewText = null },
            title = { Text("Clear Marks for this Quiz?") },
            text = { Text(preview) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.applyClearMarks(session.quizId)
                        clearMarksPreviewText = null
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { clearMarksPreviewText = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReviewQuestionItem(
    question: QuestionEntity,
    session: SessionEntity,
    visibleDetails: Set<ReviewDetail>,
    index: Int
) {
    val userAnswers = session.answers[question.id]
    val isCorrect = userAnswers != null && question.correctAnswers.toSet() == userAnswers.toSet()
    
    val statusColor = when {
        question.isDropped -> MaterialTheme.colorScheme.onSurfaceVariant
        userAnswers == null -> MaterialTheme.colorScheme.onSurfaceVariant
        isCorrect -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (visibleDetails.contains(ReviewDetail.QUESTION_NUMBER)) {
                    Text(
                        stringResource(R.string.question_number_prefix, index + 1),
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                val statusText = when {
                    question.isDropped -> stringResource(R.string.drop).uppercase()
                    userAnswers == null -> stringResource(R.string.navigation_unanswered).uppercase()
                    isCorrect -> stringResource(R.string.navigation_answered).uppercase()
                    else -> stringResource(R.string.navigation_missed).uppercase()
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }

            if (visibleDetails.contains(ReviewDetail.STEM)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = question.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            if (visibleDetails.contains(ReviewDetail.OPTIONS)) {
                Spacer(modifier = Modifier.height(12.dp))
                question.options.forEachIndexed { optIdx, option ->
                    val isUserSelected = userAnswers?.contains(optIdx) == true
                    val isCorrectOption = question.correctAnswers.contains(optIdx)
                    
                    val optionBgColor = when {
                        isCorrectOption -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        isUserSelected -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else -> Color.Transparent
                    }
                    
                    val optionBorderColor = when {
                        isCorrectOption -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                        isUserSelected -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = optionBgColor,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, optionBorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    isCorrectOption -> Icons.Default.CheckCircle
                                    isUserSelected -> Icons.Default.Cancel
                                    else -> Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = when {
                                    isCorrectOption -> Color(0xFF4CAF50)
                                    isUserSelected -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCorrectOption || isUserSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (visibleDetails.contains(ReviewDetail.EXPLANATION) && !question.explanation.isNullOrBlank()) {
                DetailSection("Explanation", question.explanation!!, Icons.Default.Lightbulb)
            }

            if (visibleDetails.contains(ReviewDetail.HINT) && !question.hint.isNullOrBlank()) {
                DetailSection("Hint", question.hint!!, Icons.Default.TipsAndUpdates)
            }

            if (visibleDetails.contains(ReviewDetail.HIGH_YIELD) && !question.additionalInfo.isNullOrBlank()) {
                DetailSection("High Yield Info", question.additionalInfo!!, Icons.Default.Star)
            }

            if (visibleDetails.contains(ReviewDetail.REFERENCE) && !question.reference.isNullOrBlank()) {
                DetailSection("Reference", question.reference!!, Icons.AutoMirrored.Filled.MenuBook)
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryStatsRow(category: String, stats: CategoryStats) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category, fontWeight = FontWeight.Bold, color = contentColor)
                Text("${stats.correct}/${stats.total} correct", style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.7f))
            }
            Text(
                "${stats.percentage}%",
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    stats.percentage >= 70 -> Color(0xFF4CAF50)
                    stats.percentage >= 40 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }
            )
        }
    }
}

// Deleted HardestQuestionItem
