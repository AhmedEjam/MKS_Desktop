package com.ahmedyejam.mks.data.importer.parser

class SpreadsheetHeaderMapper {
    private val aliases =
        mapOf(
            "question" to listOf("question", "question summary", "question text", "q text", "question_text", "q", "stem", "rephrased stem", "question stem", "text", "front", "السؤال", "سؤال", "نص السؤال", "نص", "الوجه الأمامي"),
            "answer" to listOf("answer", "answers", "ans", "an", "correct", "key", "correct answer", "answer key", "correct_option", "back", "الإجابة", "الجواب", "مفتاح الإجابة", "الوجه الخلفي"),
            "explanation" to listOf("explanation", "explanation summary", "rationale", "exp", "reason", "summary", "logic", "الشرح", "تفسير"),
            "hint" to listOf("hint", "hints\\hint", "hints/hint", "high yield", "high-yield", "tip", "clue", "تلميح", "ملمح"),
            "reference" to listOf("reference", "ref", "source", "chapter q", "ch q", "chapter_q", "ch_q", "q#", "question number", "citation", "مرجع", "reference text"),
            "additional" to listOf("additional info", "additional", "info", "notes", "extra", "high-yield", "high yield", "facts", "fact", "idea", "معلومة", "ملاحظات"),
            "categories" to listOf("categories", "category", "tags", "tag", "topic", "topics", "تصنيف", "فئة"),
            "image" to listOf("image", "image url", "img", "picture", "media", "image source", "option image", "صورة"),
            "id" to listOf("id", "external id", "guid", "uid", "question id", "رقم السؤال", "معرف"),
        )

    private val fieldWeights =
        mapOf(
            "question" to 100,
            "answer" to 80,
            "id" to 10,
            "explanation" to 10,
            "hint" to 5,
            "reference" to 5,
            "categories" to 5,
            "image" to 5,
            "additional" to 2,
        )

    private val optionAliases =
        listOf(
            listOf("option a", "opt a", "choice a", "answer a", "a", "أ"),
            listOf("option b", "opt b", "choice b", "answer b", "b", "ب"),
            listOf("option c", "opt c", "choice c", "answer c", "c", "ج"),
            listOf("option d", "opt d", "choice d", "answer d", "d", "د"),
            listOf("option e", "opt e", "choice e", "answer e", "e", "هـ"),
            listOf("option f", "opt f", "choice f", "answer f", "f", "و"),
            listOf("الخيار أ", "الخيار 1", "خيار 1"),
            listOf("الخيار ب", "الخيار 2", "خيار 2"),
            listOf("الخيار ج", "الخيار 3", "خيار 3"),
            listOf("الخيار د", "الخيار 4", "خيار 4"),
        )

    fun calculateRowScore(headerRow: List<String>): Int {
        if (headerRow.isEmpty()) return -100

        var score = 0
        val mapped = mapHeaders(headerRow)

        for ((field, idx) in mapped) {
            val weight = fieldWeights[field] ?: 0
            val cellValue = headerRow.getOrNull(idx) ?: ""

            // Penalty for long text in header cells (headers are usually < 6 words)
            if (cellValue.split(Regex("\\s+")).size > 6) {
                score -= 100
            } else {
                score += weight
            }
        }

        val options = guessOptionColumns(headerRow, mapped)
        score += (options.size * 20).coerceAtMost(120)

        // Penalty for empty rows
        if (headerRow.all { it.isBlank() }) score -= 200

        // Penalty for mostly numeric rows (likely data, not headers)
        val numericCount = headerRow.count { it.isNotBlank() && it.all { c -> (c.isDigit() || c == '.') } }
        if (numericCount > headerRow.size / 2) score -= 40

        return score
    }

    fun mapHeaders(headerRow: List<String>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val usedCols = mutableSetOf<Int>()

        // Sort fields by weight so we map Question/Answer first
        val sortedFields = aliases.keys.sortedByDescending { fieldWeights[it] ?: 0 }

        for (field in sortedFields) {
            val aliasList = aliases[field] ?: continue

            // 1. Try Exact match first
            var foundIdx =
                headerRow.indexOfFirst { cell ->
                    if (headerRow.indexOf(cell) in usedCols) return@indexOfFirst false
                    val normalizedCell = normalize(cell)
                    aliasList.any { alias -> normalizedCell == normalize(alias) }
                }

            // 2. Try Word match for longer aliases
            if (foundIdx == -1) {
                foundIdx =
                    headerRow.indexOfFirst { cell ->
                        if (headerRow.indexOf(cell) in usedCols) return@indexOfFirst false
                        val normalizedCell = normalize(cell)
                        aliasList.any { alias ->
                            val normAlias = normalize(alias)
                            if (normAlias.length < 3) return@any false
                            // Use unicode-aware word boundaries or manual checks
                            val pattern = "(^|[^\\p{L}\\p{N}])${Regex.escape(normAlias)}($|[^\\p{L}\\p{N}])"
                            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(normalizedCell)
                        }
                    }
            }

            // 3. Try fallback substring for very specific aliases
            if (foundIdx == -1) {
                foundIdx =
                    headerRow.indexOfFirst { cell ->
                        if (headerRow.indexOf(cell) in usedCols) return@indexOfFirst false
                        val normalizedCell = normalize(cell)
                        aliasList.any { alias ->
                            val normAlias = normalize(alias)
                            if (normAlias.length < 5) return@any false
                            normalizedCell.contains(normAlias)
                        }
                    }
            }

            if (foundIdx != -1) {
                result[field] = foundIdx
                usedCols.add(foundIdx)
            }
        }
        return result
    }

    fun guessOptionColumns(
        headerRow: List<String>,
        mapped: Map<String, Int>,
    ): List<Int> {
        val options = mutableListOf<Int>()
        val mappedCols = mapped.values.toSet()

        headerRow.forEachIndexed { index, cell ->
            if (index in mappedCols) return@forEachIndexed
            val norm = normalize(cell)
            if (norm.isBlank()) return@forEachIndexed

            // 1. Matches predefined aliases (A, B, C..., Arabic...)
            val isAliasMatch = optionAliases.any { list -> list.any { alias -> norm == normalize(alias) } }

            // 2. Matches "Option 1", "Opt 1", "1", etc.
            val isNumericOption =
                Regex("""^(option|opt|choice|answer option|mcq|ans|q|خيار|الخيار)\s*\d+$""", RegexOption.IGNORE_CASE).containsMatchIn(norm) ||
                    Regex("""^\d+$""").matches(norm)

            // 3. Matches "Option A", "Opt A", etc.
            val isAlphaOption =
                Regex(
                    """^(option|opt|choice|answer option|mcq|ans|q|خيار|الخيار)\s*[a-z]$""",
                    RegexOption.IGNORE_CASE,
                ).containsMatchIn(norm)

            if (isAliasMatch || isNumericOption || isAlphaOption) {
                options.add(index)
            }
        }

        if (options.isEmpty()) {
            val qIdx = mapped["question"] ?: -1
            val aIdx = mapped["answer"] ?: headerRow.size
            if (qIdx != -1) {
                val start = minOf(qIdx, aIdx)
                val end = maxOf(qIdx, aIdx)
                for (i in (start + 1) until end) {
                    if (i !in mappedCols) options.add(i)
                }
            }
        }

        return options.take(120) // Safety limit
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("""[\u064B-\u065F\u0670]"""), "") // Arabic diacritics
            .replace("ـ", "") // Arabic Tatweel
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .replace(Regex("""[_\-–—]+"""), " ")
            .replace("\\", " ") // Handle backslashes as spaces for matching
            .trim()
    }
}
