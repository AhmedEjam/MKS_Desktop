package com.ahmedyejam.mks.data.importer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TextArticleParserTest {
    private lateinit var parser: TextArticleParser

    @Before
    fun setup() {
        parser = TextArticleParser()
    }

    // ── BASIC mode ──

    @Test
    fun basic_singleArticle_parses() {
        val text = """
            My Article Title

            This is the summary paragraph.

            This is the body paragraph.
        """.trimIndent()

        val articles = parser.parse(text, collectionId = 1L, mode = TextArticleParseMode.BASIC)
        assertEquals(1, articles.size)
        assertEquals("My Article Title", articles[0].title)
        assertEquals("This is the summary paragraph.", articles[0].summary)
        assertEquals("This is the body paragraph.", articles[0].body)
    }

    @Test
    fun basic_multipleArticles_separatedByHr() {
        val text = """
            Article 1 Title

            Summary 1
            ---
            Article 2 Title

            Summary 2
        """.trimIndent()

        val articles = parser.parse(text, collectionId = 1L, mode = TextArticleParseMode.BASIC)
        assertEquals(2, articles.size)
        assertEquals("Article 1 Title", articles[0].title)
        assertEquals("Article 2 Title", articles[1].title)
    }

    @Test
    fun basic_titleOnly_nullSummaryEmptyBody() {
        val text = "Just a title"

        val articles = parser.parse(text, collectionId = 1L, mode = TextArticleParseMode.BASIC)
        assertEquals(1, articles.size)
        assertEquals("Just a title", articles[0].title)
        assertNull(articles[0].summary)
        assertEquals("", articles[0].body)
    }

    @Test
    fun basic_titleAndSummary_noBody() {
        val text = """
            Title

            Summary paragraph only
        """.trimIndent()

        val articles = parser.parse(text, collectionId = 1L, mode = TextArticleParseMode.BASIC)
        assertEquals(1, articles.size)
        assertEquals("Title", articles[0].title)
        assertEquals("Summary paragraph only", articles[0].summary)
        assertEquals("", articles[0].body)
    }

    @Test
    fun basic_emptyInput_returnsEmpty() {
        val articles = parser.parse("", collectionId = 1L, mode = TextArticleParseMode.BASIC)
        assertTrue(articles.isEmpty())
    }

    @Test
    fun basic_correctCollectionId() {
        val text = """
            Title

            Summary
        """.trimIndent()

        val articles = parser.parse(text, collectionId = 42L, mode = TextArticleParseMode.BASIC)
        assertEquals(42L, articles[0].collectionId)
    }

    @Test
    fun basic_defaultMode_isSimpleNote() {
        val text = """
            Title

            Summary
        """.trimIndent()

        val articles = parser.parse(text, collectionId = 1L, mode = TextArticleParseMode.BASIC)
        assertEquals("SIMPLE_NOTE", articles[0].blueprintMode)
    }

    @Test
    fun basic_customDefaultMode_applied() {
        val text = """
            Title

            Summary
        """.trimIndent()

        val articles = parser.parse(
            text, collectionId = 1L,
            mode = TextArticleParseMode.BASIC,
            defaultMode = "ARTICLE"
        )
        assertEquals("ARTICLE", articles[0].blueprintMode)
    }

    // ── EXPLICIT_LABELS mode ──

    @Test
    fun explicitLabels_allFields_parses() {
        val text =
            "Title: My Note\nSummary: A brief summary\nBody: The full body content\nBullet: Point one\nBullet: Point two\nTag: kotlin\nTag: android"

        val articles =
            parser.parse(text, collectionId = 1L, mode = TextArticleParseMode.EXPLICIT_LABELS)
        assertEquals(1, articles.size)
        assertEquals("My Note", articles[0].title)
        assertEquals("A brief summary", articles[0].summary)
        assertEquals("The full body content", articles[0].body)
        assertTrue("Should have at least 1 bullet point", articles[0].bulletPoints.isNotEmpty())
        assertTrue("Should have at least 1 tag", articles[0].tags.isNotEmpty())
    }

    @Test
    fun explicitLabels_titleOnly_fallback() {
        val text = "No labels, just raw text content"

        val articles =
            parser.parse(text, collectionId = 1L, mode = TextArticleParseMode.EXPLICIT_LABELS)
        assertEquals(1, articles.size)
        // Fallback: takes first 50 chars + "..." as title
        assertTrue(articles[0].title.endsWith("..."))
        assertEquals("No labels, just raw text content", articles[0].body)
    }

    @Test
    fun explicitLabels_multipleArticles() {
        val text = """
            Title: Article 1
            Body: Body 1
            ---
            Title: Article 2
            Body: Body 2
        """.trimIndent()

        val articles =
            parser.parse(text, collectionId = 1L, mode = TextArticleParseMode.EXPLICIT_LABELS)
        assertEquals(2, articles.size)
        assertEquals("Article 1", articles[0].title)
        assertEquals("Article 2", articles[1].title)
    }

    @Test
    fun explicitLabels_emptyInput_returnsEmpty() {
        val articles =
            parser.parse("", collectionId = 1L, mode = TextArticleParseMode.EXPLICIT_LABELS)
        assertTrue(articles.isEmpty())
    }
}
