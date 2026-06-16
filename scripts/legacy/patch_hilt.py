import re

with open('app/src/main/java/com/ahmedyejam/mks/di/HiltDataModule.kt', 'r') as f:
    content = f.read()

missing_daos = [
    "BookDao", "QuizDao", "QuestionDao", "SessionDao", "CategoryMetadataDao",
    "SlideshowCourseDao", "CourseSlideDao", "NoteBlueprintDao", "PromptDao",
    "KnowledgeStudySessionDao", "QuestionCategoryDao", "AssetReferenceDao",
    "QuestionAssetDao", "SourceDocumentDao", "SourceDocumentAssetDao",
    "GlobalSearchDao", "AnnotationDao", "NoteCollectionDao", "StudySessionDao"
]

additions = ""
for dao in missing_daos:
    if f"fun provide{dao}" not in content:
        additions += f"""
    @Provides
    @Singleton
    fun provide{dao}(appModule: AppModule): {dao} {{
        return appModule.database.{dao[0].lower() + dao[1:]}()
    }}
"""

if additions:
    content = content.replace('// DAOs', '// DAOs\n' + additions)
    with open('app/src/main/java/com/ahmedyejam/mks/di/HiltDataModule.kt', 'w') as f:
        f.write(content)
    print("Patched HiltDataModule.kt")
