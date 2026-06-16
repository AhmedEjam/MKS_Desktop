package com.ahmedyejam.mks.ui.slideshow

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
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
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.local.entity.CourseSlideEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.local.entity.SlideshowCourseEntity
import com.ahmedyejam.mks.ui.theme.LocalMksDesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideshowCourseScreen(
    courseId: Long,
    focusedSlideId: Long? = null,
    onBack: () -> Unit
) {
    val tokens = LocalMksDesignTokens.current
    val vm: SlideshowCourseViewModel = hiltViewModel()

    val state by vm.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    BackHandler(enabled = state.isPresentationMode) {
        vm.setPresentationMode(false)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state.isPresentationMode) {
        if (state.isPresentationMode) {
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

    LaunchedEffect(focusedSlideId, state.slides) {
        if (focusedSlideId != null && state.slides.isNotEmpty()) {
            val index = state.slides.indexOfFirst { it.id == focusedSlideId }
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
    var showSlideEditor by remember { mutableStateOf<CourseSlideEntity?>(null) }
    var showAddSlide by rememberSaveable { mutableStateOf(false) }
    var showCourseEditor by rememberSaveable { mutableStateOf(false) }
    var showPasteTextDialog by rememberSaveable { mutableStateOf(false) }
    var showGeneratorDialog by rememberSaveable { mutableStateOf(false) }

    val pptxPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            vm.importFromPptx(uri)
        }
    }

    LaunchedEffect(courseId) { vm.loadCourseSafe(courseId) }
    LaunchedEffect(state.message, state.error) {
        val text = state.message ?: state.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            vm.clearMessage()
        }
    }

    var showOptionsMenu by remember { mutableStateOf(false) }

    state.course?.let { course ->
        if (showCourseEditor) {
            com.ahmedyejam.mks.ui.components.EntityEditDialog(
                title = "Edit Course",
                initialName = course.title,
                initialDescription = course.description ?: "",
                initialImage = course.coverImage ?: "",
                showImage = true,
                onDismiss = { showCourseEditor = false },
                onSave = { title, desc, image ->
                    vm.updateCourse(title, desc.ifBlank { null }, image.ifBlank { null })
                    showCourseEditor = false
                }
            )
        }
    }

    if (showAddSlide || showSlideEditor != null) {
        SlideEditorDialog(
            slide = showSlideEditor,
            onDismiss = {
                showAddSlide = false
                showSlideEditor = null
            },
            onSave = { title, body, notes, imagePath ->
                val slide = showSlideEditor
                if (slide == null) vm.addSlide(title, body, notes, imagePath) else vm.updateSlide(slide, title, body, notes, imagePath)
                showAddSlide = false
                showSlideEditor = null
            }
        )
    }

    if (showGeneratorDialog) {
        SlideGeneratorDialog(
            quizzes = state.quizzes,
            categories = state.categories,
            onDismiss = { showGeneratorDialog = false },
            onConfirm = { source, sourceId, clearMarks ->
                when (source) {
                    "ALL" -> vm.generateFromBook(com.ahmedyejam.mks.data.model.SlideGenerationConfig.DEFAULT, clearMarks)
                    "MARKED" -> vm.generateFromMarked(com.ahmedyejam.mks.data.model.SlideGenerationConfig.DEFAULT, clearMarks)
                    "MISSED" -> vm.generateFromMissed(com.ahmedyejam.mks.data.model.SlideGenerationConfig.DEFAULT)
                    "QUIZ" -> sourceId?.toLongOrNull()?.let { vm.generateFromQuiz(it, com.ahmedyejam.mks.data.model.SlideGenerationConfig.DEFAULT, clearMarks) }
                    "CATEGORY" -> sourceId?.let { vm.generateFromCategory(it, com.ahmedyejam.mks.data.model.SlideGenerationConfig.DEFAULT, clearMarks) }
                }
                showGeneratorDialog = false
            }
        )
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (state.selectedSlideIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${state.selectedSlideIds.size} selected") },
                    navigationIcon = { IconButton(onClick = { vm.clearSelection() }) { Icon(Icons.Default.Close, contentDescription = "Clear selection") } },
                    actions = {
                        IconButton(onClick = { vm.selectAllSlides() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { vm.deleteSelectedSlides() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            } else {
                var subtitle: String? = null
                if (state.isPresentationMode) {
                    val elapsed by vm.elapsedSeconds.collectAsState()
                    val minutes = elapsed / 60
                    val seconds = elapsed % 60
                    subtitle =
                        String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
                }

                com.ahmedyejam.mks.ui.components.StudyTopAppBar(
                    title = state.course?.title ?: stringResource(R.string.study_slides),
                    subtitle = subtitle,
                    onNavigateBack = onBack,
                    actions = {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showOptionsMenu, onDismissRequest = { showOptionsMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit Course") },
                                onClick = { showCourseEditor = true; showOptionsMenu = false }
                            )
                        }
                        IconButton(onClick = { vm.setPresentationMode(!state.isPresentationMode) }) {
                            Icon(
                                if (state.isPresentationMode) Icons.AutoMirrored.Filled.ViewList else Icons.Default.PlayArrow,
                                contentDescription = "Toggle presentation mode"
                            )
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
                state.course == null -> Text("Course not found", Modifier.align(Alignment.Center))
                state.isPresentationMode -> SlidePresentationContent(
                    state = state, 
                    viewModel = vm
                )
                else -> SlideshowCourseDetailContent(
                    state = state,
                    onAddSlide = { showAddSlide = true },
                    onEditSlide = { showSlideEditor = it },
                    onDeleteSlide = { vm.deleteSlide(it) },
                    onMoveSlide = { slide, direction -> vm.moveSlide(slide, direction) },
                    onGenerateSlides = { showGeneratorDialog = true },
                    onStartPresentation = { vm.setPresentationMode(true) },
                    onToggleSelect = { vm.toggleSlideSelection(it) },
                    onImportText = { showPasteTextDialog = true },
                    onImportPptx = { pptxPickerLauncher.launch("application/vnd.openxmlformats-officedocument.presentationml.presentation") }
                )
            }
        }
    }
}

@Composable
private fun SlideshowCourseDetailContent(
    state: SlideshowCourseUiState,
    onAddSlide: () -> Unit,
    onEditSlide: (CourseSlideEntity) -> Unit,
    onDeleteSlide: (CourseSlideEntity) -> Unit,
    onMoveSlide: (CourseSlideEntity, Int) -> Unit,
    onGenerateSlides: () -> Unit,
    onStartPresentation: () -> Unit,
    onToggleSelect: (Long) -> Unit,
    onImportText: () -> Unit,
    onImportPptx: () -> Unit
) {
    val course = state.course ?: return
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CourseStatsCard(course = course, slideCount = state.slides.size)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onAddSlide, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_slide))
                }
                OutlinedButton(onClick = onStartPresentation, enabled = state.slides.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Present")
                }
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onImportText, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.TextSnippet, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.import_text))
                }
                OutlinedButton(onClick = onImportPptx, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Slideshow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.import_pptx))
                }
            }
        }
        item {
            val tokens = LocalMksDesignTokens.current
            Button(
                onClick = onGenerateSlides,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(tokens.cardRadius)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.generate_from_questions), fontWeight = FontWeight.Bold)
            }
        }
        if (state.slides.isEmpty()) {
            item {
                val tokens = LocalMksDesignTokens.current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(tokens.cardRadius),
                    elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No slides yet", fontWeight = FontWeight.SemiBold)
                        Text("Create slides manually or generate them from marked/missed questions.")
                    }
                }
            }
        } else {
            items(state.slides, key = { it.id }) { slide ->
                SlideListItem(
                    slide = slide,
                    isSelected = state.selectedSlideIds.contains(slide.id),
                    isSelectionMode = state.selectedSlideIds.isNotEmpty(),
                    onToggleSelect = { onToggleSelect(slide.id) },
                    onEdit = { onEditSlide(slide) },
                    onDelete = { onDeleteSlide(slide) },
                    onMoveUp = { onMoveSlide(slide, -1) },
                    onMoveDown = { onMoveSlide(slide, 1) }
                )
            }
        }
    }
}

