package com.ahmedyejam.mks.ui.quiz

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.ahmedyejam.mks.data.model.CategoryWithMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedQuizPlayerScreen(
    viewModel: QuizViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val timerState by viewModel.timerState.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            QuizTopBar(
                state = state,
                timerState = timerState,
                onBack = onBack
            )
        }
    ) { padding ->
        Row(Modifier.padding(padding).fillMaxSize()) {
            // Main Content
            Box(Modifier.weight(1f).fillMaxHeight()) {
                if (state.questions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.error ?: "No questions found.", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    QuestionContent(
                        state = state,
                        viewModel = viewModel
                    )
                }
            }

            // Desktop Side Panel
            Surface(
                modifier = Modifier.width(320.dp).fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                QuizSidePanel(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTopBar(
    state: QuizState,
    timerState: TimerState,
    onBack: () -> Unit
) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Question ${if (state.questions.isEmpty()) 0 else state.currentIndex + 1} of ${state.questions.size}")
                    if (!state.sessionLabel.isNullOrBlank()) {
                        Text(state.sessionLabel, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                    TimerChip(Icons.Rounded.Timer, formatTime(timerState.timeLeft))
                    if (state.quizTimerSeconds > 0) {
                        Spacer(Modifier.width(8.dp))
                        TimerChip(Icons.Rounded.HourglassBottom, formatTime(timerState.quizTimeLeft), MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "${state.score}/${state.initialQuestionCount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
        if (state.initialQuestionCount > 0) {
            val progress = ((state.currentIndex + 1).toFloat() / state.initialQuestionCount).coerceAtMost(1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun TimerChip(icon: ImageVector, text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun QuestionContent(
    state: QuizState,
    viewModel: QuizViewModel
) {
    val currentQuestion = state.questions.getOrNull(state.currentIndex) ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(currentQuestion.text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        if (!currentQuestion.imagePath.isNullOrBlank()) {
            item {
                AsyncImage(
                    model = currentQuestion.imagePath,
                    contentDescription = "Question Image",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                )
            }
        }

        itemsIndexed(state.shuffledOptions) { index, option ->
            if (!state.isOneByOne || index < state.visibleOptionsCount || state.isAnswered) {
                val originalIndex = state.optionMapping[index]
                OptionItem(
                    text = option,
                    isSelected = state.selectedOptions.contains(originalIndex),
                    isAnswered = state.isAnswered,
                    isCorrect = currentQuestion.correctAnswers.contains(originalIndex),
                    isDropped = state.droppedOptions.contains(originalIndex),
                    isSingleChoice = currentQuestion.type == QuestionType.SINGLE_CHOICE,
                    onClick = { viewModel.onOptionSelected(index) }
                )
            }
        }

        if (state.isAnswered) {
            item {
                QuestionExplanation(state.isCorrect, currentQuestion.explanation)
            }
        }

        if (state.showHint && currentQuestion.hint != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Text(currentQuestion.hint, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun OptionItem(
    text: String,
    isSelected: Boolean,
    isAnswered: Boolean,
    isCorrect: Boolean,
    isDropped: Boolean,
    isSingleChoice: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isAnswered && isCorrect -> Color(0xFFE8F5E9)
        isAnswered && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isAnswered && isCorrect -> Color(0xFF2E7D32)
        isAnswered && isSelected && !isCorrect -> MaterialTheme.colorScheme.onErrorContainer
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        enabled = !isAnswered && !isDropped,
        modifier = Modifier.fillMaxWidth().alpha(if (isDropped) 0.5f else 1f),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        contentColor = contentColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = if (isSingleChoice) {
                if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked
            } else {
                if (isSelected) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank
            }
            Icon(icon, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, textDecoration = if (isDropped) TextDecoration.LineThrough else TextDecoration.None)
            
            if (isAnswered) {
                Spacer(Modifier.weight(1f))
                if (isCorrect) Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF2E7D32))
                else if (isSelected) Icon(Icons.Rounded.Cancel, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun QuestionExplanation(isCorrect: Boolean, explanation: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, tint = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(if (isCorrect) "Correct!" else "Incorrect", fontWeight = FontWeight.Bold, color = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
            }
            if (!explanation.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(explanation, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizSidePanel(
    state: QuizState,
    viewModel: QuizViewModel
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Quiz Controls", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.submitAnswer() },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedOptions.isNotEmpty() || state.isAnswered || (state.isOneByOne && state.visibleOptionsCount < state.shuffledOptions.size)
        ) {
            Text(if (state.isAnswered) "Next" else if (state.isOneByOne && state.visibleOptionsCount < state.shuffledOptions.size) "Reveal" else "Submit")
        }

        Spacer(Modifier.height(16.dp))
        
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlToggle(Icons.Rounded.Lightbulb, "Hint", state.showHint) { viewModel.toggleHint() }
            ControlToggle(Icons.Rounded.Bookmark, "Mark", state.questions.getOrNull(state.currentIndex)?.isMarked == true) { viewModel.toggleMarked() }
            ControlToggle(Icons.Rounded.Bolt, "Rapid", state.isRapidMode) { viewModel.toggleRapidMode() }
            ControlToggle(Icons.Rounded.Filter1, "1-by-1", state.isOneByOne) { viewModel.toggleOneByOne() }
            ControlToggle(Icons.Rounded.DoneAll, "Finish", false) { viewModel.finishSession() }
        }

        Spacer(Modifier.height(24.dp))
        Text("Questions", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(40.dp),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.questions.size) { index ->
                val status = viewModel.getQuestionStatus(index)
                val color = when (status) {
                    QuestionStatus.CURRENT -> MaterialTheme.colorScheme.primary
                    QuestionStatus.CORRECT -> Color(0xFF4CAF50)
                    QuestionStatus.INCORRECT -> Color(0xFFF44336)
                    QuestionStatus.UNANSWERED -> MaterialTheme.colorScheme.outline
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(color).clickable { viewModel.jumpToQuestion(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text((index + 1).toString(), color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ControlToggle(icon: ImageVector, label: String, checked: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = checked,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) }
    )
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
