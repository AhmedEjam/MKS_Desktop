package com.ahmedyejam.mks.di

import com.ahmedyejam.mks.data.focus.DesktopFocusManager
import com.ahmedyejam.mks.data.focus.FocusManager
import com.ahmedyejam.mks.data.importer.parser.BundleFileParser
import com.ahmedyejam.mks.data.importer.parser.DesktopBundleFileParser
import com.ahmedyejam.mks.data.importer.parser.DesktopSpreadsheetDataProviderFactory
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetDataProviderFactory
import com.ahmedyejam.mks.data.preferences.createDesktopDataStore
import com.ahmedyejam.mks.db.DatabaseDriverFactory
import com.ahmedyejam.mks.platform.DesktopFileDialog
import com.ahmedyejam.mks.platform.DesktopFileManager
import com.ahmedyejam.mks.platform.DesktopImageLoader
import com.ahmedyejam.mks.platform.DesktopTtsManager
import com.ahmedyejam.mks.platform.DesktopOcrManager
import com.ahmedyejam.mks.platform.FileDialog
import com.ahmedyejam.mks.platform.FileManager
import com.ahmedyejam.mks.platform.ImageLoader
import com.ahmedyejam.mks.platform.TtsManager
import com.ahmedyejam.mks.platform.OcrManager
import org.koin.dsl.module

val desktopPlatformModule = module {
    // Platform abstractions
    single<FileManager> { DesktopFileManager() }
    single<TtsManager> { DesktopTtsManager() }
    single<ImageLoader> { DesktopImageLoader() }
    single<FocusManager> { DesktopFocusManager() }
    single<OcrManager> { DesktopOcrManager() }
    single<FileDialog> { DesktopFileDialog() }
    single<BundleFileParser> { DesktopBundleFileParser() }
    single<SpreadsheetDataProviderFactory> { DesktopSpreadsheetDataProviderFactory() }

    // DataStore
    single { createDesktopDataStore() }

    // Database driver factory (expect/actual — must be registered per platform)
    single { DatabaseDriverFactory() }
}
