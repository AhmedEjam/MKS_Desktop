package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.model.ParsedOption
import com.ahmedyejam.mks.data.importer.model.ParsedQuestion
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.squareup.moshi.Moshi

class JsonQuestionParser(
    private val imageExtractor: GenericImageExtractor,
) {
    private val moshi = Moshi.Builder().build()

    fun parse(text: String): List<ParsedQuestion> {
        val trimmed = text.trim()
        val adapter = moshi.adapter(Any::class.java)
        val parsed = adapter.fromJson(trimmed) ?: return emptyList()

        return when (parsed) {
            is List<*> -> parseList(parsed)
            is Map<*, *> ->
                @Suppress("UNCHECKED_CAST")
                parseMap(parsed as Map<String, Any>)
            else -> emptyList()
        }
    }

    private fun parseList(list: List<*>): List<ParsedQuestion> {
        val result = mutableListOf<ParsedQuestion>()
        list.forEachIndexed { index, item ->
            if (item is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                result.add(parseSingle(item as Map<String, Any>, index + 1))
            }
        }
        return result
    }

    private fun parseMap(map: Map<String, Any>): List<ParsedQuestion> {
        val questions = map["questions"] ?: map["quizData"]
        if (questions is List<*>) {
            return parseList(questions)
        }
        // Fallback for single question object
        if (map.containsKey("question") || map.containsKey("stem") || map.containsKey("q")) {
            return listOf(parseSingle(map, 1))
        }
        return emptyList()
    }

    private fun parseSingle(
        map: Map<String, Any>,
        line: Int,
    ): ParsedQuestion {
        val stem = (map["stem"] ?: map["question"] ?: map["q"] ?: map["text"] ?: "").toString()
        val optionsRaw = map["options"]

        val options = mutableListOf<ParsedOption>()
        if (optionsRaw is List<*>) {
            optionsRaw.forEachIndexed { i, opt ->
                val optText = if (opt is Map<*, *>) opt["text"] ?: opt["label"] ?: opt["t"] else opt
                val isCorrect = if (opt is Map<*, *>) opt["correct"] == true else false
                options.add(
                    ParsedOption(
                        id = "opt_${(65 + i).toChar()}",
                        text = optText.toString().trim(),
                        marked = isCorrect,
                    ),
                )
            }
        } else {
            // Fallback to A, B, C... fields
            for (i in 0 until 8) {
                val letter = (65 + i).toChar().toString()
                val valLower = map[letter.lowercase()]
                val valUpper = map[letter]
                val text = (valLower ?: valUpper)?.toString() ?: ""
                if (text.isNotBlank()) {
                    options.add(ParsedOption(id = "opt_$letter", text = text.trim()))
                }
            }
        }

        val answerRaw = (map["answer"] ?: map["correctAnswer"] ?: map["correct"] ?: "").toString()
        val correctIds = resolveCorrect(answerRaw, options)

        val imageSource = (map["imageDataUrl"] ?: map["image"] ?: map["imageUrl"] ?: map["photo"] ?: map["img"] ?: "").toString()
        val resolvedImage = if (imageSource.isNotBlank()) imageExtractor.extractFromText(imageSource) else null

        val stemImage = imageExtractor.extractFromText(stem)

        return ParsedQuestion(
            stem = stem,
            options = options,
            correctAnswers = correctIds,
            explanation = (map["explanation"] ?: map["e"] ?: "").toString(),
            hint = (map["hint"] ?: map["hintText"] ?: "").toString(),
            reference = (map["reference"] ?: map["ref"] ?: "").toString(),
            additionalInfo = (map["additionalInfo"] ?: map["additional"] ?: map["info"] ?: "").toString(),
            categories = parseCategories(map["categories"] ?: map["category"]),
            imageDataUrl = (resolvedImage ?: stemImage)?.imageDataUrl,
            imageSource = (resolvedImage ?: stemImage)?.imageSource ?: if (!imageSource.startsWith("data:")) imageSource else "",
            sourceLine = line,
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

        // 1. Exact textual match against option text
        options.forEach { opt ->
            if (opt.text.equals(trimmedAnswer, ignoreCase = true)) {
                result.add(opt.id)
            }
        }

        // 2. Exact match against option letter (A, B, C...)
        if (result.isEmpty()) {
            val upperTrimmed = trimmedAnswer.uppercase()
            options.forEach { opt ->
                val letter = opt.id.removePrefix("opt_")
                if (upperTrimmed == letter) {
                    result.add(opt.id)
                }
            }
        }

        // 3. Multi-answer match (e.g. "A, B" or "A;B")
        if (result.isEmpty()) {
            val parts =
                trimmedAnswer.uppercase()
                    .split(Regex("[,;\\s]+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

            parts.forEach { part ->
                options.forEach { opt ->
                    val letter = opt.id.removePrefix("opt_")
                    if (part == letter || opt.text.equals(part, ignoreCase = true)) {
                        result.add(opt.id)
                    }
                }
            }
        }

        // 4. Fallback to marked options in JSON
        if (result.isEmpty()) {
            options.forEach { opt ->
                if (opt.marked) result.add(opt.id)
            }
        }

        // 5. Smart fallback for letters contained in answer text (Word boundaries)
        if (result.isEmpty() && answerRaw.isNotBlank()) {
            val upperAnswer = answerRaw.uppercase()
            options.forEach { opt ->
                val letter = opt.id.removePrefix("opt_")
                if (letter.length == 1) {
                    // Match ONLY if letter is isolated (fixes Issue 6)
                    val regex = Regex("\\b$letter\\b")
                    if (upperAnswer.contains(regex)) {
                        result.add(opt.id)
                    }
                } else {
                    if (upperAnswer.contains(letter)) {
                        result.add(opt.id)
                    }
                }
            }
        }

        return result.toList().sorted()
    }

    private fun parseCategories(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.map { it.toString() }
            is String -> value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }
}
