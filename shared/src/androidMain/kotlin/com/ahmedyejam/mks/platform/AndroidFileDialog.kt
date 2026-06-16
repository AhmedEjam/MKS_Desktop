package com.ahmedyejam.mks.platform

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Android implementation of [FileDialog] using Storage Access Framework
 * via ActivityResultContracts.OpenDocument / CreateDocument.
 *
 * Must be registered with an Activity's ActivityResultRegistry.
 * Call [launch] from the Activity, ideally via a launcher registered in onCreate.
 */
class AndroidFileDialog(private val activity: Activity) : FileDialog {
    private var pendingCallback: ((FileDialogResult?) -> Unit)? = null

    private val openLauncher = activity.activityResultRegistry.register(
        "mks_file_open",
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val callback = pendingCallback ?: return@register
        pendingCallback = null
        if (uri != null) {
            val result = buildResult(uri)
            callback(result)
        } else {
            callback(null)
        }
    }

    private val createLauncher = activity.activityResultRegistry.register(
        "mks_file_save",
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val callback = pendingCallback ?: return@register
        pendingCallback = null
        if (uri != null) {
            callback(FileDialogResult(path = uri.toString(), displayName = ""))
        } else {
            callback(null)
        }
    }

    override fun openFile(filters: List<FileFilter>, onResult: (FileDialogResult?) -> Unit) {
        pendingCallback = onResult
        val mimeTypes = if (filters.isEmpty()) {
            arrayOf("*/*")
        } else {
            filters.flatMap { filter ->
                filter.extensions.map { ext ->
                    when (ext.lowercase()) {
                        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        "xls" -> "application/vnd.ms-excel"
                        "csv" -> "text/csv"
                        "tsv" -> "text/tab-separated-values"
                        "json" -> "application/json"
                        "html" -> "text/html"
                        "txt" -> "text/plain"
                        "zip" -> "application/zip"
                        "pdf" -> "application/pdf"
                        "png" -> "image/png"
                        "jpg", "jpeg" -> "image/jpeg"
                        else -> "*/*"
                    }
                }
            }.distinct().toTypedArray()
        }
        openLauncher.launch(mimeTypes)
    }

    override fun saveFile(suggestedName: String, onResult: (FileDialogResult?) -> Unit) {
        pendingCallback = onResult
        createLauncher.launch(suggestedName)
    }

    private fun buildResult(uri: Uri): FileDialogResult {
        var displayName = "unknown"
        var mimeType: String? = null
        var fileSize = 0L

        activity.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: displayName

                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
            }
        }
        mimeType = activity.contentResolver.getType(uri)

        // Copy to app-local storage for persistent access
        val inputStream = activity.contentResolver.openInputStream(uri)
        val destFile = java.io.File(activity.cacheDir, "import_${displayName}")
        inputStream?.use { input ->
            java.io.FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        return FileDialogResult(
            path = destFile.absolutePath,
            displayName = displayName,
            mimeType = mimeType,
            fileSize = fileSize
        )
    }
}
