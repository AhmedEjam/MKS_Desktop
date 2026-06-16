package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.dto.LibraryBundleDto
import com.ahmedyejam.mks.data.importer.security.ImportLimits
import com.ahmedyejam.mks.util.readTextWithLimit
import kotlinx.serialization.json.*
import java.io.InputStream

open class JsonLibraryParser {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            encodeDefaults = true
        }

    private object LibraryTransformingSerializer : JsonTransformingSerializer<LibraryBundleDto>(LibraryBundleDto.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement {
            val root =
                when (element) {
                    is JsonArray -> {
                        // If it's an array, assume it's a list of quizzes or questions
                        val first = element.firstOrNull()
                        if (first is JsonObject && ("questions" in first || "items" in first || "stem" in first || "question" in first)) {
                            // Probably a list of quizzes or questions
                            if (first.containsKey("stem") || first.containsKey("question")) {
                                // List of questions, wrap in a quiz then in a bundle
                                buildJsonObject {
                                    put(
                                        "quizzes",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("title", "Imported Questions")
                                                    put("questions", element)
                                                },
                                            )
                                        },
                                    )
                                }
                            } else {
                                // List of quizzes, wrap in a bundle
                                buildJsonObject {
                                    put("quizzes", element)
                                }
                            }
                        } else {
                            // Unknown array, just wrap it as quizzes and hope for the best
                            buildJsonObject {
                                put("quizzes", element)
                            }
                        }
                    }
                    is JsonObject -> element
                    else -> return element
                }.toMutableMap()

            // Normalize top-level quizzes/quizes
            if ("quizes" in root && "quizzes" !in root) {
                root["quizes"]?.let { root["quizzes"] = it }
            }
            if ("bookList" in root && "books" !in root) {
                root["bookList"]?.let { root["books"] = it }
            }

            // Normalize books
            val books = root["books"]
            if (books is JsonArray) {
                root["books"] =
                    JsonArray(
                        books.map { book ->
                            if (book !is JsonObject) return@map book
                            val bMap = book.toMutableMap()
                            bMap["name"]?.let { if ("title" !in bMap) bMap["title"] = it }
                            JsonObject(bMap)
                        },
                    )
            }

            // If we have quizzes but no books, create a default book
            if ("quizzes" in root && ("books" !in root || (root["books"] as? JsonArray)?.isEmpty() == true)) {
                val bookId = java.util.UUID.randomUUID().toString()
                root["books"] =
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("id", bookId)
                                put("title", "Default Book")
                            },
                        )
                    }
                // Ensure quizzes refer to this book and have unique IDs
                val quizzes = root["quizzes"]
                if (quizzes is JsonArray) {
                    root["quizzes"] =
                        JsonArray(
                            quizzes.map { quiz ->
                                if (quiz !is JsonObject) return@map quiz
                                val qMap = quiz.toMutableMap()
                                if ("id" !in qMap) qMap["id"] = JsonPrimitive(java.util.UUID.randomUUID().toString())
                                if ("bookId" !in qMap) qMap["bookId"] = JsonPrimitive(bookId)
                                JsonObject(qMap)
                            },
                        )
                }
            }

            // Normalize quizzes content
            val quizzes = root["quizzes"]
            if (quizzes is JsonArray) {
                root["quizzes"] =
                    JsonArray(
                        quizzes.map { quiz ->
                            if (quiz !is JsonObject) return@map quiz
                            val qMap = quiz.toMutableMap()

                            qMap["name"]?.let { if ("title" !in qMap) qMap["title"] = it }

                            // quizzes items -> questions
                            if ("items" in qMap && "questions" !in qMap) {
                                qMap["items"]?.let { qMap["questions"] = it }
                            }
                            if ("questionList" in qMap && "questions" !in qMap) {
                                qMap["questionList"]?.let { qMap["questions"] = it }
                            }

                            val questions = qMap["questions"]
                            if (questions is JsonArray) {
                                qMap["questions"] =
                                    JsonArray(
                                        questions.map { question ->
                                            if (question !is JsonObject) return@map question
                                            val questMap = question.toMutableMap()

                                            // stem aliases
                                            if ("stem" !in questMap) {
                                                questMap["question"]?.let { questMap["stem"] = it }
                                                    ?: questMap["text"]?.let { questMap["stem"] = it }
                                            }

                                            // options aliases: can be List<String> or List<JsonObject>
                                            var options = questMap["options"] ?: questMap["answers"] ?: questMap["choices"] ?: questMap["optionList"]
                                            if (options is JsonArray) {
                                                val firstOpt = options.firstOrNull()
                                                if (firstOpt is JsonPrimitive && firstOpt.isString) {
                                                    // List of strings, convert to OptionDto list
                                                    options =
                                                        JsonArray(
                                                            options.mapIndexed { idx, opt ->
                                                                buildJsonObject {
                                                                    put("id", "opt_$idx")
                                                                    put("text", opt)
                                                                }
                                                            },
                                                        )
                                                } else if (firstOpt is JsonObject) {
                                                    // Handle cases where options are objects but might use "value" or "content" instead of "text"
                                                    options =
                                                        JsonArray(
                                                            options.mapIndexed { idx, opt ->
                                                                val oMap = (opt as JsonObject).toMutableMap()
                                                                if ("id" !in oMap) oMap["id"] = JsonPrimitive("opt_$idx")
                                                                if ("text" !in oMap) {
                                                                    val textVal = oMap["text"] ?: oMap["value"] ?: oMap["content"] ?: oMap["answer"] ?: JsonPrimitive("")
                                                                    oMap["text"] = textVal
                                                                }
                                                                JsonObject(oMap)
                                                            },
                                                        )
                                                }
                                                questMap["options"] = options
                                            }

                                            // correct aliases: can be List<Int> (indices) or List<String> (IDs)
                                            var correct = questMap["correct"] ?: questMap["answer"] ?: questMap["correctAnswers"] ?: questMap["solution"] ?: questMap["correct_index"]
                                            if (correct is JsonPrimitive) {
                                                correct = JsonArray(listOf(correct))
                                            }

                                            if (correct is JsonArray && options is JsonArray) {
                                                questMap["correct"] =
                                                    JsonArray(
                                                        correct.map { c ->
                                                            if (c is JsonPrimitive && !c.isString) {
                                                                // Index-based, convert to ID
                                                                val idx = c.content.toIntOrNull() ?: -1
                                                                if (idx != -1) JsonPrimitive("opt_$idx") else c
                                                            } else if (c is JsonPrimitive && c.isString) {
                                                                // If it's a string, it might be the actual text of the answer or an ID
                                                                // If it matches an opt_X pattern or an ID in options, keep it.
                                                                // Otherwise, try to find the option with matching text.
                                                                val cStr = c.content
                                                                val matchingOpt =
                                                                    options.filterIsInstance<JsonObject>().find {
                                                                        it["id"]?.jsonPrimitive?.content == cStr || it["text"]?.jsonPrimitive?.content == cStr
                                                                    }
                                                                matchingOpt?.get("id") ?: c
                                                            } else {
                                                                c
                                                            }
                                                        },
                                                    )
                                            }

                                            // explanation, hint, reference aliases
                                            if ("explanation" !in questMap) {
                                                questMap["explanation"] = questMap["rational"] ?: questMap["feedback"] ?: questMap["comment"] ?: JsonPrimitive("")
                                            }
                                            if ("imageDataUrl" !in questMap) {
                                                questMap["imageDataUrl"] = questMap["image"] ?: questMap["imageUrl"] ?: questMap["photo"] ?: questMap["img"] ?: JsonPrimitive("")
                                            }

                                            JsonObject(questMap)
                                        },
                                    )
                            }
                            JsonObject(qMap)
                        },
                    )
            }

            return JsonObject(root)
        }
    }

    open fun parse(jsonString: String): LibraryBundleDto {
        return json.decodeFromString(LibraryTransformingSerializer, jsonString)
    }

    fun parse(inputStream: InputStream): LibraryBundleDto {
        val jsonString = inputStream.readTextWithLimit(ImportLimits.MAX_TEXT_IMPORT_BYTES)
        return parse(jsonString)
    }
}
