package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.model.ParsedQuestion

class HtmlQuestionParser(
    private val jsonParser: JsonQuestionParser,
) {
    fun parse(text: String): List<ParsedQuestion> {
        val patterns =
            listOf(
                Regex("""id=["']quiz-data["'][^>]*>([\s\S]*?)<\/script>""", RegexOption.IGNORE_CASE),
                Regex("""window\.__QUIZ__\s*=\s*(\[[\s\S]*?\]|\{[\s\S]*?\});?""", RegexOption.IGNORE_CASE),
                Regex("""window\.QUIZ_DATA\s*=\s*(\[[\s\S]*?\]|\{[\s\S]*?\});?""", RegexOption.IGNORE_CASE),
                Regex("""const\s+allData\s*=\s*(\[[\s\S]*?\]);""", RegexOption.IGNORE_CASE),
            )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                try {
                    return jsonParser.parse(match.groupValues[1])
                } catch (e: Exception) {
                    // Continue to next pattern
                }
            }
        }

        throw Exception("No embedded quiz JSON found in the HTML.")
    }
}
