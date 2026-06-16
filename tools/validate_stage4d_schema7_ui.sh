#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ARCHIVE="$ROOT/app/src/main/java/com/ahmedyejam/mks/data/exchange/v7/MksExchangeV7Archive.kt"
VM="$ROOT/app/src/main/java/com/ahmedyejam/mks/ui/library/LibraryViewModel.kt"
FIXTURE="$ROOT/docs/migration/stage4d/MKS_STAGE4D_SCHEMA7_MINIMAL_FIXTURE.zip"

grep -q "ZipOutputStream(outputStream)" "$ARCHIVE"
grep -q "intentionally ignored" "$ARCHIVE"
! grep -q "EncryptionMethod.AES" "$ARCHIVE"
grep -q "exportQuizToSchema7Zip" "$VM"
grep -q "exportBundleToSchema7Zip" "$VM"
grep -q "exportAllToSchema7Zip" "$VM"
test -f "$FIXTURE"
python3 - <<'EOFVALID'
import zipfile, json, pathlib
root = pathlib.Path.cwd()
fixture = root / 'docs/migration/stage4d/MKS_STAGE4D_SCHEMA7_MINIMAL_FIXTURE.zip'
required = ['manifest.json','workspace.json','data/books.json','data/quizzes.json','data/questions.json','data/question_categories.json','data/asset_references.json','data/question_assets.json','data/source_documents.json','data/annotations.json','data/media_manifest.json','data/soft_deletes.json']
with zipfile.ZipFile(fixture) as z:
    names = set(z.namelist())
    missing = [x for x in required if x not in names]
    if missing: raise SystemExit('missing fixture entries: ' + ', '.join(missing))
    manifest = json.loads(z.read('manifest.json').decode('utf-8'))
    assert manifest['format'] == 'mks.exchange'
    assert manifest['schemaVersion'] == 7
EOFVALID
echo "Stage 4D schema-7 UI wiring validation PASS"
