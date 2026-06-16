package com.ahmedyejam.mks.data.importer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpreadsheetHeaderMapperTest {
    private lateinit var mapper: SpreadsheetHeaderMapper

    @Before
    fun setup() {
        mapper = SpreadsheetHeaderMapper()
    }

    @Test
    fun mapHeaders_exactMatch_works() {
        val header = listOf("Question", "Answer", "Explanation", "Hint")
        val mapping = mapper.mapHeaders(header)

        assertEquals(0, mapping["question"])
        assertEquals(1, mapping["answer"])
        assertEquals(2, mapping["explanation"])
        assertEquals(3, mapping["hint"])
    }

    @Test
    fun mapHeaders_arabicMatch_works() {
        val header = listOf("سؤال", "الإجابة", "تلميح")
        val mapping = mapper.mapHeaders(header)

        assertEquals(0, mapping["question"])
        assertEquals(1, mapping["answer"])
        assertEquals(2, mapping["hint"])
    }

    @Test
    fun mapHeaders_substringMatch_isStricter() {
        // "q" is a short alias for question. It should not match "Sequence" unless exact or whole word.
        val header = listOf("Sequence", "Answer")
        val mapping = mapper.mapHeaders(header)

        assertNull("Should not match 'q' in 'Sequence'", mapping["question"])
        assertEquals(1, mapping["answer"])
    }

    @Test
    fun calculateRowScore_weightedCorrectly() {
        val validHeader = listOf("Question", "Answer", "Option A", "Option B")
        val score = mapper.calculateRowScore(validHeader)

        // Question (100) + Answer (80) + 2 Options (20*2) = 220
        assertTrue("Score for valid header should be high, was: $score", score >= 200)

        val dataRow = listOf("This is a long question text about something.", "Paris", "London", "Berlin")
        val dataScore = mapper.calculateRowScore(dataRow)

        // "Question" might be in text, but with penalties for length it should be low
        assertTrue("Score for data row should be low: $dataScore", dataScore < 50)
    }

    @Test
    fun guessOptionColumns_excludesMappedFields() {
        val header = listOf("Question", "Option 1", "Option 2", "Answer", "Explanation")
        val mapping = mapper.mapHeaders(header)

        val options = mapper.guessOptionColumns(header, mapping)

        assertEquals(2, options.size)
        assertTrue(options.contains(1))
        assertTrue(options.contains(2))
        assertFalse(options.contains(0))
        assertFalse(options.contains(3))
        assertFalse(options.contains(4))
    }

    @Test
    fun guessOptionColumns_identifiesArabicOptions() {
        val header = listOf("السؤال", "الخيار أ", "الخيار ب", "الجواب")
        val mapping = mapper.mapHeaders(header)

        val options = mapper.guessOptionColumns(header, mapping)

        assertEquals(2, options.size)
        assertTrue(options.contains(1))
        assertTrue(options.contains(2))
    }
}
