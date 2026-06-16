# MKS Enhancement Plan

> Last updated: 2026-06-16. Current state: Compose Multiplatform migration Phase 1–5 complete.

---

## Migration Status: ✅ Phase 1–5 COMPLETE

MKS has been migrated from Android-only (Hilt + Room, 6-module architecture) to Compose Multiplatform (Koin + SQLDelight, shared module + app-android + app-desktop).

### Completed Phases

| Phase | Description | Status |
|-------|-------------|--------|
| **Phase 1** | Gradle Restructuring | ✅ Complete |
| **Phase 2** | Database Layer (Room → SQLDelight) | ✅ Complete |
| **Phase 3** | Dependency Injection (Hilt → Koin) | ✅ Complete |
| **Phase 4** | Platform Abstractions (expect/actual) | ✅ Complete |
| **Phase 5** | Desktop Build & Packaging | ✅ Complete |

### Current Architecture

```
shared/src/
├── commonMain/  53 files (entities, repos, VMs, DI, platform interfaces, SQLDelight)
├── androidMain/  9 files (platform impls)
└── desktopMain/  9 files (platform impls)

app-desktop/  → Runnable desktop app with 4-tab Material3 UI
app/          → Android app (legacy source still in core/* + feature/*)
```

### Tech Stack (Post-Migration)

| Component | Before | After |
|-----------|--------|-------|
| UI | Jetpack Compose (Android) | Compose Multiplatform 1.10.1 |
| DI | Dagger Hilt | Koin 4.0.3 |
| Database | Room v30 | SQLDelight 2.0.2 |
| Image Loading | Coil (Android) | Coil (Android) / LRU+disk (Desktop) |
| TTS | Android TTS | Android TTS / espeak-ng (Desktop) |
| Serialization | Moshi | kotlinx.serialization |
| Build | Single-module | 10-module KMP |

---

## Remaining Work

### 🔴 Priority: Complete Feature Parity

| Feature | Desktop Status | Detail |
|---------|---------------|--------|
| Library (book CRUD) | ✅ Working | Full BookRepository + WorkspaceRepository + LibraryScreen |
| Quiz Player | ⬜ Stub | ViewModel + repository exist; screen not ported |
| Flashcard Study | ⬜ Stub | ViewModel + repository exist; screen not ported |
| Slideshow Player | ⬜ Stub | ViewModel + repository exist; screen not ported |
| Note Blueprints | ⬜ Stub | Repository exists; no screen |
| AI Prompt Decks | ⬜ Stub | Repository exists; no screen |
| Import Pipeline | ⬜ Not ported | XLSX/CSV parsing is JVM-compatible; needs FileDialog wiring |
| Global Search | ⬜ Stub | ViewModel exists; no screen |
| Settings | ⬜ Partial | Settings tab shows platform info |
| TTS | ⬜ Scaffolded | espeak-ng code exists; not wired to UI |

### 🟡 Medium: Polish & Testing

- Fill remaining repository methods (Annotation cascades, asset sync, derived content)
- Add SQLDelight Flow observation wrappers for real-time UI updates
- Add repository integration tests with in-memory SQLite
- Migrate remaining screens from `feature/ui/` (20+ screens)
- Complete Compose Multiplatform navigation (currently tab-based)

### 🟢 Low: Legacy Cleanup

- Remove Hilt-annotated code from `core/*` and `feature/*` (still has @Inject/@HiltViewModel)
- Delete Room entities from `core/model` (duplicated in shared/commonMain)
- Remove Room DAOs from `core/database` (replaced by SQLDelight queries)
- Consolidate `core/*` modules into `shared` or delete

---

## Infrastructure Status

| Item | Status | Notes |
|------|--------|-------|
| Version Catalog | ✅ | `gradle/libs.versions.toml` — all deps centralized |
| Gradle Wrapper | ✅ | Gradle 9.4.1 |
| KMP Build | ✅ | shared compiles for both android + desktop targets |
| Desktop Packaging | ✅ | `./gradlew :app-desktop:packageDistributionForCurrentOS` works |
| CI Pipeline | ⬜ | Needs update for multiplatform build |
| Repository Tests | ⬜ | Zero tests for new repositories |
| Legacy Cleanup | ⬜ | `core/*` and `feature/*` still have old Hilt/Room code |

---

**Plan document:** `docs/migration/multiplatform_desktop_build_plan.md`  
**Migration history:** `README.md` § Migration History
