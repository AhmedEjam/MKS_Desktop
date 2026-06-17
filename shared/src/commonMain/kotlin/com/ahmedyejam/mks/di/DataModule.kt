package com.ahmedyejam.mks.di

import com.ahmedyejam.mks.data.export.ExportManager
import com.ahmedyejam.mks.data.importer.repository.ImportLibraryManager
import com.ahmedyejam.mks.data.preferences.DataStoreManager
import com.ahmedyejam.mks.data.repository.AssetRepository
import com.ahmedyejam.mks.data.repository.BookRepository
import com.ahmedyejam.mks.data.repository.KnowledgeRepository
import com.ahmedyejam.mks.data.repository.MksDatabaseSeeder
import com.ahmedyejam.mks.data.repository.QuizRepository
import com.ahmedyejam.mks.data.repository.StudyRepository
import com.ahmedyejam.mks.data.repository.WorkspaceRepository
import com.ahmedyejam.mks.data.review.ReviewRepository
import com.ahmedyejam.mks.data.search.GlobalSearchRepository
import com.ahmedyejam.mks.data.repository.ai.OllamaRepository
import com.ahmedyejam.mks.db.DatabaseDriverFactory
import com.ahmedyejam.mks.db.MksDatabase
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val APPLICATION_SCOPE = "ApplicationScope"

val dataModule = module {

    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    single { OllamaRepository(get()) }

    single(named(APPLICATION_SCOPE)) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
    }

    // Preferences & Core
    single { DataStoreManager(get()) }
    // FocusManager is provided by platform modules

    // Database
    single<MksDatabase> {
        val factory: DatabaseDriverFactory = get()
        MksDatabase(factory.createDriver())
    }

    // Repositories — KoinComponent.inject() handles circular deps at use-time
    single { AssetRepository(get()) }
    single { StudyRepository(get()) }
    single { ReviewRepository(get()) }
    single { GlobalSearchRepository(get()) }
    single { BookRepository(get()) }
    single { QuizRepository(get()) }
    single { KnowledgeRepository(get()) }
    single { MksDatabaseSeeder(get()) }
    single { WorkspaceRepository(get(), get()) }
    single { ExportManager(get()) }
    single { ImportLibraryManager(get(), get(), get()) }
}
