package com.ahmedyejam.mks.platform

import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.util.MksLogger
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.UUID

private const val MAX_IMAGE_INPUT_BYTES = 12 * 1024 * 1024

/**
 * Desktop (Linux) implementation of [FileManager].
 *
 * Uses FHS-compliant paths:
 *   Data root:   ~/.local/share/mks/
 *   Images:      ~/.local/share/mks/images/
 */
class DesktopFileManager : FileManager {
    private val dataRoot: File by lazy {
        File(System.getProperty("user.home"), ".local/share/mks").also { it.mkdirs() }
    }
    private val imagesDir: File by lazy {
        File(dataRoot, "images").also { it.mkdirs() }
    }

    override fun getImagesDir(): String = imagesDir.absolutePath
    override fun getDataDir(): String = dataRoot.absolutePath

    override fun saveBase64AsImage(base64String: String): String? =
        saveBase64AsImageDetailed(base64String).getOrNull()

    override fun saveBase64AsImageDetailed(base64String: String): MksResult<String> {
        if (base64String.isBlank()) return MksResult.Error(message = "Image data is empty.")
        return try {
            val pureBase64 = if (base64String.contains(",")) {
                base64String.substring(base64String.indexOf(",") + 1)
            } else base64String
            val estimatedBytes = (pureBase64.length.toLong() * 3L) / 4L
            if (estimatedBytes > MAX_IMAGE_INPUT_BYTES) {
                return MksResult.Error(message = "Image exceeds ${MAX_IMAGE_INPUT_BYTES / (1024 * 1024)} MB input limit.")
            }
            val bytes = Base64.getDecoder().decode(pureBase64)
            saveImageDetailed(bytes)
        } catch (e: Exception) {
            MksResult.Error(message = e.message ?: "Invalid Base64 image data.", exception = e)
        }
    }

    override fun saveImage(bytes: ByteArray): String? =
        saveImageDetailed(bytes).getOrNull()

    override fun saveImageDetailed(bytes: ByteArray): MksResult<String> {
        if (bytes.isEmpty()) return MksResult.Error(message = "Image data is empty.")
        if (bytes.size > MAX_IMAGE_INPUT_BYTES) {
            return MksResult.Error(message = "Image exceeds ${MAX_IMAGE_INPUT_BYTES / (1024 * 1024)} MB input limit.")
        }
        return try {
            val ext = detectImageExtension(bytes) ?: "png"
            val fileName = "img_${UUID.randomUUID()}.$ext"
            val file = File(imagesDir, fileName)
            file.writeBytes(bytes)
            MksResult.Success(file.absolutePath)
        } catch (e: Exception) {
            MksResult.Error(message = e.message ?: "Failed to save image.", exception = e)
        }
    }

    override fun saveImage(inputStream: InputStream): String? =
        saveImageDetailed(inputStream).getOrNull()

    override fun saveImageDetailed(inputStream: InputStream): MksResult<String> {
        return try {
            val bytes = inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                val chunks = mutableListOf<ByteArray>()
                var totalRead = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    totalRead += read
                    if (totalRead > MAX_IMAGE_INPUT_BYTES) {
                        return MksResult.Error(message = "Image exceeds ${MAX_IMAGE_INPUT_BYTES / (1024 * 1024)} MB input limit.")
                    }
                    chunks.add(buffer.copyOf(read))
                }
                chunks.fold(ByteArray(0)) { acc, chunk -> acc + chunk }
            }
            saveImageDetailed(bytes)
        } catch (e: Exception) {
            MksResult.Error(message = e.message ?: "Failed to read image stream.", exception = e)
        }
    }

    override fun downloadAndSaveImage(url: String): String? =
        runCatching { runBlocking { downloadAndSaveImageDetailed(url) } }.getOrNull()?.getOrNull()

    override suspend fun downloadAndSaveImageDetailed(url: String): MksResult<String> {
        if (url.isBlank()) return MksResult.Error(message = "URL is empty.")
        val scheme = runCatching { URL(url).protocol?.lowercase() }.getOrNull()
        if (scheme != "http" && scheme != "https") {
            return MksResult.Error(message = "Only HTTP(S) image URLs are supported.")
        }
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode !in 200..299) {
                return MksResult.Error(message = "HTTP ${connection.responseCode}: ${connection.responseMessage}")
            }

            val contentType = connection.contentType ?: ""
            val ext = when {
                contentType.contains("png") -> "png"
                contentType.contains("jpeg") || contentType.contains("jpg") -> "jpg"
                contentType.contains("webp") -> "webp"
                contentType.contains("gif") -> "gif"
                else -> "png"
            }
            val fileName = "img_${UUID.randomUUID()}.$ext"
            val file = File(imagesDir, fileName)
            connection.inputStream.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            MksResult.Success(file.absolutePath)
        } catch (e: Exception) {
            MksLogger.w("DesktopFileManager", "Image download failed: ${url}", e)
            MksResult.Error(message = e.message ?: "Failed to download image.", exception = e)
        }
    }

    override fun copyToInternalStorage(sourceUri: String, originalName: String?): String? {
        val sourceFile = File(sourceUri)
        if (!sourceFile.exists()) return null
        val destName = originalName ?: sourceFile.name
        val destFile = File(dataRoot, destName)
        return try {
            sourceFile.copyTo(destFile, overwrite = true)
            destFile.absolutePath
        } catch (_: Exception) { null }
    }

    override fun deleteFile(relativePath: String): Boolean {
        return File(relativePath).delete()
    }

    override fun fileExists(relativePath: String): Boolean {
        return File(relativePath).exists()
    }

    override fun getAbsolutePath(relativePath: String): String {
        return File(relativePath).absolutePath
    }

    private fun detectImageExtension(bytes: ByteArray): String? {
        if (bytes.size < 4) return null
        return when {
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpg"
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "png"
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() -> "gif"
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() -> "webp"
            else -> null
        }
    }
}
