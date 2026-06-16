package com.ahmedyejam.mks.ui.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.data.importer.model.ImportResult
import com.ahmedyejam.mks.data.local.entity.BookEntity
import com.ahmedyejam.mks.data.local.entity.QuizEntity
import com.ahmedyejam.mks.data.model.CategoryWithMetadata
import com.ahmedyejam.mks.data.preferences.DataStoreManager
import com.ahmedyejam.mks.ui.category.CategoryBrowserDialog
import com.ahmedyejam.mks.ui.category.CategoryEditDialog
import com.ahmedyejam.mks.ui.components.EntityEditDialog
import com.ahmedyejam.mks.ui.importer.ImportViewModel
import com.ahmedyejam.mks.ui.library.components.BookOptionsSheet
import com.ahmedyejam.mks.ui.library.components.DeleteConfirmDialog
import com.ahmedyejam.mks.ui.library.components.ImportReviewDialog
import com.ahmedyejam.mks.ui.library.components.ImportWarningsDialog
import com.ahmedyejam.mks.ui.library.components.LibraryContentGrid
import com.ahmedyejam.mks.ui.library.components.LibraryFabMenu
import com.ahmedyejam.mks.ui.library.components.LibraryTopBar
import com.ahmedyejam.mks.ui.library.components.QuizOptionsSheet
import com.ahmedyejam.mks.ui.library.components.SortDialog
import com.ahmedyejam.mks.ui.quiz.CompilerViewModel
import com.ahmedyejam.mks.ui.trash.TrashBinDialog
import com.ahmedyejam.mks.ui.workspace.WorkspaceManagerDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    compilerViewModel: CompilerViewModel,
    importViewModel: ImportViewModel,
    sharedUris: List<Uri>? = null,
    onConsumedSharedUris: () -> Unit = {},
    onQuizSelected: (Long) -> Unit,
    onScanSelected: (Long) -> Unit,
    onAdaptiveSelected: (String, String) -> Unit,
    onBrowseQuestions: (Long) -> Unit,
    onCategorySelected: (String) -> Unit,
    onSettingsSelected: () -> Unit,
    onBookDashboardSelected: (Long) -> Unit = {},
    returnResetSignal: Int = 0,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val dataStoreManager = remember(context) { DataStoreManager(context) }
    val currentThemeMode by dataStoreManager.themeMode.collectAsState(initial = "DAWN")

    val importState by importViewModel.importState.collectAsState()
    val workspaceId by viewModel.currentWorkspaceId.collectAsState()

    val showCompilerDialogState = remember { mutableStateOf(value = false) }
    var showCompilerDialog by showCompilerDialogState
    val completedImportResultState = remember { mutableStateOf<ImportResult?>(null) }
    var completedImportResult by completedImportResultState

    fun handleImportUri(uri: Uri, targetQuizId: Long? = null, targetBookId: Long? = null) {
        val format = importViewModel.detectFormat(uri)
        if ((format == com.ahmedyejam.mks.data.importer.model.ImportFormat.XLSX) ||
            (format == com.ahmedyejam.mks.data.importer.model.ImportFormat.CSV_TSV)) {
            compilerViewModel.onFileSelected(uri, targetQuizId, activeWorkspaceId = workspaceId)
            showCompilerDialog = true
        } else if (format == com.ahmedyejam.mks.data.importer.model.ImportFormat.ZIP) {
            importViewModel.getImportPreview(
                uri = uri,
                targetBookId = targetBookId,
                targetQuizId = targetQuizId,
            )
        }
    }

    LaunchedEffect(sharedUris) {
        sharedUris?.firstOrNull()?.let { uri ->
            handleImportUri(uri)
            onConsumedSharedUris()
        }
    }

    LaunchedEffect(returnResetSignal) {
        if (returnResetSignal > 0) {
            viewModel.resetToLibraryRoot()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is LibraryUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val books by viewModel.books.collectAsStateWithLifecycle(initialValue = emptyList())
    val quizzes by viewModel.quizzes.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentQuizzes by viewModel.recentQuizzes.collectAsStateWithLifecycle(initialValue = emptyList())
    val resumeQuiz by viewModel.resumeQuiz.collectAsStateWithLifecycle(initialValue = null)
    val categories by viewModel.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val libraryViewMode by viewModel.libraryViewMode.collectAsStateWithLifecycle()
    val bookViewMode by viewModel.bookViewMode.collectAsStateWithLifecycle()

    val currentViewMode by remember {
        derivedStateOf {
            if (selectedCategory == null) libraryViewMode else bookViewMode
        }
    }

    val showCovers by viewModel.showCovers.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val bookSortBy by viewModel.bookSortBy.collectAsState()

    val showSortDialogState = remember { mutableStateOf(value = false) }
    var showSortDialog by showSortDialogState
    val showBookEditDialogState = rememberSaveable { mutableStateOf(value = false) }
    var showBookEditDialog by showBookEditDialogState
    val editingBookState = remember { mutableStateOf<BookEntity?>(null) }
    var editingBook by editingBookState
    val showQuizEditDialogState = rememberSaveable { mutableStateOf(value = false) }
    var showQuizEditDialog by showQuizEditDialogState
    val editingQuizEditState = remember { mutableStateOf<QuizEntity?>(null) }
    var editingQuizEdit by editingQuizEditState
    val showDeleteConfirmBookState = remember { mutableStateOf<BookEntity?>(null) }
    var showDeleteConfirmBook by showDeleteConfirmBookState
    val showDeleteConfirmQuizState = remember { mutableStateOf<QuizEntity?>(null) }
    var showDeleteConfirmQuiz by showDeleteConfirmQuizState

    val editingCategoryState = remember { mutableStateOf<CategoryWithMetadata?>(null) }
    var editingCategory by editingCategoryState

    var showWorkspaceManager by remember { mutableStateOf(false) }
    var showTrashBin by remember { mutableStateOf(false) }

    val menuBookState = remember { mutableStateOf<BookEntity?>(null) }
    var menuBook by menuBookState
    val menuQuizState = remember { mutableStateOf<QuizEntity?>(null) }
    var menuQuiz by menuQuizState

    var pendingExportBookId by remember { mutableStateOf<Long?>(null) }
    var pendingExportQuizId by remember { mutableStateOf<Long?>(null) }

    val categoriesExpandedState = rememberSaveable { mutableStateOf(value = true) }
    var categoriesExpanded by categoriesExpandedState
    val showCategoryBrowserState = remember { mutableStateOf(value = false) }
    var showCategoryBrowser by showCategoryBrowserState
    val previewCategories by remember(categories) {
        derivedStateOf {
            val pinned = categories.asSequence()
                .filter { it.isPinned }
                .sortedByDescending { it.lastEditedAt }
                .toList()
            val recent = categories.asSequence()
                .filter { !it.isPinned && (it.lastEditedAt > 0) }
                .sortedByDescending { it.lastEditedAt }
                .toList()
            (pinned + recent).take(10)
        }
    }
    val showOverflowMenuState = remember { mutableStateOf(value = false) }
    var showOverflowMenu by showOverflowMenuState

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            handleImportUri(it, pendingExportQuizId, pendingExportBookId)
        }
    }

    // Handle Import States
    when (val state = importState) {
        is ImportViewModel.ImportState.Review -> {
            ImportReviewDialog(
                preview = state.preview,
                onDismiss = {
                    importViewModel.resetState()
                },
            ) { strategy, allowInsecureHttpImages ->
                importViewModel.importLibrary(
                    uri = state.uri,
                    strategy = strategy,
                    targetBookId = state.targetBookId,
                    targetQuizId = state.targetQuizId,
                    allowInsecureRemoteImages = allowInsecureHttpImages,
                    activeWorkspaceId = workspaceId
                )
            }
        }
        is ImportViewModel.ImportState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text(state.message) },
                text = {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        }
        is ImportViewModel.ImportState.Success -> {
            LaunchedEffect(state) {
                val message = if (state.result.warnings.isNotEmpty()) {
                    context.getString(
                        R.string.import_completed_with_warning_count,
                        state.result.warnings.size,
                    )
                } else if (state.result.partiallyImported) {
                    context.getString(
                        R.string.import_completed_with_skipped_count,
                        state.result.skippedRecordsCount,
                    )
                } else {
                    context.getString(R.string.import_successful)
                }
                snackbarHostState.showSnackbar(message)
                completedImportResultState.value = state.result.takeIf { (it.warnings.isNotEmpty()) || (it.skippedRecordsCount > 0) }
                importViewModel.resetState()
            }
        }
        is ImportViewModel.ImportState.Error -> {
            LaunchedEffect(state) {
                snackbarHostState.showSnackbar(context.getString(R.string.import_error, state.message))
                importViewModel.resetState()
            }
        }
        else -> {}
    }

    completedImportResult?.let { result ->
        ImportWarningsDialog(
            result = result,
        ) { completedImportResultState.value = null }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let { exportUri ->
            scope.launch {
                try {
                    val result = context.contentResolver.openOutputStream(exportUri)?.use { stream ->
                        pendingExportBookId?.let { bookId ->
                            viewModel.exportBook(bookId, stream)
                        } ?: pendingExportQuizId?.let { quizId ->
                            viewModel.exportQuiz(quizId, stream)
                        } ?: viewModel.exportAll(stream)
                    }
                    if (result?.success == true) {
                        val message = if (result.failedAssetCount > 0) {
                            context.getString(
                                R.string.export_completed_with_warnings,
                                result.failedAssetCount,
                            )
                        } else {
                            context.getString(R.string.export_successful)
                        }
                        snackbarHostState.showSnackbar(message)
                    } else {
                        snackbarHostState.showSnackbar(
                            context.getString(
                                R.string.export_failed,
                                result?.errorMessage ?: context.getString(R.string.unknown_error),
                            ),
                        )
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(context.getString(R.string.export_failed, e.message))
                } finally {
                    pendingExportBookId = null
                    pendingExportQuizId = null
                }
            }
        }
    }

    val fabExpandedState = remember { mutableStateOf(value = false) }
    var fabExpanded by fabExpandedState

    BackHandler(enabled = fabExpanded || (selectedCategory != null) || isSearching) {
        if (fabExpanded) {
            fabExpandedState.value = false
        } else if (isSearching) {
            viewModel.setSearching(searching = false)
        } else {
            viewModel.resetToLibraryRoot()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val gridState = rememberLazyGridState()

    LaunchedEffect(selectedCategory, isSearching) {
        gridState.scrollToItem(0)
    }

    val columns by remember {
        derivedStateOf {
            if (currentViewMode == "GRID") GridCells.Fixed(2) else GridCells.Fixed(1)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LibraryTopBar(
                scrollBehavior = scrollBehavior,
                isSearching = isSearching,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                selectedBookId = -1L,
                selectedCategory = selectedCategory,
                currentBookTitle = null,
                onNavigationClick = {
                    if (isSearching) viewModel.setSearching(searching = false)
                    else viewModel.resetToLibraryRoot()
                },
                onSearchClick = { viewModel.setSearching(searching = true) },
                onSettingsClick = onSettingsSelected,
                onSortClick = { showSortDialog = true },
                currentViewMode = currentViewMode,
                onToggleViewMode = {
                    if (selectedCategory == null) {
                        viewModel.toggleLibraryViewMode()
                    } else {
                        viewModel.toggleBookViewMode()
                    }
                },
                showOverflowMenu = showOverflowMenu,
                onOverflowMenuToggle = { showOverflowMenuState.value = it },
                onContactClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://linktr.ee/MKSpace".toUri(),
                    )
                    context.startActivity(intent)
                },
                onWorkspaceManagerClick = { showWorkspaceManager = true },
                onTrashBinClick = { showTrashBin = true }
            )
        },
        floatingActionButton = {
            LibraryFabMenu(
                fabExpanded = fabExpanded,
                onFabExpandedChange = { fabExpandedState.value = it },
                selectedCategory = selectedCategory,
                onAdaptiveSelected = onAdaptiveSelected,
                onNewBookClick = {
                    editingBookState.value = null
                    showBookEditDialogState.value = true
                },
                onImportClick = {
                    importLauncher.launch(
                        arrayOf(
                            "application/zip",
                            "application/x-zip-compressed",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-excel",
                            "text/csv",
                            "text/comma-separated-values",
                            "text/tab-separated-values",
                        ),
                    )
                },
            ) {
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                pendingExportBookId = null
                exportLauncher.launch("mks_full_library_$timestamp.zip")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LibraryContentGrid(
                gridState = gridState,
                columns = columns,
                padding = padding,
                selectedCategory = selectedCategory,
                resumeQuiz = resumeQuiz,
                recentQuizzes = recentQuizzes,
                currentThemeMode = currentThemeMode,
                onQuizClick = { onQuizSelected(it) },
                onLinkClick = { url ->
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        url.toUri(),
                    )
                    context.startActivity(intent)
                },
                categoriesExpanded = categoriesExpanded,
                previewCategories = previewCategories,
                onCategorySelected = onCategorySelected,
                onCategoryLongClick = { editingCategory = it },
                showCategoryBrowser = { showCategoryBrowser = true },
                books = books,
                quizzes = quizzes,
                currentViewMode = currentViewMode,
                showCovers = showCovers,
                onBookClick = { onBookDashboardSelected(it.id) },
                onBookLongClick = { menuBook = it },
                onQuizLongClick = { menuQuiz = it },
            )

            AnimatedVisibility(
                visible = fabExpanded,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            fabExpandedState.value = false
                        },
                )
            }
        }
    }

    // Dialogs management
    menuBook?.let { book ->
        BookOptionsSheet(
            book = book,
            onDismiss = { menuBookState.value = null },
            onPinClick = { viewModel.toggleBookPinned(book) },
            onEditClick = { editingBookState.value = book; showBookEditDialogState.value = true },
            onExportClick = {
                pendingExportBookId = book.id
                pendingExportQuizId = null
                exportLauncher.launch("${book.title.replace(" ", "_")}.mks.zip")
            },
            onImportClick = {
                pendingExportQuizId = null
                pendingExportBookId = book.id
                importLauncher.launch(arrayOf("*/*"))
            },
            onDeleteClick = { showDeleteConfirmBookState.value = book },
        ) { onBookDashboardSelected(book.id) }
    }

    menuQuiz?.let { quiz ->
        QuizOptionsSheet(
            quiz = quiz,
            onDismiss = { menuQuizState.value = null },
            onBrowseQuestionsClick = { onBrowseQuestions(quiz.id) },
            onScanClick = { onScanSelected(quiz.id) },
            onPinClick = { viewModel.toggleQuizPinned(quiz) },
            onEditClick = { editingQuizEditState.value = quiz; showQuizEditDialogState.value = true },
            onExportClick = {
                pendingExportQuizId = quiz.id
                pendingExportBookId = null
                exportLauncher.launch("${quiz.title.replace(" ", "_")}.mks.zip")
            },
            onImportClick = {
                pendingExportQuizId = quiz.id
                pendingExportBookId = null
                importLauncher.launch(arrayOf("*/*"))
            },
        ) { showDeleteConfirmQuizState.value = quiz }
    }

    if (showCategoryBrowser) {
        CategoryBrowserDialog(
            categories = categories,
            onDismiss = { showCategoryBrowserState.value = false },
            onCategorySelected = {
                onCategorySelected(it.name)
                showCategoryBrowserState.value = false
            },
        ) {
            editingCategoryState.value = it
            showCategoryBrowserState.value = false
        }
    }

    editingCategory?.let { category ->
        CategoryEditDialog(
            category = category,
            allCategories = categories,
            books = books,
            onDismiss = { editingCategoryState.value = null },
            onDelete = { viewModel.deleteCategory(category.name); editingCategoryState.value = null },
            onRename = { viewModel.renameCategory(category.name, it); editingCategoryState.value = null },
            onMerge = { viewModel.mergeCategory(category.name, it); editingCategoryState.value = null },
            onGetMergePreview = { viewModel.getMergePreview(category.name, it) },
            onCreateQuiz = { title, bId -> viewModel.createQuizFromCategory(category.name, title, bId); editingCategoryState.value = null },
            onTogglePin = { viewModel.updateCategoryMetadata(category.name, category.emoji, category.color, !category.isPinned) },
            onUpdateEmoji = { viewModel.updateCategoryMetadata(category.name, it, category.color, category.isPinned) },
            onUpdateColor = { viewModel.updateCategoryMetadata(category.name, category.emoji, it, category.isPinned) },
        ) { onCategorySelected(category.name) }
    }

    if (showCompilerDialog) {
        com.ahmedyejam.mks.ui.quiz.CompilerDialog(
            viewModel = compilerViewModel,
            books = books,
            quizzes = quizzes,
        ) {
            showCompilerDialogState.value = false
        }
    }

    if (showBookEditDialog) {
        EntityEditDialog(
            title = if (editingBook == null) stringResource(R.string.new_book) else stringResource(R.string.edit),
            initialName = editingBook?.title ?: "",
            initialDescription = editingBook?.description ?: "",
            initialImage = editingBook?.coverImage ?: "",
            titleLabel = stringResource(R.string.book_title_label),
            descriptionLabel = stringResource(R.string.description_label),
            showImage = true,
            onDismiss = { showBookEditDialogState.value = false },
        ) { title, desc, cover ->
            if (editingBook == null) {
                viewModel.insertBook(
                    BookEntity(
                        workspaceId = workspaceId,
                        externalId = java.util.UUID.randomUUID().toString(),
                        title = title,
                        description = desc,
                    ),
                    coverUri = cover,
                )
            } else {
                editingBook?.let { book ->
                    viewModel.updateBook(
                        book.copy(title = title, description = desc),
                        newCoverUri = cover,
                    )
                }
            }
            showBookEditDialogState.value = false
        }
    }

    showDeleteConfirmBook?.let { book ->
        DeleteConfirmDialog(
            title = stringResource(R.string.delete_book_title),
            message = stringResource(R.string.delete_book_msg, book.title),
            onDismiss = { showDeleteConfirmBookState.value = null },
        ) { viewModel.deleteBook(book); showDeleteConfirmBookState.value = null }
    }

    showDeleteConfirmQuiz?.let { quiz ->
        DeleteConfirmDialog(
            title = stringResource(R.string.delete_quiz_title),
            message = stringResource(R.string.delete_quiz_msg, quiz.title),
            onDismiss = { showDeleteConfirmQuizState.value = null },
        ) { viewModel.deleteQuiz(quiz); showDeleteConfirmQuizState.value = null }
    }

    if (showSortDialog) {
        val isQuizzes = (selectedCategory != null)
        SortDialog(
            currentOption = if (isQuizzes) bookSortBy else sortBy,
            onOptionSelected = {
                if (isQuizzes) {
                    viewModel.setBookSortBy(it)
                } else {
                    viewModel.setSortBy(it)
                }
                showSortDialogState.value = false
            },
        ) { showSortDialogState.value = false }
    }

    if (showWorkspaceManager) {
        WorkspaceManagerDialog(
            viewModel = viewModel,
            activeWorkspaceId = workspaceId,
            onDismiss = { showWorkspaceManager = false }
        )
    }

    if (showTrashBin) {
        TrashBinDialog(
            viewModel = viewModel,
            onDismiss = { showTrashBin = false }
        )
        if (showQuizEditDialog) {
        EntityEditDialog(
            title = stringResource(R.string.edit),
            initialName = editingQuizEdit?.title ?: "",
            initialDescription = editingQuizEdit?.description ?: "",
            initialImage = editingQuizEdit?.coverImage ?: "",
            titleLabel = stringResource(R.string.quiz_title_label),
            descriptionLabel = stringResource(R.string.description_label),
            showImage = true,
            onDismiss = { showQuizEditDialog = false }
        ) { title, desc, cover ->
            editingQuizEdit?.let { quiz ->
                viewModel.updateQuiz(quiz.copy(title = title, description = desc), cover.ifBlank { null })
            }
            showQuizEditDialog = false
        }
    }
}
}
