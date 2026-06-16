package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.local.entity.FlashcardEntity
import java.util.UUID

enum class TextParseMode {
    ALTERNATING_PARAGRAPHS,
    EXPLICIT_LABELS,
    HEADER_BODY_NOTES,
}

class TextFlashcardParser {
    fun parse(
        text: String,
        deckId: Long,
        startIndex: Int = 0,
        mode: TextParseMode = TextParseMode.ALTERNATING_PARAGRAPHS,
    ): List<FlashcardEntity> {
        val result = mutableListOf<FlashcardEntity>()

        val paragraphs = text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        var order = startIndex

        if (mode == TextParseMode.ALTERNATING_PARAGRAPHS) {
            var i = 0
            while (i < paragraphs.size) {
                val frontParagraph = paragraphs[i]
                var backParagraph = ""
                if (i + 1 < paragraphs.size) {
                    backParagraph = paragraphs[i + 1]
                    i += 2
                } else {
                    break
                }

                val frontClean = frontParagraph.replace(Regex("^(Front|Q|Question):\\s*", RegexOption.IGNORE_CASE), "").trim()
                val backClean = backParagraph.replace(Regex("^(Back|A|Answer):\\s*", RegexOption.IGNORE_CASE), "").trim()

                result.add(
                    FlashcardEntity(
                        externalId = UUID.randomUUID().toString(),
                        deckId = deckId,
                        frontText = frontClean,
                        backText = backClean,
                        orderIndex = order++,
                    ),
                )
            }
        } else {
            // Explicit Labels Mode
            val frontRegex = Regex("(?m)^\\s*(?:Front|Q|Question):\\s*", RegexOption.IGNORE_CASE)
            val frontMatches = frontRegex.findAll(text).toList()

            if (frontMatches.isEmpty()) {
                // Fallback: if no labels at all, just treat the whole text as one card
                val parts = text.split(Regex("(?m)^\\s*(?:Back|A|Answer):\\s*", RegexOption.IGNORE_CASE), limit = 2)
                if (parts.size == 2) {
                    result.add(
                        FlashcardEntity(
                            externalId = UUID.randomUUID().toString(),
                            deckId = deckId,
                            frontText = parts[0].trim(),
                            backText = parts[1].trim(),
                            orderIndex = order++,
                        ),
                    )
                } else {
                    result.add(
                        FlashcardEntity(
                            externalId = UUID.randomUUID().toString(),
                            deckId = deckId,
                            frontText = text.trim(),
                            backText = "",
                            orderIndex = order++,
                        ),
                    )
                }
            } else {
                for (i in frontMatches.indices) {
                    val match = frontMatches[i]
                    val startIndex = match.range.last + 1
                    val endIndex = if (i + 1 < frontMatches.size) frontMatches[i + 1].range.first else text.length

                    val block = text.substring(startIndex, endIndex).trim()
                    if (block.isEmpty()) continue

                    val parts = block.split(Regex("(?m)^\\s*(?:Back|A|Answer):\\s*", RegexOption.IGNORE_CASE), limit = 2)

                    if (parts.size == 2) {
                        result.add(
                            FlashcardEntity(
                                externalId = UUID.randomUUID().toString(),
                                deckId = deckId,
                                frontText = parts[0].trim(),
                                backText = parts[1].trim(),
                                orderIndex = order++,
                            ),
                        )
                    } else {
                        result.add(
                            FlashcardEntity(
                                externalId = UUID.randomUUID().toString(),
                                deckId = deckId,
                                frontText = block.trim(),
                                backText = "",
                                orderIndex = order++,
                            ),
                        )
                    }
                }
            }
        }
        return result
    }
}
