package com.ahmedyejam.mks.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.LinkedHashMap

/**
 * Desktop (Linux) implementation of [ImageLoader].
 *
 * Uses a simple LRU memory cache + disk cache in ~/.local/share/mks/cache/.
 * Skiko Canvas integration will be added when Compose Multiplatform is enabled
 * for desktop in a later phase.
 */
class DesktopImageLoader : ImageLoader {
    private val dataRoot: File = File(System.getProperty("user.home"), ".local/share/mks")
    private val cacheDir: File = File(dataRoot, "cache").also { it.mkdirs() }

    // LRU memory cache (max 128 entries)
    private val memoryCache = object : LinkedHashMap<String, ByteArray>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean {
            return size > 128
        }
    }

    override suspend fun loadBytes(source: String): ByteArray? = withContext(Dispatchers.IO) {
        // Check memory cache
        memoryCache[source]?.let { return@withContext it }

        // Check disk cache
        val cacheFile = cachePath(source)
        if (cacheFile.exists()) {
            val bytes = cacheFile.readBytes()
            memoryCache[source] = bytes
            return@withContext bytes
        }

        // Load from source
        val bytes = when {
            source.startsWith("http://") || source.startsWith("https://") -> downloadBytes(source)
            source.startsWith("data:") -> decodeBase64(source)
            else -> loadLocalBytes(source)
        }

        if (bytes != null) {
            memoryCache[source] = bytes
            cacheFile.writeBytes(bytes)
        }
        bytes
    }

    override suspend fun prefetch(source: String) {
        loadBytes(source) // loadBytes already caches, discard result
    }

    override fun clearMemoryCache() {
        memoryCache.clear()
    }

    override fun clearDiskCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    private fun downloadBytes(urlString: String): ByteArray? {
        return try {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.connect()
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.use { it.readBytes() }
        } catch (_: Exception) { null }
    }

    private fun decodeBase64(dataUri: String): ByteArray? {
        return try {
            val pureBase64 = if (dataUri.contains(",")) {
                dataUri.substring(dataUri.indexOf(",") + 1)
            } else dataUri
            Base64.getDecoder().decode(pureBase64)
        } catch (_: Exception) { null }
    }

    private fun loadLocalBytes(path: String): ByteArray? {
        val file = File(dataRoot, path)
        return if (file.exists()) file.readBytes() else null
    }

    private fun cachePath(source: String): File {
        val hash = source.hashCode().toString(16)
        return File(cacheDir, "img_$hash")
    }
}
