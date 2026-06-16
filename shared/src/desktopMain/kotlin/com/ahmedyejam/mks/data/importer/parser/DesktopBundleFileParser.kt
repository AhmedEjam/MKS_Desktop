package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.dto.LibraryBundleDto
import com.ahmedyejam.mks.data.importer.dto.ManifestDto
import com.ahmedyejam.mks.data.importer.model.ImportFormat
import com.ahmedyejam.mks.data.importer.xlsx.XlsxLibraryCompiler
import com.ahmedyejam.mks.util.readTextWithLimit
import java.io.File
import java.io.InputStream

/**
 * Desktop implementation of [BundleFileParser].
 *
 * Handles formats requiring JVM-only dependencies:
 * - XLSX via Apache POI ([XlsxLibraryCompiler])
 * - JSON library bundles via kotlinx.serialization ([JsonLibraryParser])
 * - ZIP archives via java.util.zip ([ZipLibraryParser])
 */
class DesktopBundleFileParser : BundleFileParser {

    private val jsonLibraryParser = JsonLibraryParser()
    private val zipParser = ZipLibraryParser(jsonLibraryParser)
    private val xlsxCompiler = XlsxLibraryCompiler()

    override fun parseFile(filePath: String, format: ImportFormat): ParsedBundleResult? {
        return when (format) {
            ImportFormat.JSON -> parseJsonFile(filePath)
            ImportFormat.ZIP -> parseZipFile(filePath)
            ImportFormat.XLSX -> parseXlsxFile(filePath)
            else -> null // CSV/TSV, TEXT, HTML handled in commonMain
        }
    }

    override fun parseStream(format: ImportFormat, inputStream: InputStream): ParsedBundleResult? {
        return when (format) {
            ImportFormat.JSON -> {
                try {
                    val bundle = inputStream.use { jsonLibraryParser.parse(it) }
                    ParsedBundleResult(bundle = bundle, format = ImportFormat.JSON)
                } catch (e: Exception) {
                    null
                }
            }
            ImportFormat.ZIP -> {
                try {
                    val result = inputStream.use { zipParser.parse(it) }
                    ParsedBundleResult(
                        bundle = result.bundle,
                        format = ImportFormat.ZIP,
                        zipExtractDir = result.extractDir.absolutePath,
                        manifest = null,
                    )
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    private fun parseJsonFile(filePath: String): ParsedBundleResult? {
        return try {
            val file = File(filePath)
            val bundle = jsonLibraryParser.parse(file.inputStream())
            ParsedBundleResult(bundle = bundle, format = ImportFormat.JSON)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseZipFile(filePath: String): ParsedBundleResult? {
        return try {
            val file = File(filePath)
            val result = file.inputStream().use { zipParser.parse(it) }
            ParsedBundleResult(
                bundle = result.bundle,
                format = ImportFormat.ZIP,
                zipExtractDir = result.extractDir.absolutePath,
                manifest = null,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseXlsxFile(filePath: String): ParsedBundleResult? {
        return try {
            val bundle = xlsxCompiler.compile(filePath)
            ParsedBundleResult(bundle = bundle, format = ImportFormat.XLSX)
        } catch (e: Exception) {
            null
        }
    }
}
