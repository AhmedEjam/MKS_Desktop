# Base Definition — R1 Android Schema-7 Repository Repair

## Confirmed process base

- Android: `MKS_ANDROID.V26_STAGE4_FINAL.zip`
- iOS: `iOS_MKS.V15_STAGE6E_MEDIA_PREVIEW_DETAILS.zip`

## New base after patch

- Android: `MKS_ANDROID.V26_R1_SCHEMA7_REPO_REPAIR.zip`
- iOS: unchanged

## Scope

R1 is a narrow Android repair gate. It fixes repository-level schema-7 export wiring so the existing Stage 4 export UI path can compile against the repository layer.

## Non-goals

- No Android Room schema change.
- No iOS patch.
- No new UI features.
- No expansion of assets/sessions/mistake logs beyond the existing Stage 4 final exchange scope.
