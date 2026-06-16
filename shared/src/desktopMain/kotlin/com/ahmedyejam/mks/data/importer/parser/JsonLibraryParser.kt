package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.dto.LibraryBundleDto
import com.ahmedyejam.mks.data.importer.security.ImportLimits
import com.ahmedyejam.mks.util.readTextWithLimit
import kotlinx.serialization.json.*
import java.io.InputStream

class JsonLibraryParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true; encodeDefaults = true }

    private object LibraryTransformingSerializer : JsonTransformingSerializer<LibraryBundleDto>(LibraryBundleDto.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement {
            val root = when (element) {
                is JsonArray -> {
                    val first = element.firstOrNull()
                    if (first is JsonObject && ("questions" in first || "items" in first || "stem" in first || "question" in first)) {
                        if (first.containsKey("stem") || first.containsKey("question")) {
                            buildJsonObject { put("quizzes", buildJsonArray { add(buildJsonObject { put("title", "Imported Questions"); put("questions", element) }) }) }
                        } else {
                            buildJsonObject { put("quizzes", element) }
                        }
                    } else buildJsonObject { put("quizzes", element) }
                }
                is JsonObject -> element
                else -> return element
            }.toMutableMap()

            if ("quizes" in root && "quizzes" !in root) root["quizes"]?.let { root["quizzes"] = it }
            if ("bookList" in root && "books" !in root) root["bookList"]?.let { root["books"] = it }

            val books = root["books"]
            if (books is JsonArray) {
                root["books"] = JsonArray(books.map { book ->
                    if (book !is JsonObject) return@map book
                    val bMap = book.toMutableMap()
                    bMap["name"]?.let { if ("title" !in bMap) bMap["title"] = it }
                    JsonObject(bMap)
                })
            }

            if ("quizzes" in root && ("books" !in root || (root["books"] as? JsonArray)?.isEmpty() == true)) {
                val bookId = java.util.UUID.randomUUID().toString()
                root["books"] = buildJsonArray { add(buildJsonObject { put("id", bookId); put("title", "Default Book") }) }
                val quizzes = root["quizzes"]
                if (quizzes is JsonArray) {
                    root["quizzes"] = JsonArray(quizzes.map { quiz ->
                        if (quiz !is JsonObject) return@map quiz
                        val qMap = quiz.toMutableMap()
                        if ("id" !in qMap) qMap["id"] = JsonPrimitive(java.util.UUID.randomUUID().toString())
                        if ("bookId" !in qMap) qMap["bookId"] = JsonPrimitive(bookId)
                        JsonObject(qMap)
                    })
                }
            }

            return JsonObject(root)
        }
    }

    fun parse(jsonString: String): LibraryBundleDto =
        json.decodeFromString(LibraryTransformingSerializer, jsonString)

    fun parse(inputStream: InputStream): LibraryBundleDto {
        val jsonString = inputStream.readTextWithLimit(ImportLimits.MAX_TEXT_IMPORT_BYTES)
        return parse(jsonString)
    }
}
