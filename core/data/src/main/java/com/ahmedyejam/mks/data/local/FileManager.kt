package com.ahmedyejam.mks.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.ahmedyejam.mks.data.model.AssetFileSaveResult
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.data.network.RemoteAssetFetcher
import com.ahmedyejam.mks.data.network.RemoteAssetPolicy
import com.ahmedyejam.mks.util.MksLogger
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class FileManager(private val context: Context) {
    private val remoteAssetFetcher by lazy { RemoteAssetFetcher() }

    companion object {
        private const val MAX_IMAGE_INPUT_BYTES = 12 * 1024 * 1024
        private const val MAX_IMAGE_DIMENSION = 2048
        private const val MAX_IMAGE_PIXELS = 12_000_000L
        private const val IMAGE_QUALITY = 82
        private const val MAX_ASSET_INPUT_BYTES = 50 * 1024 * 1024
    }

    private data class NormalizedImageResult(
        val bytes: ByteArray? = null,
        val error: String? = null,
    )

    fun getContext(): Context = context

    fun saveBase64AsImage(base64String: String): String? {
        return saveBase64AsImageDetailed(base64String).getOrNull()
    }

    fun saveBase64AsImageDetailed(base64String: String): MksResult<String> {
        if (base64String.isBlank()) return MksResult.Error(message = "Image data is empty.")

        return try {
            val pureBase64 =
                if (base64String.contains(",")) {
                    base64String.substring(base64String.indexOf(",") + 1)
                } else {
                    base64String
                }

            val estimatedBytes = ((pureBase64.length.toLong() * 3L) / 4L)
            if (estimatedBytes > MAX_IMAGE_INPUT_BYTES) {
                return MksResult.Error(message = "Image exceeds ${MAX_IMAGE_INPUT_BYTES / (1024 * 1024)} MB input limit.")
            }

            val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            saveImageDetailed(imageBytes)
        } catch (e: Exception) {
            MksResult.Error(message = e.message ?: "Invalid Base64 image data.", exception = e)
        }
    }

    fun saveImage(base64String: String?): String? {
        return saveImageDetailed(base64String).getOrNull()
    }

    fun copyAssetUriToInternalStorage(
        uriString: String,
        originalName: String? = null,
    ): MksResult<AssetFileSaveResult> {
        if (uriString.isBlank()) return MksResult.Error(message = "Asset URI is empty.")
        return try {
            val uri = Uri.parse(uriString)
            val scheme = uri.scheme?.lowercase()
            if (scheme != "content" && scheme != "file") {
                return MksResult.Error(message = "Only content:// or file:// local asset URIs can be copied.")
            }

            val mimeType = context.contentResolver.getType(uri)
            val safeName =
                sanitizeAssetFileName(
                    originalName?.takeIf { it.isNotBlank() }
                        ?: uri.lastPathSegment
                        ?: "asset",
                )
            val extension =
                safeName.substringAfterLast('.', missingDelimiterValue = "bin")
                    .takeIf { it.isNotBlank() }
                    ?: "bin"
            val fileName = "asset_${UUID.randomUUID()}.$extension"
            val target = File(getQuestionAssetsDir(), fileName).canonicalFile

            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes =
                    readBytesWithLimit(input, MAX_ASSET_INPUT_BYTES)
                        ?: return MksResult.Error(message = "Asset exceeds ${MAX_ASSET_INPUT_BYTES / (1024 * 1024)} MB input limit.")
                FileOutputStream(target).use { output -> output.write(bytes) }
                return MksResult.Success(
                    AssetFileSaveResult(
                    path = target.absolutePath,
                    mimeType = mimeType,
                    fileName = safeName,
                    fileSizeBytes = bytes.size.toLong(),
                    )
                )
            }
            MksResult.Error(message = "Unable to open asset URI.")
        } catch (e: Exception) {
            MksLogger.w("FileManager", "Asset copy operation failed", e)
            MksResult.Error(message = e.message ?: "Failed to copy asset.", exception = e)
        }
    }

    fun deleteAssetFile(path: String?) {
        if (path.isNullOrBlank()) return
        if (!isPathInsideAssetsDir(path)) return
        try {
            val file = File(path).canonicalFile
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            MksLogger.w("FileManager", "Asset delete operation failed", e)
        }
    }

    private fun getQuestionAssetsDir(): File {
        val dir = File(context.filesDir, "question_assets")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun isPathInsideAssetsDir(path: String): Boolean {
        return try {
            val file = File(path).canonicalFile
            val assetsDir = getQuestionAssetsDir().canonicalFile
            file == assetsDir || file.toPath().startsWith(assetsDir.toPath())
        } catch (e: Exception) {
            false
        }
    }

    private fun sanitizeAssetFileName(value: String): String {
        return value.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "asset" }
            .take(96)
    }

    fun saveImageDetailed(base64String: String?): MksResult<String> {
        if (base64String.isNullOrBlank()) return MksResult.Error(message = "Image reference is empty.")

        // Check if it's already a local path or a relative path
        if (base64String.startsWith("/") || base64String.startsWith("assets/")) {
            // SECURITY: Reject absolute paths to prevent path injection
            if (base64String.startsWith("/")) {
                // If it's already inside our internal images directory, it's fine
                if (isPathInsideImagesDir(base64String)) {
                    return MksResult.Success(base64String)
                }
                return MksResult.Error(message = "Absolute image paths outside the app images directory are not allowed.")
            }
            return MksResult.Error(message = "Relative asset references are not accepted here.")
        }

        return saveBase64AsImageDetailed(base64String)
    }

    fun saveImage(
        bytes: ByteArray,
        extension: String = "webp",
    ): String? {
        return saveImageDetailed(bytes, extension).getOrNull()
    }

    fun saveImageDetailed(
        bytes: ByteArray,
        extension: String = "webp",
    ): MksResult<String> {
        return try {
            val normalized = normalizeImageBytes(bytes)
            val finalBytes =
                normalized.bytes
                    ?: return MksResult.Error(
                        message = normalized.error ?: "Image normalization failed."
                    )
            val path =
                writeNormalizedImage(finalBytes)
                    ?: return MksResult.Error(message = "Failed to persist normalized image.")
            MksResult.Success(path)
        } catch (e: Exception) {
            MksLogger.w("FileManager", "Image operation failed", e)
            MksResult.Error(message = e.message ?: "Failed to save image.", exception = e)
        }
    }

    suspend fun downloadAndSaveImage(
        url: String,
        policy: RemoteAssetPolicy = RemoteAssetPolicy.Default,
    ): String? {
        return downloadAndSaveImageDetailed(url, policy).getOrNull()
    }

    suspend fun downloadAndSaveImageDetailed(
        url: String,
        policy: RemoteAssetPolicy = RemoteAssetPolicy.Default,
    ): MksResult<String> {
        if (!isHttpOrHttpsUrl(url)) return MksResult.Error(message = "Only HTTP(S) image URLs are supported.")

        val fetched = remoteAssetFetcher.fetch(url, policy.copy(maxBytes = MAX_IMAGE_INPUT_BYTES.toLong()))
        val bytes =
            fetched.bytes ?: return MksResult.Error(
                message = fetched.error ?: fetched.warning ?: "Remote image was not downloaded.",
            )
        val saved = saveImageDetailed(bytes)
        return if (saved is MksResult.Success && fetched.warning != null) {
            // In case of success with warning, we still return Success but could log the warning
            MksLogger.w("FileManager", "Image downloaded with warning: ${fetched.warning}")
            saved
        } else {
            saved
        }
    }

    fun saveImage(
        inputStream: InputStream,
        originalName: String? = null,
    ): String? {
        return saveImageDetailed(inputStream, originalName).getOrNull()
    }

    fun saveImageDetailed(
        inputStream: InputStream,
        originalName: String? = null,
    ): MksResult<String> {
        return try {
            val bytes =
                readBytesWithLimit(inputStream, MAX_IMAGE_INPUT_BYTES)
                    ?: return MksResult.Error(message = "Image exceeds ${MAX_IMAGE_INPUT_BYTES / (1024 * 1024)} MB input limit.")
            saveImageDetailed(bytes)
        } catch (e: Exception) {
            MksLogger.w("FileManager", "Image operation failed", e)
            MksResult.Error(message = e.message ?: "Failed to read image stream.", exception = e)
        }
    }

    fun saveImage(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                saveImage(input, uri.lastPathSegment)
            }
        } catch (e: Exception) {
            MksLogger.w("FileManager", "Image operation failed", e)
            null
        }
    }

    private fun getImagesDir(): File {
        val dir = File(context.filesDir, "images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun isPathInsideImagesDir(path: String): Boolean {
        return try {
            val file = File(path).canonicalFile
            val imagesDir = getImagesDir().canonicalFile
            file == imagesDir || file.toPath().startsWith(imagesDir.toPath())
        } catch (e: Exception) {
            false
        }
    }

    private fun isHttpOrHttpsUrl(value: String): Boolean {
        val scheme = runCatching { Uri.parse(value).scheme?.lowercase() }.getOrNull()
        return scheme == "http" || scheme == "https"
    }

    fun getFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        // Security check: only allow files from internal images directory
        if (!isPathInsideImagesDir(path)) return null

        val file = File(path)
        return if (file.exists()) file else null
    }

    fun getBase64Image(path: String?): String {
        if (path.isNullOrBlank()) return ""
        if (!isPathInsideImagesDir(path)) return ""

        return try {
            val file = File(path)
            if (!file.exists()) return ""

            val bytes = file.readBytes()
            val mimeType =
                when (file.extension.lowercase()) {
                    "png" -> "image/png"
                    "jpg", "jpeg" -> "image/jpeg"
                    "webp" -> "image/webp"
                    else -> "image/webp"
                }

            "data:$mimeType;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            MksLogger.w("FileManager", "Image operation failed", e)
            ""
        }
    }

    fun deleteImage(path: String?) {
        if (path.isNullOrBlank()) return
        // SECURITY: Only allow deleting files within our images directory
        if (!isPathInsideImagesDir(path)) return

        try {
            val file = File(path).canonicalFile
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            MksLogger.w("FileManager", "Image operation failed", e)
        }
    }

    private fun normalizeImageBytes(bytes: ByteArray): NormalizedImageResult {
        if (bytes.isEmpty()) return NormalizedImageResult(error = "Image data is empty.")
        if (bytes.size > MAX_IMAGE_INPUT_BYTES) {
            return NormalizedImageResult(error = "Image exceeds ${MAX_IMAGE_INPUT_BYTES / (1024 * 1024)} MB input limit.")
        }

        val bounds =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sourceWidth = bounds.outWidth
        val sourceHeight = bounds.outHeight
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return NormalizedImageResult(error = "Unsupported or invalid image format.")
        }

        val sourcePixels = sourceWidth.toLong() * sourceHeight.toLong()
        if (sourcePixels <= 0L) {
            return NormalizedImageResult(error = "Invalid image dimensions.")
        }

        val decodeOptions =
            BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = calculateInSampleSize(sourceWidth, sourceHeight, sourcePixels)
            }

        val decoded =
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                ?: return NormalizedImageResult(error = "Failed to decode image.")
        val scaled = scaleBitmapIfNeeded(decoded)
        if (scaled !== decoded) {
            decoded.recycle()
        }

        return try {
            ByteArrayOutputStream().use { output ->
                val format =
                    if (scaled.hasAlpha()) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    }
                if (!scaled.compress(format, IMAGE_QUALITY, output)) {
                    return NormalizedImageResult(error = "Failed to compress normalized image.")
                }
                NormalizedImageResult(bytes = output.toByteArray())
            }
        } finally {
            scaled.recycle()
        }
    }

    private fun writeNormalizedImage(bytes: ByteArray): String? {
        val fileName = "img_${UUID.randomUUID()}.webp"
        val file = File(getImagesDir(), fileName).canonicalFile
        FileOutputStream(file).use { output ->
            output.write(bytes)
        }
        return file.absolutePath
    }

    private fun readBytesWithLimit(
        inputStream: InputStream,
        maxBytes: Int,
    ): ByteArray? {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalRead = 0
        val output = ByteArrayOutputStream()
        while (true) {
            val read = inputStream.read(buffer)
            if (read == -1) break
            totalRead += read
            if (totalRead > maxBytes) {
                return null
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        totalPixels: Long,
    ): Int {
        var sampleSize = 1
        while (
            width / sampleSize > MAX_IMAGE_DIMENSION ||
            height / sampleSize > MAX_IMAGE_DIMENSION ||
            totalPixels / (sampleSize.toLong() * sampleSize.toLong()) > MAX_IMAGE_PIXELS
        ) {
            sampleSize *= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val largestDimension = maxOf(width, height)
        if (largestDimension <= MAX_IMAGE_DIMENSION) {
            return bitmap
        }

        val scale = MAX_IMAGE_DIMENSION.toFloat() / largestDimension.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
