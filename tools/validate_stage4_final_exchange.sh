#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ARCHIVE="$ROOT/app/src/main/java/com/ahmedyejam/mks/data/exchange/v7/MksExchangeV7Archive.kt"
PARSER="$ROOT/app/src/main/java/com/ahmedyejam/mks/data/import/parser/ZipLibraryParser.kt"
MODELS="$ROOT/app/src/main/java/com/ahmedyejam/mks/data/exchange/v7/MksExchangeV7Models.kt"
grep -q "stage4-final-schema7-media-exchange" "$ARCHIVE"
grep -q "buildMediaPayload" "$ARCHIVE"
grep -q "MessageDigest.getInstance(\"SHA-256\")" "$ARCHIVE"
grep -q "extractPlainZip" "$PARSER"
grep -q "ZipInputStream" "$PARSER"
grep -q "MEDIA_DIRECTORY" "$MODELS"
echo "PASS: Android Stage 4 final schema-7 media exchange patch markers present"
