import os

bak_path = "core/data/src/main/java/com/ahmedyejam/mks/data/repository/MksRepository.kt.bak"
with open(bak_path, "r") as f:
    lines = f.readlines()

# Find where the class declaration starts
class_decl_idx = 0
for i, line in enumerate(lines):
    if "class MksRepository" in line:
        class_decl_idx = i
        break

header_lines = lines[:class_decl_idx]
# Modify header slightly
header = "".join(header_lines)
header = header.replace("package com.ahmedyejam.mks.data.repository", "package com.ahmedyejam.mks.data.repository\n\nimport javax.inject.Inject\nimport javax.inject.Singleton")

def write_repo(name, daos, line_ranges):
    # Flatten line ranges
    content_lines = []
    for start, end in line_ranges:
        # start and end are 1-based, inclusive
        content_lines.extend(lines[start-1 : end])
        
    daos_str = ",\n    ".join(["private val " + d for d in daos])
    
    code = f"""{header}
@Singleton
class {name} @Inject constructor(
    {daos_str}
) {{

"""
    code += "".join(content_lines)
    code += "\n}\n"
    
    # We must ensure that any remaining `deletePreviewService?.` uses the correctly typed daos if needed,
    # but since these are literal line copies, they should work exactly as they did in MksRepository.
    
    with open(f"core/data/src/main/java/com/ahmedyejam/mks/data/repository/{name}.kt", "w") as f:
        f.write(code)
        
    print(f"Generated {name}.kt")

book_daos = [
    "workspaceDao: WorkspaceDao", "bookDao: BookDao",
    "quizDao: QuizDao", "questionDao: QuestionDao",
    "fileManager: FileManager"
]
book_ranges = [(207, 277), (651, 731)]
write_repo("BookRepository", book_daos, book_ranges)

quiz_daos = [
    "quizDao: QuizDao", "questionDao: QuestionDao", "sessionDao: SessionDao",
    "questionCategoryDao: QuestionCategoryDao", "categoryMetadataDao: CategoryMetadataDao",
    "deletePreviewService: DeletePreviewService? = null",
    "categoryMergePreviewService: CategoryMergePreviewService? = null",
    "clearMarksPreviewService: ClearMarksPreviewService? = null",
    "fileManager: FileManager"
]
quiz_ranges = [(732, 869), (1294, 1689)]
write_repo("QuizRepository", quiz_daos, quiz_ranges)

knowledge_daos = [
    "flashcardDeckDao: FlashcardDeckDao", "flashcardDao: FlashcardDao",
    "slideshowCourseDao: SlideshowCourseDao", "courseSlideDao: CourseSlideDao",
    "noteBlueprintDao: NoteBlueprintDao", "noteCollectionDao: com.ahmedyejam.mks.data.local.dao.NoteCollectionDao",
    "promptDao: PromptDao", "promptDeckDao: PromptDeckDao", "promptCardDao: PromptCardDao", "promptRunDao: PromptRunDao",
    "knowledgeStudySessionDao: KnowledgeStudySessionDao", "fileManager: FileManager",
    "bookDao: BookDao", "quizDao: QuizDao", "questionDao: QuestionDao"
]
knowledge_ranges = [(1690, 3000)]
write_repo("KnowledgeRepository", knowledge_daos, knowledge_ranges)

study_daos = [
    "mistakeLogDao: MistakeLogDao", "annotationDao: AnnotationDao", "bookDao: BookDao"
]
study_ranges = [(278, 650)]
write_repo("StudyRepository", study_daos, study_ranges)

asset_daos = [
    "questionAssetDao: QuestionAssetDao", "sourceDocumentDao: SourceDocumentDao",
    "sourceDocumentAssetDao: com.ahmedyejam.mks.data.local.dao.SourceDocumentAssetDao",
    "assetReferenceDao: AssetReferenceDao", "fileManager: FileManager",
    "bookDao: BookDao", "quizDao: QuizDao", "questionDao: QuestionDao"
]
asset_ranges = [(870, 1293)]
write_repo("AssetRepository", asset_daos, asset_ranges)

