# MKS Exchange Schema 7 - Stage 4 Final Spec

Verdict: PASS WITH FIXES

## Required ZIP entries

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
- optional `media/**` payload entries listed by `data/media_manifest.json`

## Manifest

- `format = mks.exchange`
- `schemaVersion = 7`
- `androidRoomSchema = 26`
- `archiveKind = stage4-final-schema7-media-exchange` when media is included
- `includesMedia = true` only when media payloads are bundled

## Media rules

- Media bytes live under `media/<ownerKind>/<ownerExternalId-or-id>/<fileName>`.
- `data/media_manifest.json` is authoritative for media payload ownership.
- Missing local media is a warning, not an import crash.
- Each bundled media file should include `sha256` and `sizeBytes`.
- Importers must reject path traversal and absolute ZIP entry paths.

## Conflict policy

Stage 4 final preserves the existing policies: `preferIncoming`, `keepLocal`, and `skipDuplicates`. Records are matched by `externalId` where available.

## Deferred payload domains

Sessions and mistake logs are intentionally deferred from authoritative exchange until later hardening.
