package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.model.ImportMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SourceDetectorTest {
    private lateinit var detector: SourceDetector

    @Before
    fun setup() {
        detector = SourceDetector()
    }

    // ── Extension-based detection ──

    @Test
    fun detectMode_xlsx_returnsSpreadsheet() {
        assertEquals(ImportMode.SPREADSHEET, detector.detectMode("data.xlsx", null))
    }

    @Test
    fun detectMode_xls_returnsSpreadsheet() {
        assertEquals(ImportMode.SPREADSHEET, detector.detectMode("data.xls", null))
    }

    @Test
    fun detectMode_csv_returnsSpreadsheet() {
        assertEquals(ImportMode.SPREADSHEET, detector.detectMode("data.csv", null))
    }

    @Test
    fun detectMode_tsv_returnsSpreadsheet() {
        assertEquals(ImportMode.SPREADSHEET, detector.detectMode("data.tsv", null))
    }

    @Test
    fun detectMode_json_returnsJson() {
        assertEquals(ImportMode.JSON, detector.detectMode("data.json", null))
    }

    @Test
    fun detectMode_html_returnsHtml() {
        assertEquals(ImportMode.HTML, detector.detectMode("page.html", null))
    }

    @Test
    fun detectMode_htm_returnsHtml() {
        assertEquals(ImportMode.HTML, detector.detectMode("page.htm", null))
    }

    // ── Content-based detection ──

    @Test
    fun detectMode_jsonArray_returnsJson() {
        assertEquals(ImportMode.JSON, detector.detectMode("file.txt", "[{\"question\": \"test\"}]"))
    }

    @Test
    fun detectMode_jsonObjectWithKind_returnsJson() {
        assertEquals(ImportMode.JSON, detector.detectMode("file.txt", "{\"kind\": \"quiz\"}"))
    }

    @Test
    fun detectMode_htmlContent_returnsHtml() {
        assertEquals(
            ImportMode.HTML,
            detector.detectMode("file.txt", "<html><body>Test</body></html>")
        )
    }

    @Test
    fun detectMode_htmlScript_returnsHtml() {
        assertEquals(
            ImportMode.HTML,
            detector.detectMode("file.txt", "<script>alert('hi')</script>")
        )
    }

    @Test
    fun detectMode_htmlImg_returnsHtml() {
        assertEquals(ImportMode.HTML, detector.detectMode("file.txt", """<img src="test.png">"""))
    }

    @Test
    fun detectMode_tabularCsv_returnsSpreadsheet() {
        val csvContent = """
            question,answer,explanation
            What is 1+1?,2,Basic math
            What is 2+2?,4,Addition
            What is 3+3?,6,Addition
            What is 4+4?,8,Addition
            What is 5+5?,10,Addition
        """.trimIndent()
        assertEquals(ImportMode.SPREADSHEET, detector.detectMode("file.txt", csvContent))
    }

    @Test
    fun detectMode_tabularTsv_returnsSpreadsheet() {
        val tsvContent = "question\tanswer\texplanation\n" +
                "What is 1+1?\t2\tBasic math\n" +
                "What is 2+2?\t4\tAddition\n" +
                "What is 3+3?\t6\tAddition\n" +
                "What is 4+4?\t8\tAddition\n" +
                "What is 5+5?\t10\tAddition"
        assertEquals(ImportMode.SPREADSHEET, detector.detectMode("file.txt", tsvContent))
    }

    // ── Fallback ──

    @Test
    fun detectMode_plainText_returnsText() {
        assertEquals(ImportMode.TEXT, detector.detectMode("file.txt", "Just some plain text"))
    }

    @Test
    fun detectMode_nullContent_usesExtension() {
        assertEquals(ImportMode.JSON, detector.detectMode("data.json", null))
    }

    @Test
    fun detectMode_nullBoth_returnsText() {
        assertEquals(ImportMode.TEXT, detector.detectMode(null, null))
    }

    @Test
    fun detectMode_emptyContent_returnsText() {
        assertEquals(ImportMode.TEXT, detector.detectMode(null, ""))
    }

    // ── Extension priority over content ──

    @Test
    fun detectMode_xlsxExtension_overridesJsonContent() {
        // Extension should take priority for spreadsheet formats
        assertEquals(
            ImportMode.SPREADSHEET,
            detector.detectMode("data.xlsx", "[{\"question\": \"test\"}]")
        )
    }

    @Test
    fun detectMode_caseInsensitiveExtension() {
        // Extensions are lowercased, so .XLSX should also work
        assertEquals(ImportMode.SPREADSHEET, detector.detectMode("data.XLSX", null))
    }
}
