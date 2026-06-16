package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.model.ImportMode

class SourceDetector {
    fun detectMode(
        fileName: String?,
        content: String?,
    ): ImportMode {
        val ext = fileName?.substringAfterLast('.')?.lowercase() ?: ""

        if (ext in listOf("xlsx", "xls", "csv", "tsv")) {
            return ImportMode.SPREADSHEET
        }

        if (ext == "json") return ImportMode.JSON
        if (ext in listOf("html", "htm")) return ImportMode.HTML

        val trimmed = content?.trim() ?: ""
        if (trimmed.startsWith("[") || (trimmed.startsWith("{") && trimmed.contains("\"kind\""))) {
            return ImportMode.JSON
        }

        if (trimmed.contains("<html", ignoreCase = true) ||
            trimmed.contains("<script", ignoreCase = true) ||
            trimmed.contains("<img", ignoreCase = true)
        ) {
            return ImportMode.HTML
        }

        // Guess spreadsheet if it looks like CSV/TSV and has keywords
        if (isTabular(trimmed)) {
            return ImportMode.SPREADSHEET
        }

        return ImportMode.TEXT
    }

    private fun isTabular(content: String): Boolean {
        val lines = content.lines().take(5)
        if (lines.isEmpty()) return false

        val commonSeparators = listOf('\t', ',', ';')
        for (sep in commonSeparators) {
            val counts = lines.map { it.count { char -> char == sep } }
            if (counts.all { it > 0 } && counts.distinct().size <= 2) {
                // Check for keywords in the first row
                val header = lines.first().lowercase()
                val keywords = listOf("question", "answer", "correct", "stem", "السؤال", "الإجابة")
                if (keywords.any { header.contains(it) }) return true
            }
        }
        return false
    }
}
