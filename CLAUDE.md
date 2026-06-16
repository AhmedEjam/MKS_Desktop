# CLAUDE.md — MKS Multiplatform Project

> Last updated: 2026-06-16. Compose Multiplatform migration complete.  
> **DI:** Koin 4.0.3 (replaced Hilt). **DB:** SQLDelight 2.0.2 (replaced Room).  
> **Targets:** Android + JVM Desktop (Fedora Linux).

## Project Overview

MKS (My Knowledge Space) is a Compose Multiplatform quiz and knowledge-bank application. It imports educational content from spreadsheets, presentations, and documents, then presents interactive quizzes, flashcards, slideshows, and study materials. Targets Android and Linux Desktop with shared business logic in `shared/`.

## Build & Development Commands

```bash
# Verify project model (ALWAYS run after build file changes)
./gradlew projects --no-configuration-cache

# Compile shared module (desktop target)
./gradlew :shared:compileKotlinDesktop

# Compile full desktop app
./gradlew :app-desktop:compileKotlin

# Package desktop distribution (macOS .app / Fedora .rpm)
./gradlew :app-desktop:packageDistributionForCurrentOS

# Fedora RPM
./gradlew :app-desktop:packageRpm

# SQLDelight code generation
./gradlew :shared:generateCommonMainMksDatabaseInterface

# Full build
./gradlew build
```

## Module Structure

```
shared/           ← KMP shared code (ALL new development)
  commonMain/      Platform-agnostic code
  androidMain/     Android platform implementations
  desktopMain/     Desktop platform implementations

app/              ← Android application
app-desktop/      ← Desktop JVM application
core/             ← Legacy Android modules (being migrated to shared)
feature/          ← Legacy feature UI module (being migrated to shared)
```

## Architecture Rules

### 1. New code goes in shared/commonMain
All new entities, repositories, ViewModels, and UI code should live in `shared/src/commonMain/`. Only platform-specific implementations go in `androidMain` or `desktopMain`.

### 2. Database: SQLDelight
- Schema: `shared/.../sqldelight/.../Schema.sq` (26 CREATE TABLE statements)
- Migration: `shared/.../sqldelight/.../1.sqm` (CREATE TABLE IF NOT EXISTS)
- Queries: 12 `.sq` files with globally unique names (prefix: `bk_`, `qz_`, etc.)
- Add `.sq` queries → run `generateCommonMainMksDatabaseInterface` → use generated code

### 3. DI: Koin
- Modules in `shared/.../di/DataModule.kt`, `ViewModelModule.kt`, `UtilityModule.kt`
- ViewModels use `KoinComponent` + `by inject()` (not `@Inject constructor`)
- Circular deps resolved by KoinComponent.inject() (replaces Hilt's Provider<T>)

### 4. Platform abstractions
- Interfaces in `shared/.../platform/`: FileManager, TtsManager, ImageLoader, FileDialog
- Implementations: androidMain/desktopMain with platform-specific code
- Registered in Koin via AndroidPlatformModule / DesktopPlatformModule

### 5. Compose Multiplatform
- Use `androidx.compose.*` directly (Compose MP 1.10.1 provides these in commonMain)
- No Jetpack Compose BOM in shared module
- No `androidx.lifecycle.ViewModel` in commonMain — use plain classes with KoinComponent
- ViewModels registered as `factory { }` (not `viewModel { }`) in commonMain

## Key Files

| File | Purpose |
|------|---------|
| `shared/build.gradle.kts` | KMP targets + source sets + dependencies |
| `gradle/libs.versions.toml` | Centralized versions |
| `shared/.../sqldelight/.../1.sqm` | Database migration |
| `shared/.../di/DataModule.kt` | Repository + DB bindings |
| `shared/.../di/ViewModelModule.kt` | ViewModel factories |
| `app-desktop/.../Main.kt` | Desktop entry point |
| `docs/migration/multiplatform_desktop_build_plan.md` | Migration roadmap |
