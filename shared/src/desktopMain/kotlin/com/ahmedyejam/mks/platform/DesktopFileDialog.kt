package com.ahmedyejam.mks.platform

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Desktop (Linux) implementation of [FileDialog].
 *
 * Uses AWT javax.swing.JFileChooser for file open/save dialogs.
 * Note: On headless systems, this will fail at runtime.
 * A future XDG Portal implementation could replace this.
 */
class DesktopFileDialog : FileDialog {
    override fun openFile(filters: List<FileFilter>, onResult: (FileDialogResult?) -> Unit) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Open File"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false

            if (filters.isNotEmpty()) {
                // Add combined filter for all supported extensions
                val allExtensions = filters.flatMap { it.extensions }.toTypedArray()
                addChoosableFileFilter(FileNameExtensionFilter("All Supported Files", *allExtensions))
            }

            // Add individual filters
            filters.forEach { filter ->
                if (filter.extensions.isNotEmpty()) {
                    addChoosableFileFilter(
                        FileNameExtensionFilter(
                            filter.description,
                            *filter.extensions.toTypedArray()
                        )
                    )
                }
            }
        }

        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            onResult(
                FileDialogResult(
                    path = file.absolutePath,
                    displayName = file.name,
                    mimeType = guessMimeType(file.extension),
                    fileSize = file.length()
                )
            )
        } else {
            onResult(null)
        }
    }

    override fun saveFile(suggestedName: String, onResult: (FileDialogResult?) -> Unit) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Save File"
            fileSelectionMode = JFileChooser.FILES_ONLY
            if (suggestedName.isNotEmpty()) {
                selectedFile = File(suggestedName)
            }
        }

        val result = chooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            onResult(
                FileDialogResult(
                    path = file.absolutePath,
                    displayName = file.name,
                    mimeType = guessMimeType(file.extension),
                    fileSize = if (file.exists()) file.length() else 0L
                )
            )
        } else {
            onResult(null)
        }
    }

    private fun guessMimeType(extension: String): String = when (extension.lowercase()) {
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "xls" -> "application/vnd.ms-excel"
        "csv" -> "text/csv"
        "tsv" -> "text/tab-separated-values"
        "json" -> "application/json"
        "html", "htm" -> "text/html"
        "txt" -> "text/plain"
        "zip" -> "application/zip"
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "application/octet-stream"
    }
}
