package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.model.ParsedOption
import com.ahmedyejam.mks.data.importer.model.ParsedQuestion
import com.ahmedyejam.mks.data.importer.model.ResolvedImage
import com.ahmedyejam.mks.data.local.entity.QuestionType

class SpreadsheetQuestionParser(
    private val mapping: Map<String, Int>,
    private val optionCols: List<Int>,
    private val sheetAddressImages: Map<String, String>,
    private val sheetRowImages: Map<Int, String>,
    private val imageExtractor: GenericImageExtractor,
) {
    companion object {
        private val CATEGORY_DELIMITER = Regex("""[,،;؛/|]+""")
        private val MARKED_REGEX = Regex("""[*✓✔☑]""")
        private val DISPIMG_REGEX =
            Regex(
                """(?:_xlfn\.)?DISPIMG\([^)]*\)""",
                RegexOption.IGNORE_CASE,
            )
        private val WHITESPACE_REGEX = Regex("""\s+""")
        private val NORMALIZED_KEY_REGEX = Regex("[^A-Z0-9,;|\\s]")
        private val PART_DELIMITER_REGEX = Regex("[,;|\\s]+")
    }

    fun parseRow(
        row: List<String>,
        rowNumberOneBased: Int,
    ): ParsedQuestion? {
        val stem = cleanCellText(get(row, "question"))
        val externalId = cleanCellText(get(row, "id")).ifBlank { null }
        val answerRaw = cleanCellText(get(row, "answer"))

        val options =
            optionCols.mapIndexedNotNull { index, colIdx ->
                val cell = cleanCellText(row.getOrNull(colIdx))
                if (cell.isBlank()) {
                    null
                } else {
                    ParsedOption(
                        id = generateOptionId(index),
                        text = cell,
                        marked = isMarked(cell),
                    )
                }
            }

        val resolvedCorrectAnswers = resolveCorrectAnswers(answerRaw, options)
        val questionColIdx = mapping["question"] ?: -1
        val imageColIdx = mapping["image"] ?: -1
        val resolvedImage = resolveImage(row, rowNumberOneBased, questionColIdx, imageColIdx, optionCols)

        val categories =
            cleanCellText(get(row, "categories"))
                .split(CATEGORY_DELIMITER)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .ifEmpty { listOf("none") }

        val type = if (resolvedCorrectAnswers.size > 1) QuestionType.MULTIPLE_CHOICE else QuestionType.SINGLE_CHOICE

        if (stem.isBlank() && options.isEmpty() && resolvedImage.imageDataUrl.isNullOrBlank() && resolvedImage.imageSource.isNullOrBlank()) {
            return null
        }

        val issues = mutableListOf<String>()
        if (options.isEmpty()) issues.add("No options found")
        if (resolvedCorrectAnswers.isEmpty() && options.isNotEmpty()) issues.add("No correct answer identified")

        return ParsedQuestion(
            stem = stem,
            externalId = externalId,
            options = options,
            correctAnswers = resolvedCorrectAnswers,
            explanation = cleanCellText(get(row, "explanation")).ifBlank { null },
            hint = cleanCellText(get(row, "hint")).ifBlank { null },
            reference = cleanCellText(get(row, "reference")).ifBlank { null },
            additionalInfo = cleanCellText(get(row, "additional")).ifBlank { null },
            categories = categories,
            imageDataUrl = resolvedImage.imageDataUrl,
            imageSource = resolvedImage.imageSource,
            derivedFlags = listOfNotNull(resolvedImage.via?.let { "Image resolved via $it" }),
            sourceLine = rowNumberOneBased,
            type = type,
            issues = issues,
            answerMode = if (type == QuestionType.MULTIPLE_CHOICE) "multiple" else "single",
            isIncluded = true,
        )
    }

    private fun generateOptionId(index: Int): String {
        return if (index < 26) {
            "opt_${('A'.code + index).toChar()}"
        } else {
            "opt_${index + 1}"
        }
    }

    private fun resolveImage(
        row: List<String>,
        rowNumber: Int,
        qIdx: Int,
        imgIdx: Int,
        optIdxs: List<Int>,
    ): ResolvedImage {
        if (imgIdx != -1) {
            resolveFromCell(row.getOrNull(imgIdx), cellRef(imgIdx, rowNumber), rowNumber)?.let { return it }
        }
        if (qIdx != -1) {
            resolveFromCell(row.getOrNull(qIdx), cellRef(qIdx, rowNumber), rowNumber)?.let { return it }
        }
        for (idx in optIdxs) {
            resolveFromCell(row.getOrNull(idx), cellRef(idx, rowNumber), rowNumber)?.let { return it }
        }
        val rowFallback = sheetRowImages[rowNumber]
        return if (!rowFallback.isNullOrBlank()) {
            ResolvedImage(imageDataUrl = rowFallback, via = "row-fallback")
        } else {
            ResolvedImage()
        }
    }

    private fun resolveFromCell(
        value: String?,
        ref: String,
        rowNum: Int,
    ): ResolvedImage? {
        imageExtractor.extractFromText(value)?.let { return it }
        sheetAddressImages[ref]?.takeIf { it.isNotBlank() }?.let {
            return ResolvedImage(imageDataUrl = it, via = "exact-cell")
        }
        return sheetRowImages[rowNum]?.takeIf { it.isNotBlank() }?.let {
            ResolvedImage(imageDataUrl = it, via = "row-fallback")
        }
    }

    private fun get(
        row: List<String>,
        field: String,
    ): String? {
        val idx = mapping[field] ?: return null
        return row.getOrNull(idx)
    }

    private fun isMarked(text: String): Boolean = text.contains(MARKED_REGEX)

    private fun cleanCellText(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw
            .replace(DISPIMG_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private fun cellRef(
        colIdx: Int,
        rowNum: Int,
    ): String {
        var n = colIdx + 1
        var letters = ""
        while (n > 0) {
            val rem = (n - 1) % 26
            letters = (65 + rem).toChar() + letters
            n = (n - 1) / 26
        }
        return letters + rowNum
    }

    private fun resolveCorrectAnswers(
        answerRaw: String,
        options: List<ParsedOption>,
    ): List<String> {
        val indices = linkedSetOf<String>()
        val normalizedKey = answerRaw.uppercase().replace(NORMALIZED_KEY_REGEX, " ")
        val parts = normalizedKey.split(PART_DELIMITER_REGEX).filter { it.isNotEmpty() }

        parts.forEach { part ->
            // Check for A-Z
            if (part.length == 1 && part[0] in 'A'..'Z') {
                val index = part[0] - 'A'
                if (index in options.indices) indices.add(options[index].id)
            }
            // Check for multi-char string of letters like "ABC"
            else if (part.all { it in 'A'..'Z' } && part.length > 1 && part.length <= options.size) {
                part.forEach { char ->
                    val index = char - 'A'
                    if (index in options.indices) indices.add(options[index].id)
                }
            }
            // Check for numeric 1-26
            else {
                val numericIndex = part.toIntOrNull()?.minus(1)
                if (numericIndex != null && numericIndex in options.indices) {
                    indices.add(options[numericIndex].id)
                } else {
                    // Direct text match
                    val directIndex =
                        options.indexOfFirst { option ->
                            option.text.trim().equals(part.trim(), ignoreCase = true)
                        }
                    if (directIndex != -1) indices.add(options[directIndex].id)
                }
            }
        }

        // Direct text match fallback
        if (indices.isEmpty() && answerRaw.isNotBlank()) {
            val directIndex =
                options.indexOfFirst { option ->
                    option.text.trim().equals(answerRaw.trim(), ignoreCase = true)
                }
            if (directIndex != -1) indices.add(options[directIndex].id)
        }

        // Only include marked options if no explicit answers were found
        if (indices.isEmpty()) {
            options.forEach { option ->
                if (option.marked) indices.add(option.id)
            }
        }

        return indices.toList().sorted()
    }
}
