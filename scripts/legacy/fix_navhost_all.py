import re

def fix_navhost():
    filepath = "feature/ui/src/main/java/com/ahmedyejam/mks/ui/MksNavHost.kt"
    with open(filepath, "r") as f:
        content = f.read()

    # 1. Replace the ones with no parameters
    content = content.replace('composable("global_search") {', 'composable<GlobalSearchRoute> {')
    content = content.replace('composable("data_tools") {', 'composable<DataToolsRoute> {')
    content = content.replace('composable("settings") {', 'composable<SettingsRoute> {')

    # 2. Replace the complex ones
    replacements = [
        (r'composable\(\s*route\s*=\s*"quiz_questions/\{quizId\}\?questionId=\{questionId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?val questionId = [^\n]*\n', 
         'composable<QuizQuestionsRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<QuizQuestionsRoute>()\n            val quizId = route.quizId\n            val questionId = route.questionId\n'),
         
        (r'composable\(\s*route\s*=\s*"category/\{category\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?return@composable\s*\}\n', 
         'composable<CategoryRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<CategoryRoute>()\n            val category = route.category\n'),
         
        (r'composable\(\s*route\s*=\s*"adaptive/\{type\}/\{id\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?return@composable\s*\}\n', 
         'composable<AdaptiveRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<AdaptiveRoute>()\n            val type = route.type\n            val id = route.id\n'),
         
        (r'composable\(\s*route\s*=\s*"quiz/\{quizId\}\?sessionId=\{sessionId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?val sessionId = if \(sessionIdInput == -1L\) null else sessionIdInput\n', 
         'composable<QuizRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<QuizRoute>()\n            val quizId = route.quizId\n            val sessionId = route.sessionId?.takeIf { it > 0 }\n'),
         
        (r'composable\(\s*route\s*=\s*"sessions/\{quizId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?return@composable\s*\}\n', 
         'composable<SessionsRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<SessionsRoute>()\n            val quizId = route.quizId\n'),
         
        (r'composable\(\s*route\s*=\s*"summary/\{sessionId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?return@composable\s*\}\n', 
         'composable<SummaryRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<SummaryRoute>()\n            val sessionId = route.sessionId\n'),
         
        (r'composable\(\s*route\s*=\s*"scanner/\{quizId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?return@composable\s*\}\n', 
         'composable<ScannerRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<ScannerRoute>()\n            val quizId = route.quizId\n'),
         
        (r'composable\(\s*route\s*=\s*"book_dashboard/\{bookId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?return@composable\s*\}\n', 
         'composable<BookDashboardRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BookDashboardRoute>()\n            val bookId = route.bookId\n'),
         
        (r'composable\(\s*route\s*=\s*"book_slideshows/\{bookId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?return@composable\s*\}\n', 
         'composable<BookSlideshowsRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BookSlideshowsRoute>()\n            val bookId = route.bookId\n'),
         
        (r'composable\(\s*route\s*=\s*"book_blueprints/\{bookId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?return@composable\s*\}\n', 
         'composable<BookBlueprintsRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BookBlueprintsRoute>()\n            val bookId = route.bookId\n'),
         
        (r'composable\(\s*route\s*=\s*"book_sources/\{bookId\}\?sourceId=\{sourceId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?val sourceId = [^\n]*\n', 
         'composable<BookSourcesRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BookSourcesRoute>()\n            val bookId = route.bookId\n            val sourceId = route.sourceId\n'),
         
        (r'composable\(\s*route\s*=\s*"book_notes/\{bookId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?return@composable\s*\}\n', 
         'composable<BookNotesRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BookNotesRoute>()\n            val bookId = route.bookId\n'),
         
        (r'composable\(\s*route\s*=\s*"book_prompts/\{bookId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?return@composable\s*\}\n', 
         'composable<BookPromptsRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BookPromptsRoute>()\n            val bookId = route.bookId\n'),
         
        (r'composable\(\s*route\s*=\s*"flashcards/\{deckId\}\?cardId=\{cardId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?val cardId = [^\n]*\n', 
         'composable<FlashcardsRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<FlashcardsRoute>()\n            val deckId = route.deckId\n            val cardId = route.cardId\n'),
         
        (r'composable\(\s*route\s*=\s*"slideshow/\{courseId\}\?slideId=\{slideId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?val slideId = [^\n]*\n', 
         'composable<SlideshowRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<SlideshowRoute>()\n            val courseId = route.courseId\n            val slideId = route.slideId\n'),
         
        (r'composable\(\s*route\s*=\s*"blueprint/\{noteId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?return@composable\s*\}\n', 
         'composable<BlueprintRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<BlueprintRoute>()\n            val noteId = route.noteId\n'),
         
        (r'composable\(\s*route\s*=\s*"prompt_deck/\{promptId\}\?cardId=\{cardId\}&runId=\{runId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?val runIdInput = [^\n]*\n', 
         'composable<PromptDeckRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<PromptDeckRoute>()\n            val promptId = route.promptId\n            val cardId = route.cardId\n            val runId = route.runId\n'),
         
        (r'composable\(\s*route\s*=\s*"review_dashboard\?mistakeId=\{mistakeId\}"[\s\S]*?\) \{ backStackEntry ->[\s\S]*?val mistakeIdInput = [^\n]*\n', 
         'composable<ReviewDashboardRoute> { backStackEntry ->\n            val route = backStackEntry.toRoute<ReviewDashboardRoute>()\n            val mistakeId = route.mistakeId\n')
    ]

    for pattern, replacement in replacements:
        content, count = re.subn(pattern, replacement, content)
        if count == 0:
            print(f"Failed to match: {pattern}")
        else:
            print(f"Matched and replaced {count} times")

    # Final cleanup just in case there are missing imports
    if "import androidx.navigation.toRoute" not in content:
        content = "import androidx.navigation.toRoute\n" + content
        
    with open(filepath, "w") as f:
        f.write(content)

if __name__ == "__main__":
    fix_navhost()
