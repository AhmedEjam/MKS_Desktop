package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.local.entity.CourseSlideEntity
import java.util.UUID

class TextSlideParser {
    fun parse(
        text: String,
        courseId: Long,
        startIndex: Int = 0,
        mode: TextParseMode = TextParseMode.ALTERNATING_PARAGRAPHS,
    ): List<CourseSlideEntity> {
        val result = mutableListOf<CourseSlideEntity>()

        val paragraphs = text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        var order = startIndex

        val now = System.currentTimeMillis()

        if (mode == TextParseMode.ALTERNATING_PARAGRAPHS) {
            var i = 0
            while (i < paragraphs.size) {
                val titleParagraph = paragraphs[i]
                var bodyParagraph = ""
                if (i + 1 < paragraphs.size) {
                    bodyParagraph = paragraphs[i + 1]
                    i += 2
                } else {
                    break
                }

                val titleClean = titleParagraph.replace(Regex("^(Title|Front|Q|Question):\\s*", RegexOption.IGNORE_CASE), "").trim()
                val bodyClean = bodyParagraph.replace(Regex("^(Body|Back|A|Answer):\\s*", RegexOption.IGNORE_CASE), "").trim()

                result.add(
                    CourseSlideEntity(
                        externalId = UUID.randomUUID().toString(),
                        courseId = courseId,
                        title = titleClean,
                        body = bodyClean,
                        orderIndex = order++,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
        } else if (mode == TextParseMode.EXPLICIT_LABELS) {
            // Explicit Labels Mode
            for (paragraph in paragraphs) {
                val titleMatch =
                    Regex(
                        "^(?:Title|Front|Q|Question):\\s*(.*?)(?:\\n(?:Body|Back|A|Answer):|$)",
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                    ).find(paragraph)
                val bodyMatch =
                    Regex(
                        "\\n(?:Body|Back|A|Answer):\\s*(.*)$",
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                    ).find(paragraph)

                if (titleMatch != null && bodyMatch != null) {
                    result.add(
                        CourseSlideEntity(
                            externalId = UUID.randomUUID().toString(),
                            courseId = courseId,
                            title = titleMatch.groupValues.getOrNull(1)?.trim() ?: "",
                            body = bodyMatch.groupValues.getOrNull(1)?.trim() ?: "",
                            orderIndex = order++,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                } else {
                    // Fallback
                    result.add(
                        CourseSlideEntity(
                            externalId = UUID.randomUUID().toString(),
                            courseId = courseId,
                            title = paragraph.trim(),
                            body = "",
                            orderIndex = order++,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                }
            }
        } else if (mode == TextParseMode.HEADER_BODY_NOTES) {
            val slideBlocks = text.split(Regex("(?m)^---$|^#\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
            for (block in slideBlocks) {
                val lines = block.lines()
                var title = ""
                var body = ""
                var notes: String? = null

                val headerMatch =
                    Regex(
                        "^(?:Header|Title|Front|Q|Question):\\s*(.*?)(?:\\n(?:Body|Back|A|Answer|Notes):|$)",
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                    ).find(block)
                val bodyMatch =
                    Regex(
                        "\\n(?:Body|Back|A|Answer):\\s*(.*?)(?:\\nNotes:|$)",
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                    ).find(block)
                val notesMatch = Regex("\\nNotes:\\s*(.*)$", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(block)

                if (headerMatch != null || bodyMatch != null || notesMatch != null) {
                    title = headerMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
                    body = bodyMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
                    notes = notesMatch?.groupValues?.getOrNull(1)?.trim()
                } else {
                    title = lines.firstOrNull()?.removePrefix("#")?.trim() ?: ""
                    val remainingLines = lines.drop(1)
                    val bodyLines = mutableListOf<String>()
                    val notesLines = mutableListOf<String>()
                    for (line in remainingLines) {
                        if (line.trim().startsWith(">")) {
                            notesLines.add(line.trim().removePrefix(">").trim())
                        } else {
                            bodyLines.add(line)
                        }
                    }
                    body = bodyLines.joinToString("\n").trim()
                    notes = if (notesLines.isNotEmpty()) notesLines.joinToString("\n").trim() else null
                }

                result.add(
                    CourseSlideEntity(
                        externalId = UUID.randomUUID().toString(),
                        courseId = courseId,
                        title = title,
                        body = body,
                        speakerNotes = notes,
                        orderIndex = order++,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
        }
        return result
    }
}
