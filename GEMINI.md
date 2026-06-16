# MKS Multiplatform Project — Gemini + MCP Configuration

> Last updated: 2026-06-16. Compose Multiplatform migration complete (Phase 1–5).

## Project Quick Reference

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.2.10 (Multiplatform) |
| UI | Compose Multiplatform 1.10.1 |
| DI | Koin 4.0.3 |
| Database | SQLDelight 2.0.2 (26 tables) |
| Targets | Android (api 26+) + JVM Desktop (Java 17) |
| Build | Gradle 9.4.1 / AGP 9.2.1 |

## Key Build Commands

```bash
./gradlew projects --no-configuration-cache     # Verify project model
./gradlew :shared:compileKotlinDesktop            # Compile shared for desktop
./gradlew :app-desktop:compileKotlin              # Compile desktop app
./gradlew :app-desktop:packageDistributionForCurrentOS # Package
```

## Module Map

```
shared/commonMain/  → Platform-agnostic code (entities, repos, VMs, DI, SQLDelight)
shared/androidMain/ → Android-specific implementations
shared/desktopMain/ → Desktop-specific implementations (FHS paths, espeak-ng)
app-desktop/        → Desktop entry point (Main.kt)
app/                → Android entry point (MksApplication.kt)
```

## Key Files for AI Agents

| File | Path |
|------|------|
| Version catalog | `gradle/libs.versions.toml` |
| KMP build config | `shared/build.gradle.kts` |
| DB schema | `shared/src/commonMain/sqldelight/.../Schema.sq` |
| DB migration | `shared/src/commonMain/sqldelight/.../1.sqm` |
| Koin DataModule | `shared/src/commonMain/.../di/DataModule.kt` |
| Koin ViewModelModule | `shared/src/commonMain/.../di/ViewModelModule.kt` |
| Platform interfaces | `shared/src/commonMain/.../platform/*.kt` |
| Desktop UI | `app-desktop/src/.../desktop/Main.kt` |
| Migration plan | `docs/migration/multiplatform_desktop_build_plan.md` |

---

## 1. Accessing MCP Settings

To configure MCP servers in Android Studio:

* **macOS:** **Android Studio > Settings > Tools > AI > MCP Servers**
* **Windows/Linux:** **File > Settings > Tools > AI > MCP Servers**

## 2. Enabling and Configuring

1. Check **Enable MCP Servers**
2. Enter server details in JSON format (stored in `mcp.json`)
3. Click **OK**

### Configuration Schema

| Option | Description |
|--------|-------------|
| `httpUrl` | Required for streamable HTTP endpoints |
| `url` | Use for SSE (Server-Sent Events) endpoints |
| `headers` | Custom HTTP headers map |
| `timeout` | Connection timeout in milliseconds |

### Example `mcp.json`

```json
{
  "mcpServers": {
    "github": {
      "httpUrl": "https://api.github.com/mcp",
      "headers": {
        "Authorization": "Bearer <TOKEN>"
      }
    }
  }
}
```
