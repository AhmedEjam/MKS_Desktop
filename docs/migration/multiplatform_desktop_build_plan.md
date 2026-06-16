# MKS Desktop Build Plan: Phase 2–5

**Goal:** Fully buildable MKS application package for Linux Fedora (RPM).
**Current state:** Phase 1 complete — Gradle scaffold, SQLDelight schema, Koin DI, platform abstractions.
**Target:** `./gradlew :app-desktop:packageRpm` produces a runnable MKS desktop app.

---

## Phase 2: Data Layer Migration (Entities + DAOs → commonMain)

**Goal:** All 26 entities and their queries live in `shared/src/commonMain/`.

### 2.1 — Strip Room annotations from entities (~2 hrs)
- Remove `@Entity`, `@PrimaryKey`, `@ForeignKey`, `@ColumnInfo`, `@Index` from 26 entity classes
- Remove `@JsonClass(generateAdapter = true)` (Moshi) — replace with `kotlinx.serialization`
- Move from `core/model/src/main/java/` → `shared/src/commonMain/kotlin/com/ahmedyejam/mks/data/entity/`
- Remove `androidx.room.*` imports
- Entities become pure Kotlin data classes with `@Serializable`

**Files:** 26 × `/core/model/.../entity/*Entity.kt` → 26 × `/shared/.../data/entity/*Entity.kt`

### 2.2 — Verify SQLDelight queries cover all DAO operations (~1 hr)
- The `.sq` files from Task 2 already cover basic CRUD for all 26 tables
- Cross-reference against the 28 Room DAOs for missing query types:
  - Flow-based observation queries (Room returns `Flow<List<T>>`)
  - Upsert operations (INSERT OR REPLACE)
  - Join queries (e.g., `BookWithQuizCount`)
  - Delete by criteria queries
- Add missing `.sq` queries as needed

### 2.3 — Add `kotlinx-datetime` for timestamp fields (~30 min)
- Replace `System.currentTimeMillis()` defaults with `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()`
- Entities use `Long` for timestamps — compatible with SQLDelight `INTEGER`

**New dependency:** `kotlinx-datetime`

### 2.4 — Build verification
- `./gradlew :shared:compileKotlinDesktop` passes
- SQLDelight generates all typed queries

---

## Phase 3: Repository Layer (Koin + SQLDelight)

**Goal:** 6 mega-repositories rewritten to use SQLDelight queries and Koin injection.

### 3.1 — Create repository stubs in commonMain (~1 hr)
- Define repository interfaces in `shared/src/commonMain/`
- Each repository gets a clean Koin `single<T>` declaration in `DataModule.kt`
- The ~25–30 DAO constructor parameters collapse into a single `MksDatabase` parameter

**Before (Hilt + Room, ~30 params):**
```kotlin
@Singleton
class BookRepository @Inject constructor(
    workspaceDao: WorkspaceDao,
    bookDao: BookDao,
    quizDao: QuizDao,
    // ... 25 more DAOs
    quizRepositoryProvider: Provider<QuizRepository>,
    // ...
)
```

**After (Koin + SQLDelight, 1 param):**
```kotlin
class BookRepository(private val db: MksDatabase) {
    // All 28 DAOs accessed via db.bookQueries, db.quizQueries, etc.
}
```

### 3.2 — Rewrite repositories one by one (~4 hrs total)
**Order (by dependency, leaf nodes first):**
1. `AssetRepository` — no outgoing Provider<T>, simplest
2. `WorkspaceRepository` — depends on `MksDatabaseSeeder`
3. `StudyRepository` — depends on `QuizRepository` via Provider
4. `BookRepository` — depends on `QuizRepository` + `AssetRepository` via Provider
5. `QuizRepository` — depends on `BookRepository` + `AssetRepository` via Provider
6. `KnowledgeRepository` — depends on all three via Provider

**Circular dependency resolution with Koin:**
```kotlin
// DataModule.kt
single { AssetRepository(get()) }
single { WorkspaceRepository(get(), get()) }
single { StudyRepository(get()) }  // Koin resolves QuizRepository lazily
single { BookRepository(get()) }
single { QuizRepository(get()) }
single { KnowledgeRepository(get()) }
```
Koin's `get()` inside `single {}` is lazy — the cycle breaks naturally.

### 3.3 — Replace Room Flow<T> with SQLDelight Flow equivalents (~1 hr)
- Room DAOs return `Flow<List<BookEntity>>` via `@Query`
- SQLDelight queries return plain `Query<T>` — wrap with `mapToList()` or `mapToOneOrNull()` extensions
- Create a `QueryExtensions.kt` helper in commonMain:
```kotlin
fun <T : Any> Query<T>.asFlow(): Flow<Query<T>> = // coroutine wrapper
```

### 3.4 — Migrate non-repository services (~1 hr)
- `FileManager` → already done (Task 4 interface + Android delegation)
- `ExportManager`, `ImportLibraryManager` → rewrite to use SQLDelight + platform FileManager
- `DataStoreManager` → wrap `kotlinx-datetime` + platform preferences
- `FocusManager`, `GlobalErrorHandler` → pure Kotlin, direct migration
- `MksDatabaseSeeder` → rewrite using SQLDelight queries

