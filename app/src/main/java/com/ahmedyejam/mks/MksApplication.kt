package com.ahmedyejam.mks

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.ahmedyejam.mks.di.allModules
import com.ahmedyejam.mks.di.androidPlatformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MksApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        setupKoin()
        setupCrashHandler()
    }

    private fun setupKoin() {
        startKoin {
            androidContext(this@MksApplication)
            modules(allModules + androidPlatformModule)
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            com.ahmedyejam.mks.util.MksLogger.e(
                "MksApplication",
                "Uncaught exception in thread ${thread.name}",
                throwable
            )
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private const val MEMORY_CACHE_PERCENT = 0.25
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(MEMORY_CACHE_PERCENT)
                    .strongReferencesEnabled(enable = true)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(enable = true)
            .build()
    }
}
