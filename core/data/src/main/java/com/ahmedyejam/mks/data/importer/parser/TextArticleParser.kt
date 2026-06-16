package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.local.entity.NoteBlueprintEntity
import java.util.UUID

enum class TextArticleParseMode {
    BASIC,
    EXPLICIT_LABELS,
}

class TextArticleParser {
    fun parse(
        text: String,
        collectionId: Long,
        mode: TextArticleParseMode = TextArticleParseMode.BASIC,
        defaultMode: String = "SIMPLE_NOTE",
    ): List<NoteBlueprintEntity> {
        val result = mutableListOf<NoteBlueprintEntity>()

        if (mode == TextArticleParseMode.BASIC) {
            // Split by double newline to treat blocks as separate articles
            val articlesText = text.split(Regex("(?m)^---$")).map { it.trim() }.filter { it.isNotEmpty() }

            for (articleText in articlesText) {
                val paragraphs = articleText.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }

                if (paragraphs.isEmpty()) continue

                val title = paragraphs[0]
                val summary = if (paragraphs.size > 1) paragraphs[1] else null
                val body = if (paragraphs.size > 2) paragraphs.drop(2).joinToString("\n\n") else ""

                result.add(
                    NoteBlueprintEntity(
                        externalId = UUID.randomUUID().toString(),
                        collectionId = collectionId,
                        title = title,
                        summary = summary,
                        body = body,
                        blueprintMode = defaultMode,
                    ),
                )
            }
        } else {
            // Explicit Labels Mode
            // Articles are separated by "---"
            val articlesText = text.split(Regex("(?m)^---$")).map { it.trim() }.filter { it.isNotEmpty() }

            for (articleText in articlesText) {
                val titleMatch =
                    Regex(
                        "^(?:Title):\\s*(.*?)(?:\\n(?:Summary|Body|Bullet|Tag):|$)",
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                    ).find(articleText)
                val summaryMatch =
                    Regex(
                        "\\n(?:Summary):\\s*(.*?)(?:\\n(?:Title|Body|Bullet|Tag):|$)",
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                    ).find(articleText)
                val bodyMatch =
                    Regex(
                        "\\n(?:Body):\\s*(.*?)(?:\\n(?:Title|Summary|Bullet|Tag):|$)",
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                    ).find(articleText)

                val bulletMatches = Regex("\\n(?:Bullet):\\s*(.*?)(?:\\n|$)", setOf(RegexOption.IGNORE_CASE)).findAll(articleText)
                val bullets = bulletMatches.map { it.groupValues[1].trim() }.toList()

                val tagMatches = Regex("\\n(?:Tag):\\s*(.*?)(?:\\n|$)", setOf(RegexOption.IGNORE_CASE)).findAll(articleText)
                val tags = tagMatches.map { it.groupValues[1].trim() }.toList()

                if (titleMatch != null) {
                    result.add(
                        NoteBlueprintEntity(
                            externalId = UUID.randomUUID().toString(),
                            collectionId = collectionId,
                            title = titleMatch.groupValues.getOrNull(1)?.trim() ?: "Untitled",
                            summary = summaryMatch?.groupValues?.getOrNull(1)?.trim(),
                            body = bodyMatch?.groupValues?.getOrNull(1)?.trim() ?: "",
                            bulletPoints = bullets,
                            tags = tags,
                            blueprintMode = defaultMode,
                        ),
                    )
                } else {
                    // Fallback
                    result.add(
                        NoteBlueprintEntity(
                            externalId = UUID.randomUUID().toString(),
                            collectionId = collectionId,
                            title = articleText.take(50).trim() + "...",
                            body = articleText.trim(),
                            blueprintMode = defaultMode,
                        ),
                    )
                }
            }
        }
        return result
    }
}
