import re
import os

def refactor_navhost():
    filepath = "feature/ui/src/main/java/com/ahmedyejam/mks/ui/MksNavHost.kt"
    with open(filepath, "r") as f:
        content = f.read()

    # Add necessary imports for type-safe navigation
    imports_to_add = """
import androidx.navigation.toRoute
import com.ahmedyejam.mks.ui.*
import com.ahmedyejam.mks.ui.category.CategoryRoute
import com.ahmedyejam.mks.ui.quiz.*
import com.ahmedyejam.mks.ui.booktools.*
import com.ahmedyejam.mks.ui.summary.*
import com.ahmedyejam.mks.ui.scanner.*
import com.ahmedyejam.mks.ui.session.*
"""
    if "import androidx.navigation.toRoute" not in content:
        content = content.replace("import androidx.navigation.compose.composable", "import androidx.navigation.compose.composable\n" + imports_to_add)

    # Replace startDestination
    content = content.replace("val startDestination = if (showWelcomeOnStartup) MksRoutes.WELCOME else MksRoutes.LIBRARY", "val startDestination = if (showWelcomeOnStartup) WelcomeRoute else LibraryRoute")
    
    # Replace navigate calls
    navigate_replacements = {
        "navController.navigate(MksRoutes.LIBRARY)": "navController.navigate(LibraryRoute)",
        "popUpTo(MksRoutes.LIBRARY)": "popUpTo(LibraryRoute)",
        "popUpTo(MksRoutes.WELCOME)": "popUpTo(WelcomeRoute)",
        "navController.navigate(MksRoutes.sessions(quizId))": "navController.navigate(SessionsRoute(quizId))",
        "navController.navigate(MksRoutes.scanner(quizId))": "navController.navigate(ScannerRoute(quizId))",
        "navController.navigate(MksRoutes.adaptive(type, id))": "navController.navigate(AdaptiveRoute(type, id))",
        "navController.navigate(MksRoutes.quizQuestions(quizId))": "navController.navigate(QuizQuestionsRoute(quizId))",
        "navController.navigate(MksRoutes.category(category))": "navController.navigate(CategoryRoute(category))",
        "navController.navigate(MksRoutes.SETTINGS)": "navController.navigate(SettingsRoute)",
        "navController.navigate(MksRoutes.bookDashboard(bookId))": "navController.navigate(BookDashboardRoute(bookId))",
        "navController.navigate(MksRoutes.sessions(it))": "navController.navigate(SessionsRoute(it))",
        "navController.navigate(MksRoutes.flashcards(it))": "navController.navigate(FlashcardsRoute(it))",
        "navController.navigate(MksRoutes.slideshow(it))": "navController.navigate(SlideshowRoute(it))",
        "navController.navigate(MksRoutes.blueprint(it))": "navController.navigate(BlueprintRoute(it))",
        "navController.navigate(MksRoutes.promptDeck(it))": "navController.navigate(PromptDeckRoute(it))",
        "navController.navigate(MksRoutes.bookSources(bookId, it))": "navController.navigate(BookSourcesRoute(bookId, it))",
        "navController.navigate(MksRoutes.reviewDashboard(it.toLongOrNull()))": "navController.navigate(ReviewDashboardRoute(it.toLongOrNull()))",
        "navController.navigate(MksRoutes.summary(sessionId))": "navController.navigate(SummaryRoute(sessionId))",
        "popUpTo(MksRoutes.quiz(quizId, sessionId))": "popUpTo(QuizRoute(quizId, sessionId))",
        "popUpTo(MksRoutes.sessions(quizId))": "popUpTo(SessionsRoute(quizId))",
        "navController.navigate(MksRoutes.quiz(quizId, sessionId))": "navController.navigate(QuizRoute(quizId, sessionId))",
        "popUpTo(MksRoutes.scanner(quizId))": "popUpTo(ScannerRoute(quizId))",
        "navController.navigate(MksRoutes.bookSlideshows(bookId))": "navController.navigate(BookSlideshowsRoute(bookId))",
        "navController.navigate(MksRoutes.bookBlueprints(bookId))": "navController.navigate(BookBlueprintsRoute(bookId))",
        "navController.navigate(MksRoutes.bookNotes(bookId))": "navController.navigate(BookNotesRoute(bookId))",
        "navController.navigate(MksRoutes.bookSources(bookId))": "navController.navigate(BookSourcesRoute(bookId))",
        "navController.navigate(MksRoutes.bookPrompts(bookId))": "navController.navigate(BookPromptsRoute(bookId))",
    }
    
    for old, new in navigate_replacements.items():
        content = content.replace(old, new)
        
    # Now replace composable(...) blocks
    # WELCOME
    content = content.replace("composable(MksRoutes.WELCOME)", "composable<WelcomeRoute>")
    # LIBRARY
    content = content.replace("composable(MksRoutes.LIBRARY)", "composable<LibraryRoute>")
    # SETTINGS
    content = content.replace("composable(MksRoutes.SETTINGS)", "composable<SettingsRoute>")
    # GLOBAL_SEARCH
    content = content.replace("composable(MksRoutes.GLOBAL_SEARCH)", "composable<GlobalSearchRoute>")
    # DATA_TOOLS
    content = content.replace("composable(MksRoutes.DATA_TOOLS)", "composable<DataToolsRoute>")
    
    # REVIEW_DASHBOARD
    content = re.sub(
        r'composable\(\s*route = "review_dashboard\?mistakeId=\{mistakeId\}"[^)]*\)\s*\{[^}]*val mistakeId = backStackEntry.arguments\?.getString\("mistakeId"\)\?.toLongOrNull\(\)',
        'composable<ReviewDashboardRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<ReviewDashboardRoute>()\n            val mistakeId = route.mistakeId',
        content, flags=re.DOTALL
    )
    
    # quiz_questions
    content = re.sub(
        r'composable\(\s*route = "quiz_questions/\{quizId\}\?questionId=\{questionId\}"[^)]*\)\s*\{[^}]*val quizId = backStackEntry.requirePositiveLongArg\("quizId"\)\s*if \(quizId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}\s*val questionId = backStackEntry\.arguments\?\.getLong\("questionId", -1L\)\?.takeIf \{ it > 0 \}',
        'composable<QuizQuestionsRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<QuizQuestionsRoute>()\n            val quizId = route.quizId\n            val questionId = route.questionId?.takeIf { it > 0 }',
        content, flags=re.DOTALL
    )
    
    # category
    content = re.sub(
        r'composable\(\s*route = "category/\{category\}"[^)]*\)\s*\{[^}]*val category = backStackEntry.requireNonBlankStringArg\("category"\)\s*if \(category == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}',
        'composable<CategoryRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<CategoryRoute>()\n            val category = route.category',
        content, flags=re.DOTALL
    )
    
    # adaptive
    content = re.sub(
        r'composable\(\s*route = "adaptive/\{type\}/\{id\}"[^)]*\)\s*\{[^}]*val type = backStackEntry.requireNonBlankStringArg\("type"\)\s*val id = backStackEntry.requireNonBlankStringArg\("id"\)\s*if \(type == null \|\| id == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}',
        'composable<AdaptiveRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<AdaptiveRoute>()\n            val type = route.type\n            val id = route.id',
        content, flags=re.DOTALL
    )
    
    # sessions
    content = re.sub(
        r'composable\(\s*route = "sessions/\{quizId\}"[^)]*\)\s*\{[^}]*val quizId = backStackEntry.requirePositiveLongArg\("quizId"\)\s*if \(quizId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}',
        'composable<SessionsRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<SessionsRoute>()\n            val quizId = route.quizId',
        content, flags=re.DOTALL
    )
    
    # quiz
    content = re.sub(
        r'composable\(\s*route = "quiz/\{quizId\}\?sessionId=\{sessionId\}"[^)]*\)\s*\{[^}]*val quizId = backStackEntry.requirePositiveLongArg\("quizId"\)\s*if \(quizId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}\s*val sessionId = backStackEntry\.arguments\?\.getLong\("sessionId", -1L\)\?.takeIf \{ it > 0 \}',
        'composable<QuizRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<QuizRoute>()\n            val quizId = route.quizId\n            val sessionId = route.sessionId?.takeIf { it > 0 }',
        content, flags=re.DOTALL
    )
    
    # summary
    content = re.sub(
        r'composable\(\s*route = "summary/\{sessionId\}"[^)]*\)\s*\{[^}]*val sessionId = backStackEntry.requirePositiveLongArg\("sessionId"\)\s*if \(sessionId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}',
        'composable<SummaryRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<SummaryRoute>()\n            val sessionId = route.sessionId',
        content, flags=re.DOTALL
    )
    
    # scanner
    content = re.sub(
        r'composable\(\s*route = "scanner/\{quizId\}"[^)]*\)\s*\{[^}]*val quizId = backStackEntry.requirePositiveLongArg\("quizId"\)\s*if \(quizId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}',
        'composable<ScannerRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<ScannerRoute>()\n            val quizId = route.quizId',
        content, flags=re.DOTALL
    )
    
    # book_dashboard
    content = re.sub(
        r'composable\(\s*route = "book_dashboard/\{bookId\}"[^)]*\)\s*\{[^}]*val bookId = backStackEntry.requirePositiveLongArg\("bookId"\)\s*if \(bookId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}',
        'composable<BookDashboardRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BookDashboardRoute>()\n            val bookId = route.bookId',
        content, flags=re.DOTALL
    )
    
    # book_slideshows
    content = re.sub(
        r'composable\(\s*route = "book_slideshows/\{bookId\}"[^)]*\)\s*\{[^}]*val bookId = backStackEntry.requirePositiveLongArg\("bookId"\)\s*if \(bookId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}',
        'composable<BookSlideshowsRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BookSlideshowsRoute>()\n            val bookId = route.bookId',
        content, flags=re.DOTALL
    )
    
    # book_blueprints
    content = re.sub(
        r'composable\(\s*route = "book_blueprints/\{bookId\}"[^)]*\)\s*\{[^}]*val bookId = backStackEntry.requirePositiveLongArg\("bookId"\)\s*if \(bookId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}',
        'composable<BookBlueprintsRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BookBlueprintsRoute>()\n            val bookId = route.bookId',
        content, flags=re.DOTALL
    )
    
    # book_sources
    content = re.sub(
        r'composable\(\s*route = "book_sources/\{bookId\}\?sourceId=\{sourceId\}"[^)]*\)\s*\{[^}]*val bookId = backStackEntry.requirePositiveLongArg\("bookId"\)\s*if \(bookId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}\s*val sourceId = backStackEntry\.arguments\?\.getLong\("sourceId", -1L\)\?.takeIf \{ it > 0 \}',
        'composable<BookSourcesRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BookSourcesRoute>()\n            val bookId = route.bookId\n            val sourceId = route.sourceId?.takeIf { it > 0 }',
        content, flags=re.DOTALL
    )
    
    # book_notes
    content = re.sub(
        r'composable\(\s*route = "book_notes/\{bookId\}"[^)]*\)\s*\{[^}]*val bookId = backStackEntry.requirePositiveLongArg\("bookId"\)\s*if \(bookId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}',
        'composable<BookNotesRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BookNotesRoute>()\n            val bookId = route.bookId',
        content, flags=re.DOTALL
    )
    
    # book_prompts
    content = re.sub(
        r'composable\(\s*route = "book_prompts/\{bookId\}"[^)]*\)\s*\{[^}]*val bookId = backStackEntry.requirePositiveLongArg\("bookId"\)\s*if \(bookId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}',
        'composable<BookPromptsRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BookPromptsRoute>()\n            val bookId = route.bookId',
        content, flags=re.DOTALL
    )
    
    # flashcards
    content = re.sub(
        r'composable\(\s*route = "flashcards/\{deckId\}\?cardId=\{cardId\}"[^)]*\)\s*\{[^}]*val deckId = backStackEntry.requirePositiveLongArg\("deckId"\)\s*if \(deckId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}\s*val cardId = backStackEntry\.arguments\?\.getLong\("cardId", -1L\)\?.takeIf \{ it > 0 \}',
        'composable<FlashcardsRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<FlashcardsRoute>()\n            val deckId = route.deckId\n            val cardId = route.cardId?.takeIf { it > 0 }',
        content, flags=re.DOTALL
    )
    
    # slideshow
    content = re.sub(
        r'composable\(\s*route = "slideshow/\{courseId\}\?slideId=\{slideId\}"[^)]*\)\s*\{[^}]*val courseId = backStackEntry.requirePositiveLongArg\("courseId"\)\s*if \(courseId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}\s*val slideId = backStackEntry\.arguments\?\.getLong\("slideId", -1L\)\?.takeIf \{ it > 0 \}',
        'composable<SlideshowRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<SlideshowRoute>()\n            val courseId = route.courseId\n            val slideId = route.slideId?.takeIf { it > 0 }',
        content, flags=re.DOTALL
    )
    
    # blueprint
    content = re.sub(
        r'composable\(\s*route = "blueprint/\{noteId\}"[^)]*\)\s*\{[^}]*val noteId = backStackEntry.requirePositiveLongArg\("noteId"\)\s*if \(noteId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}',
        'composable<BlueprintRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BlueprintRoute>()\n            val noteId = route.noteId',
        content, flags=re.DOTALL
    )
    
    # prompt_deck
    content = re.sub(
        r'composable\(\s*route = "prompt_deck/\{promptId\}\?cardId=\{cardId\}&runId=\{runId\}"[^)]*\)\s*\{[^}]*val promptId = backStackEntry.requirePositiveLongArg\("promptId"\)\s*if \(promptId == null\) \{\s*InvalidRouteScreen.*?\n[^\n]*\}\s*val cardId = backStackEntry\.arguments\?\.getLong\("cardId", -1L\)\?.takeIf \{ it > 0 \}\s*val runId = backStackEntry\.arguments\?\.getLong\("runId", -1L\)\?.takeIf \{ it > 0 \}',
        'composable<PromptDeckRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<PromptDeckRoute>()\n            val promptId = route.promptId\n            val cardId = route.cardId?.takeIf { it > 0 }\n            val runId = route.runId?.takeIf { it > 0 }',
        content, flags=re.DOTALL
    )
    
    # General cleanup for unused imports (we might do this via Android Studio / ktlint later, but let's try to remove NavArgument)
    content = content.replace("import androidx.navigation.navArgument", "")
    content = content.replace("import androidx.navigation.NavType", "")

    with open(filepath, "w") as f:
        f.write(content)

if __name__ == "__main__":
    refactor_navhost()
    print("MksNavHost refactoring complete")
