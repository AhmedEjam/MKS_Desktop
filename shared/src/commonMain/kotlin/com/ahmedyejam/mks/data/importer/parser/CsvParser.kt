package com.ahmedyejam.mks.data.importer.parser

class CsvParser {
    fun parse(
        content: String,
        delimiter: Char? = null,
    ): List<List<String>> {
        if (content.isBlank()) return emptyList()

        val sep = delimiter ?: inferDelimiter(content)
        val result = mutableListOf<List<String>>()
        val currentLine = mutableListOf<String>()
        val currentValue = StringBuilder()

        var inQuotes = false
        var justClosedQuote = false
        var i = 0

        while (i < content.length) {
            val char = content[i]

            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < content.length && content[i + 1] == '"') {
                        // Escaped quote
                        currentValue.append('"')
                        i++
                    } else if (inQuotes) {
                        // Close a quoted field. We only honor quotes as structural
                        // when they appear at field boundaries.
                        inQuotes = false
                        justClosedQuote = true
                    } else if (isAtQuotedFieldStart(currentValue)) {
                        // Discard leading whitespace before an opening quote.
                        currentValue.setLength(0)
                        inQuotes = true
                        justClosedQuote = false
                    } else {
                        // Treat misplaced quotes as literal content.
                        currentValue.append(char)
                        justClosedQuote = false
                    }
                }
                char == sep && !inQuotes -> {
                    currentLine.add(currentValue.toString().trim())
                    currentValue.setLength(0)
                    justClosedQuote = false
                }
                (char == '\n' || char == '\r') && !inQuotes -> {
                    if (char == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
                        i++
                    }
                    currentLine.add(currentValue.toString().trim())
                    if (currentLine.any { it.isNotEmpty() }) {
                        result.add(ArrayList(currentLine))
                    }
                    currentLine.clear()
                    currentValue.setLength(0)
                    justClosedQuote = false
                }
                else -> {
                    if (justClosedQuote) {
                        if (char.isWhitespace()) {
                            // Ignore trailing whitespace between a closing quote and a delimiter/newline.
                        } else {
                            // The quote was misplaced after all, so preserve it as data.
                            currentValue.append('"')
                            currentValue.append(char)
                        }
                        justClosedQuote = false
                    } else {
                        currentValue.append(char)
                    }
                }
            }
            i++
        }

        // Handle last value and line if not ended with newline
        if (currentValue.isNotEmpty() || currentLine.isNotEmpty()) {
            currentLine.add(currentValue.toString().trim())
            if (currentLine.any { it.isNotEmpty() }) {
                result.add(ArrayList(currentLine))
            }
        }

        return result
    }

    private fun inferDelimiter(content: String): Char {
        val testDelimiters = listOf(',', '\t', ';')
        val lines = content.lineSequence().filter { it.isNotBlank() }.toList()

        if (lines.isEmpty()) return ','

        return testDelimiters
            .map { delimiter -> buildDelimiterScore(delimiter, lines.map { line -> countFields(line, delimiter) }) }
            .maxWithOrNull(
                compareBy<DelimiterScore> { it.supportingRows }
                    .thenBy { it.modeFrequency }
                    .thenBy { it.modeFieldCount }
                    .thenByDescending { it.varianceScore },
            )?.delimiter ?: ','
    }

    private fun countFields(
        line: String,
        delimiter: Char,
    ): Int {
        var count = 1
        var inQuotes = false
        var justClosedQuote = false
        val currentField = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    i++
                } else if (inQuotes) {
                    inQuotes = false
                    justClosedQuote = true
                } else if (isAtQuotedFieldStart(currentField)) {
                    inQuotes = true
                    currentField.setLength(0)
                } else {
                    // Misplaced quotes are treated as literal content.
                    currentField.append(c)
                }
            } else if (c == delimiter && !inQuotes) {
                count++
                currentField.setLength(0)
                justClosedQuote = false
            } else if (justClosedQuote) {
                if (c.isWhitespace()) {
                    // Ignore trailing whitespace before the delimiter/newline.
                } else {
                    currentField.append('"')
                    currentField.append(c)
                }
                justClosedQuote = false
            } else {
                currentField.append(c)
            }
            i++
        }
        return count
    }

    private fun isAtQuotedFieldStart(currentValue: CharSequence): Boolean {
        return currentValue.isEmpty() || currentValue.all { it.isWhitespace() }
    }

    private fun buildDelimiterScore(
        delimiter: Char,
        counts: List<Int>,
    ): DelimiterScore {
        val supportingCounts = counts.filter { it > 1 }
        if (supportingCounts.isEmpty()) {
            return DelimiterScore(
                delimiter = delimiter,
                supportingRows = 0,
                modeFrequency = 0,
                modeFieldCount = 0,
                varianceScore = Double.NEGATIVE_INFINITY,
            )
        }

        val grouped = supportingCounts.groupingBy { it }.eachCount()
        val modeFieldCount =
            grouped.maxWithOrNull(
                compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { it.key },
            )?.key ?: 0
        val modeFrequency = grouped[modeFieldCount] ?: 0
        val avg = supportingCounts.average()
        val variance = supportingCounts.map { (it - avg) * (it - avg) }.sum() / supportingCounts.size

        return DelimiterScore(
            delimiter = delimiter,
            supportingRows = supportingCounts.size,
            modeFrequency = modeFrequency,
            modeFieldCount = modeFieldCount,
            varianceScore = -variance,
        )
    }

    private data class DelimiterScore(
        val delimiter: Char,
        val supportingRows: Int,
        val modeFrequency: Int,
        val modeFieldCount: Int,
        val varianceScore: Double,
    )
}
