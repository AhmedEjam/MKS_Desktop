package com.ahmedyejam.mks.data.importer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TextSlideParserTest {
    private lateinit var parser: TextSlideParser

    @Before
    fun setup() {
        parser = TextSlideParser()
    }

    // ── ALTERNATING_PARAGRAPHS mode ──

    @Test
    fun alternating_basicPair_parsesSingleSlide() {
        val text = """
            Introduction to Kotlin

            Kotlin is a modern programming language for JVM.
        """.trimIndent()

        val slides = parser.parse(text, courseId = 1L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        assertEquals(1, slides.size)
        assertEquals("Introduction to Kotlin", slides[0].title)
        assertEquals("Kotlin is a modern programming language for JVM.", slides[0].body)
        assertEquals(0, slides[0].orderIndex)
    }

    @Test
    fun alternating_multipleSlides_parsesAll() {
        val text = """
            Title 1

            Body 1

            Title 2

            Body 2
        """.trimIndent()

        val slides = parser.parse(text, courseId = 1L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        assertEquals(2, slides.size)
        assertEquals("Title 1", slides[0].title)
        assertEquals("Body 1", slides[0].body)
        assertEquals("Title 2", slides[1].title)
        assertEquals("Body 2", slides[1].body)
    }

    @Test
    fun alternating_stripsLabels() {
        val text = """
            Title: Slide Title

            Body: Slide body content
        """.trimIndent()

        val slides = parser.parse(text, courseId = 1L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        assertEquals(1, slides.size)
        assertEquals("Slide Title", slides[0].title)
        assertEquals("Slide body content", slides[0].body)
    }

    @Test
    fun alternating_oddParagraphs_skipsOrphan() {
        val text = """
            Title

            Body

            Orphan
        """.trimIndent()

        val slides = parser.parse(text, courseId = 1L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        assertEquals(1, slides.size)
    }

    @Test
    fun alternating_emptyInput_returnsEmpty() {
        val slides = parser.parse("", courseId = 1L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        assertTrue(slides.isEmpty())
    }

    @Test
    fun alternating_correctCourseId() {
        val text = """
            Title

            Body
        """.trimIndent()

        val slides = parser.parse(text, courseId = 99L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        assertEquals(99L, slides[0].courseId)
    }

    @Test
    fun alternating_hasTimestamps() {
        val text = """
            Title

            Body
        """.trimIndent()

        val before = System.currentTimeMillis()
        val slides = parser.parse(text, courseId = 1L, mode = TextParseMode.ALTERNATING_PARAGRAPHS)
        val after = System.currentTimeMillis()

        assertTrue(slides[0].createdAt in before..after)
        assertTrue(slides[0].updatedAt in before..after)
    }

    // ── EXPLICIT_LABELS mode ──

    @Test
    fun explicitLabels_titAndBody_parses() {
        val text = """
            Title: Slide 1
            Body: Body content here
        """.trimIndent()

        val slides = parser.parse(text, courseId = 1L, mode = TextParseMode.EXPLICIT_LABELS)
        assertEquals(1, slides.size)
        assertEquals("Slide 1", slides[0].title)
        assertEquals("Body content here", slides[0].body)
    }

    @Test
    fun explicitLabels_fallbackNoLabels() {
        val text = "Just a paragraph without labels"

        val slides = parser.parse(text, courseId = 1L, mode = TextParseMode.EXPLICIT_LABELS)
        assertEquals(1, slides.size)
        assertEquals("Just a paragraph without labels", slides[0].title)
        assertEquals("", slides[0].body)
    }

    // ── HEADER_BODY_NOTES mode ──

    @Test
    fun headerBodyNotes_separatorSplit_parses() {
        val text = """
            # Slide 1
            Body content
            > Speaker note
            ---
            # Slide 2
            More body
        """.trimIndent()

        val slides = parser.parse(text, courseId = 1L, mode = TextParseMode.HEADER_BODY_NOTES)
        assertTrue(slides.isNotEmpty())
    }

    @Test
    fun headerBodyNotes_explicitLabels_parses() {
        val text = """
            Header: My Title
            Body: My body content
            Notes: Important speaker notes
        """.trimIndent()

        val slides = parser.parse(text, courseId = 1L, mode = TextParseMode.HEADER_BODY_NOTES)
        assertEquals(1, slides.size)
        assertEquals("My Title", slides[0].title)
        assertEquals("My body content", slides[0].body)
        assertNotNull(slides[0].speakerNotes)
        assertEquals("Important speaker notes", slides[0].speakerNotes)
    }

    @Test
    fun headerBodyNotes_blockquoteAsNotes() {
        val text = """
            # My Title
            Body content here
            > This is a speaker note
        """.trimIndent()

        val slides = parser.parse(text, courseId = 1L, mode = TextParseMode.HEADER_BODY_NOTES)
        assertTrue(slides.isNotEmpty())
        val slide = slides.first()
        assertEquals("My Title", slide.title)
    }

    @Test
    fun headerBodyNotes_emptyInput_returnsEmpty() {
        val slides = parser.parse("", courseId = 1L, mode = TextParseMode.HEADER_BODY_NOTES)
        assertTrue(slides.isEmpty())
    }

    @Test
    fun orderIndex_incrementsCorrectly() {
        val text = """
            Title 1

            Body 1

            Title 2

            Body 2

            Title 3

            Body 3
        """.trimIndent()

        val slides = parser.parse(
            text,
            courseId = 1L,
            startIndex = 10,
            mode = TextParseMode.ALTERNATING_PARAGRAPHS
        )
        assertEquals(3, slides.size)
        assertEquals(10, slides[0].orderIndex)
        assertEquals(11, slides[1].orderIndex)
        assertEquals(12, slides[2].orderIndex)
    }
}
