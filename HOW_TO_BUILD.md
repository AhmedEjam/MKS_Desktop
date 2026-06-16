# MKS — Build, Install & Debug Guide

> Last updated: 2026-06-16. Compose Multiplatform (Android + Linux Desktop).

---

## 📋 Prerequisites

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **JDK** | 17 | 21 (JetBrains Runtime) |
| **Gradle** | 9.4.1 (wrapper included) | — |
| **Kotlin** | 2.2.10 (managed by Gradle) | — |
| **Android SDK** | API 35 (for Android builds only) | Platform 35, Build Tools 35 |
| **Linux Desktop** | Fedora / Ubuntu | espeak-ng, aplay (for TTS) |

### Fedora Desktop Dependencies

```bash
sudo dnf install espeak-ng alsa-utils    # TTS + audio playback
```

### macOS (development only)

```bash
brew install espeak-ng                    # TTS
# Android SDK via Android Studio or command-line tools
```

---

## 🔧 Quick Start

```bash
# Clone and enter the project
git clone https://github.com/AhmedEjam/MKS_FInal.git
cd MKS_FInal
# (local directory may be named "linux MKS" or similar)

# Verify everything is set up correctly
./gradlew projects --no-configuration-cache
```

---

## 🖥️ Desktop Build & Run

### Compile Only

```bash
# Compile shared module (both targets, but desktop specifically)
./gradlew :shared:compileKotlinDesktop

# Compile full desktop app
./gradlew :app-desktop:compileKotlin

# Compile everything
./gradlew :app-desktop:build
```

### Run (Development)

```bash
# Standard run
./gradlew :app-desktop:run

# Hot Reload — recompiles on code changes and reloads running app
./gradlew :app-desktop:hotRun --mainClass=com.ahmedyejam.mks.desktop.MainKt

# Dev mode with composable hot reload
./gradlew :app-desktop:hotDev
```

### Run from Built Distribution

```bash
./gradlew :app-desktop:runDistributable
```

### Package for Distribution

```bash
# Package for current OS (auto-detects macOS/Linux/Windows)
./gradlew :app-desktop:packageDistributionForCurrentOS

# Fedora / RHEL RPM
./gradlew :app-desktop:packageRpm

# Debian / Ubuntu DEB
./gradlew :app-desktop:packageDeb

# Uber JAR (single executable JAR)
./gradlew :app-desktop:packageUberJarForCurrentOS

# All release packages
./gradlew :app-desktop:packageReleaseDistributionForCurrentOS
```

**Output locations:**

| Task | Output |
|------|--------|
| `run` / `hotRun` | N/A — launches app directly |
| `packageDistributionForCurrentOS` | `app-desktop/build/compose/binaries/main/app/mks.app` (macOS) |
| `packageRpm` | `app-desktop/build/compose/binaries/main/rpm/mks-1.0.0.rpm` |
| `packageDeb` | `app-desktop/build/compose/binaries/main/deb/mks_1.0.0.deb` |
| `packageUberJarForCurrentOS` | `app-desktop/build/compose/jars/mks-1.0.0.jar` |

### Install RPM on Fedora

```bash
./gradlew :app-desktop:packageRpm
sudo dnf install app-desktop/build/compose/binaries/main/rpm/mks-1.0.0.rpm
mks   # launch from terminal or application menu
```

### Debug Desktop App

```bash
# Run with debug logging
./gradlew :app-desktop:run --info

# Stacktrace on failure
./gradlew :app-desktop:run --stacktrace

# Specific log categories
./gradlew :app-desktop:run -Dorg.gradle.logging.level=debug
```

---

## 📱 Android Build & Install

### Prerequisites

Create `local.properties` in the project root:

```
sdk.dir=/path/to/Android/sdk
```

Or set `ANDROID_HOME` environment variable.

### Compile

```bash
# Compile shared module for Android target
./gradlew :shared:compileAndroidMain

# Compile Android app (debug)
./gradlew :app:assembleDebug

# Compile Android app (release)
./gradlew :app:assembleRelease
```

### Install to Device/Emulator

```bash
# Install debug APK
./gradlew :app:installDebug

# Install release APK
./gradlew :app:installRelease

# Uninstall
./gradlew :app:uninstallDebug
```

### Run Tests

```bash
# Unit tests (all modules)
./gradlew test

# Android instrumentation tests (requires device/emulator)
./gradlew :app:connectedAndroidTest

# Specific test module
./gradlew :core:database:test
```

---

## 🗄️ Database Tasks

