pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "MKS"

// ---- Existing modules (legacy Android, still in use) ----
include(":app")                     // Android application
include(":core:model")              // Entity classes & domain models
include(":core:database")           // Room database, DAOs, migrations
include(":core:data")               // Repositories, import pipeline, file manager
include(":core:network")            // Remote asset fetcher, Ollama
include(":core:ui")                 // Shared UI components, theme, TTS
include(":feature:ui")              // All screens & ViewModels

// ---- New Compose Multiplatform modules (Phase 1) ----
include(":shared")                  // KMP shared code (SQLDelight, Koin, expect/actual)
include(":app-desktop")             // Desktop JVM application