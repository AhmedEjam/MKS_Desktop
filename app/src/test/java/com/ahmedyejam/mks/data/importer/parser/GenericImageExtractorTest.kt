package com.ahmedyejam.mks.data.importer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GenericImageExtractorTest {
    private lateinit var extractor: GenericImageExtractor

    @Before
    fun setup() {
        extractor = GenericImageExtractor()
    }

    // ── extractFromText ──

    @Test
    fun extractFromText_null_returnsNull() {
        assertNull(extractor.extractFromText(null))
    }

    @Test
    fun extractFromText_empty_returnsNull() {
        assertNull(extractor.extractFromText(""))
    }

    @Test
    fun extractFromText_blankSpaces_returnsNull() {
        assertNull(extractor.extractFromText("   "))
    }

    @Test
    fun extractFromText_plainText_returnsNull() {
        assertNull(extractor.extractFromText("Just some regular text without images"))
    }

    @Test
    fun extractFromText_dataUrl_extractsCorrectly() {
        val dataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA"
        val result = extractor.extractFromText(dataUrl)
        assertNotNull(result)
        assertEquals(dataUrl, result!!.imageDataUrl)
        assertEquals("direct-data-url", result.via)
        assertNull(result.imageSource)
    }

    @Test
    fun extractFromText_httpsUrl_extractsCorrectly() {
        val url = "https://example.com/image.png"
        val result = extractor.extractFromText(url)
        assertNotNull(result)
        assertEquals(url, result!!.imageSource)
        assertEquals("direct-url", result.via)
        assertNull(result.imageDataUrl)
    }

    @Test
    fun extractFromText_httpUrl_extractsCorrectly() {
        val url = "http://example.com/photo.jpg"
        val result = extractor.extractFromText(url)
        assertNotNull(result)
        assertEquals(url, result!!.imageSource)
        assertEquals("direct-url", result.via)
    }

    @Test
    fun extractFromText_jpegExtension_extracts() {
        val url = "https://cdn.example.com/images/photo.jpeg"
        val result = extractor.extractFromText(url)
        assertNotNull(result)
        assertEquals(url, result!!.imageSource)
    }

    @Test
    fun extractFromText_webpExtension_extracts() {
        val url = "https://cdn.example.com/images/photo.webp"
        val result = extractor.extractFromText(url)
        assertNotNull(result)
        assertEquals(url, result!!.imageSource)
    }

    @Test
    fun extractFromText_htmlImgTag_extractsSrc() {
        val html = """<img src="https://example.com/photo.png" alt="Photo">"""
        val result = extractor.extractFromText(html)
        assertNotNull(result)
        // URL extraction may match the full URL first via the HTTP regex,
        // or match the <img> tag. Either way, the source should be present.
        assertNotNull(result!!.imageSource ?: result.imageDataUrl)
    }

    @Test
    fun extractFromText_htmlImgTag_dataUrlSrc() {
        val html = """<img src="data:image/png;base64,abc123" alt="Photo">"""
        val result = extractor.extractFromText(html)
        assertNotNull(result)
        assertNotNull(result!!.imageDataUrl)
    }

    @Test
    fun extractFromText_markdownSyntax_extracts() {
        val md = "![Alt text](https://example.com/img.png)"
        val result = extractor.extractFromText(md)
        assertNotNull(result)
        // Markdown or URL regex should pick this up
        assertNotNull(result!!.imageSource ?: result.imageDataUrl)
    }

    @Test
    fun extractFromText_markdownSyntax_dataUrl() {
        val md = "![Alt](data:image/gif;base64,R0lGODlhAQABAIAAAP)"
        val result = extractor.extractFromText(md)
        assertNotNull(result)
        assertNotNull(result!!.imageDataUrl)
    }

    @Test
    fun extractFromText_priority_dataUrlFirst() {
        // Data URL should be detected before HTTP URL
        val text = "data:image/png;base64,abc123 https://example.com/img.png"
        val result = extractor.extractFromText(text)
        assertNotNull(result)
        assertEquals("direct-data-url", result!!.via)
    }

    // ── extractFromFields ──

    @Test
    fun extractFromFields_null_returnsNull() {
        assertNull(extractor.extractFromFields(null))
    }

    @Test
    fun extractFromFields_noPrefix_returnsNull() {
        assertNull(extractor.extractFromFields("Just some text"))
    }

    @Test
    fun extractFromFields_imagePrefix_extracts() {
        val text = "Image: https://example.com/photo.jpg"
        val result = extractor.extractFromFields(text)
        assertNotNull(result)
        assertEquals("https://example.com/photo.jpg", result!!.imageSource)
    }

    @Test
    fun extractFromFields_imgPrefix_extracts() {
        val text = "img: https://cdn.example.com/img.png"
        val result = extractor.extractFromFields(text)
        assertNotNull(result)
    }

    @Test
    fun extractFromFields_arabicPrefix_extracts() {
        val text = "صورة: https://example.com/photo.png"
        val result = extractor.extractFromFields(text)
        assertNotNull(result)
    }

    @Test
    fun extractFromFields_caseInsensitive() {
        val text = "IMAGE: https://example.com/photo.png"
        val result = extractor.extractFromFields(text)
        assertNotNull(result)
    }

    @Test
    fun extractFromFields_multipleLines_firstMatch() {
        val text = """
            Name: Ahmed
            Image: https://example.com/photo.png
            Description: A photo
        """.trimIndent()
        val result = extractor.extractFromFields(text)
        assertNotNull(result)
        assertEquals("https://example.com/photo.png", result!!.imageSource)
    }

    @Test
    fun extractFromFields_noUrl_fieldMatch() {
        val text = "Image: local_path_to_file.png"
        val result = extractor.extractFromFields(text)
        assertNotNull(result)
        assertEquals("field-match", result!!.via)
        assertEquals("local_path_to_file.png", result.imageSource)
    }
}
