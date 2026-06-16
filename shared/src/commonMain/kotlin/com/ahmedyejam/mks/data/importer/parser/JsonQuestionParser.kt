package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.model.ParsedOption
import com.ahmedyejam.mks.data.importer.model.ParsedQuestion
import com.ahmedyejam.mks.data.local.entity.QuestionType
import kotlinx.serialization.json.*

class JsonQuestionParser(
    private val imageExtractor: GenericImageExtractor,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(text: String): List<ParsedQuestion> {
        val trimmed = text.trim()
        val element = try { json.parseToJsonElement(trimmed) } catch (_: Exception) { return emptyList() }

        return when (element) {
            is JsonArray -> parseList(element)
            is JsonObject -> parseMap(element)
            else -> emptyList()
        }
    }

    private fun parseList(list: JsonArray): List<ParsedQuestion> {
        return list.mapIndexedNotNull { index, item ->
            if (item is JsonObject) parseSingle(item, index + 1) else null
        }
    }

    private fun parseMap(map: JsonObject): List<ParsedQuestion> {
        val questions = map["questions"] ?: map["quizData"]
        if (questions is JsonArray) return parseList(questions)
        if (map.containsKey("question") || map.containsKey("stem") || map.containsKey("q"))
            return listOf(parseSingle(map, 1))
        return emptyList()
    }

    private fun parseSingle(map: JsonObject, line: Int): ParsedQuestion {
        val stem = (map["stem"] ?: map["question"] ?: map["q"] ?: map["text"] ?: jsonPrimitive("")).jsonPrimitive.content
        val optionsRaw = map["options"]
        val options = mutableListOf<ParsedOption>()

        if (optionsRaw is JsonArray) {
            optionsRaw.forEachIndexed { i, opt ->
                val id = "opt_${(65 + i).toChar()}"
                when (opt) {
                    is JsonObject -> options.add(ParsedOption(id = id, text = (opt["text"] ?: opt["label"] ?: opt["t"] ?: jsonPrimitive("")).jsonPrimitive.content.trim(), marked = opt["correct"]?.jsonPrimitive?.boolean == true))
                    is JsonPrimitive -> options.add(ParsedOption(id = id, text = opt.content.trim()))
                    else -> { /* skip unknown types */ }
                }
            }
        } else {
            for (i in 0 until 8) {
                val letter = (65 + i).toChar().toString()
                val text = (map[letter.lowercase()] ?: map[letter])?.jsonPrimitiveOrNull?.content ?: ""
                if (text.isNotBlank()) options.add(ParsedOption(id = "opt_$letter", text = text.trim()))
            }
        }

        val answerRaw = (map["answer"] ?: map["correctAnswer"] ?: map["correct"] ?: jsonPrimitive("")).jsonPrimitiveOrNull?.content ?: ""
        val correctIds = resolveCorrect(answerRaw, options)

        val imageSource = (map["imageDataUrl"] ?: map["image"] ?: map["imageUrl"] ?: map["photo"] ?: map["img"] ?: jsonPrimitive("")).jsonPrimitiveOrNull?.content ?: ""
        val resolvedImage = if (imageSource.isNotBlank()) imageExtractor.extractFromText(imageSource) else null
        val stemImage = imageExtractor.extractFromText(stem)

        return ParsedQuestion(
            stem = stem,
            options = options,
            correctAnswers = correctIds,
            explanation = (map["explanation"] ?: map["e"] ?: jsonPrimitive("")).jsonPrimitiveOrNull?.content.orEmpty(),
            hint = (map["hint"] ?: map["hintText"] ?: jsonPrimitive("")).jsonPrimitiveOrNull?.content.orEmpty(),
            reference = (map["reference"] ?: map["ref"] ?: jsonPrimitive("")).jsonPrimitiveOrNull?.content.orEmpty(),
            additionalInfo = (map["additionalInfo"] ?: map["additional"] ?: map["info"] ?: jsonPrimitive("")).jsonPrimitiveOrNull?.content.orEmpty(),
            categories = parseCategories(map["categories"] ?: map["category"]),
            imageDataUrl = (resolvedImage ?: stemImage)?.imageDataUrl,
            imageSource = (resolvedImage ?: stemImage)?.imageSource ?: if (!imageSource.startsWith("data:")) imageSource else "",
            sourceLine = line,
            type = if (correctIds.size > 1) QuestionType.MULTIPLE_CHOICE else QuestionType.SINGLE_CHOICE,
        )
    }

    private fun resolveCorrect(answerRaw: String, options: List<ParsedOption>): List<String> {
        val result = mutableSetOf<String>()
        val trimmedAnswer = answerRaw.trim()
        if (options.isEmpty() || trimmedAnswer.isEmpty()) return emptyList()

        options.forEach { opt -> if (opt.text.equals(trimmedAnswer, ignoreCase = true)) result.add(opt.id) }
        if (result.isEmpty()) {
            val upper = trimmedAnswer.uppercase()
            options.forEach { opt -> if (upper == opt.id.removePrefix("opt_")) result.add(opt.id) }
        }
        if (result.isEmpty()) {
            trimmedAnswer.uppercase().split(Regex("[,;\\s]+")).filter { it.isNotEmpty() }.forEach { part ->
                options.forEach { opt -> if (part == opt.id.removePrefix("opt_") || opt.text.equals(part, ignoreCase = true)) result.add(opt.id) }
            }
        }
        if (result.isEmpty()) options.forEach { opt -> if (opt.marked) result.add(opt.id) }
        if (result.isEmpty() && answerRaw.isNotBlank()) {
            val upperAnswer = answerRaw.uppercase()
            options.forEach { opt ->
                val letter = opt.id.removePrefix("opt_")
                if (letter.length == 1 && Regex("\\b$letter\\b").containsMatchIn(upperAnswer)) result.add(opt.id)
                else if (upperAnswer.contains(letter)) result.add(opt.id)
            }
        }
        return result.toList().sorted()
    }

    private fun parseCategories(value: JsonElement?): List<String> = when (value) {
        is JsonArray -> value.map { it.jsonPrimitive.content }
        is JsonPrimitive -> value.content.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        else -> emptyList()
    }
}

private fun jsonPrimitive(value: String) = JsonPrimitive(value)
private val JsonElement.jsonPrimitiveOrNull: JsonPrimitive? get() = this as? JsonPrimitive
private val JsonElement.jsonPrimitive: JsonPrimitive get() = this as JsonPrimitive
