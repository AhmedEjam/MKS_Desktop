# Repair Follow-up Plan After Pass 2

## Immediate local checks

1. Run `./gradlew clean assembleDebug` with network access and a normal Android/Gradle cache.
2. If KSP/AGP/Gradle compatibility fails, pin the versions to a known compatible set instead of changing app code.
3. Install over an existing app database and inspect Room migration logs.
4. Test fresh install startup, restored-data startup, and import/export screens.

## Next repair targets

1. Add migration test fixtures for old database versions.
2. Add a source-document delete test proving question asset source references are cleared.
3. Add mistake-log idempotency tests for repeated wrong answers in the same session.
4. Add flashcard rating tests proving dueAt changes for Again / Good / Easy.
5. Add review-dashboard tests proving snoozed blueprints do not immediately reappear.
6. Add full export save/restore smoke tests.
