package com.ahmedyejam.mks.data.importer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TextFlashcardParserTest {
    private lateinit var parser: TextFlashcardParser

    @Before
    fun setup() {
        parser = TextFlashcardParser()
    }

    // ── ALTERNATING_PARAGRAPHS mode ──

    @Test
    fun alternating_basicPair_parsesSingleCard() {
        val text = """
            What is the capital of France?

            Paris
        """.trimIndent()

        val cards = parser.parse(text, deckId = 1L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        assertEquals(1, cards.size)
        assertEquals("What is the capital of France?", cards[0].frontText)
        assertEquals("Paris", cards[0].backText)
        assertEquals(0, cards[0].orderIndex)
    }

    @Test
    fun alternating_multipleCards_parsesAll() {
        val text = """
            Front 1

            Back 1

            Front 2

            Back 2

            Front 3

            Back 3
        """.trimIndent()

        val cards = parser.parse(text, deckId = 1L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        assertEquals(3, cards.size)
        assertEquals("Front 1", cards[0].frontText)
        assertEquals("Back 1", cards[0].backText)
        assertEquals("Front 3", cards[2].frontText)
        assertEquals("Back 3", cards[2].backText)
    }

    @Test
    fun alternating_oddParagraphs_skipsOrphan() {
        val text = """
            Front 1

            Back 1

            Orphan
        """.trimIndent()

        val cards = parser.parse(text, deckId = 1L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        // The orphan paragraph has no pair, so it should be skipped
        assertEquals(1, cards.size)
    }

    @Test
    fun alternating_stripsLabels() {
        val text = """
            Question: What is 2+2?

            Answer: 4
        """.trimIndent()

        val cards = parser.parse(text, deckId = 1L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        assertEquals(1, cards.size)
        assertEquals("What is 2+2?", cards[0].frontText)
        assertEquals("4", cards[0].backText)
    }

    @Test
    fun alternating_emptyInput_returnsEmpty() {
        val cards = parser.parse("", deckId = 1L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        assertTrue(cards.isEmpty())
    }

    @Test
    fun alternating_respectsStartIndex() {
        val text = """
            Front

            Back
        """.trimIndent()

        val cards = parser.parse(
            text,
            deckId = 1L,
            startIndex = 5,
            mode = TextParseMode.ALTERNATING_PARAGRAPHS
        )
        assertEquals(1, cards.size)
        assertEquals(5, cards[0].orderIndex)
    }

    @Test
    fun alternating_correctDeckId() {
        val text = """
            Front

            Back
        """.trimIndent()

        val cards = parser.parse(text, deckId = 42L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        assertEquals(42L, cards[0].deckId)
    }

    // ── EXPLICIT_LABELS mode ──

    @Test
    fun explicitLabels_basicCard_parses() {
        val text = """
            Question: What is the sun?
            Answer: A star
        """.trimIndent()

        val cards = parser.parse(text, deckId = 1L, mode = TextParseMode.EXPLICIT_LABELS)
        assertEquals(1, cards.size)
        assertEquals("What is the sun?", cards[0].frontText)
        assertEquals("A star", cards[0].backText)
    }

    @Test
    fun explicitLabels_multipleCards_parses() {
        val text = """
            Q: Question 1
            A: Answer 1
            Q: Question 2
            A: Answer 2
        """.trimIndent()

        val cards = parser.parse(text, deckId = 1L, mode = TextParseMode.EXPLICIT_LABELS)
        assertEquals(2, cards.size)
        assertEquals("Question 1", cards[0].frontText)
        assertEquals("Answer 1", cards[0].backText)
        assertEquals("Question 2", cards[1].frontText)
        assertEquals("Answer 2", cards[1].backText)
    }

    @Test
    fun explicitLabels_noLabels_fallbackWholeText() {
        val text = "Just some text without any labels"

        val cards = parser.parse(text, deckId = 1L, mode = TextParseMode.EXPLICIT_LABELS)
        assertEquals(1, cards.size)
        assertEquals("Just some text without any labels", cards[0].frontText)
        assertEquals("", cards[0].backText)
    }

    @Test
    fun explicitLabels_frontOnly_emptyBack() {
        val text = """
            Front: Card with no answer
        """.trimIndent()

        val cards = parser.parse(text, deckId = 1L, mode = TextParseMode.EXPLICIT_LABELS)
        assertEquals(1, cards.size)
        assertEquals("Card with no answer", cards[0].frontText)
        assertEquals("", cards[0].backText)
    }

    @Test
    fun explicitLabels_emptyInput_returnsSingleEmptyCard() {
        // Empty string still gets the fallback path
        val cards = parser.parse("", deckId = 1L, mode = TextParseMode.EXPLICIT_LABELS)
        // The empty string doesn't match Front: pattern, falls through to the no-labels fallback
        assertEquals(1, cards.size)
    }
}
