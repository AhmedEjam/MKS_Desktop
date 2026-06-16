package com.ahmedyejam.mks.platform

import com.ahmedyejam.mks.data.model.MksResult
import java.io.InputStream

/**
 * Platform file I/O abstraction.
 *
 * Android: uses Context.filesDir, ContentResolver, and SAF URIs.
 * Desktop: uses ~/.local/share/mks/ (FHS-compliant).
 */
interface FileManager {
    /** Directory where images are stored. Platform-specific absolute path. */
    fun getImagesDir(): String

    /** Directory where app data is stored. */
    fun getDataDir(): String

    /** Decode Base64 string and save as image file. Returns relative path or null. */
    fun saveBase64AsImage(base64String: String): String?

    /** Decode Base64 string and save as image file. Returns MksResult with path or error. */
    fun saveBase64AsImageDetailed(base64String: String): MksResult<String>

    /** Save raw bytes as image file. Returns relative path or null. */
    fun saveImage(bytes: ByteArray): String?

    /** Save bytes as image file. Returns MksResult with path or error. */
    fun saveImageDetailed(bytes: ByteArray): MksResult<String>

    /** Save bytes from stream as file. Returns relative path or null. */
    fun saveImage(inputStream: InputStream): String?

    /** Save bytes from stream as file. Returns MksResult with path or error. */
    fun saveImageDetailed(inputStream: InputStream): MksResult<String>

    /** Download an image from HTTP URL and save locally. Returns relative path or null. */
    fun downloadAndSaveImage(url: String): String?

    /** Download an image from HTTP URL and save locally. Returns MksResult with path or error. */
    suspend fun downloadAndSaveImageDetailed(url: String): MksResult<String>

    /** Copy a file identified by URI/path to internal storage. Returns destination path or null. */
    fun copyToInternalStorage(sourceUri: String, originalName: String? = null): String?

    /** Delete a file by its relative path. Returns true if deleted. */
    fun deleteFile(relativePath: String): Boolean

    /** Check if a file exists. */
    fun fileExists(relativePath: String): Boolean

    /** Get absolute path for a relative path. */
    fun getAbsolutePath(relativePath: String): String
}
