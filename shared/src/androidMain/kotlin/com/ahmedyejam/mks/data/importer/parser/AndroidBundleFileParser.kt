package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.model.ImportFormat
import java.io.InputStream

/**
 * Android stub implementation of [BundleFileParser].
 *
 * The Android app still uses the legacy [ImportLibraryManager] in core/data
 * (Hilt-managed) for XLSX/ZIP/JSON imports. This stub is provided solely
 * to satisfy the Koin dependency graph so [ImportLibraryManager] in
 * shared/commonMain can be resolved. It returns null for all formats,
 * meaning the KMP ImportLibraryManager falls back to commonMain text
 * parsers — which is sufficient for the Android app's current use.
 *
 * When the Android import pipeline is fully migrated to KMP, this stub
 * will be replaced with a real implementation.
 */
class AndroidBundleFileParser : BundleFileParser {
    override fun parseFile(filePath: String, format: ImportFormat): ParsedBundleResult? = null
    override fun parseStream(format: ImportFormat, inputStream: InputStream): ParsedBundleResult? = null
}
