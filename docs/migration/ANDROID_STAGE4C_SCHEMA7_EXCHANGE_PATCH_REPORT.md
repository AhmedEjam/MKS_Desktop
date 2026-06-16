# Android Stage 4C Schema-7 Exchange Patch Report

## Verdict

PASS WITH FIXES

## Active bases

- Android input base: `MKS-GPT4-v26-annotation-cleanup-ui-patched.zip`
- iOS contract base: `iOS_MKS.V04.zip`
- Android output base: `MKS_ANDROID.V26_STAGE4C_SCHEMA7.zip`

## Confirmed patch scope

This patch keeps Android Room at schema v26 and adds a conservative schema-7 exchange bridge matching the iOS V04 contract.

Patched / added Android files:

- `app/src/main/java/com/ahmedyejam/mks/data/exchange/v7/MksExchangeV7Models.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/exchange/v7/MksExchangeV7Archive.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/import/parser/ZipLibraryParser.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/repository/ExportManager.kt`
- `tools/validate_stage4c_schema7_android.sh`
- `docs/migration/ANDROID_STAGE4C_SCHEMA7_EXCHANGE_PATCH_REPORT.md`
- `docs/migration/MKS_EXCHANGE_SCHEMA7_STAGE4C_ANDROID_SPEC.md`

## Implemented

- Detects schema-7 `.mks.zip` archives by `manifest.json` with `format = mks.exchange` and `schemaVersion = 7`.
- Reads iOS V04-style split entries and bridges them into Android's existing `LibraryBundleDto` import path.
- Adds schema-7 writer helpers for quiz, book, and full-library exports.
- Writes the Stage 4A/4B entry layout:
  - `manifest.json`
  - `workspace.json`
  - `data/books.json`
  - `data/quizzes.json`
  - `data/questions.json`
  - `data/question_categories.json`
  - `data/asset_references.json`
  - `data/question_assets.json`
  - `data/source_documents.json`
  - `data/annotations.json`
  - `data/media_manifest.json`
  - `data/soft_deletes.json`
- Preserves the existing encrypted ZIP behavior and password constant.

## Not implemented yet

- UI button/path wiring for schema-7 export choice.
- DAO-native import of asset/source/annotation metadata into Room.
- Media byte copying into `media/`.
- Authoritative sessions/mistake-log exchange payloads.
- Full Android Gradle build verification in this environment.

## Confirmed facts

- Android Room schema remains v26.
- No Room migration was added.
- iOS V04 remains the contract reference.

## Safest next stage

Stage 4D: connect Android schema-7 export/import through the existing Data Tools / import-export UI, then run cross-platform round-trip fixtures.
