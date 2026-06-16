# Android map/plan.md - Stage 4 Final

## Active Android base

`MKS_ANDROID.V26_STAGE4_FINAL.zip`

Initial Android base for this migration line: `MKS-GPT4-v26-annotation-cleanup-ui-patched.zip`.

## Current verdict

PASS WITH FIXES

Android remains the canonical Room v26 reference and now has schema-7 exchange reader/writer parity through Stage 4 final.

## Implemented so far

- Stage 0: PASS - inventory and validation.
- Stage 1: PASS WITH FIXES - Android backend accepted as canonical model source.
- Stage 2: PASS WITH FIXES - schema-7 exchange contract defined.
- Stage 4C: PASS WITH FIXES - Android schema-7 bridge added.
- Stage 4D: PASS WITH FIXES - normal export path writes schema-7 standard ZIP.
- Stage 4E: PASS WITH FIXES - shared round-trip fixture added.
- Stage 4 Final: PASS WITH FIXES - media-byte entries and standard ZIP import handling patched.

## Stage 4 final patched paths

- `app/src/main/java/com/ahmedyejam/mks/data/exchange/v7/MksExchangeV7Models.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/exchange/v7/MksExchangeV7Archive.kt`
- `app/src/main/java/com/ahmedyejam/mks/data/import/parser/ZipLibraryParser.kt`
- `tools/validate_stage4_final_exchange.sh`

## Required objects/elements now present

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
- `media/**` payload entries when reachable media exists

## Remaining Android maturity patches

### Stage 5 - Core study flow parity support

1. Keep Android stable while iOS catches up in UI.
2. Use Android only for reference behavior unless exchange bugs are found.
3. Preserve library/book/quiz/question/session behavior.

### Stage 6 - Assets/sources/annotations hardening

1. Add wider media fixtures: image, PDF, link, source citation.
2. Add missing-media and duplicate-media tests.
3. Add annotation import/export edge cases.

### Stage 7 - Learning tools exchange expansion

1. Add authoritative sessions.
2. Add mistake logs.
3. Add flashcards, notes, slides, prompts as exchange payloads if not already covered by legacy bundle.

### Stage 8 - UI/path coverage

Use the refined P01-P20 and S01-S06 path/interactable map to audit Android behavior against iOS-native equivalents.

### Stage 9 - Release readiness

Run Gradle build/tests, corrupted-import tests, large-library tests, missing-media tests, soft-delete restore/purge tests, and backup/restore tests.

---

# R1 — Android Schema-7 Repository Wiring Repair

## Base

- Input base: `MKS_ANDROID.V26_STAGE4_FINAL.zip`
- Output base: `MKS_ANDROID.V26_R1_SCHEMA7_REPO_REPAIR.zip`

## Status

PASS WITH FIXES

## Implemented patch

- Added repository-level wrappers on `MksRepository` for:
  - `exportQuizToSchema7Zip`
  - `exportBundleToSchema7Zip`
  - `exportAllToSchema7Zip`
- Preserved `ExportManager` as the implementation owner.
- Preserved Room schema v26.

## Remaining gate

Run full Gradle compile/tests in a normal Android environment before marking Android Stage 4 exchange build-ready.
