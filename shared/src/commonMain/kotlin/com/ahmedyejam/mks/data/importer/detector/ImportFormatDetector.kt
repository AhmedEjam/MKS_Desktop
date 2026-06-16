package com.ahmedyejam.mks.data.importer.detector

import com.ahmedyejam.mks.data.importer.model.ImportFormat
import java.io.File

class ImportFormatDetector {
    fun detectFormat(filePath: String): ImportFormat {
        val file = File(filePath)
        val ext = file.extension.lowercase()

        return when (ext) {
            "json" -> ImportFormat.JSON
            "zip" -> ImportFormat.ZIP
            "xlsx", "xlsm", "xls" -> ImportFormat.XLSX
            "csv", "tsv" -> ImportFormat.CSV_TSV
            "txt" -> ImportFormat.TEXT
            "html", "htm" -> ImportFormat.HTML
            else -> ImportFormat.UNKNOWN
        }
    }
}