@Composable
private fun CourseStatsCard(course: SlideshowCourseEntity, slideCount: Int) {
    val tokens = LocalMksDesignTokens.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.cardElevation)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(course.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            course.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("$slideCount slides") })
                AssistChip(onClick = {}, label = { Text("${course.studiedSlideCount} completed") })
                AssistChip(onClick = {}, label = { Text("${(course.progress * 100).toInt()}% progress") })
            }
            LinearProgressIndicator(progress = { course.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlideListItem(
    slide: CourseSlideEntity,
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
                onClick = { if (isSelectionMode) onToggleSelect() else onEdit() },
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
                    Icon(Icons.Default.Slideshow, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(slide.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(slide.body, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    slide.speakerNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.NoteAlt, null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                notes, style = MaterialTheme.typography.labelSmall,
                                maxLines = 1, color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (!isSelectionMode) {
                    IconButton(onClick = onMoveUp) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up") }
                    IconButton(onClick = onMoveDown) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down") }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlidePresentationContent(
    state: SlideshowCourseUiState,
    viewModel: SlideshowCourseViewModel
) {
    val tokens = LocalMksDesignTokens.current
    if (state.slides.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No slides to present")
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = state.currentIndex,
        pageCount = { state.slides.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentIndex(pagerState.currentPage)
    }
    LaunchedEffect(state.currentIndex) {
        if (pagerState.currentPage != state.currentIndex) {
            pagerState.animateScrollToPage(state.currentIndex)
        }
    }

    Column(Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { (state.currentIndex + 1f) / state.slides.size.coerceAtLeast(1) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1,
            pageSpacing = 16.dp
        ) { page ->
            val slide = state.slides[page]
            SlidePageContent(slide)
        }

        val currentSlide = state.currentSlide
        if (currentSlide != null) {
            var notesExpanded by rememberSaveable(currentSlide.id) { mutableStateOf(false) }
            currentSlide.speakerNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { notesExpanded = !notesExpanded },
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.NoteAlt, null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    stringResource(R.string.slide_notes),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                if (notesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                        AnimatedVisibility(notesExpanded) {
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(state.slides.size) { index ->
                    val isActive = index == state.currentIndex
                    val isStudied = state.slides[index].isCompleted
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isActive) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> MaterialTheme.colorScheme.primary
                                    isStudied -> tokens.success
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                            .clickable { viewModel.setCurrentIndex(index) }
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { viewModel.previousSlide() }, enabled = state.currentIndex > 0) {
                    Icon(Icons.Default.ChevronLeft, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Previous")
                }
                FilledTonalButton(onClick = { viewModel.toggleSlideStudied() }) {
                    Icon(
                        if (currentSlide.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (currentSlide.isCompleted) "Studied" else "Mark studied")
                }
                Button(onClick = { viewModel.nextSlide() }, enabled = state.currentIndex < state.slides.size - 1) {
                    Text("Next")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
    }
}

@Composable
private fun SlidePageContent(slide: CourseSlideEntity) {
    val tokens = LocalMksDesignTokens.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        slide.imagePath?.takeIf { it.isNotBlank() }?.let { path ->
            AsyncImage(
                model = path,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(tokens.cardRadius)),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = slide.body,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 26.sp
        )
    }
}

@Composable
private fun SlideEditorDialog(
    slide: CourseSlideEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, body: String, notes: String?, imagePath: String?) -> Unit
) {
    var title by rememberSaveable(slide?.id) { mutableStateOf(slide?.title ?: "") }
    var body by rememberSaveable(slide?.id) { mutableStateOf(slide?.body ?: "") }
    var notes by rememberSaveable(slide?.id) { mutableStateOf(slide?.speakerNotes ?: "") }
    var imagePath by rememberSaveable(slide?.id) { mutableStateOf(slide?.imagePath ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (slide == null) stringResource(R.string.add_slide) else stringResource(R.string.edit_slide)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.slide_header)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(body, { body = it }, label = { Text(stringResource(R.string.slide_body)) }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.slide_notes)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(imagePath, { imagePath = it }, label = { Text("Image URL/Path") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(enabled = title.isNotBlank(), onClick = {
                onSave(
                    title.trim(),
                    body.trim(),
                    notes.trim().ifBlank { null },
                    imagePath.trim().ifBlank { null }
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlideGeneratorDialog(
    quizzes: List<QuizEntity>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (source: String, sourceId: String?, clearMarks: Boolean) -> Unit
) {
    var source by rememberSaveable { mutableStateOf("ALL") } // ALL, MARKED, MISSED, QUIZ, CATEGORY
    var sourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var clearMarks by rememberSaveable { mutableStateOf(false) }

    var expandedSource by remember { mutableStateOf(false) }
    var expandedSubSource by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.generate_from_questions)) },
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
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    source,
                    sourceId,
                    clearMarks
                )
            }) { Text("Generate") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
                    Text("Alternating Paragraphs (odd title, even body)", style = MaterialTheme.typography.bodySmall)
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
                    Text("Explicit Labels (Title: ..., Body: ...)", style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            mode =
                                com.ahmedyejam.mks.data.importer.parser.TextParseMode.HEADER_BODY_NOTES
                        }) {
                    androidx.compose.material3.RadioButton(
                        selected = mode == com.ahmedyejam.mks.data.importer.parser.TextParseMode.HEADER_BODY_NOTES,
                        onClick = { mode = com.ahmedyejam.mks.data.importer.parser.TextParseMode.HEADER_BODY_NOTES }
                    )
                    Text("Header/Body/Notes (or separated by ---)", style = MaterialTheme.typography.bodySmall)
                }
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Slides text") },
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
