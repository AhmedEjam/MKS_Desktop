package com.ahmedyejam.mks.data.importer.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable
data class LibraryBundleDto(
    val schema: Int = 6,
    val kind: String = "library-bundle",
    val exportedAt: Long? = null,
    val books: List<BookDto> = emptyList(),
    val quizzes: List<QuizDto> = emptyList(),
    val flashcardDecks: List<FlashcardDeckDto> = emptyList(),
    val slideshowCourses: List<SlideshowCourseDto> = emptyList(),
    val noteBlueprints: List<NoteBlueprintDto> = emptyList(),
    val promptDecks: List<PromptDeckDto> = emptyList(),
    val studySessions: List<KnowledgeStudySessionDto> = emptyList(),
    val progress: Map<String, JsonElement>? = null,
    val sessions: List<SessionDto>? = null,
    val categories: List<CategoryMetadataDto> = emptyList(),
)

@Serializable
data class FlashcardDeckDto(
    val id: String,
    val bookId: String,
    val title: String,
    val description: String? = null,
    val iconName: String? = null,
    val coverImage: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val cards: List<FlashcardDto> = emptyList(),
)

@Serializable
data class FlashcardDto(
    val id: String,
    val frontText: String,
    val backText: String,
    val hint: String? = null,
    val imagePath: String? = null,
    val tags: List<String> = emptyList(),
    val orderIndex: Int = 0,
    val sourceQuestionId: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

@Serializable
data class SlideshowCourseDto(
    val id: String,
    val bookId: String,
    val title: String,
    val description: String? = null,
    val coverImage: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val slides: List<CourseSlideDto> = emptyList(),
)

@Serializable
data class CourseSlideDto(
    val id: String,
    val title: String,
    val body: String,
    val speakerNotes: String? = null,
    val imagePath: String? = null,
    val orderIndex: Int = 0,
    val isCompleted: Boolean = false,
    val sourceQuestionId: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

@Serializable
data class NoteBlueprintDto(
    val id: String,
    val bookId: String,
    val title: String,
    val summary: String? = null,
    val body: String,
    val bulletPoints: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val mode: String = "SIMPLE_NOTE",
    val reviewStatus: String = "NEW",
    val sourceQuestionId: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

@Serializable
data class PromptDeckDto(
    val id: String,
    val bookId: String,
    val title: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val cards: List<PromptCardDto> = emptyList(),
)

@Serializable
data class PromptCardDto(
    val id: String,
    val title: String,
    val promptText: String,
    val variablesJson: String? = null,
    val outputType: String = "OTHER",
    val sortOrder: Int = 0,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)

@Serializable
data class KnowledgeStudySessionDto(
    val id: String? = null,
    val bookId: String,
    val contentId: String,
    val type: String,
    val progress: Float = 0f,
    val isCompleted: Boolean = false,
    val lastAccessedAt: Long? = null,
)

@Serializable
data class CategoryMetadataDto(
    val name: String,
    val emoji: String? = null,
    val color: Int? = null,
    val isPinned: Boolean = false,
)

@Serializable
data class BookDto(
    val id: String = "default_book",
    val workspaceExternalId: String? = null,
    val title: String = "Untitled Book",
    val note: String = "",
    val coverIcon: String = "📚",
    val coverImage: String = "",
    val createdAt: Long? = null,
    val contentUpdatedAt: Long? = null,
    val updatedAt: Long? = null,
    val lastStudiedAt: Long? = null,
)

@Serializable
data class QuizDto(
    val id: String = "default_quiz",
    val storageKey: String? = null,
    val bookId: String = "default_book",
    val title: String = "Untitled Quiz",
    val note: String = "",
    val coverIcon: String = "🎯",
    val coverImage: String = "",
    val createdAt: Long? = null,
    val contentUpdatedAt: Long? = null,
    val updatedAt: Long? = null,
    val lastStudiedAt: Long? = null,
    val questions: List<QuestionDto> = emptyList(),
)

@Serializable
data class QuestionDto(
    val id: String = "default_question",
    val stem: String = "",
    val options: List<OptionDto> = emptyList(),
    val correct: List<String> = emptyList(),
    val explanation: String = "",
    val hint: String = "",
    val reference: String = "",
    val imageDataUrl: String = "",
    val imageSource: String = "",
    val imageName: String = "",
    val categories: List<String> = emptyList(),
    val answerMode: String? = null,
    val sourceQuizId: String = "",
    val sourceQuestionId: String = "",
    val sourceBookId: String = "",
    val droppedAt: Long = 0,
    val additionalInfo: String = "",
    /** Best-effort origin line/row number used for import preview and skip reports. */
    val sourceLine: Int? = null,
)

@Serializable
data class OptionDto(
    val id: String,
    val text: String,
)

@Serializable
data class SessionDto(
    val id: String,
    val quizId: String,
    val label: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val finishedAt: Long? = null,
    val setup: JsonElement? = null,
    val initialItemCount: Int? = null,
    val items: List<SessionItemDto>? = null,
    val cursor: Int? = null,
    val resultsByOccurrence: Map<String, JsonElement>? = null,
    val retryQueue: List<JsonElement>? = null,
    val revealedCountsByOccurrence: Map<String, Int>? = null,
    val navigationFilter: String? = null,
    val showHint: Boolean? = null,
    val showReference: Boolean? = null,
    val showNavigation: Boolean? = null,
    val showOptionDropButtons: Boolean? = null,
    val droppedOptionIdsByOccurrence: Map<String, List<String>>? = null,
    val timer: JsonElement? = null,
    // v5+ fields
    val score: Int = 0,
    val incorrectCount: Int = 0,
    val answers: Map<String, List<Int>> = emptyMap(),
    val lastModifiedAt: Long? = null,
    val lastStudiedAt: Long? = null,
    val lastEditedAt: Long? = null,
    val shuffleQuestions: Boolean? = null,
    val shuffleOptions: Boolean? = null,
    val rapidMode: Boolean? = null,
    val repeatWrong: Boolean? = null,
    val quizTimerSeconds: Int? = null,
    val questionTimerSeconds: Int? = null,
    val rangeFrom: Int? = null,
    val rangeTo: Int? = null,
    val includeFilters: List<String>? = null,
    val droppedOptions: Map<String, List<Int>>? = null,
    val visibleOptionsCount: Map<String, Int>? = null,
)

@Serializable
data class SessionItemDto(
    val occurrenceId: String,
    val question: QuestionDto? = null,
    val isRetry: Boolean = false,
    val sourceQuestionId: String = "",
    val sourceQuizId: String = "",
    val sourceBookId: String = "",
)

@Serializable
data class ManifestDto(
    val version: Int = 1,
    @Serializable(with = AssetsSerializer::class)
    val assets: Map<String, String> = emptyMap(), // path -> assetId or mapping
)

object AssetsSerializer : KSerializer<Map<String, String>> {
    private val delegate = MapSerializer(String.serializer(), String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(
        encoder: Encoder,
        value: Map<String, String>,
    ) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): Map<String, String> {
        val input = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        val element = input.decodeJsonElement()

        return when (element) {
            is JsonObject -> {
                input.json.decodeFromJsonElement(delegate, element)
            }
            is JsonArray -> {
                // If it's an array, try to extract path/id or just treat it as empty
                val result = mutableMapOf<String, String>()
                element.forEach { item ->
                    if (item is JsonObject) {
                        val path = item["path"]?.jsonPrimitive?.content
                        val id = item["id"]?.jsonPrimitive?.content ?: path
                        if (path != null) {
                            result[path] = id ?: ""
                        }
                    } else if (item is JsonPrimitive && item.isString) {
                        result[item.content] = item.content
                    }
                }
                result
            }
            else -> emptyMap()
        }
    }
}
