# Active Repair Checklist

This file tracks the detailed tasks for each stage of the implementation plan.

## Stage 1: Navigation Route Repair

- [x] Centralize all routes in `MksRoutes.kt`
- [x] Implement `MksRouteBuilders.kt`
- [x] Implement `MksRouteValidator.kt`
- [x] Implement `InvalidRouteScreen.kt`
- [x] Update `MksNavHost.kt` to use builders and validators
- [x] Fix detail route argument names
- [x] Fix generated search/review routes
- [x] Add unit tests for route builders and validators

## Stage 2: Settings Sanitizer

- [x] Implement `SettingsSanitizer.kt`
- [x] Add unit tests for `SettingsSanitizer`
- [x] Integrate into `DataStoreManager.kt`
- [x] Fix default mismatches
- [x] Validate last session IDs

## Stage 3: Converter & Migration Hardening

- [x] Harden all converters in `Converters.kt` with try-catch and fallbacks
- [x] Centralize all migrations in `MksMigrations.kt`
- [x] Clean up `MksDatabase.kt`
- [x] Expose unified `ALL_MIGRATIONS` array

## Stage 4: Question & Session Validators

- [x] Implement `QuestionValidator.kt`
- [x] Implement `SessionStateValidator.kt`
- [x] Add unit tests for validators
- [x] Integrate `QuestionValidator` into `QuizQuestionsViewModel`
- [x] Integrate `SessionStateValidator` into `QuizViewModel`

## Stage 5: Destructive Action Preview Framework

- [x] Implement `DeletePreviewService.kt`
- [x] Implement `CategoryMergePreviewService.kt`
- [x] Implement `ClearMarksPreviewService.kt`
- [x] Integrate preview services into `MksRepository`

## Stage 6: Asset Reference & File Safety

- [x] Harden `FileManager.kt` with canonical paths and better directory guards
- [x] Implement `AssetReferenceAuditService.kt`
- [x] Add `getAllReferences` to `AssetReferenceDao`
- [x] Integrate audit service into `MksRepository`

## Stage 7: Import & Export Hardening

- [x] Strengthen ZipSlip protection in `ZipLibraryParser.kt`
- [x] Verify `getImportPreview` is side-effect free
- [x] Ensure canonical paths during extraction

## Stage 8: Quiz & Session Repair

- [x] Validate session in `QuizViewModel.startQuiz`
- [x] Implement repair logic for out-of-bounds index
- [x] Add placeholder for missing questions in `SummaryViewModel`
- [x] Block session resume if current question data is missing

## Stage 9: Question & Category Consistency

- [x] Use `QuestionValidator` in `QuizQuestionsViewModel`
- [x] Ensure `QuestionCategoryEntity` is synced during insertions/updates
- [x] Implement consistent category rename/merge logic in `MksRepository`
