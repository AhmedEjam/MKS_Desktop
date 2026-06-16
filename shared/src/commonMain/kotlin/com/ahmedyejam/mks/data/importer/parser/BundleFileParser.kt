package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.dto.LibraryBundleDto
import com.ahmedyejam.mks.data.importer.dto.ManifestDto
import com.ahmedyejam.mks.data.importer.model.ImportFormat

/**
 * Result of parsing a file into a [LibraryBundleDto].
 *
 * For ZIP imports, [zipExtractDir] holds the temporary extraction directory
 * that must be cleaned up after import completes.
 */
data class ParsedBundleResult(
    val bundle: LibraryBundleDto,
    val format: ImportFormat,
    val zipExtractDir: String? = null,
    val manifest: ManifestDto? = null,
)

/**
 * Platform-specific file parser that converts a file path into a [LibraryBundleDto].
 *
 * CommonMain handles text-based formats (CSV/TSV, TEXT, HTML) directly via
 * [CsvParser], [TextQuestionParser], [HtmlQuestionParser]. This interface
 * covers formats requiring JVM-only dependencies (XLSX via Apache POI,
 * ZIP via java.util.zip, JSON library bundles via kotlinx.serialization).
 *
 * Android and Desktop each provide their own actual implementation.
 */
interface BundleFileParser {
    /** Parse a file at [filePath] into a bundle. Returns null if format is not handled. */
    fun parseFile(filePath: String, format: ImportFormat): ParsedBundleResult?

    /** Parse an InputStream for JSON/ZIP formats. Used for preview pipeline. */
    fun parseStream(format: ImportFormat, inputStream: java.io.InputStream): ParsedBundleResult?
}
