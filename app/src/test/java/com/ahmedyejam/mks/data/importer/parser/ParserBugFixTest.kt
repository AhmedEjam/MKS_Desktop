package com.ahmedyejam.mks.data.importer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserBugFixTest {
    @Test
    fun spreadsheet_emptyStemWithImage_importsSuccessfully() {
        val mapping = mapOf("question" to 0, "image" to 1)
        val parser =
            SpreadsheetQuestionParser(
                mapping = mapping,
                optionCols = emptyList(),
                sheetAddressImages = emptyMap(),
                sheetRowImages = emptyMap(),
                imageExtractor = GenericImageExtractor(),
            )

        val row = listOf("", "https://example.com/image.png")
        val result = parser.parseRow(row, 1)

        assertNotNull("Row with empty stem but image should not be null", result)
        assertEquals("", result!!.stem)
        assertEquals("https://example.com/image.png", result.imageSource)
    }

    @Test
    fun spreadsheet_explicitAnswer_takesPrecedenceOverMarkedOption() {
        val mapping = mapOf("question" to 0, "answer" to 1)
        val optionCols = listOf(2, 3)
        val parser =
            SpreadsheetQuestionParser(
                mapping = mapping,
                optionCols = optionCols,
                sheetAddressImages = emptyMap(),
                sheetRowImages = emptyMap(),
                imageExtractor = GenericImageExtractor(),
            )

        // Row has Answer "A", but Option B is marked with asterisk
        val row = listOf("Question", "A", "Option A", "*Option B")
        val result = parser.parseRow(row, 1)

        assertNotNull(result)
        assertEquals(1, result!!.correctAnswers.size)
        assertEquals("opt_A", result.correctAnswers[0])
    }

    @Test
    fun spreadsheet_noExplicitAnswer_usesMarkedOption() {
        val mapping = mapOf("question" to 0, "answer" to 1)
        val optionCols = listOf(2, 3)
        val parser =
            SpreadsheetQuestionParser(
                mapping = mapping,
                optionCols = optionCols,
                sheetAddressImages = emptyMap(),
                sheetRowImages = emptyMap(),
                imageExtractor = GenericImageExtractor(),
            )

        // Row has empty Answer, but Option B is marked with asterisk
        val row = listOf("Question", "", "Option A", "*Option B")
        val result = parser.parseRow(row, 1)

        assertNotNull(result)
        assertEquals(1, result!!.correctAnswers.size)
        assertEquals("opt_B", result.correctAnswers[0])
    }

    @Test
    fun json_answerParsing_avoidsFalsePositives() {
        val parser = JsonQuestionParser(GenericImageExtractor())

        // Answer "Paris" should match option A (opt_A) textually
        val json =
            """
            {
                "question": "What is the capital of France?",
                "options": ["Paris", "London"],
                "answer": "Paris"
            }
            """.trimIndent()

        val results = parser.parse(json)
        assertEquals(1, results.size)
        val q = results[0]

        assertEquals("opt_A", q.correctAnswers[0])

        // Test with "A" which is a substring of "Paris"
        val json2 =
            """
            {
                "question": "Q",
                "options": ["Paris", "Berlin"],
                "answer": "A"
            }
            """.trimIndent()
        val results2 = parser.parse(json2)
        assertEquals("opt_A", results2[0].correctAnswers[0])

        // Test with an answer that is NOT an option letter and NOT an option text
        val json3 =
            """
            {
                "question": "Q",
                "options": ["Option X", "Option Y"],
                "answer": "Z"
            }
            """.trimIndent()
        val results3 = parser.parse(json3)
        assertTrue("Answer 'Z' should not match any options", results3[0].correctAnswers.isEmpty())
    }

    @Test
    fun text_answerParsing_avoidsFalsePositives() {
        val parser = TextQuestionParser(GenericImageExtractor())

        val text =
            """
            1) Question
            A) Paris
            B) London
            Answer: Paris
            """.trimIndent()

        val results = parser.parse(text)
        assertEquals(1, results.size)
        assertEquals("opt_A", results[0].correctAnswers[0])

        val text2 =
            """
            1) Question
            A) Option X
            B) Option Y
            Answer: Z
            """.trimIndent()
        val results2 = parser.parse(text2)
        assertTrue("Answer 'Z' should not match any options", results2[0].correctAnswers.isEmpty())
    }
}
