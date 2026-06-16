import os
import re

repo_dir = "core/data/src/main/java/com/ahmedyejam/mks/data/repository/"
files = ["BookRepository.kt", "QuizRepository.kt", "KnowledgeRepository.kt", "StudyRepository.kt", "AssetRepository.kt"]

for fname in files:
    path = os.path.join(repo_dir, fname)
    with open(path, 'r') as f:
        code = f.read()
    
    # Remove 'private ' from function declarations so they can be called by other Repositories
    code = re.sub(r'^\s*private\s+(suspend\s+)?fun\s+', r'    \1fun ', code, flags=re.MULTILINE)
    
    # Fix QuizRepository missing questionAssetDao
    if fname == "QuizRepository.kt":
        if "questionAssetDao: QuestionAssetDao" not in code:
            code = code.replace("private val fileManager: FileManager", 
                                "private val fileManager: FileManager,\n    private val questionAssetDao: QuestionAssetDao")
                                
    with open(path, 'w') as f:
        f.write(code)
    print(f"Fixed {fname}")

