#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO="$ROOT/app/src/main/java/com/ahmedyejam/mks/data/repository/MksRepository.kt"
VM="$ROOT/app/src/main/java/com/ahmedyejam/mks/ui/library/LibraryViewModel.kt"
EXPORT="$ROOT/app/src/main/java/com/ahmedyejam/mks/data/repository/ExportManager.kt"

grep -q 'R1 repair gate: repository-level schema-7 export wrappers' "$REPO"
grep -q 'suspend fun exportQuizToSchema7Zip' "$REPO"
grep -q 'suspend fun exportBundleToSchema7Zip' "$REPO"
grep -q 'suspend fun exportAllToSchema7Zip' "$REPO"
grep -q 'exportManager?.exportQuizToSchema7Zip' "$REPO"
grep -q 'exportManager?.exportBundleToSchema7Zip' "$REPO"
grep -q 'exportManager?.exportAllToSchema7Zip' "$REPO"
grep -q 'repository.exportQuizToSchema7Zip' "$VM"
grep -q 'repository.exportBundleToSchema7Zip' "$VM"
grep -q 'repository.exportAllToSchema7Zip' "$VM"
grep -q 'suspend fun exportQuizToSchema7Zip' "$EXPORT"
grep -q 'suspend fun exportBundleToSchema7Zip' "$EXPORT"
grep -q 'suspend fun exportAllToSchema7Zip' "$EXPORT"

echo "PASS: R1 schema-7 repository wiring markers present"
