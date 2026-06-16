package com.ahmedyejam.mks.ui.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Filter1
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.ahmedyejam.mks.data.model.CategoryWithMetadata
import com.ahmedyejam.mks.ui.category.CategoryChip
import com.ahmedyejam.mks.ui.category.CategoryEditDialog
import com.ahmedyejam.mks.ui.category.QuestionAssetsReadOnlyDialog
import com.ahmedyejam.mks.ui.theme.LocalMksDesignTokens
import com.ahmedyejam.mks.ui.theme.normalizeMksThemeMode
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * Enum representing the possible states of the bottom sliding sheet.
 */
enum class QuizSheetValue {
    Collapsed,
    PartiallyExpanded,
    Expanded
}

/**
 * Main screen for playing a quiz.
 * Provides a user interface for answering questions, viewing results, and managing quiz settings.
 *
 * @param viewModel The [QuizViewModel] that manages the state and logic of the quiz session.
 * @param onQuizFinished A callback triggered when the quiz is completed, providing the score and total questions.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun QuizPlayerScreen(
    viewModel: QuizViewModel,
    onQuizFinished: (Long, Int, Int) -> Unit,
    onAskAiClick: (Long) -> Unit = {},
    onBack: () -> Unit,
    onViewCategoryQuestions: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    val currentQuestion by remember(state.currentIndex, state.questions) {
        derivedStateOf {
            state.questions.getOrNull(state.currentIndex)
        }
    }
    
    val totalQuestions = state.questions.size

    val fullscreenImageUrlState = remember { mutableStateOf<Any?>(null) }
    var fullscreenImageUrl by fullscreenImageUrlState
    val showDropQuestionDialogState = rememberSaveable { mutableStateOf(value = false) }
    var showDropQuestionDialog by showDropQuestionDialogState
    val showQuestionAssetsState = rememberSaveable { mutableStateOf(value = false) }
    var showQuestionAssets by showQuestionAssetsState
    var selectedCategoryForEdit by rememberSaveable(state.allCategoriesWithMetadata) { mutableStateOf<CategoryWithMetadata?>(null) }


    if (showDropQuestionDialog) {
        AlertDialog(
            onDismissRequest = { showDropQuestionDialogState.value = false },
            title = { Text(stringResource(R.string.drop_question_title)) },
            text = { Text(stringResource(R.string.drop_question_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dropQuestion()
                        showDropQuestionDialogState.value = false
                    },
                ) {
                    Text(stringResource(R.string.drop), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDropQuestionDialogState.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    val currentQuestionAssets by viewModel
        .assetsForQuestion(currentQuestion?.id ?: -1L)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val currentBookSources by viewModel
        .sourceDocumentsForCurrentQuiz()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    if (showQuestionAssets) {
        currentQuestion?.let { question ->
            QuestionAssetsReadOnlyDialog(
                question = question,
                assets = currentQuestionAssets,
                sourceDocuments = currentBookSources,
            ) { showQuestionAssetsState.value = false }
        }
    }

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            onQuizFinished(state.sessionId ?: -1L, state.score, state.initialQuestionCount)
        }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fullHeight = this.maxHeight.value * LocalDensity.current.density
        val density = LocalDensity.current
        var sheetContentHeight by remember { mutableFloatStateOf(0f) }
        
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val collapsedHeight = with(density) { (56.dp + navBarHeight).toPx() }
        val partialHeight = with(density) { (100.dp + navBarHeight).toPx() }
        val expandedOffset = with(density) { 64.dp.toPx() }
        
        val anchors = remember(fullHeight, sheetContentHeight) {
            DraggableAnchors {
                QuizSheetValue.Collapsed at (fullHeight - collapsedHeight)
                QuizSheetValue.PartiallyExpanded at (fullHeight - partialHeight)
                
                val targetExpanded = if (sheetContentHeight > 0) {
                    (fullHeight - sheetContentHeight).coerceAtLeast(expandedOffset)
                } else {
                    expandedOffset
                }
                QuizSheetValue.Expanded at targetExpanded
            }
        }
        
        val draggableState = remember(anchors) {
            AnchoredDraggableState(
                initialValue = QuizSheetValue.PartiallyExpanded,
                anchors = anchors,
                positionalThreshold = { distance: Float -> distance * 0.5f },
                velocityThreshold = { with(density) { 100.dp.toPx() } },
                snapAnimationSpec = spring(),
                decayAnimationSpec = exponentialDecay(),
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                QuizTopBar(
                    currentIndex = state.currentIndex,
                    totalQuestions = totalQuestions,
                    initialQuestionCount = state.initialQuestionCount,
                    isMarked = currentQuestion?.isMarked ?: false,
                    focusModeEnabled = state.focusModeEnabled,
                    sessionLabel = state.sessionLabel,
                    timerStateFlow = viewModel.timerState,
                    quizTimerSeconds = state.quizTimerSeconds,
                    questionTimerSeconds = state.questionTimerSeconds,
                    isAnswered = state.isAnswered,
                    score = state.score,
                    currentStreak = state.currentStreak,
                    onAskAiClick = { currentQuestion?.id?.let(onAskAiClick) },
                    onBack = onBack,
                )
            }
        ) { padding ->
            val currentOffset = draggableState.offset
            val isOffsetNan = currentOffset.isNaN()
            val bottomPadding = if (isOffsetNan) 100.dp else with(density) { (fullHeight - currentOffset).toDp() }

            Box(
                modifier = Modifier
                    .padding(padding)
                    .padding(bottom = bottomPadding.coerceAtLeast(0.dp))
                    .fillMaxSize(),
            ) {
                if (totalQuestions == 0) {
                    EmptyQuestionsPlaceholder(error = state.error)
                } else {
                    currentQuestion?.let { question ->
                        QuestionContent(
                            state = state,
                            currentQuestion = question,
                            viewModel = viewModel,
                            onEditCategory = { selectedCategoryForEdit = it },
                            onImageClick = { fullscreenImageUrl = it },
                            assetCount = currentQuestionAssets.size,
                        ) { showQuestionAssetsState.value = true }
                    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        // Custom Bottom Sheet
        Surface(
            modifier = Modifier
                .offset {
                    val yOffset = draggableState.offset
                    IntOffset(
                        x = 0,
                        y = if (yOffset.isNaN()) (fullHeight - partialHeight).roundToInt() else yOffset.roundToInt()
                    )
                }
                .anchoredDraggable(draggableState, Orientation.Vertical)
                .wrapContentHeight(align = Alignment.Top)
                .heightIn(max = with(density) { (fullHeight - expandedOffset).toDp() }),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            tonalElevation = 0.dp,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .onGloballyPositioned { coords ->
                        sheetContentHeight = coords.size.height.toFloat()
                    }
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
                
                QuizSheetContent(
                    state = state,
                    viewModel = viewModel,
                    onDropQuestion = { showDropQuestionDialogState.value = true },
                    onAskAiClick = onAskAiClick
                )
            }
        }
    }

    fullscreenImageUrl?.let { data ->
        ZoomableImageDialog(
            imageData = data,
        ) { fullscreenImageUrlState.value = null }
    }

    selectedCategoryForEdit?.let { category ->
        key(category.name) {
            CategoryEditDialog(
                category = category,
                allCategories = state.allCategoriesWithMetadata,
                books = emptyList(),
                onDismiss = { selectedCategoryForEdit = null },
                onDelete = {
                    viewModel.deleteCategory(category.name)
                    selectedCategoryForEdit = null
                },
                onRename = { newName ->
                    viewModel.renameCategory(category.name, newName)
                    selectedCategoryForEdit = null
                },
                onMerge = { target ->
                    viewModel.mergeCategory(category.name, target)
                    selectedCategoryForEdit = null
                },
                onGetMergePreview = { target ->
                    viewModel.getMergePreview(category.name, target)
                },
                onCreateQuiz = { _, _ -> },
                onTogglePin = { viewModel.onToggleCategoryPin(category.name) },
                onUpdateEmoji = { emoji -> viewModel.onUpdateCategoryEmoji(category.name, emoji) },
                onUpdateColor = { color -> viewModel.onUpdateCategoryColor(category.name, color) },
                onViewQuestions = { onViewCategoryQuestions(category.name) }
            )
        }
    }
}

@Composable
fun EmptyQuestionsPlaceholder(error: String? = null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = if (error != null) Icons.Rounded.ErrorOutline else Icons.Rounded.Inbox,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (error != null) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error ?: stringResource(R.string.no_questions_found),
                style = MaterialTheme.typography.titleMedium,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun QuestionContent(
    state: QuizState,
    currentQuestion: com.ahmedyejam.mks.data.local.entity.QuestionEntity,
    viewModel: QuizViewModel,
    onEditCategory: (CategoryWithMetadata) -> Unit,
    onImageClick: (Any) -> Unit,
    assetCount: Int = 0,
    onAssetsClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var newCategoryName by rememberSaveable { mutableStateOf("")    }
    var totalDrag by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = with(LocalDensity.current) { 50.dp.toPx() }

    val tokens = LocalMksDesignTokens.current
    AnimatedContent(
        targetState = state.currentIndex,
        transitionSpec = {
            if (tokens.isPlain) {
                fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
            } else {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally { width ->
                    if (isRtl) -direction * width else direction * width
                } + fadeIn()).togetherWith(
                    slideOutHorizontally { width ->
                        if (isRtl) direction * width else -direction * width
                    } + fadeOut()
                ).using(
                    SizeTransform(clip = false)
                )
            }
        },
        label = "QuestionTransition",
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isRtl) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (isRtl) {
                            if (totalDrag > swipeThreshold) {
                                viewModel.nextQuestion()
                            } else if (totalDrag < -swipeThreshold) {
                                viewModel.previousQuestion()
                            }
                        } else {
                            if (totalDrag > swipeThreshold) {
                                viewModel.previousQuestion()
                            } else if (totalDrag < -swipeThreshold) {
                                viewModel.nextQuestion()
                            }
                        }
                        totalDrag = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    }
                )
            }
    ) { targetIndex ->
        val animQuestion = state.questions.getOrNull(targetIndex) ?: currentQuestion
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp)
        ) {
            item {
                Column {
                    Text(
                        text = animQuestion.text,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val finalImage = resolveQuestionImage(
                        imagePath = animQuestion.imagePath,
                        imageSource = animQuestion.imageSource,
                        themeMode = themeMode,
                        isRtl = isRtl
                    )

                    if (assetCount > 0) {
                        OutlinedButton(onClick = onAssetsClick, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.AttachFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Assets ($assetCount)")
                        }
                    }

                    if ((finalImage != null) && ((finalImage !is String) || finalImage.isNotBlank())) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(finalImage)
                    .crossfade(enable = true)
                    .build(),
                            contentDescription = stringResource(R.string.question_image_desc),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onImageClick(finalImage) },
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }

            itemsIndexed(
                state.shuffledOptions,
                key = { _, option -> option },
            ) { index, option ->
                if ((!state.isOneByOne) || (index < state.visibleOptionsCount) || state.isAnswered) {
                    val originalIndex = state.optionMapping[index]
                    val isDropped = state.droppedOptions.contains(originalIndex)

                    OptionItem(
                        text = option,
                        isSelected = state.selectedOptions.contains(originalIndex),
                        isAnswered = state.isAnswered,
                        isCorrect = animQuestion.correctAnswers.contains(originalIndex),
                        isDropped = isDropped,
                        isSingleChoice = animQuestion.type == QuestionType.SINGLE_CHOICE,
                        eliminationModeEnabled = state.eliminationModeEnabled,
                        onClick = {
                            if (!tokens.isPlain) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.onOptionSelected(index)
                        },
                        onDoubleClick = {
                            if (!tokens.isPlain) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onOptionDoubleClicked(index)
                        },
                        onDrop = { viewModel.dropOption(index) }
                    )
                }
            }

            if (state.isAnswered) {
                item {
                    QuestionExplanation(
                        isCorrect = state.isCorrect,
                        explanation = animQuestion.explanation,
                    )
                }
            }

            if (state.showCategorization && state.isAnswered) {
                item {
                    QuestionCategories(
                        allCategories = state.allCategoriesWithMetadata,
                        questionCategories = animQuestion.categories,
                        onToggleCategory = { viewModel.toggleQuestionCategory(it) },
                        onEditCategory = onEditCategory,
                        newCategoryName = newCategoryName,
                        onNewCategoryNameChange = { newCategoryName = it }
                    )
                }
            }

            if (state.isAnswered) {
                item {
                    QuestionNotes(
                        permanentNote = animQuestion.notes ?: "",
                        onPermanentNoteChange = { viewModel.updateQuestionNote(it) }
                    )
                }
            }

            if (state.hintsEnabled && animQuestion.hint != null) {
                item {
                    QuestionHint(
                        showHint = state.showHint,
                        hintText = animQuestion.hint!!,
                        onToggleHint = { viewModel.toggleHint() }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun QuestionExplanation(isCorrect: Boolean, explanation: String?) {
    val successContainer = if (isSystemInDarkTheme()) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
    val onSuccessContainer = if (isSystemInDarkTheme()) Color(0xFFA5D6A7) else Color(0xFF1B5E20)

    val tokens = LocalMksDesignTokens.current
    Card(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(tokens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect)
                successContainer else MaterialTheme.colorScheme.errorContainer,
            contentColor = if (isCorrect)
                onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    contentDescription = null,
                    tint = if (isCorrect) onSuccessContainer else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCorrect) "Excellent!" else "Not quite right",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCorrect) onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }

            if (!explanation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    color = if (isCorrect)
                        onSuccessContainer.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Explanation:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) onSuccessContainer else MaterialTheme.colorScheme.error
                )
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCorrect) onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionCategories(
    allCategories: List<CategoryWithMetadata>,
    questionCategories: List<String>,
    onToggleCategory: (String) -> Unit,
    onEditCategory: (CategoryWithMetadata) -> Unit,
    newCategoryName: String,
    onNewCategoryNameChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(stringResource(R.string.import_review_categories), style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allCategories.forEach { category ->
                CategoryChip(
                    category = category,
                    isSelected = questionCategories.contains(category.name),
                    onCategorySelected = { onToggleCategory(it.name) },
                    onCategoryLongClick = { onEditCategory(it) }
                )
            }
        }
        OutlinedTextField(
            value = newCategoryName,
            onValueChange = onNewCategoryNameChange,
            label = { Text(stringResource(R.string.add_new_category_label)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onToggleCategory(newCategoryName)
                            onNewCategoryNameChange("")
                        }
                    },
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_new_category_label))
                }
            }
        )
    }
}

@Composable
fun QuestionNotes(
    permanentNote: String,
    onPermanentNoteChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text("Notes", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)

        OutlinedTextField(
            value = permanentNote,
            onValueChange = onPermanentNoteChange,
            label = { Text(stringResource(R.string.permanent_note_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            minLines = 2,
            placeholder = { Text(stringResource(R.string.permanent_note_placeholder)) }
        )
    }
}

@Composable
fun QuestionHint(
    showHint: Boolean,
    hintText: String,
    onToggleHint: () -> Unit
) {
    TextButton(
        onClick = onToggleHint,
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(if (showHint) "Hide Hint" else stringResource(R.string.hint_label))
    }

    AnimatedVisibility(visible = showHint) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Text(
                text = hintText,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Top app bar for the quiz player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTopBar(
    currentIndex: Int,
    totalQuestions: Int,
    initialQuestionCount: Int,
    isMarked: Boolean,
    focusModeEnabled: Boolean,
    sessionLabel: String?,
    timerStateFlow: StateFlow<TimerState>,
    quizTimerSeconds: Int,
    questionTimerSeconds: Int,
    isAnswered: Boolean,
    score: Int,
    currentStreak: Int,
    onAskAiClick: () -> Unit = {},
    onBack: () -> Unit
) {
    val timerState by timerStateFlow.collectAsStateWithLifecycle()

    Column {
        com.ahmedyejam.mks.ui.components.StudyTopAppBar(
            title = stringResource(
                R.string.question_counter,
                if (totalQuestions == 0) 0 else currentIndex + 1,
                totalQuestions
            ),
            subtitle = sessionLabel,
            onNavigateBack = onBack,
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isMarked) {
                        Icon(
                            Icons.Rounded.Bookmark,
                            contentDescription = stringResource(R.string.mark_toggle),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onAskAiClick) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = "Ask AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (focusModeEnabled) {
                        Icon(
                            Icons.Rounded.NotificationsOff,
                            contentDescription = stringResource(R.string.focus_toggle),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(
                                text = formatTime(timerState.timeLeft),
                                modifier = Modifier.padding(start = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (quizTimerSeconds > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.HourglassBottom, contentDescription = "Quiz Timer", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = formatTime(timerState.quizTimeLeft),
                                    modifier = Modifier.padding(start = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (questionTimerSeconds > 0 && !isAnswered) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Update, contentDescription = "Question Timer", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = formatTime(timerState.questionTimeLeft),
                                    modifier = Modifier.padding(start = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, contentDescription = "Score", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "$score/$initialQuestionCount",
                                modifier = Modifier.padding(start = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (currentStreak > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Whatshot, contentDescription = "Streak", modifier = Modifier.size(14.dp), tint = Color(0xFFFF9800))
                                Text(
                                    text = currentStreak.toString(),
                                    modifier = Modifier.padding(start = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }
                }
            }
        )
        if (initialQuestionCount > 0) {
            LinearProgressIndicator(
                progress = { ((currentIndex + 1).toFloat() / initialQuestionCount).coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizSheetContent(
    state: QuizState,
    viewModel: QuizViewModel,
    onDropQuestion: () -> Unit,
    onAskAiClick: (Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val currentQuestion = remember(state.questions, state.currentIndex) {
        if (state.questions.isNotEmpty() && state.currentIndex in state.questions.indices) {
            state.questions[state.currentIndex]
        } else null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Fixed Header part (Visible in peek)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Performance Stats in Peek
                Column(
                    modifier = Modifier.weight(0.3f),
                    horizontalAlignment = Alignment.Start
                ) {
                    val answered = state.questionResultsByIndex.size
                    val accuracy = if (answered > 0) (state.score * 100 / answered) else 0
                    Text(
                        text = stringResource(R.string.accuracy_peek, accuracy),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (accuracy >= 80) Color(0xFF4CAF50) else if (accuracy >= 50) Color(0xFFFF9800) else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.streak_peek, state.currentStreak),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9800)
                    )
                }

                Button(
                    modifier = Modifier
                        .weight(0.7f)
                        .height(48.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.submitAnswer()
                    },
                    enabled = (state.isAnswered ||
                            state.selectedOptions.isNotEmpty() ||
                            (state.isOneByOne && state.visibleOptionsCount < state.shuffledOptions.size) ||
                            state.skipUnansweredGlobal) && state.questions.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isAnswered) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (state.isAnswered) stringResource(R.string.next) else if (state.isOneByOne && state.visibleOptionsCount < state.shuffledOptions.size) stringResource(R.string.reveal) else stringResource(R.string.submit),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Expandable Body part
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Toggles Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val toggleModifier = Modifier
                    .weight(1f)
                    .widthIn(min = 100.dp)
                ControlToggle(
                    modifier = toggleModifier,
                    icon = Icons.Rounded.Category,
                    label = stringResource(R.string.categories_toggle),
                    checked = state.showCategorization,
                    onCheckedChange = { viewModel.toggleCategorization() }
                )
                ControlToggle(
                    modifier = toggleModifier,
                    icon = Icons.Rounded.Lightbulb,
                    label = "Explain",
                    checked = false,
                    onCheckedChange = {
                        val qId = currentQuestion?.id ?: return@ControlToggle
                        onAskAiClick(qId)
                    }
                )
                ControlToggle(
                    modifier = toggleModifier,
                    icon = Icons.Rounded.Filter1,
                    label = stringResource(R.string.one_by_one_toggle),
                    checked = state.isOneByOne,
                    onCheckedChange = { viewModel.toggleOneByOne() }
                )
                ControlToggle(
                    modifier = toggleModifier,
                    icon = Icons.Rounded.Bolt,
                    label = stringResource(R.string.rapid_toggle),
                    checked = state.isRapidMode,
                    onCheckedChange = { viewModel.toggleRapidMode() }
                )
                ControlToggle(
                    modifier = toggleModifier,
                    icon = Icons.Rounded.Block,
                    label = stringResource(R.string.eliminate_toggle),
                    checked = state.eliminationModeEnabled,
                    onCheckedChange = { viewModel.toggleEliminationMode() }
                )
                ControlToggle(
                    modifier = toggleModifier,
                    icon = Icons.Rounded.DeleteForever,
                    label = stringResource(R.string.drop_q_toggle),
                    checked = false,
                    onCheckedChange = { onDropQuestion() }
                )
                ControlToggle(
                    modifier = toggleModifier,
                    icon = if (state.focusModeEnabled) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications,
                    label = stringResource(R.string.focus_toggle),
                    checked = state.focusModeEnabled,
                    onCheckedChange = { viewModel.toggleFocusMode() }
                )
                ControlToggle(
                    modifier = toggleModifier,
                    icon = if (state.questions.isNotEmpty() && state.questions[state.currentIndex].isMarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    label = stringResource(R.string.mark_toggle),
                    checked = state.questions.isNotEmpty() && state.questions[state.currentIndex].isMarked,
                    onCheckedChange = { viewModel.toggleMarked() }
                )
                ControlToggle(
                    modifier = toggleModifier,
                    icon = Icons.Rounded.DoneAll,
                    label = stringResource(R.string.finish_quiz),
                    checked = false,
                    onCheckedChange = { viewModel.finishSession() }
                )
            }

            HorizontalDivider()

            // Navigation Controls
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(NavigationFilter.entries, key = { it.name }) { filter ->
                    FilterChip(
                        selected = state.navigationFilter == filter,
                        onClick = { viewModel.setNavigationFilter(filter) },
                        label = { Text(filter.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            currentQuestion?.let { question ->
                if (question.isDropped) {
                    AssistChip(
                        onClick = { viewModel.restoreCurrentDroppedQuestion() },
                        label = { Text("Restore dropped question") },
                        leadingIcon = { Icon(Icons.Rounded.Restore, contentDescription = null) },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            val filteredIndices = remember(state.questions, state.questionResultsByIndex, state.navigationFilter) {
                state.questions.indices.filter { index ->
                    val result = state.questionResultsByIndex[index]
                    when (state.navigationFilter) {
                        NavigationFilter.ALL -> true
                        NavigationFilter.ANSWERED -> result != null
                        NavigationFilter.UNANSWERED -> result == null
                        NavigationFilter.MISSED -> result == false
                        NavigationFilter.MARKED -> state.questions[index].isMarked
                        NavigationFilter.DROPPED -> state.questions[index].isDropped
                    }
                }
            }

            if (filteredIndices.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredIndices, key = { it }) { index ->
                        val status = viewModel.getQuestionStatus(index)
                        val isMarked = state.questions[index].isMarked

                        val color = when (status) {
                            QuestionStatus.CURRENT -> MaterialTheme.colorScheme.primary
                            QuestionStatus.CORRECT -> Color(0xFF4CAF50)
                            QuestionStatus.INCORRECT -> Color(0xFFF44336)
                            QuestionStatus.UNANSWERED -> MaterialTheme.colorScheme.outline
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color.copy(alpha = if (status == QuestionStatus.CURRENT) 1f else 0.6f))
                                .border(
                                    width = if (status == QuestionStatus.CURRENT) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.jumpToQuestion(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isMarked) {
                                Icon(
                                    Icons.Rounded.Bookmark,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp),
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * A toggle control button used in the quiz scaffold.
 *
 * @param icon The icon to display.
 * @param label The text label for the control.
 * @param checked Whether the toggle is currently active.
 * @param onCheckedChange Callback triggered when the toggle is clicked.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun ControlToggle(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalMksDesignTokens.current
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        animationSpec = tokens.animationSpec(),
        label = "toggleBackground"
    )
    val contentColor = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        contentColor = contentColor,
        border = BorderStroke(
            1.dp, 
            if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Represents a single selectable option in a question.
 *
 * @param text The text content of the option.
 * @param isSelected Whether this option is currently selected by the user.
 * @param isAnswered Whether the current question has been submitted.
 * @param isCorrect Whether this option is one of the correct answers.
 * @param isDropped Whether this option has been marked as "dropped" (ignored).
 * @param isSingleChoice Whether this is a single choice question.
 * @param eliminationModeEnabled Whether elimination mode is active.
 * @param onClick Callback for when the option is tapped.
 * @param onDrop Callback for when the option is long-pressed or "X" is clicked to be dropped.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OptionItem(
    text: String,
    isSelected: Boolean,
    isAnswered: Boolean,
    isCorrect: Boolean,
    isDropped: Boolean,
    isSingleChoice: Boolean,
    eliminationModeEnabled: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onDrop: () -> Unit
) {
    val tokens = LocalMksDesignTokens.current
    val haptic = LocalHapticFeedback.current

    val successColor = tokens.success
    val successContainer = successColor.copy(alpha = 0.12f)

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isAnswered && isCorrect -> successContainer
            isAnswered && isSelected -> MaterialTheme.colorScheme.errorContainer
            isSelected -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tokens.animationSpec(),
        label = "optionBackground"
    )

    val contentColor = when {
        isAnswered && isCorrect -> successColor
        isAnswered && isSelected -> MaterialTheme.colorScheme.onErrorContainer
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor by animateColorAsState(
        targetValue = when {
            isAnswered && isCorrect -> successColor
            isAnswered && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
            isSelected -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        animationSpec = tokens.animationSpec(),
        label = "optionBorder"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected && !isAnswered) 1.01f else 1f,
        animationSpec = tokens.animationSpec(),
        label = "optionScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDropped) 0.5f else 1f)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .combinedClickable(
                enabled = !isAnswered && !isDropped,
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDrop()
                }
            ),
        shape = RoundedCornerShape(tokens.cardRadius),
        color = backgroundColor,
        contentColor = contentColor,
        border = BorderStroke(if (isSelected || isAnswered) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = if (isSingleChoice) CircleShape else RoundedCornerShape(6.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (isDropped) TextDecoration.LineThrough else TextDecoration.None
                ),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )

            if (eliminationModeEnabled && !isAnswered && !isDropped) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDrop()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = stringResource(R.string.eliminate_label),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            if (isAnswered) {
                if (isCorrect) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = successColor)
                } else if (isSelected) {
                    Icon(Icons.Rounded.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Formats a duration in seconds into a MM:SS string.
 *
 * @param seconds The total number of seconds.
 * @return A formatted time string.
 */
fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

/**
 * Resolves the image to display for a question, handling special placeholders like "contact_banner".
 */
@Composable
fun resolveQuestionImage(
    imagePath: String?,
    imageSource: String?,
    themeMode: String,
    isRtl: Boolean
): Any? {
    val displayImage = imagePath ?: imageSource
    if (displayImage != "contact_banner") return displayImage

    val normalizedTheme = normalizeMksThemeMode(themeMode)
    val isSystemDark = isSystemInDarkTheme()
    val isActuallyDark = when (normalizedTheme) {
        "MIDNIGHT", "PLAIN_DARK" -> true
        "SYSTEM" -> isSystemDark
        else -> false
    }

    return when {
        normalizedTheme == "FOREST" -> if (isRtl) R.drawable.contact_banner_ar_light_forest_path else R.drawable.contact_banner_en_light_forest_path
        normalizedTheme == "LAVENDER" || normalizedTheme == "PLAIN_LIGHT" -> if (isRtl) R.drawable.contact_banner_ar_light_lavender_valley else R.drawable.contact_banner_en_light_lavender_valley
        isActuallyDark -> if (isRtl) R.drawable.contact_banner_ar_dark_moonlit_valley else R.drawable.contact_banner_en_dark_moonlit_valley
        else -> if (isRtl) R.drawable.contact_banner_ar_light_sunrise_valley else R.drawable.contact_banner_en_light_sunrise_valley
    }
}
