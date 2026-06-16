package com.ahmedyejam.mks.data.importer.detector

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.ahmedyejam.mks.data.importer.model.ImportFormat
import java.io.InputStream
import java.nio.charset.Charset

class ImportFormatDetector(private val context: Context) {
    fun detectFormat(uri: Uri): ImportFormat {
        val fileName = getFileName(uri)
        val extension =
            fileName?.substringAfterLast('.', "")?.lowercase()
                ?: uri.path?.substringAfterLast('.', "")?.lowercase()

        // Check extension first as it's the most reliable for XLSX/ZIP
        val formatFromExt =
            when (extension) {
                "json" -> ImportFormat.JSON
                "zip" -> ImportFormat.ZIP
                "xlsx", "xlsm", "xltx", "xltm" -> ImportFormat.XLSX
                "xls" -> ImportFormat.XLSX // Map legacy XLS to XLSX for mode detection
                "csv", "tsv" -> ImportFormat.CSV_TSV
                "txt" -> ImportFormat.TEXT
                "html", "htm" -> ImportFormat.HTML
                else -> null
            }

        if (formatFromExt != null) return formatFromExt

        // Content detection if extension is missing or unclear
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                detectFromStream(inputStream)
            } ?: ImportFormat.UNKNOWN
        } catch (e: Exception) {
            ImportFormat.UNKNOWN
        }
    }

    private fun getFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) return cursor.getString(index)
                    }
                }
            } catch (e: Exception) {
            }
        }
        return uri.lastPathSegment
    }

    private fun detectFromStream(inputStream: InputStream): ImportFormat {
        // Use a buffered stream to peek at the content
        val bis = java.io.BufferedInputStream(inputStream)
        bis.mark(2048)
        val header = ByteArray(2048)
        val bytesRead = bis.read(header)
        // We don't necessarily need to reset if we are done with the stream here,
        // but it's good practice if it was passed in. However, we are in a 'use' block above.

        if (bytesRead < 4) return ImportFormat.UNKNOWN

        // Check for ZIP/XLSX magic number (PK..)
        if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
            header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
        ) {
            val headerStr = String(header, 0, bytesRead, Charset.forName("US-ASCII"))
            if (headerStr.contains("xl/") || headerStr.contains("[Content_Types].xml")) {
                return ImportFormat.XLSX
            }
            return ImportFormat.ZIP
        }

        // Check for Zip4j encrypted ZIP (PK\x07\x08)
        if (header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
            header[2] == 0x07.toByte() && header[3] == 0x08.toByte()
        ) {
            return ImportFormat.ZIP
        }

        // Check for XLS (Legacy Excel) magic number: D0 CF 11 E0 A1 B1 1A E1
        if (header[0] == 0xD0.toByte() && header[1] == 0xCF.toByte() &&
            header[2] == 0x11.toByte() && header[3] == 0xE0.toByte()
        ) {
            return ImportFormat.XLSX // Treat as XLSX for mode selection
        }

        // Check for JSON start ({ or [)
        val firstChar = header.firstOrNull { it > 32 }?.toInt()?.toChar()
        if (firstChar == '{' || firstChar == '[') {
            return ImportFormat.JSON
        }

        // Check for HTML
        val headerString = String(header, 0, bytesRead).lowercase()
        if (headerString.contains("<!doc") || headerString.contains("<html") || headerString.contains("<body")) {
            return ImportFormat.HTML
        }

        // Check for CSV/TSV like structure (simple heuristic)
        if (isLikelyTabular(headerString)) {
            return ImportFormat.CSV_TSV
        }

        return ImportFormat.TEXT
    }

    private fun isLikelyTabular(content: String): Boolean {
        val lines = content.lines().take(5).filter { it.isNotBlank() }
        if (lines.isEmpty()) return false

        val commonSeparators = listOf('\t', ',', ';')
        for (sep in commonSeparators) {
            val counts = lines.map { it.count { char -> char == sep } }
            if (counts.all { it > 0 } && counts.distinct().size <= 2) {
                return true
            }
        }
        return false
    }
}
