package com.ahmedyejam.mks.ui.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.local.entity.SessionEntity
import com.ahmedyejam.mks.data.preferences.DataStoreManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for managing quiz sessions.
 * Displays a list of existing sessions for a specific quiz and allows starting new ones.
 *
 * @param quizId The ID of the quiz whose sessions are being managed.
 * @param viewModel The [SessionViewModel] handling the data logic.
 * @param dataStoreManager Manager for persistent user preferences.
 * @param onSessionSelected Callback when a session is clicked to resume.
 * @param onNavigateBack Callback to navigate to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionManagementScreen(
    quizId: Long,
    viewModel: SessionViewModel,
    dataStoreManager: DataStoreManager,
    isEmbedded: Boolean = false,
    onSessionSelected: (Long, Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()
    val quizQuestionCounts by viewModel.quizQuestionCounts.collectAsState()
    var showNewLabelDialog by rememberSaveable { mutableStateOf(false) }
    var sessionPendingDelete by remember { mutableStateOf<SessionEntity?>(null) }

    val defIncludeFilters by dataStoreManager.defIncludeFilters.collectAsState(initial = emptySet())
    val defShuffleQuestions by dataStoreManager.defShuffleQuestions.collectAsState(initial = false)
    val defShuffleOptions by dataStoreManager.defShuffleOptions.collectAsState(initial = false)
    val defRapidMode by dataStoreManager.defRapidMode.collectAsState(initial = false)
    val defRepeatWrong by dataStoreManager.defRepeatWrong.collectAsState(initial = false)
    val defQuizTimer by dataStoreManager.defQuizTimer.collectAsState(initial = 0)
    val defQuestionTimer by dataStoreManager.defQuestionTimer.collectAsState(initial = 0)

    LaunchedEffect(quizId) {
        viewModel.loadSessions(quizId)
    }

    if (isEmbedded) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { showNewLabelDialog = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.start_new_session_title)
                    )
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                SessionManagementContent(
                    sessions = sessions,
                    quizQuestionCounts = quizQuestionCounts,
                    quizId = quizId,
                    onSessionSelected = onSessionSelected,
                    onDeleteSession = { sessionPendingDelete = it },
                    onAddSession = { showNewLabelDialog = true }
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.quiz_sessions_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showNewLabelDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_quiz))
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                SessionManagementContent(
                    sessions = sessions,
                    quizQuestionCounts = quizQuestionCounts,
                    quizId = quizId,
                    onSessionSelected = onSessionSelected,
                    onDeleteSession = { sessionPendingDelete = it },
                    onAddSession = { showNewLabelDialog = true }
                )
            }
        }
    }

    sessionPendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionPendingDelete = null },
            title = { Text("Delete session?") },
            text = { Text("This removes the saved session '${session.label.ifBlank { "Untitled session" }}'. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(session)
                        sessionPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { sessionPendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showNewLabelDialog) {
        val totalQuestions = quizQuestionCounts[quizId] ?: 0
        
        StartSessionDialog(
            totalQuestions = totalQuestions,
            defaultIncludeFilters = defIncludeFilters,
            defaultShuffleQuestions = defShuffleQuestions,
            defaultShuffleOptions = defShuffleOptions,
            defaultRapidMode = defRapidMode,
            defaultRepeatWrong = defRepeatWrong,
            defaultQuizTimerSeconds = defQuizTimer,
            defaultQuestionTimerSeconds = defQuestionTimer,
            onDismiss = { showNewLabelDialog = false },
            onConfirm = { config, saveDefaults ->
                if (saveDefaults) {
                    viewModel.saveDefaultSessionSettings(
                        filters = config.includeFilters.toSet(),
                        shuffleQ = config.shuffleQuestions,
                        shuffleO = config.shuffleOptions,
                        rapid = config.rapidMode,
                        repeatWrong = config.repeatWrong,
                        quizTimer = config.quizTimerSeconds,
                        qTimer = config.questionTimerSeconds
                    )
                }
                viewModel.createSession(
                    quizId = quizId,
                    label = config.label,
                    shuffleQuestions = config.shuffleQuestions,
                    shuffleOptions = config.shuffleOptions,
                    rapidMode = config.rapidMode,
                    repeatWrong = config.repeatWrong,
                    quizTimerSeconds = config.quizTimerSeconds,
                    questionTimerSeconds = config.questionTimerSeconds,
                    rangeFrom = config.rangeFrom,
                    rangeTo = config.rangeTo,
                    includeFilters = config.includeFilters,
                    onCreated = { sessionId ->
                        onSessionSelected(sessionId, false)
                    }
                )
                showNewLabelDialog = false
            }
        )
    }
}

@Composable
private fun SessionManagementContent(
    sessions: List<SessionEntity>,
    quizQuestionCounts: Map<Long, Int>,
    quizId: Long,
    onSessionSelected: (Long, Boolean) -> Unit,
    onDeleteSession: (SessionEntity) -> Unit,
    onAddSession: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                SessionEmptyStateCard(onClick = onAddSession)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, top = 12.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionItem(
                        session = session,
                        totalQuestions = quizQuestionCounts[quizId] ?: 0,
                        onClick = { onSessionSelected(session.id, session.isCompleted) },
                        onDelete = { onDeleteSession(session) }
                    )
                }
            }
        }
    }
}
data class SessionConfig(
    val label: String,
    val shuffleQuestions: Boolean,
    val shuffleOptions: Boolean,
    val rapidMode: Boolean,
    val repeatWrong: Boolean,
    val quizTimerSeconds: Int,
    val questionTimerSeconds: Int,
    val rangeFrom: Int,
    val rangeTo: Int,
    val includeFilters: List<String>
)

