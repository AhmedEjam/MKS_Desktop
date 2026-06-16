import re
import os
import json

with open('scripts/functions.json', 'r') as f:
    funcs = json.load(f)

# 1. Inject annotationDao into BookRepository
with open('core/data/src/main/java/com/ahmedyejam/mks/data/repository/BookRepository.kt', 'r') as f:
    book_code = f.read()

if "annotationDao: AnnotationDao" not in book_code:
    book_code = book_code.replace("private val assetRepository: AssetRepository",
                                  "private val assetRepository: AssetRepository,\n    private val annotationDao: AnnotationDao")
with open('core/data/src/main/java/com/ahmedyejam/mks/data/repository/BookRepository.kt', 'w') as f:
    f.write(book_code)

# 2. Add refreshFlashcardDeckStats to KnowledgeRepository
with open('core/data/src/main/java/com/ahmedyejam/mks/data/repository/KnowledgeRepository.kt', 'r') as f:
    know_code = f.read()

if "fun refreshFlashcardDeckStats" not in know_code:
    if "refreshFlashcardDeckStats" in funcs:
        know_code = know_code[:-2] + funcs["refreshFlashcardDeckStats"] + "\n}\n"

know_code = know_code.replace("    restoreBook(", "    bookRepository.restoreBook(")
know_code = re.sub(r'^\s*private\s+(suspend\s+)?fun\s+', r'    \1fun ', know_code, flags=re.MULTILINE)

with open('core/data/src/main/java/com/ahmedyejam/mks/data/repository/KnowledgeRepository.kt', 'w') as f:
    f.write(know_code)

# 3. Fix QuizRepository restoreBook
with open('core/data/src/main/java/com/ahmedyejam/mks/data/repository/QuizRepository.kt', 'r') as f:
    quiz_code = f.read()

quiz_code = quiz_code.replace("    restoreBook(", "    bookRepository.restoreBook(")
quiz_code = re.sub(r'^\s*private\s+(suspend\s+)?fun\s+', r'    \1fun ', quiz_code, flags=re.MULTILINE)

with open('core/data/src/main/java/com/ahmedyejam/mks/data/repository/QuizRepository.kt', 'w') as f:
    f.write(quiz_code)

# 4. Remove private completely in AssetRepository
with open('core/data/src/main/java/com/ahmedyejam/mks/data/repository/AssetRepository.kt', 'r') as f:
    asset_code = f.read()

asset_code = re.sub(r'^\s*private\s+(suspend\s+)?fun\s+', r'    \1fun ', asset_code, flags=re.MULTILINE)
with open('core/data/src/main/java/com/ahmedyejam/mks/data/repository/AssetRepository.kt', 'w') as f:
    f.write(asset_code)

print("Applied fixes.")
