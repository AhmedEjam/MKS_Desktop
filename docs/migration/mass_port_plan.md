# Mass Porting Plan — All Legacy Code → Shared Multiplatform

> 222 files in legacy modules → port to shared, then debug systematically.

## Porting Order (by dependency)

### Batch 1: model + network (42 files, ~30 min)
- `core/model/` — remaining 13 files (domain models, DTOs, search models)
- `core/network/` — 3 files (OllamaRepository, RemoteAssetFetcher, RemoteAssetPolicy)
- Strip Android/Room/Moshi imports, add @Serializable

### Batch 2: core/ui theme + components (10 files, ~20 min)
- `core/ui/` — Color, Type, Theme, MksDesignTokens, StudyTopAppBar, MksReusableComponents
- Replace `androidx.compose.*` with Compose MP imports (mostly identical)
- Replace `LocalContext.current` with platform parameters

### Batch 3: core/data services (20 files, ~45 min)
- `core/data/` — DataStoreManager, FocusManager, GlobalErrorHandler
- `core/data/preview/` — 3 preview services
- `core/data/repair/` — AssetReferenceAuditService
- `core/data/review/` — ReviewRepository + models
- `core/data/search/` — GlobalSearchRepository + models
- `core/data/validation/` — QuestionValidator, SessionStateValidator
- `core/data/simulation/` — ChangeSimulationModels
- `core/data/seeder/` — MksDatabaseSeeder (already done)
- `core/data/exchange/` — MksExchangeV7Archive (needs zip4j→java.util.zip)

### Batch 4: core/data import pipeline (20 files, ~1 hr)
- `core/data/importer/` — remaining parsers (Json, Html, ZIP)
- `core/data/importer/repository/` — ImportLibraryManager
- `core/data/importer/mapping/` — LibraryMapper (already copied, needs verification)
- Replace `Uri`→`File`, `ContentResolver`→`inputStream()`, `Context`→remove

### Batch 5: feature/ui ViewModels (16 files, ~1 hr)
- All 16 ViewModels: strip @HiltViewModel, replace @Inject with KoinComponent
- Replace `hiltViewModel()` calls with `koinViewModel()` or `remember { }`
- Migrate business logic (flows, state management)

### Batch 6: feature/ui Screens (30+ files, ~2 hrs)
- QuizPlayerScreen (1336 lines), LibraryScreen (756 lines), etc.
- Replace Android-specific composables with Compose MP equivalents
- Replace `rememberNavController()` with navigation state
- Replace `LocalContext.current` with platform dependencies

### Batch 7: core/data ExportManager + ImportLibraryManager (2 files, ~30 min)
- ExportManager: Room DAOs → SQLDelight queries, OutputStream → File
- ImportLibraryManager: Context/MksDatabase → File/MksDatabase

### Batch 8: Wiring & Cleanup (~30 min)
- Update Koin modules with all new registrations
- Remove unused files from legacy modules
- Verify build

### Batch 9: Runtime Debugging (~2-4 hrs)
- Launch desktop app, fix crashes one by one
- Verify each screen renders
- Test CRUD operations
- Test import/export

## Total: ~8-10 hours

## Execution Strategy
Port ALL files first (batches 1-8 in sequence), then debug (batch 9).
At each batch boundary, verify `./gradlew :shared:compileKotlinDesktop` passes.
