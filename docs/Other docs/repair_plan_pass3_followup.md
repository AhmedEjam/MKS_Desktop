# Repair Follow-up Plan After Pass 3

## Immediate local verification

1. Run `./gradlew clean assembleDebug` on a normal Android development machine with network access or a populated Gradle cache.
2. Install over the previous app build and verify startup on an existing database.
3. Test fresh install startup.
4. Test startup after Android backup/restore if backup is re-enabled in any future build.

## Highest-value next repair/test targets

1. Add Room migration fixtures for at least versions 17, 18, 19, 20, and 21.
2. Add a category operation test:
   - create question with categories and assets;
   - rename category;
   - merge category;
   - delete category;
   - verify the same question row remains and linked rows survive.
3. Add import merge test:
   - import existing external IDs;
   - verify update happens in place;
   - verify attempts, marks, notes, drops, and study timestamps are preserved.
4. Add marked/weak review queue test:
   - snooze marked question;
   - mark weak question reviewed;
   - verify neither immediately reappears.
5. Add flashcard review test proving Again / Good / Easy all update `dueAt`.
6. Add mistake-log idempotency test for repeated wrong answers in the same session.

## Do not do yet

- Do not add new product features during crash-repair verification.
- Do not enable destructive Room fallback in release.
- Do not re-enable Android backup without backup/restore test coverage.
- Do not replace import preview safety with direct import behavior.
