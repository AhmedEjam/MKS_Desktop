package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.model.ParsedOption
import com.ahmedyejam.mks.data.importer.model.ParsedQuestion
import com.ahmedyejam.mks.data.local.entity.QuestionType

class TextQuestionParser(
    private val imageExtractor: GenericImageExtractor,
) {
    fun parse(
        text: String,
        answersText: String? = null,
    ): List<ParsedQuestion> {
        val lines = text.lines()
        val answerMap = parseAnswerBlock(answersText)

        val result = mutableListOf<ParsedQuestion>()
        var current: RawQuestion? = null
        var qNum = 0

        lines.forEachIndexed { index, line ->
            val s = line.trim()
            if (s.isEmpty()) return@forEachIndexed

            if (isQuestionStart(s)) {
                current?.let { result.add(finalize(it, answerMap)) }
                qNum++
                current =
                    RawQuestion(
                        stem = stripQuestionMarker(s),
                        sourceLine = index + 1,
                        qNum = qNum,
                    )
            } else {
                current?.let { q ->
                    val opt = parseOption(s)
                    if (opt != null) {
                        q.options.add(opt)
                    } else {
                        val field = parseField(s)
                        if (field != null) {
                            q.fields[field.first] = field.second
                        } else if (q.options.isEmpty()) {
                            q.stem += "\n" + s
                        } else {
                            q.options.last().let { last ->
                                // Append to last option text
                                // This is a bit simplified
                            }
                        }
                    }
                }
            }
        }
        current?.let { result.add(finalize(it, answerMap)) }
        return result
    }

    private fun isQuestionStart(line: String): Boolean {
        return Regex("""^(?:q(?:uestion)?\s*)?\d+[\)\.\-:\s]+""", RegexOption.IGNORE_CASE).containsMatchIn(line) ||
            Regex("""^س(?:ؤال)?\s*\d+[\)\.\-:\s]*""").containsMatchIn(line)
    }

    private fun stripQuestionMarker(line: String): String {
        return line.replace(Regex("""^(?:q(?:uestion)?\s*)?\d+[\)\.\-:\s]+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^س(?:ؤال)?\s*\d+[\)\.\-:\s]*"""), "")
            .trim()
    }

    private fun parseOption(line: String): ParsedOption? {
        val match = Regex("""^[*✓✔☑\-\s]*([A-Z])[\)\.\-:\s]+(.+)$""", RegexOption.IGNORE_CASE).find(line)
        if (match != null) {
            return ParsedOption(
                id = "opt_${match.groupValues[1].uppercase()}",
                text = match.groupValues[2].trim(),
                marked = line.contains(Regex("""[*✓✔☑]""")),
            )
        }
        return null
    }

    private fun parseField(line: String): Pair<String, String>? {
        val prefixes =
            mapOf(
                "answer" to listOf("answer:", "ans:", "correct:", "الإجابة:", "الجواب:"),
                "explanation" to listOf("explanation:", "rationale:", "exp:", "الشرح:", "تفسير:"),
                "hint" to listOf("hint:", "tip:", "تلميح:"),
                "reference" to listOf("reference:", "ref:", "مرجع:"),
                "categories" to listOf("categories:", "category:", "tags:", "تصنيف:"),
            )
        for ((field, list) in prefixes) {
            for (p in list) {
                if (line.trim().startsWith(p, ignoreCase = true)) {
                    return field to line.trim().substring(p.length).trim()
                }
            }
        }
        return null
    }

    private fun parseAnswerBlock(text: String?): Map<Int, String> {
        if (text == null) return emptyMap()
        val map = mutableMapOf<Int, String>()
        text.lines().forEach { line ->
            val match = Regex("""(\d+)\s*[:\-=\)\.]\s*(.+)""").find(line)
            if (match != null) {
                match.groupValues[1].toIntOrNull()?.let { map[it] = match.groupValues[2].trim() }
            }
        }
        return map
    }

    private fun finalize(
        raw: RawQuestion,
        answerMap: Map<Int, String>,
    ): ParsedQuestion {
        val answerRaw = raw.fields["answer"] ?: answerMap[raw.qNum] ?: ""

        val resolvedImage =
            imageExtractor.extractFromFields(raw.stem + "\n" + raw.fields.values.joinToString("\n"))
                ?: imageExtractor.extractFromText(raw.stem)

        val correctIds = resolveCorrect(answerRaw, raw.options)

        return ParsedQuestion(
            stem = raw.stem.trim(),
            options = raw.options,
            correctAnswers = correctIds,
            explanation = raw.fields["explanation"],
            hint = raw.fields["hint"],
            reference = raw.fields["reference"],
            categories = raw.fields["categories"]?.split(",")?.map { it.trim() } ?: emptyList(),
            imageDataUrl = resolvedImage?.imageDataUrl,
            imageSource = resolvedImage?.imageSource,
            sourceLine = raw.sourceLine,
            type = if (correctIds.size > 1) QuestionType.MULTIPLE_CHOICE else QuestionType.SINGLE_CHOICE,
        )
    }

    private fun resolveCorrect(
        answerRaw: String,
        options: List<ParsedOption>,
    ): List<String> {
        val result = mutableSetOf<String>()
        val trimmedAnswer = answerRaw.trim()

        if (options.isEmpty()) return emptyList()

        // 1. Try exact textual match against options (Case insensitive)
        options.forEach { opt ->
            if (opt.text.trim().equals(trimmedAnswer, ignoreCase = true)) {
                result.add(opt.id)
            }
        }

        // 2. If no exact match, try matching by letter if trimmedAnswer is just a letter
        if (result.isEmpty()) {
            val upperTrimmed = trimmedAnswer.uppercase()
            options.forEach { opt ->
                val letter = opt.id.removePrefix("opt_")
                if (upperTrimmed == letter) {
                    result.add(opt.id)
                }
            }
        }

        // 3. If still no match, parse by delimiter (comma, semicolon, space)
        if (result.isEmpty()) {
            val upperTrimmed = trimmedAnswer.uppercase()
            val parts =
                upperTrimmed
                    .split(Regex("[,;\\s]+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

            parts.forEach { part ->
                options.forEach { opt ->
                    val letter = opt.id.removePrefix("opt_")
                    if (part == letter || opt.text.trim().equals(part, ignoreCase = true)) {
                        result.add(opt.id)
                    }
                }
            }
        }

        // 4. Fallback to marked options (if no explicit answers found)
        if (result.isEmpty()) {
            options.forEach { opt ->
                if (opt.marked) result.add(opt.id)
            }
        }

        // 5. Global fallback (isolated letters only - to fix Issue 6)
        if (result.isEmpty() && answerRaw.isNotBlank()) {
            val upperAnswer = answerRaw.uppercase()
            options.forEach { opt ->
                val letter = opt.id.removePrefix("opt_")
                if (letter.length == 1) {
                    val regex = Regex("\\b$letter\\b")
                    if (upperAnswer.contains(regex)) {
                        result.add(opt.id)
                    }
                } else if (upperAnswer.contains(letter)) {
                    result.add(opt.id)
                }
            }
        }

        return result.toList().sorted()
    }

    private data class RawQuestion(
        var stem: String,
        val options: MutableList<ParsedOption> = mutableListOf(),
        val fields: MutableMap<String, String> = mutableMapOf(),
        val sourceLine: Int,
        val qNum: Int,
    )
}
