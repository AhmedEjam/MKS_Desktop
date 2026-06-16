import json
import os
import re

repo_dir = "core/data/src/main/java/com/ahmedyejam/mks/data/repository/"

with open('scripts/functions.json', 'r') as f:
    funcs = json.load(f)

# Extract original imports from MksRepository.kt.bak
with open('core/data/src/main/java/com/ahmedyejam/mks/data/repository/MksRepository.kt.bak', 'r') as f:
    orig_code = f.read()

import_lines = []
for line in orig_code.split('\n'):
    if line.startswith("import "):
        import_lines.append(line)
        
header = "package com.ahmedyejam.mks.data.repository\n\n"
header += "\n".join(import_lines) + "\n"
header += "import javax.inject.Inject\nimport javax.inject.Singleton\n\n"

def generate_repo(name, func_names, constructor_args):
    repo_code = header
    repo_code += "@Singleton\n"
    repo_code += f"class {name} @Inject constructor(\n"
    repo_code += "    " + ",\n    ".join(constructor_args) + "\n"
    repo_code += ") {\n\n"
    
    for fname in func_names:
        if fname in funcs:
            repo_code += funcs[fname] + "\n"
            
    repo_code += "}\n"
    
    with open(os.path.join(repo_dir, f"{name}.kt"), "w") as f:
        f.write(repo_code)
    print(f"Generated {name}.kt")

asset_funcs = [
    'getQuestionAssets', 'insertQuestionAssets', 'deleteQuestionAsset',
    'getSourcesByBookId', 'getSourceById', 'insertSource', 'updateSource', 'deleteSource',
    'restoreSource', 'permanentlyDeleteSource', 'getSourceAssets', 'insertSourceAssets',
    'deleteSourceAsset', 'getReferencesForOwner', 'insertAssetReference', 'deleteAssetReference',
    'replaceOwnerAssetReferences', 'getSourcesByBookIdNow',
    'releaseOwnerAssets', 'softDeleteOwnerAnnotations', 'restoreOwnerAnnotations',
    'permanentlyDeleteOwnerAnnotations', 'softDeleteQuestionAnnotationTree',
    'permanentlyDeleteQuestionAnnotationTree', 'softDeleteQuizAnnotationTree',
    'permanentlyDeleteQuizAnnotationTree', 'softDeleteSlideshowAnnotationTree',
    'restoreSlideshowAnnotationTree', 'permanentlyDeleteSlideshowAnnotationTree',
    'syncQuestionCategories', 'releaseQuestionAssets', 'releaseQuizTreeAssets', 'releaseBookTreeAssets',
    'refreshAssetReferencesForOwner', 'refreshBookTreeAssets', 'verifyImagePath'
]
asset_args = [
    "private val questionAssetDao: QuestionAssetDao", "private val sourceDocumentDao: SourceDocumentDao",
    "private val sourceDocumentAssetDao: com.ahmedyejam.mks.data.local.dao.SourceDocumentAssetDao",
    "private val assetReferenceDao: AssetReferenceDao", "private val fileManager: FileManager",
    "private val bookDao: BookDao", "private val quizDao: QuizDao", "private val questionDao: QuestionDao",
    "private val questionCategoryDao: QuestionCategoryDao",
    "private val flashcardDeckDao: FlashcardDeckDao", "private val flashcardDao: FlashcardDao",
    "private val slideshowCourseDao: SlideshowCourseDao", "private val courseSlideDao: CourseSlideDao",
    "private val annotationDao: AnnotationDao"
]

book_funcs = [
    'getAllWorkspaces', 'getDefaultWorkspace', 'getOrCreateDefaultWorkspace',
    'ensureWorkspaceSettings', 'getWorkspaceById', 'getWorkspaceByIdIncludingDeleted',
    'getDeletedWorkspaces', 'getWorkspaceByExternalId', 'insertWorkspace',
    'updateWorkspace', 'deleteWorkspace', 'restoreWorkspace', 'permanentlyDeleteWorkspace',
    'getWorkspaceSettings', 'insertWorkspaceSettings', 'updateWorkspaceSettings',
    
    'getAllBooks', 'getBooksByWorkspace', 'getAllFields', 'getBookById',
    'getBookStudyBundle', 'insertBook', 'updateBook', 'deleteBook',
    'restoreBook', 'permanentlyDeleteBook', 'previewBookDeletion',
    
    'updateLastStudied', 'updateLastEdited', 'refreshBookStats'
]
book_args = [
    "private val workspaceDao: WorkspaceDao", "private val bookDao: BookDao",
    "private val quizDao: QuizDao", "private val questionDao: QuestionDao",
    "private val fileManager: FileManager", "private val deletePreviewService: DeletePreviewService? = null",
    "private val assetRepository: AssetRepository"
]

