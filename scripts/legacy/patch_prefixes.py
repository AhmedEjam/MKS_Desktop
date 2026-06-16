import re
import os

repo_dir = "core/data/src/main/java/com/ahmedyejam/mks/data/repository/"

book_funcs = ['updateLastStudied', 'updateLastEdited', 'refreshBookStats']
asset_funcs = [
    'syncQuestionCategories', 'replaceOwnerAssetReferences', 'releaseOwnerAssets',
    'softDeleteOwnerAnnotations', 'restoreOwnerAnnotations', 'permanentlyDeleteOwnerAnnotations',
    'softDeleteQuestionAnnotationTree', 'permanentlyDeleteQuestionAnnotationTree',
    'softDeleteQuizAnnotationTree', 'permanentlyDeleteQuizAnnotationTree',
    'softDeleteSlideshowAnnotationTree', 'restoreSlideshowAnnotationTree',
    'permanentlyDeleteSlideshowAnnotationTree', 'releaseQuestionAssets',
    'releaseQuizTreeAssets', 'releaseBookTreeAssets', 'refreshAssetReferencesForOwner',
    'refreshBookTreeAssets', 'verifyImagePath'
]

book_pattern = re.compile(r'\b(' + '|'.join(book_funcs) + r')\s*\(')
asset_pattern = re.compile(r'\b(' + '|'.join(asset_funcs) + r')\s*\(')

files_to_patch = ["QuizRepository.kt", "KnowledgeRepository.kt", "StudyRepository.kt", "BookRepository.kt"]

for fname in files_to_patch:
    path = os.path.join(repo_dir, fname)
    with open(path, 'r') as f:
        code = f.read()
    
    # Don't prefix if we are IN the repository that defines it
    if fname != "BookRepository.kt":
        code = book_pattern.sub(r'bookRepository.\1(', code)
    if fname != "AssetRepository.kt":
        code = asset_pattern.sub(r'assetRepository.\1(', code)
        
    # Ensure we didn't do something like bookRepository.bookRepository.foo
    code = code.replace("bookRepository.bookRepository.", "bookRepository.")
    code = code.replace("assetRepository.assetRepository.", "assetRepository.")
        
    with open(path, 'w') as f:
        f.write(code)
    print(f"Patched {fname}")

