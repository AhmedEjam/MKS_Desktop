# Audit patch summary

This patch set applies the selected audit remediations while preserving the current single-module Android project shape.

## Applied

- Explicit Android backup and data-extraction rules.
- Strict import size limits for ZIP, text/CSV/HTML, XLSX, images, and spreadsheet structure.
- ZIP copy limit before extraction to reduce compressed-file disk exhaustion risk.
- Exception-safe import preview/import cleanup and a stale import-cache sweeper.
- Warning-first, report-after import validation that skips invalid questions and reports line/row plus reason.
- XLSX formula evaluation disabled; cached formula values are used instead.
- Central `RemoteAssetFetcher` and `RemoteAssetPolicy` with HTTPS default and user-approved plain HTTP option.
- Redacted `MksLogger` wrapper replacing raw stack traces and direct production logging.
- Apache POI upgraded to 5.5.1.
- Release R8 minification and resource shrinking enabled.
- Room schema export enabled and database bumped to version 17.
  > **Note (2026-06-08):** The database has since advanced to v26 with 25 total migration steps.
- Normalized `question_categories` table and DAO-backed category queries.
- `asset_references` table for reference-aware local asset deletion.
- `flashcards.sourceQuestionId` index added in the 16->17 migration.
- Knowledge-bank book-level list routes added for slideshows, blueprints, and prompts.
- Knowledge-bank insert paths now re-query or keep generated IDs instead of leaving UI state with id=0.
- Startup preference reads no longer use `runBlocking` on the main thread.
- Legacy `READ_EXTERNAL_STORAGE` removed; media image and notification-policy permissions kept.
- CI workflow and Android `.gitignore` added.

## Preserved by request

- Existing ZIP password/export encryption model.
- Export-time remote image behavior.
- Dynamic Material Components dependency version, documented for later discussion.
- Private-network remote host blocking.

## Architecture note

The repository remains a compatibility facade to avoid breaking all current UI call sites in one pass. The patch extracts shared policy and infrastructure classes first (`RemoteAssetFetcher`, `ImportLimits`, asset/category DAOs, logger) so a future feature-repository split can move code behind stable boundaries with lower risk.
