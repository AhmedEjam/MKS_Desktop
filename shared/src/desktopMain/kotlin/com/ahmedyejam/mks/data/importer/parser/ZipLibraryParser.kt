package com.ahmedyejam.mks.data.importer.parser

import com.ahmedyejam.mks.data.importer.dto.LibraryBundleDto
import com.ahmedyejam.mks.data.importer.security.ImportLimits
import com.ahmedyejam.mks.util.copyToWithLimit
import com.ahmedyejam.mks.util.readTextWithLimit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

class ZipLibraryParser(
    private val jsonParser: JsonLibraryParser,
) {
    companion object {
        private const val MAX_ENTRIES = ImportLimits.MAX_ZIP_ENTRIES
        private const val MAX_SINGLE_FILE_SIZE = ImportLimits.MAX_ZIP_SINGLE_UNCOMPRESSED_BYTES
        private const val MAX_TOTAL_SIZE = ImportLimits.MAX_ZIP_TOTAL_UNCOMPRESSED_BYTES
        private const val MAX_COMPRESSED_SIZE = ImportLimits.MAX_ZIP_COMPRESSED_BYTES
    }

    data class ZipResult(val bundle: LibraryBundleDto, val extractDir: File)

    fun parse(inputStream: InputStream): ZipResult {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "mks_import_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            var totalBytes = 0L
            var totalEntries = 0
            val canonicalTempDir = tempDir.canonicalFile.toPath()

            ZipInputStream(inputStream).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) { zip.closeEntry(); continue }
                    totalEntries++
                    if (totalEntries > MAX_ENTRIES) throw IllegalStateException("Too many entries in ZIP")
                    val entryName = entry.name.replace('\\', '/')
                    if (entryName.contains("..") || entryName.startsWith("/"))
                        throw SecurityException("Illegal zip entry: $entryName")
                    if (entry.size > MAX_SINGLE_FILE_SIZE) throw IllegalStateException("Entry too large: $entryName")

                    val dest = File(tempDir, entryName)
                    val destPath = dest.canonicalFile.toPath()
                    if (!destPath.startsWith(canonicalTempDir))
                        throw SecurityException("Path traversal: $entryName")

                    dest.parentFile?.mkdirs()
                    dest.outputStream().use { output ->
                        val copied = zip.copyToWithLimit(output, MAX_SINGLE_FILE_SIZE)
                        totalBytes += copied
                        if (totalBytes > MAX_TOTAL_SIZE) throw IllegalStateException("Total size exceeds maximum")
                    }
                    zip.closeEntry()
                }
            }

            var libFile = tempDir.walkTopDown().find { it.name == "library.json" || it.name == "bundle.json" || it.name == "data.json" }
            if (libFile == null) libFile = tempDir.walkTopDown().find { it.extension.lowercase() == "json" && it.name != "manifest.json" }
            if (libFile == null) throw IllegalStateException("No JSON file found in ZIP")

            val bundle = libFile.inputStream().use { jsonParser.parse(it) }
            return ZipResult(bundle, tempDir)
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            throw e
        }
    }
}
