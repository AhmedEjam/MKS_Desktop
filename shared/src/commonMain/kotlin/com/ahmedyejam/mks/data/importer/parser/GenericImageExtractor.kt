package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.model.ResolvedImage

class GenericImageExtractor {
    fun extractFromText(text: String?): ResolvedImage? {
        val raw = text?.trim() ?: return null
        if (raw.isEmpty()) return null

        // 1. Data URL
        val dataUrlMatch =
            Regex("""(data:image/[a-zA-Z0-9.+-]+;base64,[A-Za-z0-9+/=\s]+)""", RegexOption.IGNORE_CASE)
                .find(raw)
        if (dataUrlMatch != null) {
            return ResolvedImage(imageDataUrl = dataUrlMatch.groupValues[1].replace(Regex("""\s+"""), ""), via = "direct-data-url")
        }

        // 2. HTTP/HTTPS URL
        val urlMatch =
            Regex(
                """(https?://[^\s"'<>]+\.(?:png|jpe?g|gif|webp|svg|bmp|tiff?|avif|ico|pdf|png|jpg|jpeg)(?:[?#][^\s"'<>]*)|https?://[^\s"'<>]+)""",
                RegexOption.IGNORE_CASE,
            )
                .find(raw)
        if (urlMatch != null) {
            return ResolvedImage(imageSource = urlMatch.groupValues[1], via = "direct-url")
        }

        // 3. HTML img tag
        val imgMatch =
            Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(raw)
        if (imgMatch != null) {
            val src = imgMatch.groupValues[1]
            return if (src.startsWith(
                    "data:",
                )
            ) {
                ResolvedImage(imageDataUrl = src, via = "html-img")
            } else {
                ResolvedImage(imageSource = src, via = "html-img")
            }
        }

        // 4. Markdown syntax
        val mdMatch =
            Regex("""!\[[^\]]*\]\(([^)]+)\)""")
                .find(raw)
        if (mdMatch != null) {
            val src = mdMatch.groupValues[1]
            return if (src.startsWith(
                    "data:",
                )
            ) {
                ResolvedImage(imageDataUrl = src, via = "markdown-img")
            } else {
                ResolvedImage(imageSource = src, via = "markdown-img")
            }
        }

        return null
    }

    fun extractFromFields(text: String?): ResolvedImage? {
        val raw = text ?: return null
        val lines = raw.lines()
        val prefixes = listOf("image:", "image url:", "img:", "picture:", "media:", "image source:", "صورة:", "رابط الصورة:")

        for (line in lines) {
            val trimmed = line.trim()
            for (prefix in prefixes) {
                if (trimmed.startsWith(prefix, ignoreCase = true)) {
                    val value = trimmed.substring(prefix.length).trim()
                    return extractFromText(value) ?: ResolvedImage(imageSource = value, via = "field-match")
                }
            }
        }
        return null
    }
}