```bash
# Generate SQLDelight code from .sq files
./gradlew :shared:generateCommonMainMksDatabaseInterface

# Verify SQLDelight migrations
./gradlew :shared:verifySqlDelightMigration

# Clean and regenerate
./gradlew :shared:clean :shared:generateCommonMainMksDatabaseInterface
```

---

## 🔍 Debugging & Diagnostics

### Project Model Verification

```bash
# Always run after changing build.gradle.kts files
./gradlew projects --no-configuration-cache

# Show all available tasks
./gradlew :shared:tasks --all
./gradlew :app-desktop:tasks --all
./gradlew :app:tasks --all

# Show dependency tree
./gradlew :app-desktop:dependencies
./gradlew :shared:dependencies --configuration commonMainImplementationDependenciesMetadata
```

### Build Failures

```bash
# Full stacktrace
./gradlew <task> --stacktrace

# Verbose logging
./gradlew <task> --info
./gradlew <task> --debug

# Refresh dependencies (force re-download)
./gradlew <task> --refresh-dependencies

# Clean build
./gradlew clean build --no-configuration-cache
```

### Incremental / Caching Issues

```bash
# Disable configuration cache if stale
./gradlew <task> --no-configuration-cache

# Disable build cache
./gradlew <task> --no-build-cache

# Force task re-run
./gradlew <task> --rerun-tasks
```

---

## 📂 Desktop Data & Logs

| Path | Purpose |
|------|---------|
| `~/.local/share/mks/` | Application data root |
| `~/.local/share/mks/mks_database.db` | SQLite database |
| `~/.local/share/mks/images/` | Downloaded/imported images |
| `~/.local/share/mks/cache/` | Image disk cache |

### Inspect the Database

```bash
sqlite3 ~/.local/share/mks/mks_database.db

# Useful queries
.tables                          # List all tables
.schema books                    # Show books table schema
SELECT COUNT(*) FROM books;      # Book count
SELECT COUNT(*) FROM questions;  # Question count
.quit
```

---

## 🧪 Gradle Properties

Located in `gradle.properties`. Key settings:

```properties
# JVM memory for Gradle daemon
org.gradle.jvmargs=-Xmx4096m

# Parallel project execution
org.gradle.parallel=true

# Build caching
org.gradle.caching=true

# Configuration cache (disable if stale)
org.gradle.configuration-cache=true

# Kotlin daemon memory
kotlin.daemon.jvmargs=-Xmx2048M

# AGP compatibility flags
android.disallowKotlinSourceSets=false
android.newDsl=false
```

---

## 🏗️ Module Build Targets

| Module | Target | Compile Task |
|--------|--------|-------------|
| `:shared` | common (metadata) | `compileCommonMainKotlinMetadata` |
| `:shared` | desktop (JVM) | `compileKotlinDesktop` |
| `:shared` | android | `compileAndroidMain` |
| `:app-desktop` | desktop app | `compileKotlin` |
| `:app` | android app | `assembleDebug` |
| `:core:*` | android library | `assembleDebug` (from :app) |

---

## 🚨 Common Issues

### "SDK location not found" (Android)
Create `local.properties` with `sdk.dir=/path/to/Android/sdk` or set `ANDROID_HOME`.

### "espeak-ng not found" (Desktop TTS)
Install espeak-ng: `sudo dnf install espeak-ng` (Fedora) or `sudo apt install espeak-ng` (Ubuntu). TTS is optional — the app runs without it.

### "SQLDelight generation failed"
Check for duplicate query names across `.sq` files. All query names must be globally unique. Run `./gradlew :shared:generateCommonMainMksDatabaseInterface --info` for details.

### "Compose Multiplatform plugin not found"
The version catalog specifies `compose-multiplatform = "1.10.1"`. Ensure this version is available on Maven Central. Try `--refresh-dependencies`.

### "Namespace not specified" (shared module)
The `kotlin { android { namespace = "..." } }` block is required in `shared/build.gradle.kts` when using `com.android.kotlin.multiplatform.library`.

---

## 📦 Building for Release

```bash
# Full clean build of everything
./gradlew clean build --no-configuration-cache

# Desktop release package
./gradlew :app-desktop:packageReleaseDistributionForCurrentOS

# Desktop RPM specifically
./gradlew :app-desktop:packageReleaseRpm

# Android release APK (requires signing config)
./gradlew :app:assembleRelease

# Android App Bundle
./gradlew :app:bundleRelease
```

---

## 🔗 Related Documents

| Document | Path |
|----------|------|
| Architecture map | `APP_ARCHITECTURE_MAP.md` |
| AI context guide | `AI_CONTEXT.md` |
| Migration plan | `docs/migration/multiplatform_desktop_build_plan.md` |
| Enhancement plan | `mks_enhancement_plan.md` |
| README | `README.md` |