---

## Phase 4: ViewModel + UI Migration

**Goal:** 16 ViewModels and all Compose screens in shared, rendering on desktop.

### 4.1 — Resolve Compose Multiplatform compatibility (~1 hr — critical path)
- Current blocker: Compose MP 1.8.1 `onVariant` API incompatible with AGP 9.x
- **Action:** Try Compose MP versions **2.0.0**, **2.1.0**, **1.11.0**, **1.10.0** against AGP 9.2.1
- If none work: downgrade AGP to 8.9.x (last 8.x release)
- Alternative: Use Jetpack Compose for Android, Compose MP for desktop (dual plugin strategy)

### 4.2 — Migrate ViewModels to commonMain (~3 hrs)
- Replace `@HiltViewModel` + `@Inject constructor` with plain `class`
- Repository parameters resolved by Koin: replace constructor injection with `get()` or `by inject()`
- 16 ViewModels in `feature/ui/` → `shared/src/commonMain/.../ui/`

**Pattern change:**
```kotlin
// Before (Hilt)
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val dataStoreManager: DataStoreManager,
    // ...
) : ViewModel()

// After (Koin)
class QuizViewModel(
    private val quizRepository: QuizRepository = get(),
    private val dataStoreManager: DataStoreManager = get(),
) : ViewModel()
```

### 4.3 — Migrate Compose screens (~5 hrs)
- Move screen composables from `feature/ui/` to `shared/src/commonMain/.../ui/`
- Replace `hiltViewModel()` with `koinViewModel()`
- Replace Android-specific composables with multiplatform equivalents:
  - `rememberNavController()` → Compose MP navigation
  - `LocalContext.current` → platform-specific resource access
  - `resources.getString()` → Compose MP string resources
  - Coil `AsyncImage` → shared `ImageLoader` abstraction

### 4.4 — Platform-specific UI shims
- Android: wrap shared screens in `AndroidEntryPoint`-free Activity (already done)
- Desktop: wrap shared screens in `Window` composable (already scaffolded)
- Resource strings: move to Compose MP string resources or `expect`/`actual`

---

## Phase 5: Desktop Build & Packaging

**Goal:** Runnable RPM package for Fedora.

### 5.1 — Desktop-specific implementations (already 80% done in Task 4)
- `DesktopFileManager` ✓
- `DesktopTtsManager` ✓ (espeak-ng)
- `DesktopImageLoader` ✓ (LRU + disk cache)
- `DesktopFileDialog` ✓ (AWT JFileChooser)

### 5.2 — Missing desktop implementations
- **Preferences/DataStore:** `expect`/`actual` `DataStoreManager` using `java.util.prefs.Preferences` or JSON file
- **Database driver:** `DatabaseDriverFactory` already done (Task 2)
- **Navigation:** Compose MP Navigation or custom router
- **Import pipeline:** desktop file picker already done; XLSX/CSV parsing is pure Kotlin — works as-is

### 5.3 — RPM packaging (~30 min)
- `app-desktop/build.gradle.kts` already has `nativeDistributions { targetFormats(Rpm, Deb) }`
- Add desktop icon (PNG in `app-desktop/src/main/resources/`)
- Configure `packageName = "mks"`, `packageVersion = "3.0.0"`
- Run: `./gradlew :app-desktop:packageRpm`

### 5.4 — Integration testing
- Launch desktop app on Fedora
- Verify database initialization (`~/.local/share/mks/mks_database.db`)
- Verify TTS (`espeak-ng` must be installed)
- Verify file import dialog
- Smoke test: create workspace → add book → import quiz → play quiz

---

## Dependency Order (Critical Path)

```
Phase 2 (Entities + Queries)
    ↓
Phase 3 (Repositories)
    ↓
Phase 4 (ViewModels + Screens) ← blocked by Compose MP compatibility
    ↓
Phase 5 (Desktop packaging)
```

**Total estimated effort:** ~20 hours
**Critical blocker:** Compose Multiplatform plugin compatibility with AGP 9.x

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Compose MP incompatible with AGP 9.x | Try AGP 8.9.x fallback; or dual-plugin strategy (Jetpack Compose for Android, Compose MP for desktop) |
| SQLDelight queries missing Room features | Room DAOs use `@Transaction`, `@RawQuery`, multi-table Flow — may need manual implementation |
| 6 repositories are 2000+ lines each | Migrate one at a time, verify Koin resolution after each |
| Import pipeline pulls Apache POI (Android-dependent) | POI is JVM-compatible, works on desktop. File access uses platform `FileManager` |
| Navigation differs between platforms | Use Compose MP navigation or write a thin platform-agnostic wrapper |

---

## Recommendation

Start with **Phase 2.1** (entity migration) since it's mechanical, low-risk, and unblocks everything else. Then resolve the Compose MP compatibility question (try 2.x versions first, fall back to AGP 8.9.x) before investing in Phase 4 UI migration.
