package com.ahmedyejam.mks.di

import com.ahmedyejam.mks.platform.FileDialog
import com.ahmedyejam.mks.platform.FileManager
import com.ahmedyejam.mks.platform.ImageLoader
import com.ahmedyejam.mks.platform.TtsManager
import org.koin.dsl.module

/**
 * Koin Utility Module — platform abstraction bindings.
 *
 * Actual implementations are registered in androidMain/desktopMain
 * Koin initializer modules. This module declares the interface types
 * so that they can be resolved by commonMain code.
 *
 * Android: register AndroidFileManager, AndroidTtsManager, AndroidImageLoader, AndroidFileDialog
 * Desktop: register DesktopFileManager, DesktopTtsManager, DesktopImageLoader, DesktopFileDialog
 */
val utilityModule = module {
    // These are declared as single<T> but the actual implementations
    // are registered in platform-specific module loaders.

    // Platform abstractions — resolved via platform-specific Koin modules.
    // The actual registration happens in:
    //   androidMain: AndroidPlatformModule.kt
    //   desktopMain: DesktopPlatformModule.kt
}

/**
 * All modules combined for convenient registration.
 * Platform modules are added by the platform-specific initializers.
 */
val allModules = listOf(dataModule, viewModelModule, utilityModule)
