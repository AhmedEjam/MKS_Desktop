package com.ahmedyejam.mks.platform

import android.content.Context
import coil.ImageLoader as CoilImageLoader
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of [ImageLoader] using Coil.
 *
 * Coil provides automatic memory + disk caching.
 */
class AndroidImageLoader(private val context: Context) : ImageLoader {
    private val loader = CoilImageLoader.Builder(context)
        .crossfade(true)
        .build()

    override suspend fun loadBytes(source: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(source)
                .size(Size.ORIGINAL)
                .build()
            val drawable = loader.execute(request).drawable ?: return@withContext null
            // Convert drawable to bytes via bitmap
            val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                ?: return@withContext null
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun prefetch(source: String) = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(source)
                .size(Size.ORIGINAL)
                .build()
            loader.enqueue(request)
        } catch (_: Exception) { }
    }

    override fun clearMemoryCache() {
        loader.memoryCache?.clear()
    }

    override fun clearDiskCache() {
        loader.diskCache?.clear()
    }
}
