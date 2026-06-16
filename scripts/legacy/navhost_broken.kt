package com.ahmedyejam.mks.ui

import androidx.hilt.navigation.compose.hiltViewModel

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import androidx.navigation.toRoute
import com.ahmedyejam.mks.ui.*
import com.ahmedyejam.mks.ui.category.CategoryRoute
import com.ahmedyejam.mks.ui.quiz.*
import com.ahmedyejam.mks.ui.booktools.*
import com.ahmedyejam.mks.ui.summary.*
import com.ahmedyejam.mks.ui.scanner.*
import com.ahmedyejam.mks.ui.session.*


import com.ahmedyejam.mks.di.AppModule
import com.ahmedyejam.mks.ui.booktools.AiPromptDeckListScreen
import com.ahmedyejam.mks.ui.booktools.AiPromptDeckScreen
import com.ahmedyejam.mks.ui.booktools.BookNotesScreen
import com.ahmedyejam.mks.ui.booktools.BookToolsViewModel
import com.ahmedyejam.mks.ui.booktools.ReviewBlueprintListScreen
import com.ahmedyejam.mks.ui.booktools.ReviewBlueprintScreen
import com.ahmedyejam.mks.ui.booktools.SlideshowCourseListScreen
import com.ahmedyejam.mks.ui.booktools.SourceDocumentListScreen
import com.ahmedyejam.mks.ui.category.CategoryQuestionsScreen
import com.ahmedyejam.mks.ui.category.CategoryQuestionsViewModel
import com.ahmedyejam.mks.ui.common.InvalidRouteScreen
import com.ahmedyejam.mks.ui.data.DataToolsScreen
import com.ahmedyejam.mks.ui.data.DataToolsViewModel
import com.ahmedyejam.mks.ui.flashcard.FlashcardDeckScreen
import com.ahmedyejam.mks.ui.importer.ImportViewModel
import com.ahmedyejam.mks.ui.library.LibraryScreen
import com.ahmedyejam.mks.ui.library.LibraryViewModel
import com.ahmedyejam.mks.ui.navigation.requireNonBlankStringArg
import com.ahmedyejam.mks.ui.navigation.requirePositiveLongArg
import com.ahmedyejam.mks.ui.quiz.CompilerViewModel
import com.ahmedyejam.mks.ui.quiz.QuizDetailTabsScreen
import com.ahmedyejam.mks.ui.quiz.QuizPlayerScreen
import com.ahmedyejam.mks.ui.quiz.QuizQuestionsScreen
import com.ahmedyejam.mks.ui.quiz.QuizQuestionsViewModel
import com.ahmedyejam.mks.ui.quiz.QuizViewModel
import com.ahmedyejam.mks.ui.review.ReviewDashboardScreen
import com.ahmedyejam.mks.ui.review.ReviewDashboardViewModel
import com.ahmedyejam.mks.ui.scanner.ScannerScreen
import com.ahmedyejam.mks.ui.scanner.ScannerViewModel
import com.ahmedyejam.mks.ui.search.GlobalSearchScreen
import com.ahmedyejam.mks.ui.search.GlobalSearchViewModel
import com.ahmedyejam.mks.ui.session.SessionViewModel
import com.ahmedyejam.mks.ui.settings.SettingsScreen
import com.ahmedyejam.mks.ui.slideshow.SlideshowCourseScreen
import com.ahmedyejam.mks.ui.summary.SummaryScreen
import com.ahmedyejam.mks.ui.summary.SummaryViewModel
import com.ahmedyejam.mks.ui.theme.LocalMksDesignTokens
import com.ahmedyejam.mks.ui.welcome.WelcomeScreen
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
@Composable
fun MksNavHost(
    navController: NavHostController,
    appModule: AppModule,
    showWelcomeOnStartup: Boolean,
    sharedUris: List<Uri>? = null,
    onConsumedSharedUris: () -> Unit = {},
) {
    val currentThemeMode by appModule.dataStoreManager.themeMode.collectAsState(initial = "DAWN")
    val scope = rememberCoroutineScope()
    val startDestination = if (showWelcomeOnStartup) WelcomeRoute else LibraryRoute
    var libraryResetSignal by remember { mutableIntStateOf(0) }

    fun returnToLibraryRoot() {
        navController.navigate(LibraryRoute) {
            popUpTo(LibraryRoute) { inclusive = true }
            launchSingleTop = true
        }
    }

    val tokens = LocalMksDesignTokens.current
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { if (tokens.isPlain) EnterTransition.None else fadeIn() + slideInHorizontally { it } },
        exitTransition = { if (tokens.isPlain) ExitTransition.None else fadeOut() + slideOutHorizontally { -it } },
        popEnterTransition = { if (tokens.isPlain) EnterTransition.None else fadeIn() + slideInHorizontally { -it } },
        popExitTransition = { if (tokens.isPlain) ExitTransition.None else fadeOut() + slideOutHorizontally { it } },
    ) {
        composable<WelcomeRoute> {
            // Get initial language for WelcomeScreen without observing it here to avoid NavHost restarts
            val currentLanguage by appModule.dataStoreManager.language.collectAsState(initial = "en")

            WelcomeScreen(
                currentLanguage = currentLanguage,
                themeMode = currentThemeMode,
                onLanguageChanged = { lang ->
                    scope.launch { appModule.dataStoreManager.setLanguage(lang) }
                },
            ) {
                scope.launch { appModule.dataStoreManager.setShowWelcomeOnStartup(enabled = false) }
                navController.navigate(LibraryRoute) {
                    popUpTo(WelcomeRoute) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        composable<LibraryRoute> {
            val libraryViewModel: LibraryViewModel = hiltViewModel()
            val compilerViewModel: CompilerViewModel = hiltViewModel()
            val importViewModel: ImportViewModel = hiltViewModel()

            LibraryScreen(
                viewModel = libraryViewModel,
                compilerViewModel = compilerViewModel,
                importViewModel = importViewModel,
                returnResetSignal = libraryResetSignal,
                sharedUris = sharedUris,
                onConsumedSharedUris = onConsumedSharedUris,
                onQuizSelected = { quizId -> navController.navigate(SessionsRoute(quizId)) },
                onScanSelected = { quizId -> navController.navigate(ScannerRoute(quizId)) },
                onAdaptiveSelected = { type, id -> navController.navigate(AdaptiveRoute(type, id)) },
                onBrowseQuestions = { quizId -> navController.navigate(QuizQuestionsRoute(quizId)) },
                onCategorySelected = { category -> navController.navigate(CategoryRoute(category)) },
                onSettingsSelected = { navController.navigate(SettingsRoute) },
                onBookDashboardSelected = { bookId -> navController.navigate(BookDashboardRoute(bookId)) },
            )
        }

        composable(
            route = "quiz_questions/{quizId}?questionId={questionId}",
            { type = NavType.LongType },
                navArgument("questionId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { backStackEntry ->
            val quizId = backStackEntry.requirePositiveLongArg("quizId")
            if (quizId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val questionIdInput = backStackEntry.arguments?.getLong("questionId") ?: -1L
            val questionId = if (questionIdInput == -1L) null else questionIdInput

            val viewModel: QuizQuestionsViewModel = hiltViewModel()

            LaunchedEffect(quizId) {
                viewModel.loadQuiz(quizId)
            }

            QuizQuestionsScreen(
                viewModel = viewModel,
                focusedQuestionId = questionId,
            ) { navController.popBackStack() }
        }

        composable(
            route = "flashcards/{deckId}?cardId={cardId}",
            { type = NavType.LongType },
                navArgument("cardId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { backStackEntry ->
            val deckId = backStackEntry.requirePositiveLongArg("deckId")
            if (deckId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val cardIdInput = backStackEntry.arguments?.getLong("cardId") ?: -1L
            val cardId = if (cardIdInput == -1L) null else cardIdInput
            FlashcardDeckScreen(
                deckId = deckId,
                focusedCardId = cardId,
                appModule = appModule,
            ) { navController.popBackStack() }
        }

        composable(
            route = "book_dashboard/{bookId}",
            { type = NavType.LongType }),
        ) { backStackEntry ->
            val bookId = backStackEntry.requirePositiveLongArg("bookId")
            if (bookId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val viewModel: BookToolsViewModel = hiltViewModel()
            com.ahmedyejam.mks.ui.booktools.BookKnowledgeDashboardScreen(
                bookId = bookId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenQuiz = { navController.navigate(SessionsRoute(it)) },
                onOpenFlashcard = { navController.navigate(FlashcardsRoute(it)) },
                onOpenSlideshow = { navController.navigate(SlideshowRoute(it)) },
                onOpenNote = { navController.navigate(BlueprintRoute(it)) },
                onOpenPrompt = { navController.navigate(PromptDeckRoute(it)) },
                onOpenSource = { navController.navigate(BookSourcesRoute(bookId, it)) }
            )
        }
        composable(
            route = "book_slideshows/{bookId}",
            { type = NavType.LongType }),
        ) { backStackEntry ->
            val bookId = backStackEntry.requirePositiveLongArg("bookId")
            if (bookId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val viewModel: BookToolsViewModel = hiltViewModel()
            SlideshowCourseListScreen(
                bookId = bookId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            ) { courseId -> navController.navigate("slideshow/$courseId") }
        }

        composable(
            route = "book_blueprints/{bookId}",
            { type = NavType.LongType }),
        ) { backStackEntry ->
            val bookId = backStackEntry.requirePositiveLongArg("bookId")
            if (bookId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val viewModel: BookToolsViewModel = hiltViewModel()
            ReviewBlueprintListScreen(
                bookId = bookId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            ) { noteId -> navController.navigate("blueprint/$noteId") }
        }


        composable(
            route = "book_sources/{bookId}?sourceId={sourceId}",
            { type = NavType.LongType },
                navArgument("sourceId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { backStackEntry ->
            val bookId = backStackEntry.requirePositiveLongArg("bookId")
            if (bookId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val sourceIdInput = backStackEntry.arguments?.getLong("sourceId") ?: -1L
            val sourceId = if (sourceIdInput == -1L) null else sourceIdInput
            val viewModel: BookToolsViewModel = hiltViewModel()
            SourceDocumentListScreen(
                bookId = bookId,
                focusedSourceId = sourceId,
                viewModel = viewModel,
            ) { navController.popBackStack() }
        }

        composable(
            route = "book_prompts/{bookId}",
            { type = NavType.LongType }),
        ) { backStackEntry ->
            val bookId = backStackEntry.requirePositiveLongArg("bookId")
            if (bookId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val viewModel: BookToolsViewModel = hiltViewModel()
            AiPromptDeckListScreen(
                bookId = bookId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            ) { promptId -> navController.navigate("prompt_deck/$promptId") }
        }

        composable(
            route = "slideshow/{courseId}?slideId={slideId}",
            { type = NavType.LongType },
                navArgument("slideId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { backStackEntry ->
            val courseId = backStackEntry.requirePositiveLongArg("courseId")
            if (courseId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val slideIdInput = backStackEntry.arguments?.getLong("slideId") ?: -1L
            val slideId = if (slideIdInput == -1L) null else slideIdInput
            SlideshowCourseScreen(
                courseId = courseId,
                focusedSlideId = slideId,
                appModule = appModule,
            ) { navController.popBackStack() }
        }

        composable(
            route = "blueprint/{noteId}",
            { type = NavType.LongType }),
        ) { backStackEntry ->
            val noteId = backStackEntry.requirePositiveLongArg("noteId")
            if (noteId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val viewModel: BookToolsViewModel = hiltViewModel()
            ReviewBlueprintScreen(
                noteId = noteId,
                viewModel = viewModel,
            ) { navController.popBackStack() }
        }

        composable(
            route = "book_notes/{bookId}",
            { type = NavType.LongType }),
        ) { backStackEntry ->
            val bookId = backStackEntry.requirePositiveLongArg("bookId")
            if (bookId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val viewModel: BookToolsViewModel = hiltViewModel()
            BookNotesScreen(
                bookId = bookId,
                viewModel = viewModel,
            ) { navController.popBackStack() }
        }

        composable(
            route = "prompt_deck/{promptId}?cardId={cardId}&runId={runId}",
            { type = NavType.LongType },
                navArgument("cardId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("runId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { backStackEntry ->
            val promptId = backStackEntry.requirePositiveLongArg("promptId")
            if (promptId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val cardIdInput = backStackEntry.arguments?.getLong("cardId") ?: -1L
            val cardId = if (cardIdInput == -1L) null else cardIdInput
            val runIdInput = backStackEntry.arguments?.getLong("runId") ?: -1L
            val runId = if (runIdInput == -1L) null else runIdInput

            val viewModel: BookToolsViewModel = hiltViewModel()
            AiPromptDeckScreen(
                promptId = promptId,
                focusedCardId = cardId,
                focusedRunId = runId,
                viewModel = viewModel,
            ) { navController.popBackStack() }
        }


        composable("global_search") {
            val searchViewModel: GlobalSearchViewModel = hiltViewModel()
            GlobalSearchScreen(
                viewModel = searchViewModel,
                onBack = { navController.popBackStack() },
            ) { route -> navController.navigate(route) }
        }

        composable(
            route = "review_dashboard?mistakeId={mistakeId}",
            { type = NavType.LongType; defaultValue = -1L }),
        ) { backStackEntry ->
            val mistakeIdInput = backStackEntry.arguments?.getLong("mistakeId") ?: -1L
            val mistakeId = if (mistakeIdInput == -1L) null else mistakeIdInput
            val reviewViewModel: ReviewDashboardViewModel = hiltViewModel()
            ReviewDashboardScreen(
                viewModel = reviewViewModel,
                focusedMistakeId = mistakeId,
                onBack = { navController.popBackStack() },
            ) { route -> navController.navigate(route) }
        }

        composable("data_tools") {
            val dataToolsViewModel: DataToolsViewModel = hiltViewModel()
            DataToolsScreen(
                viewModel = dataToolsViewModel,
            ) { navController.popBackStack() }
        }

        composable("settings") {
            SettingsScreen(
                appModule = appModule,
                onBack = { returnToLibraryRoot() },
                onGlobalSearch = { navController.navigate("global_search") },
                onReviewDashboard = { navController.navigate("review_dashboard") },
            ) { navController.navigate("data_tools") }
        }

        composable(
            route = "category/{category}",
            { type = NavType.StringType }),
        ) { backStackEntry ->
            val category = backStackEntry.requireNonBlankStringArg("category")
            if (category == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val viewModel: CategoryQuestionsViewModel = hiltViewModel()

            LaunchedEffect(category) {
                viewModel.loadCategory(category)
            }

            CategoryQuestionsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            ) { categoryName ->
                navController.navigate("adaptive/CATEGORY/${Uri.encode(categoryName)}")
            }
        }

        composable(
            route = "quiz/{quizId}?sessionId={sessionId}",
            { type = NavType.LongType },
                navArgument("sessionId") { 
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { backStackEntry ->
            val quizId = backStackEntry.requirePositiveLongArg("quizId")
            if (quizId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val sessionIdInput = backStackEntry.arguments?.getLong("sessionId") ?: -1L
            val sessionId = if (sessionIdInput == -1L) null else sessionIdInput
            
            val quizViewModel: QuizViewModel = hiltViewModel()
            
            LaunchedEffect(quizId, sessionId) {
                quizViewModel.startQuiz(quizId, sessionId)
            }
            
            QuizPlayerScreen(
                viewModel = quizViewModel,
                onQuizFinished = { sId, _, _ ->
                    navController.navigate("summary/$sId") {
                        popUpTo("library") { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() },
            ) { category ->
                navController.navigate("category/${Uri.encode(category)}")
            }
        }

        composable(
            route = "sessions/{quizId}",
            { type = NavType.LongType }),
        ) { backStackEntry ->
            val quizId = backStackEntry.requirePositiveLongArg("quizId")
            if (quizId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val sessionViewModel: SessionViewModel = hiltViewModel()
            val questionsViewModel: QuizQuestionsViewModel = hiltViewModel()
            
            LaunchedEffect(quizId) {
                questionsViewModel.loadQuiz(quizId)
            }
            
            val questionsState by questionsViewModel.uiState.collectAsState()

            QuizDetailTabsScreen(
                quizId = quizId,
                quizTitle = questionsState.quiz?.title,
                sessionViewModel = sessionViewModel,
                questionsViewModel = questionsViewModel,
                dataStoreManager = appModule.dataStoreManager,
                onSessionSelected = { sessionId, isCompleted ->
                    if (isCompleted) {
                        navController.navigate("summary/$sessionId")
                    } else {
                        navController.navigate("quiz/$quizId?sessionId=$sessionId")
                    }
                },
                onBack = {
                    navController.navigate("library") {
                        popUpTo("library") { inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(
            route = "scanner/{quizId}",
            { type = NavType.LongType }),
        ) { backStackEntry ->
            val quizId = backStackEntry.requirePositiveLongArg("quizId")
            if (quizId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val scannerViewModel: ScannerViewModel = hiltViewModel()
            
            ScannerScreen(
                quizId = quizId,
                viewModel = scannerViewModel,
            ) { navController.popBackStack() }
        }

        composable(
            route = "adaptive/{type}/{id}",
            { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val type = backStackEntry.requireNonBlankStringArg("type")
            val id = backStackEntry.requireNonBlankStringArg("id")
            if ((type == null) || (id == null)) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val quizViewModel: QuizViewModel = hiltViewModel()
            
            LaunchedEffect(type, id) {
                quizViewModel.startAdaptiveTraining(type, id)
            }
            
            QuizPlayerScreen(
                viewModel = quizViewModel,
                onQuizFinished = { sId, _, _ ->
                    if (sId != -1L) {
                        navController.navigate("summary/$sId") {
                            popUpTo("library") { inclusive = false }
                        }
                    } else {
                        navController.navigate("library") {
                            popUpTo("library") { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "summary/{sessionId}",
            { type = NavType.LongType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.requirePositiveLongArg("sessionId")
            if (sessionId == null) {
                InvalidRouteScreen { navController.popBackStack() }
                return@composable
            }
            val summaryViewModel: SummaryViewModel = hiltViewModel()

            SummaryScreen(
                viewModel = summaryViewModel,
                sessionId = sessionId,
                onHomeClick = {
                    navController.navigate("library") {
                        popUpTo("library") { inclusive = true }
                    }
                },
                onRetryClick = { navController.popBackStack() },
            )
        }
    }
}
