# MKS Exchange Schema 7 - Stage 4C Android Spec

## Purpose

Make Android able to read and write the same schema-7 archive shape introduced by iOS V03/V04, without changing Android Room schema v26.

## Archive detection

A ZIP is treated as schema-7 when `manifest.json` contains:

```json
{
  "format": "mks.exchange",
  "schemaVersion": 7
}
```

## Required entries

- `manifest.json`
- `workspace.json`
- `data/books.json`
- `data/quizzes.json`
- `data/questions.json`
- `data/question_categories.json`
- `data/soft_deletes.json`

## Stage 4B metadata entries accepted/written

- `data/asset_references.json`
- `data/question_assets.json`
- `data/source_documents.json`
- `data/annotations.json`
- `data/media_manifest.json`

## Current Android bridge behavior

- Reader: converts schema-7 core records into `LibraryBundleDto` so Android's existing import flow can ingest workspace/book/quiz/question/category data.
- Writer: converts existing `LibraryBundleDto` exports into schema-7 split JSON files.
- Media: metadata-only; binary media copy remains deferred.
- Soft delete: contract entries exist; full DAO-native soft-delete replay remains deferred.

## Compatibility target

- iOS source contract: `iOS_MKS.V04.zip`
- Android database: Room v26
- Exchange schema: 7