quiz_funcs = [
    'getQuizzesByBookId', 'getQuizzesByCategory', 'getAllQuizzesFlow',
    'getAllCategories', 'getQuizById', 'insertQuiz', 'updateQuiz', 'deleteQuiz', 'restoreQuiz', 'permanentlyDeleteQuiz',
    'previewQuizDeletion', 'previewCategoryMerge', 'applyCategoryMerge',
    'clearMarksForQuizWithPreview', 'applyClearMarksForQuiz', 'refreshQuizStats',
    
    'getQuestionsByQuizId', 'searchAllQuestions', 'getQuestionsByCategoryFlow',
    'getQuestionsByCategory', 'getQuestionById', 'getQuestionsByIds',
    'insertQuestion', 'insertQuestions', 'updateQuestion', 'deleteQuestion',
    'restoreQuestion', 'permanentlyDeleteQuestion', 'updateQuestionMark',
    'updateQuestionDrop', 'clearQuizQuestions',
    
    'getOptionsByQuestionId', 'insertOptions', 'updateOptions', 'deleteOptions',
    
    'getCategoryMetadataFlow', 'updateCategoryMetadata', 'clearAllCategoryMetadata',
    
    'getActiveSession', 'getAllSessionsForQuiz', 'getSessionById',
    'insertSession', 'updateSession', 'deleteSession'
]
quiz_args = [
    "private val bookDao: BookDao", "private val quizDao: QuizDao", "private val questionDao: QuestionDao",
    "private val sessionDao: SessionDao", "private val questionCategoryDao: QuestionCategoryDao",
    "private val categoryMetadataDao: CategoryMetadataDao",
    "private val deletePreviewService: DeletePreviewService? = null",
    "private val categoryMergePreviewService: CategoryMergePreviewService? = null",
    "private val clearMarksPreviewService: ClearMarksPreviewService? = null",
    "private val bookRepository: BookRepository", "private val assetRepository: AssetRepository",
    "private val fileManager: FileManager"
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
    'updateKnowledgeSession', 'deleteKnowledgeSession',
    
    'deriveQuestionsFromDocument', 'extractQuestionsFromMarkdown',
    'deriveSlidesFromQuestions', 'deriveNotesFromQuestions', 'deriveFlashcardsFromQuestions'
]
knowledge_args = [
    "private val flashcardDeckDao: FlashcardDeckDao", "private val flashcardDao: FlashcardDao",
    "private val slideshowCourseDao: SlideshowCourseDao", "private val courseSlideDao: CourseSlideDao",
    "private val noteBlueprintDao: NoteBlueprintDao", "private val noteCollectionDao: com.ahmedyejam.mks.data.local.dao.NoteCollectionDao",
    "private val promptDao: PromptDao", "private val promptDeckDao: PromptDeckDao", "private val promptCardDao: PromptCardDao", "private val promptRunDao: PromptRunDao",
    "private val knowledgeStudySessionDao: KnowledgeStudySessionDao", "private val fileManager: FileManager",
    "private val bookDao: BookDao", "private val quizDao: QuizDao", "private val questionDao: QuestionDao", "private val sourceDocumentDao: SourceDocumentDao",
    "private val bookRepository: BookRepository", "private val assetRepository: AssetRepository"
]

study_funcs = [
    'getAllMistakes', 'getMistakesByBookId', 'getMistakesByQuizId', 'insertMistake',
    'updateMistake', 'deleteMistake', 'getAnnotationsByWorkspaceId',
    'getAnnotationsByBookId', 'getAnnotationsByOwner', 'getAnnotationById',
    'insertAnnotation', 'updateAnnotation', 'deleteAnnotation'
]
study_args = [
    "private val mistakeLogDao: MistakeLogDao", "private val annotationDao: AnnotationDao", "private val bookDao: BookDao",
    "private val bookRepository: BookRepository", "private val assetRepository: AssetRepository"
]

generate_repo("AssetRepository", asset_funcs, asset_args)
generate_repo("BookRepository", book_funcs, book_args)
generate_repo("QuizRepository", quiz_funcs, quiz_args)
generate_repo("KnowledgeRepository", knowledge_funcs, knowledge_args)
generate_repo("StudyRepository", study_funcs, study_args)

