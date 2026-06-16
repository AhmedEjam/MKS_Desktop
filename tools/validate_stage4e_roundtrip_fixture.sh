#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
python3 "$ROOT/tools/validate_stage4e_schema7_fixture.py" "$ROOT/docs/migration/stage4e/MKS_STAGE4E_SCHEMA7_ROUNDTRIP_FIXTURE.zip" "$ROOT/docs/migration/stage4e/MKS_STAGE4E_SCHEMA7_ROUNDTRIP_FIXTURE.json"
grep -R "writeLegacyBundleToSchema7Zip" -n "$ROOT/app/src/main/java" >/dev/null
grep -R "schema-7" -n "$ROOT/app/src/main/java/com/ahmedyejam/mks/data/repository" >/dev/null
