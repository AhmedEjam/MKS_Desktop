import os
import re

def process_search_result():
    filepath = "core/data/src/main/java/com/ahmedyejam/mks/data/search/GlobalSearchResult.kt"
    with open(filepath, "r") as f:
        content = f.read()
    
    content = content.replace("import com.ahmedyejam.mks.ui.MksRoutes", "import com.ahmedyejam.mks.ui.*")
    content = content.replace("val route: String? = null", "val route: Any? = null")
    
    replacements = {
        "MksRoutes.LIBRARY": "LibraryRoute",
        "MksRoutes.sessions(it)": "SessionsRoute(it)",
        "MksRoutes.quizQuestions(it)": "QuizQuestionsRoute(it)",
        "MksRoutes.bookSources(it, id.toLongOrNull())": "BookSourcesRoute(it, id.toLongOrNull())",
        "MksRoutes.flashcards(it, id.toLongOrNull())": "FlashcardsRoute(it, id.toLongOrNull())",
        "MksRoutes.blueprint(it)": "BlueprintRoute(it)",
        "MksRoutes.slideshow(it, id.toLongOrNull())": "SlideshowRoute(it, id.toLongOrNull())",
        "MksRoutes.promptDeck(it)": "PromptDeckRoute(it)",
        "MksRoutes.promptDeck(it, cardId = id.toLongOrNull())": "PromptDeckRoute(it, cardId = id.toLongOrNull())",
        "MksRoutes.promptDeck(it, runId = id.toLongOrNull())": "PromptDeckRoute(it, runId = id.toLongOrNull())",
        "MksRoutes.reviewDashboard(it)": "ReviewDashboardRoute(it)",
        "MksRoutes.bookDashboard(it)": "BookDashboardRoute(it)",
        "MksRoutes.category(it)": "CategoryRoute(it)"
    }
    
    for old, new in replacements.items():
        content = content.replace(old, new)
        
    with open(filepath, "w") as f:
        f.write(content)

def process_review_repo():
    filepath = "core/data/src/main/java/com/ahmedyejam/mks/data/review/ReviewRepository.kt"
    with open(filepath, "r") as f:
        content = f.read()
    
    content = content.replace("import com.ahmedyejam.mks.ui.MksRoutes", "import com.ahmedyejam.mks.ui.*")
    
    replacements = {
        "MksRoutes.flashcards(it.deckId)": "FlashcardsRoute(it.deckId)",
        "MksRoutes.blueprint(it.id)": "BlueprintRoute(it.id)",
        "MksRoutes.REVIEW_DASHBOARD": "ReviewDashboardRoute()",
        "MksRoutes.quizQuestions(it.quizId)": "QuizQuestionsRoute(it.quizId)"
    }
    for old, new in replacements.items():
        content = content.replace(old, new)
        
    with open(filepath, "w") as f:
        f.write(content)

if __name__ == "__main__":
    process_search_result()
    process_review_repo()
    print("Done refactoring core data")
