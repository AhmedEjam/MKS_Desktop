package com.ahmedyejam.mks.platform

/**
 * Platform file dialog abstraction.
 *
 * Android: uses Storage Access Framework (SAF) via ActivityResultContracts.
 * Desktop: uses AWT JFileChooser.
 *
 * @param path The selected file path, or null if the user cancelled.
 */
data class FileDialogResult(
    val path: String,
    val displayName: String,
    val mimeType: String? = null,
    val fileSize: Long = 0L
)

/**
 * File filter for dialogs.
 */
data class FileFilter(
    val description: String,
    val extensions: List<String>  // e.g. ["xlsx", "csv", "json"]
)

/**
 * Platform file selection dialog.
 *
 * Android: launches SAF document picker via ActivityResultContracts.OpenDocument.
 * Desktop: shows javax.swing.JFileChooser (AWT).
 */
interface FileDialog {
    /**
     * Show a file open dialog filtered by supported formats.
     * @param filters List of file filters to apply.
     * @param onResult Callback with selected file info, or null if cancelled.
     */
    fun openFile(
        filters: List<FileFilter> = emptyList(),
        onResult: (FileDialogResult?) -> Unit
    )

    /**
     * Show a file save dialog.
     * @param suggestedName Default filename.
     * @param onResult Callback with selected path, or null if cancelled.
     */
    fun saveFile(
        suggestedName: String = "",
        onResult: (FileDialogResult?) -> Unit
    )
}