/**
 * Dialog for configuring and starting a new quiz session.
 *
 * @param totalQuestions Total number of questions available in the quiz.
 * @param defaultIncludeFilters Initial filters to apply.
 * @param defaultShuffleQuestions Initial value for shuffling questions.
 * @param defaultShuffleOptions Initial value for shuffling options.
 * @param defaultRapidMode Initial value for rapid mode.
 * @param defaultRepeatWrong Initial value for repeating wrong answers.
 * @param defaultQuizTimerSeconds Initial quiz timer duration.
 * @param defaultQuestionTimerSeconds Initial question timer duration.
 * @param onDismiss Callback to close the dialog.
 * @param onConfirm Callback when "Start" is clicked, providing the config and whether to save as defaults.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartSessionDialog(
    totalQuestions: Int,
    defaultIncludeFilters: Set<String>,
    defaultShuffleQuestions: Boolean,
    defaultShuffleOptions: Boolean,
    defaultRapidMode: Boolean,
    defaultRepeatWrong: Boolean,
    defaultQuizTimerSeconds: Int,
    defaultQuestionTimerSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (SessionConfig, Boolean) -> Unit
) {
    val context = LocalContext.current
    var label by rememberSaveable { mutableStateOf("") }
    var shuffleQuestions by rememberSaveable { mutableStateOf(defaultShuffleQuestions) }
    var shuffleOptions by rememberSaveable { mutableStateOf(defaultShuffleOptions) }
    var rapidMode by rememberSaveable { mutableStateOf(defaultRapidMode) }
    var repeatWrong by rememberSaveable { mutableStateOf(defaultRepeatWrong) }
    
    var rangeFrom by rememberSaveable { mutableIntStateOf(1) }
    var rangeTo by rememberSaveable { mutableIntStateOf(totalQuestions) }
    
    var enableQuizTimer by rememberSaveable { mutableStateOf(defaultQuizTimerSeconds > 0) }
    var quizTimerMinutes by rememberSaveable { mutableStateOf(if (defaultQuizTimerSeconds > 0) (defaultQuizTimerSeconds / 60).toString() else "30") }
    
    var enableQuestionTimer by rememberSaveable { mutableStateOf(defaultQuestionTimerSeconds > 0) }
    var questionTimerSeconds by rememberSaveable { mutableStateOf(if (defaultQuestionTimerSeconds > 0) defaultQuestionTimerSeconds.toString() else "30") }

    val filterOptions = listOf(
        "unanswered" to stringResource(R.string.navigation_unanswered),
        "missed" to stringResource(R.string.navigation_missed),
        "marked" to stringResource(R.string.mark_toggle),
        "categorized" to stringResource(R.string.categories_toggle),
        "uncategorized" to stringResource(R.string.uncategorized_label)
    )
    var selectedFilters by rememberSaveable { mutableStateOf(defaultIncludeFilters) }
    var rememberDefaults by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.start_new_session_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.session_label_optional)) },
                    placeholder = { Text(stringResource(R.string.session_label_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.include_only), style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilters.contains(key),
                            onClick = {
                                selectedFilters = if (selectedFilters.contains(key)) {
                                    selectedFilters - key
                                } else {
                                    selectedFilters + key
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text(stringResource(R.string.question_range), style = MaterialTheme.typography.titleSmall)
                val rangeError = remember(rangeFrom, rangeTo, totalQuestions) {
                    when {
                        rangeFrom < 1 -> context.getString(R.string.range_error_from_at_least_1)
                        totalQuestions > 0 && rangeTo > totalQuestions -> context.getString(R.string.range_error_to_at_most_n, totalQuestions)
                        rangeFrom > rangeTo -> context.getString(R.string.range_error_from_less_than_to)
                        else -> null
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = if (rangeFrom == 0) "" else rangeFrom.toString(),
                        onValueChange = { rangeFrom = it.toIntOrNull() ?: 0 },
                        label = { Text(stringResource(R.string.from_label)) },
                        modifier = Modifier.weight(1f),
                        isError = rangeError != null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    OutlinedTextField(
                        value = if (rangeTo == 0) "" else rangeTo.toString(),
                        onValueChange = { rangeTo = it.toIntOrNull() ?: 0 },
                        label = { Text(stringResource(R.string.to_label)) },
                        modifier = Modifier.weight(1f),
                        isError = rangeError != null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }
                if (rangeError != null) {
                    Text(
                        text = rangeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                HorizontalDivider()

                LabeledSwitch(
                    label = stringResource(R.string.shuffle_questions),
                    checked = shuffleQuestions,
                    onCheckedChange = { shuffleQuestions = it }
                )
                LabeledSwitch(
                    label = stringResource(R.string.shuffle_options),
                    checked = shuffleOptions,
                    onCheckedChange = { shuffleOptions = it }
                )
                LabeledSwitch(
                    label = stringResource(R.string.rapid_mode),
                    checked = rapidMode,
                    onCheckedChange = { rapidMode = it }
                )
                LabeledSwitch(
                    label = stringResource(R.string.repeat_wrong),
                    checked = repeatWrong,
                    onCheckedChange = { repeatWrong = it }
                )

                HorizontalDivider()

                TimerConfigRow(
                    label = stringResource(R.string.quiz_timer_min),
                    enabled = enableQuizTimer,
                    onEnabledChange = { enableQuizTimer = it },
                    value = quizTimerMinutes,
                    onValueChange = { quizTimerMinutes = it }
                )

                TimerConfigRow(
                    label = stringResource(R.string.question_timer_sec),
                    enabled = enableQuestionTimer,
                    onEnabledChange = { enableQuestionTimer = it },
                    value = questionTimerSeconds,
                    onValueChange = { questionTimerSeconds = it }
                )

                LabeledSwitch(
                    label = stringResource(R.string.remember_settings),
                    checked = rememberDefaults,
                    onCheckedChange = { rememberDefaults = it }
                )
            }
        },
        confirmButton = {
            val hasRangeError = rangeFrom < 1 || rangeTo > totalQuestions || rangeFrom > rangeTo
            TextButton(
                enabled = !hasRangeError,
                onClick = {
                    onConfirm(
                        SessionConfig(
                            label = label.ifBlank { "Session ${System.currentTimeMillis() % 10000}" },
                            shuffleQuestions = shuffleQuestions,
                            shuffleOptions = shuffleOptions,
                            rapidMode = rapidMode,
                            repeatWrong = repeatWrong,
                            quizTimerSeconds = if (enableQuizTimer) (quizTimerMinutes.toIntOrNull() ?: 30) * 60 else 0,
                            questionTimerSeconds = if (enableQuestionTimer) (questionTimerSeconds.toIntOrNull() ?: 30) else 0,
                            rangeFrom = (rangeFrom - 1).coerceAtLeast(0),
                            rangeTo = (rangeTo - 1).coerceAtLeast(0).coerceAtMost(totalQuestions - 1),
                            includeFilters = selectedFilters.toList()
                        ),
                        rememberDefaults
                    )
                }
            ) {
                Text(stringResource(R.string.start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * A reusable row with a label and a switch.
 */
