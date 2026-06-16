package com.ahmedyejam.mks.data.importer.xlsx

import android.util.Base64
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.max
import kotlin.math.min

data class SheetImageResolution(
    val addressImages: Map<String, String> = emptyMap(),
    val rowImages: Map<Int, String> = emptyMap(),
)

class XlsxImageResolver {
    companion object {
        private const val MAX_ENTRY_SIZE = 20 * 1024 * 1024L // 20 MB
        private val DISPIMG_REGEX =
            Regex(
                """(?:_xlfn\.)?DISPIMG\(\s*(?:N\s*)?(?:\"|')?([^,\"'&)<>\s]+)(?:\"|')?""",
                RegexOption.IGNORE_CASE,
            )
    }

    private val dbf =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }

    fun extractDispImgId(formula: String): String? {
        val match = DISPIMG_REGEX.find(formula)
        return match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun getCellImagePathMap(zipFile: ZipFile): Map<String, String> {
        val cellImagesXml = readEntry(zipFile, "xl/cellimages.xml") ?: return emptyMap()
        val relsXml = readEntry(zipFile, "xl/_rels/cellimages.xml.rels") ?: return emptyMap()

        val relMap = parseRelationships(relsXml, "xl/cellimages.xml")
        val result = mutableMapOf<String, String>()

        try {
            val doc = parseXml(cellImagesXml)
            val picNodes = doc.getElementsByTagNameNS("*", "pic")
            for (i in 0 until picNodes.length) {
                val pic = picNodes.item(i) as? Element ?: continue
                val cNvPr = pic.getElementsByTagNameNS("*", "cNvPr").item(0) as? Element
                val blip = pic.getElementsByTagNameNS("*", "blip").item(0) as? Element
                val imageId = cNvPr?.getAttribute("name").orEmpty().trim()
                val relId = extractRelationshipId(blip)
                val targetPath = relId?.let { relMap[it] }
                if (imageId.isNotEmpty() && !targetPath.isNullOrBlank()) {
                    result[imageId] = targetPath
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    fun resolveSheetFormulaImages(
        zipFile: ZipFile,
        sheetName: String,
        cellImagePathMap: Map<String, String>,
    ): Map<String, String> {
        val sheetPath = getSheetPath(zipFile, sheetName) ?: return emptyMap()
        val sheetBytes = readEntry(zipFile, sheetPath) ?: return emptyMap()

        val result = mutableMapOf<String, String>()
        val pathCache = mutableMapOf<String, String>()

        try {
            val doc = parseXml(sheetBytes)
            val cellNodes = doc.getElementsByTagName("c")
            for (i in 0 until cellNodes.length) {
                val cell = cellNodes.item(i) as? Element ?: continue
                val addr = cell.getAttribute("r").trim()
                if (addr.isEmpty()) continue
                val formulaNode = cell.getElementsByTagName("f").item(0)
                val formulaText = formulaNode?.textContent.orEmpty()
                val imageId = extractDispImgId(formulaText) ?: continue
                val zipPath = cellImagePathMap[imageId] ?: continue
                val dataUrl = pathCache.getOrPut(zipPath) { readImageAsDataUrl(zipFile, zipPath).orEmpty() }
                if (dataUrl.isNotBlank()) {
                    result[addr] = dataUrl
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    fun resolveSheetImages(
        zipFile: ZipFile,
        sheetName: String,
        cellImagePathMap: Map<String, String>,
    ): SheetImageResolution {
        val anchored = resolveAnchoredSheetImages(zipFile, sheetName)
        val formulaImages = resolveSheetFormulaImages(zipFile, sheetName, cellImagePathMap)
        val mergedAddressImages = anchored.addressImages.toMutableMap()
        formulaImages.forEach { (cell, image) ->
            if (image.isNotBlank()) {
                mergedAddressImages[cell] = image
            }
        }
        return SheetImageResolution(
            addressImages = mergedAddressImages,
            rowImages = anchored.rowImages,
        )
    }

    private fun resolveAnchoredSheetImages(
        zipFile: ZipFile,
        sheetName: String,
    ): SheetImageResolution {
        val sheetPath = getSheetPath(zipFile, sheetName) ?: return SheetImageResolution()
        val sheetBytes = readEntry(zipFile, sheetPath) ?: return SheetImageResolution()
        val sheetRelsBytes = readEntry(zipFile, buildRelsPath(sheetPath)) ?: return SheetImageResolution()
        val drawingCache = mutableMapOf<String, String>()
        val addressImages = linkedMapOf<String, String>()
        val rowImages = linkedMapOf<Int, String>()

        try {
            val sheetDoc = parseXml(sheetBytes)
            val drawingRelIds = mutableListOf<String>()
            val drawingNodes = sheetDoc.getElementsByTagNameNS("*", "drawing")
            for (i in 0 until drawingNodes.length) {
                val drawing = drawingNodes.item(i) as? Element ?: continue
                extractRelationshipId(drawing)?.let { drawingRelIds.add(it) }
            }
            if (drawingRelIds.isEmpty()) return SheetImageResolution()
            val sheetRelMap = parseRelationships(sheetRelsBytes, sheetPath)
            drawingRelIds.forEach { relId ->
                val drawingPath = sheetRelMap[relId] ?: return@forEach
                mergeDrawingAnchors(zipFile, drawingPath, drawingCache, addressImages, rowImages)
            }
        } catch (_: Exception) {
        }

        return SheetImageResolution(addressImages = addressImages, rowImages = rowImages)
    }

    private fun mergeDrawingAnchors(
        zipFile: ZipFile,
        drawingPath: String,
        pathCache: MutableMap<String, String>,
        addressImages: MutableMap<String, String>,
        rowImages: MutableMap<Int, String>,
    ) {
        val drawingBytes = readEntry(zipFile, drawingPath) ?: return
        val drawingRelsBytes = readEntry(zipFile, buildRelsPath(drawingPath)) ?: return
        val drawingRelMap = parseRelationships(drawingRelsBytes, drawingPath)

        try {
            val drawingDoc = parseXml(drawingBytes)
            consumeAnchorNodes(drawingDoc, "oneCellAnchor", drawingRelMap, zipFile, pathCache, addressImages, rowImages)
            consumeAnchorNodes(drawingDoc, "twoCellAnchor", drawingRelMap, zipFile, pathCache, addressImages, rowImages)
        } catch (_: Exception) {
        }
    }

    private fun consumeAnchorNodes(
        drawingDoc: Document,
        localName: String,
        drawingRelMap: Map<String, String>,
        zipFile: ZipFile,
        pathCache: MutableMap<String, String>,
        addressImages: MutableMap<String, String>,
        rowImages: MutableMap<Int, String>,
    ) {
        val anchorNodes = drawingDoc.getElementsByTagNameNS("*", localName)
        for (i in 0 until anchorNodes.length) {
            val anchor = anchorNodes.item(i) as? Element ?: continue
            val from = anchor.getElementsByTagNameNS("*", "from").item(0) as? Element ?: continue
            val fromCol = childInt(from, "col") ?: continue
            val fromRowZero = childInt(from, "row") ?: continue

            var toCol = fromCol
            var toRowZero = fromRowZero
            val to = anchor.getElementsByTagNameNS("*", "to").item(0) as? Element
            if (to != null) {
                toCol = childInt(to, "col") ?: fromCol
                toRowZero = childInt(to, "row") ?: fromRowZero
            }

            val blip = anchor.getElementsByTagNameNS("*", "blip").item(0) as? Element ?: continue
            val relId = extractRelationshipId(blip) ?: continue
            val imagePath = drawingRelMap[relId] ?: continue
            val dataUrl = pathCache.getOrPut(imagePath) { readImageAsDataUrl(zipFile, imagePath).orEmpty() }
            if (dataUrl.isBlank()) continue

            val rowStart = min(fromRowZero, toRowZero) + 1
            val rowEnd = max(fromRowZero, toRowZero) + 1
            val colStart = min(fromCol, toCol)
            val colEnd = max(fromCol, toCol)

            for (row in rowStart..rowEnd) {
                rowImages.putIfAbsent(row, dataUrl)
                for (col in colStart..colEnd) {
                    addressImages.putIfAbsent(toCellRef(col, row), dataUrl)
                }
            }
        }
    }

    private fun extractRelationshipId(element: Element?): String? {
        if (element == null) return null
        return element.getAttribute("r:embed").ifBlank {
            element.getAttributeNS(
                "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
                "embed",
            )
        }.ifBlank {
            element.getAttribute("r:id")
        }.ifBlank {
            element.getAttributeNS(
                "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
                "id",
            )
        }.trim().takeIf { it.isNotEmpty() }
    }

    private fun childInt(
        parent: Element,
        localName: String,
    ): Int? {
        val value = parent.getElementsByTagNameNS("*", localName).item(0)?.textContent?.trim()
        return value?.toIntOrNull()
    }

    private fun readImageAsDataUrl(
        zipFile: ZipFile,
        zipPath: String,
    ): String? {
        val bytes = readEntry(zipFile, zipPath) ?: return null
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:${getImageMimeType(zipPath)};base64,$base64"
    }

    private fun readEntry(
        zipFile: ZipFile,
        name: String,
    ): ByteArray? {
        return try {
            zipFile.getEntry(name)?.let { entry ->
                if (entry.size > MAX_ENTRY_SIZE) {
                    return null
                }
                zipFile.getInputStream(entry).use { it.readBytes() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseXml(bytes: ByteArray): Document {
        return dbf.newDocumentBuilder().parse(bytes.inputStream())
    }

    private fun parseRelationships(
        relsBytes: ByteArray,
        basePath: String,
    ): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val doc = parseXml(relsBytes)
            val relNodes = doc.getElementsByTagName("Relationship")
            for (i in 0 until relNodes.length) {
                val rel = relNodes.item(i) as? Element ?: continue
                val id = rel.getAttribute("Id").trim()
                val target = rel.getAttribute("Target").trim()
                if (id.isNotEmpty() && target.isNotEmpty()) {
                    map[id] = resolveZipPath(basePath, target)
                }
            }
        } catch (_: Exception) {
        }
        return map
    }

    private fun resolveZipPath(
        basePath: String,
        target: String,
    ): String {
        if (target.startsWith("/")) {
            return target.trimStart('/')
        }
        val baseDir = basePath.substringBeforeLast('/', "")
        val combined = if (baseDir.isEmpty()) target else "$baseDir/$target"
        val resolved = mutableListOf<String>()
        combined.split('/').forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (resolved.isNotEmpty()) resolved.removeAt(resolved.lastIndex)
                else -> resolved.add(part)
            }
        }
        return resolved.joinToString("/")
    }

    private fun buildRelsPath(path: String): String {
        val dir = path.substringBeforeLast('/', "")
        val fileName = path.substringAfterLast('/')
        return if (dir.isEmpty()) {
            "_rels/$fileName.rels"
        } else {
            "$dir/_rels/$fileName.rels"
        }
    }

    private fun getSheetPath(
        zipFile: ZipFile,
        sheetName: String,
    ): String? {
        val workbookXml = readEntry(zipFile, "xl/workbook.xml") ?: return null
        val relsXml = readEntry(zipFile, "xl/_rels/workbook.xml.rels") ?: return null
        val relMap = parseRelationships(relsXml, "xl/workbook.xml")

        return try {
            val doc = parseXml(workbookXml)
            val sheetNodes = doc.getElementsByTagNameNS("*", "sheet")
            for (i in 0 until sheetNodes.length) {
                val sheet = sheetNodes.item(i) as? Element ?: continue
                if (sheet.getAttribute("name") == sheetName) {
                    val relId =
                        sheet.getAttributeNS(
                            "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
                            "id",
                        ).ifEmpty { sheet.getAttribute("r:id") }.trim()
                    return relMap[relId]
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun toCellRef(
        zeroBasedCol: Int,
        oneBasedRow: Int,
    ): String {
        var col = zeroBasedCol + 1
        val builder = StringBuilder()
        while (col > 0) {
            val rem = (col - 1) % 26
            builder.insert(0, ('A'.code + rem).toChar())
            col = (col - 1) / 26
        }
        return builder.toString() + oneBasedRow
    }

    private fun getImageMimeType(path: String): String {
        return when (path.substringAfterLast('.', "png").lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "bmp" -> "image/bmp"
            else -> "image/png"
        }
    }
}
