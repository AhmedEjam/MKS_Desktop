package com.ahmedyejam.mks.platform

import java.io.InputStream

/**
 * Platform image loading abstraction.
 *
 * Android: uses Coil (memory + disk cache).
 * Desktop: uses Skiko Canvas rendering with LRU memory cache and disk cache.
 *
 * Images can be loaded from:
 * - Local file paths (relative to platform data dir)
 * - HTTP/HTTPS URLs
 * - Base64 data URIs
 */
interface ImageLoader {
    /**
     * Load image bytes from a source.
     * @param source Local path, HTTP URL, or Base64 data URI.
     * @return Decoded image bytes, or null if loading fails.
     */
    suspend fun loadBytes(source: String): ByteArray?

    /**
     * Prefetch an image into cache without returning the result.
     */
    suspend fun prefetch(source: String)

    /** Clear the in-memory LRU cache. */
    fun clearMemoryCache()

    /** Clear the disk cache. */
    fun clearDiskCache()
}
