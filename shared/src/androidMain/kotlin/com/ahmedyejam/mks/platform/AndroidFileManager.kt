package com.ahmedyejam.mks.platform

import android.content.Context
import com.ahmedyejam.mks.data.local.FileManager as LegacyFileManager
import com.ahmedyejam.mks.data.model.MksResult
import java.io.InputStream

/**
 * Android implementation of [FileManager] — delegates to the existing Android
 * FileManager which uses Context.filesDir paths.
 */
class AndroidFileManager(context: Context) : FileManager {
    private val delegate = LegacyFileManager(context)

    override fun getImagesDir(): String = delegate.getImagesDir().absolutePath

    override fun getDataDir(): String = delegate.getContext().filesDir.absolutePath

    override fun saveBase64AsImage(base64String: String): String? =
        delegate.saveBase64AsImage(base64String)

    override fun saveBase64AsImageDetailed(base64String: String): MksResult<String> =
        delegate.saveBase64AsImageDetailed(base64String)

    override fun saveImage(bytes: ByteArray): String? =
        delegate.saveImage(bytes)

    override fun saveImageDetailed(bytes: ByteArray): MksResult<String> =
        delegate.saveImageDetailed(bytes)

    override fun saveImage(inputStream: InputStream): String? =
        delegate.saveImage(inputStream)

    override fun saveImageDetailed(inputStream: InputStream): MksResult<String> =
        delegate.saveImageDetailed(inputStream)

    override fun downloadAndSaveImage(url: String): String? =
        delegate.downloadAndSaveImage(url)

    override suspend fun downloadAndSaveImageDetailed(url: String): MksResult<String> =
        delegate.downloadAndSaveImageDetailed(url)

    override fun copyToInternalStorage(sourceUri: String, originalName: String?): String? =
        delegate.copyAssetUriToInternalStorage(sourceUri, originalName)

    override fun deleteFile(relativePath: String): Boolean {
        val file = java.io.File(getDataDir(), relativePath)
        return file.delete()
    }

    override fun fileExists(relativePath: String): Boolean {
        val file = java.io.File(getDataDir(), relativePath)
        return file.exists()
    }

    override fun getAbsolutePath(relativePath: String): String =
        java.io.File(getDataDir(), relativePath).absolutePath
}
