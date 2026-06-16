#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
required=(
  "$ROOT/app/src/main/java/com/ahmedyejam/mks/data/exchange/v7/MksExchangeV7Models.kt"
  "$ROOT/app/src/main/java/com/ahmedyejam/mks/data/exchange/v7/MksExchangeV7Archive.kt"
  "$ROOT/app/src/main/java/com/ahmedyejam/mks/data/import/parser/ZipLibraryParser.kt"
  "$ROOT/app/src/main/java/com/ahmedyejam/mks/data/repository/ExportManager.kt"
  "$ROOT/docs/migration/ANDROID_STAGE4C_SCHEMA7_EXCHANGE_PATCH_REPORT.md"
  "$ROOT/docs/migration/MKS_EXCHANGE_SCHEMA7_STAGE4C_ANDROID_SPEC.md"
)
for path in "${required[@]}"; do
  test -f "$path" || { echo "MISSING $path"; exit 1; }
done
grep -q 'MksExchangeV7Archive.readLegacyBundleFromDirectory' "$ROOT/app/src/main/java/com/ahmedyejam/mks/data/import/parser/ZipLibraryParser.kt"
grep -q 'exportAllToSchema7Zip' "$ROOT/app/src/main/java/com/ahmedyejam/mks/data/repository/ExportManager.kt"
grep -q 'data/media_manifest.json' "$ROOT/app/src/main/java/com/ahmedyejam/mks/data/exchange/v7/MksExchangeV7Models.kt"
grep -q 'writeLegacyBundleToSchema7Zip' "$ROOT/app/src/main/java/com/ahmedyejam/mks/data/exchange/v7/MksExchangeV7Archive.kt"
echo "PASS: Android Stage 4C schema-7 bridge files and hooks are present."
