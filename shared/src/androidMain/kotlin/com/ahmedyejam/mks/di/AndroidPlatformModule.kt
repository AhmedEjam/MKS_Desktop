package com.ahmedyejam.mks.di

import com.ahmedyejam.mks.data.focus.AndroidFocusManager
import com.ahmedyejam.mks.data.focus.FocusManager
import com.ahmedyejam.mks.data.importer.parser.AndroidBundleFileParser
import com.ahmedyejam.mks.data.importer.parser.AndroidSpreadsheetDataProviderFactory
import com.ahmedyejam.mks.data.importer.parser.BundleFileParser
import com.ahmedyejam.mks.data.importer.parser.SpreadsheetDataProviderFactory
import com.ahmedyejam.mks.data.preferences.dataStore
import com.ahmedyejam.mks.db.DatabaseDriverFactory
import com.ahmedyejam.mks.platform.AndroidFileDialog
import com.ahmedyejam.mks.platform.AndroidFileManager
import com.ahmedyejam.mks.platform.AndroidImageLoader
import com.ahmedyejam.mks.platform.AndroidTtsManager
import com.ahmedyejam.mks.platform.AndroidOcrManager
import com.ahmedyejam.mks.platform.FileDialog
import com.ahmedyejam.mks.platform.FileManager
import com.ahmedyejam.mks.platform.ImageLoader
import com.ahmedyejam.mks.platform.TtsManager
import com.ahmedyejam.mks.platform.OcrManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidPlatformModule = module {
    // Platform abstractions
    single<FileManager> { AndroidFileManager(androidContext()) }
    single<TtsManager> { AndroidTtsManager(androidContext()) }
    single<ImageLoader> { AndroidImageLoader(androidContext()) }
    single<FocusManager> { AndroidFocusManager(androidContext()) }
    single<OcrManager> { AndroidOcrManager() }
    factory<FileDialog> { params -> AndroidFileDialog(activity = params.get()) }
    single<BundleFileParser> { AndroidBundleFileParser() }
    single<SpreadsheetDataProviderFactory> { AndroidSpreadsheetDataProviderFactory() }

    // DataStore
    single { androidContext().dataStore }

    // Database driver factory (expect/actual — takes Android Context)
    single { DatabaseDriverFactory(androidContext()) }
}