@Composable
fun LabeledSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

/**
 * A row for configuring timer settings, including a checkbox to enable and an input field for the value.
 */
@Composable
fun TimerConfigRow(
    label: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(checked = enabled, onCheckedChange = onEnabledChange)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.width(80.dp),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
        )
    }
}

/**
 * Represents an individual session item in the list.
 * Shows progress, score metrics, and actions (Resume/Delete).
 *
 * @param session The [SessionEntity] to display.
 * @param totalQuestions Total questions in the parent quiz for progress calculation.
 * @param onClick Callback when the session card is clicked.
 * @param onDelete Callback when the delete button is clicked.
 */
@Composable
fun SessionItem(
    session: SessionEntity,
    totalQuestions: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val progress = if (totalQuestions > 0) (session.currentQuestionIndex.toFloat() / totalQuestions).coerceIn(0f, 1f) else 0f
    val answeredCount = (session.score + session.incorrectCount).coerceAtLeast(0)
    val accuracy = if (answeredCount > 0) (session.score.toFloat() / answeredCount * 100).toInt() else 0
    val remaining = (totalQuestions - answeredCount).coerceAtLeast(0)
    val colors = MaterialTheme.colorScheme
    val correctColor = if (isSystemInDarkTheme()) Color(0xFFA5D6A7) else Color(0xFF2E7D32)

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(58.dp)) {
                    CircularProgressIndicator(
                        progress = { accuracy / 100f },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 5.dp,
                        color = colors.primary,
                        trackColor = colors.surfaceVariant.copy(alpha = 0.55f)
                    )
                    Text(
                        text = "$accuracy%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(R.string.last_active_prefix, dateFormat.format(Date(session.lastModifiedAt))),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (session.isCompleted) colors.primaryContainer else colors.secondary.copy(alpha = 0.10f),
                        contentColor = if (session.isCompleted) colors.onPrimaryContainer else colors.secondary
                    ) {
                        Text(
                            text = if (session.isCompleted) stringResource(R.string.completed_badge) else stringResource(R.string.start),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(16.dp), tint = colors.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = colors.primary,
                trackColor = colors.primaryContainer.copy(alpha = 0.35f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SessionMetric(
                    label = stringResource(R.string.session_correct),
                    value = session.score,
                    icon = Icons.Default.CheckCircle,
                    color = correctColor
                )
                SessionMetric(
                    label = stringResource(R.string.session_incorrect),
                    value = session.incorrectCount,
                    icon = Icons.Default.Error,
                    color = colors.error
                )
                SessionMetric(
                    label = stringResource(R.string.session_remaining),
                    value = remaining,
                    icon = Icons.Default.PlayArrow,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A small UI component to display a specific score metric (e.g., Correct, Incorrect).
 */
@Composable
fun SessionMetric(
    label: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.10f),
            contentColor = color
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(
                    text = value.coerceAtLeast(0).toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun SessionEmptyStateCard(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.14f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.primary.copy(alpha = 0.10f),
                contentColor = colors.primary
            ) {
                Icon(
                    Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier
                        .padding(14.dp)
                        .size(30.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.no_sessions_found), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.empty_sessions_body), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

