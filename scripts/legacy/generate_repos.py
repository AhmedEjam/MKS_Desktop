import json
import os

repo_dir = "core/data/src/main/java/com/ahmedyejam/mks/data/repository/"

with open('scripts/functions.json', 'r') as f:
    funcs = json.load(f)

with open(os.path.join(repo_dir, "MksRepository.kt"), 'r') as f:
    code = f.read()

class_idx = code.find("class MksRepository")
header = code[:class_idx]

# Define the groupings
book_funcs = [
    'getAllWorkspaces', 'getDefaultWorkspace', 'getOrCreateDefaultWorkspace',
    'ensureWorkspaceSettings', 'getWorkspaceById', 'getWorkspaceByIdIncludingDeleted',
    'getDeletedWorkspaces', 'getWorkspaceByExternalId', 'insertWorkspace',
    'updateWorkspace', 'deleteWorkspace', 'restoreWorkspace', 'permanentlyDeleteWorkspace',
    'getWorkspaceSettings', 'insertWorkspaceSettings', 'updateWorkspaceSettings',
    
    'getAllBooks', 'getBooksByWorkspace', 'getAllFields', 'getBookById',
    'getBookStudyBundle', 'insertBook', 'updateBook', 'deleteBook',
    'restoreBook', 'permanentlyDeleteBook', 'previewBookDeletion',
]

quiz_funcs = [
    'getQuizzesByBookId', 'getQuizzesByCategory', 'getAllQuizzesFlow',
    'getAllCategories', 'getQuizById', 'insertQuiz', 'updateQuiz',
    'previewQuizDeletion', 'previewCategoryMerge', 'applyCategoryMerge',
    'clearMarksForQuizWithPreview', 'applyClearMarksForQuiz',
    
    'getQuestionsByQuizId', 'searchAllQuestions', 'getQuestionsByCategoryFlow',
    'getQuestionsByCategory', 'getQuestionById', 'getQuestionsByIds',
    'insertQuestion', 'insertQuestions', 'updateQuestion', 'deleteQuestion',
    'restoreQuestion', 'permanentlyDeleteQuestion', 'updateQuestionMark',
    'updateQuestionDrop', 'syncQuestionCategories', 'clearQuizQuestions',
    
    'getOptionsByQuestionId', 'insertOptions', 'updateOptions', 'deleteOptions',
    
    'getCategoryMetadataFlow', 'updateCategoryMetadata', 'clearAllCategoryMetadata',
    
    'getQuestionAssets', 'insertQuestionAssets', 'deleteQuestionAsset',
    
    # Session stuff
    'getActiveSession', 'getAllSessionsForQuiz', 'getSessionById',
    'insertSession', 'updateSession', 'deleteSession'
]

knowledge_funcs = [
    'getFlashcardDecksByBookId', 'getFlashcardDeckById', 'insertFlashcardDeck',
    'updateFlashcardDeck', 'deleteFlashcardDeck', 'restoreFlashcardDeck',
    'permanentlyDeleteFlashcardDeck', 'getFlashcardsByDeckId', 'getFlashcardById',
    'insertFlashcard', 'updateFlashcard', 'deleteFlashcard', 'restoreFlashcard',
    'permanentlyDeleteFlashcard', 'getDueFlashcards',
    
    'getCoursesByBookId', 'getCourseById', 'insertCourse', 'updateCourse',
    'deleteCourse', 'restoreCourse', 'permanentlyDeleteCourse',
    'getSlidesByCourseId', 'getSlideById', 'insertSlide', 'updateSlide',
    'deleteSlide', 'restoreSlide', 'permanentlyDeleteSlide',
    
    'getNotesByBookId', 'getNoteById', 'insertNote', 'updateNote',
    'deleteNote', 'restoreNote', 'permanentlyDeleteNote',
    'getCollectionsByBookId', 'getCollectionById', 'insertCollection',
    'updateCollection', 'deleteCollection',
    
    'getPromptsByBookId', 'getPromptById', 'insertPrompt', 'updatePrompt',
    'deletePrompt', 'getDecksByBookId', 'getDeckById', 'insertDeck',
    'updateDeck', 'deleteDeck', 'getCardsByDeckId', 'getCardById',
    'insertCard', 'updateCard', 'deleteCard', 'getRunsByCardId',
    'insertRun', 'deleteRun',
    
    'getActiveKnowledgeSession', 'getKnowledgeSessionsByAsset', 'insertKnowledgeSession',
    'updateKnowledgeSession', 'deleteKnowledgeSession'
]

def generate_repo(name, func_names, extra_imports, constructor_args):
    repo_code = header
    repo_code += extra_imports + "\n\n"
    repo_code += "@javax.inject.Singleton\n"
    repo_code += f"class {name} @javax.inject.Inject constructor(\n"
    repo_code += "    " + ",\n    ".join(constructor_args) + "\n"
    repo_code += ") {\n\n"
    
    for fname in func_names:
        if fname in funcs:
            repo_code += funcs[fname] + "\n\n"
        else:
            # print(f"Warning: {fname} not found in MksRepository")
            pass
            
    repo_code += "}\n"
    
    with open(os.path.join(repo_dir, f"{name}.kt"), "w") as f:
        f.write(repo_code)
    print(f"Generated {name}.kt")

book_args = [
    "private val workspaceDao: WorkspaceDao",
    "private val bookDao: BookDao",
    "private val quizDao: QuizDao",
    "private val questionDao: QuestionDao",
    "private val flashcardDeckDao: FlashcardDeckDao",
    "private val slideshowCourseDao: SlideshowCourseDao",
    "private val noteBlueprintDao: NoteBlueprintDao",
    "private val promptDao: PromptDao",
    "private val promptDeckDao: PromptDeckDao",
    "private val sourceDocumentDao: SourceDocumentDao",
    "private val mistakeLogDao: MistakeLogDao",
    "private val deletePreviewService: DeletePreviewService? = null",
    "private val fileManager: FileManager",
    "private val assetRepository: AssetRepository"
]

quiz_args = [
    "private val quizDao: QuizDao",
    "private val questionDao: QuestionDao",
    "private val questionCategoryDao: QuestionCategoryDao",
    "private val categoryMetadataDao: CategoryMetadataDao",
    "private val questionAssetDao: QuestionAssetDao",
    "private val sessionDao: SessionDao",
    "private val deletePreviewService: DeletePreviewService? = null",
    "private val categoryMergePreviewService: CategoryMergePreviewService? = null",
    "private val clearMarksPreviewService: ClearMarksPreviewService? = null",
    "private val fileManager: FileManager",
    "private val assetRepository: AssetRepository"
]

knowledge_args = [
    "private val flashcardDeckDao: FlashcardDeckDao",
    "private val flashcardDao: FlashcardDao",
    "private val slideshowCourseDao: SlideshowCourseDao",
    "private val courseSlideDao: CourseSlideDao",
    "private val noteCollectionDao: com.ahmedyejam.mks.data.local.dao.NoteCollectionDao",
    "private val noteBlueprintDao: NoteBlueprintDao",
    "private val promptDao: PromptDao",
    "private val promptDeckDao: PromptDeckDao",
    "private val promptCardDao: PromptCardDao",
    "private val promptRunDao: PromptRunDao",
    "private val knowledgeStudySessionDao: KnowledgeStudySessionDao",
    "private val fileManager: FileManager",
    "private val assetRepository: AssetRepository"
]

generate_repo("BookRepository", book_funcs, "", book_args)
generate_repo("QuizRepository", quiz_funcs, "", quiz_args)
generate_repo("KnowledgeRepository", knowledge_funcs, "", knowledge_args)

