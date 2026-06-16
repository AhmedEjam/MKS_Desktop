package com.ahmedyejam.mks.data.importer.parser

import android.content.Context
import com.ahmedyejam.mks.data.exchange.v7.MksExchangeV7Archive
import com.ahmedyejam.mks.data.importer.dto.LibraryBundleDto
import com.ahmedyejam.mks.data.importer.dto.ManifestDto
import com.ahmedyejam.mks.data.importer.security.ImportLimits
import com.ahmedyejam.mks.util.copyToWithLimit
import com.ahmedyejam.mks.util.readTextWithLimit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

class ZipLibraryParser(
    private val context: Context,
    private val jsonParser: JsonLibraryParser,
) {
    companion object {
        private const val MAX_ENTRIES = ImportLimits.MAX_ZIP_ENTRIES
        private const val MAX_SINGLE_FILE_SIZE = ImportLimits.MAX_ZIP_SINGLE_UNCOMPRESSED_BYTES
        private const val MAX_TOTAL_SIZE = ImportLimits.MAX_ZIP_TOTAL_UNCOMPRESSED_BYTES
        private const val MAX_COMPRESSED_SIZE = ImportLimits.MAX_ZIP_COMPRESSED_BYTES
    }

    private val json = Json { ignoreUnknownKeys = true }

    data class ZipResult(
        val bundle: LibraryBundleDto,
        val manifest: ManifestDto?,
        val libraryDir: File,
        val rootDir: File,
    )

    fun parse(inputStream: InputStream): ZipResult {
        val tempDir = File(context.cacheDir, "import_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val tempZipFile = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}.zip")

        try {
            // Save inputStream to a temporary file because Zip4j ZipFile works with Files.
            tempZipFile.outputStream().use { output ->
                inputStream.copyToWithLimit(output, MAX_COMPRESSED_SIZE)
            }

            ZipFile(tempZipFile).use { zipFile ->
                if (zipFile.isEncrypted) {
                    zipFile.setPassword("mks_secure_bundle_2024".toCharArray())

                    // Pre-extraction validation and individual file extraction.
                    var totalBytes = 0L
                    var totalEntries = 0
                    val canonicalTempDir = tempDir.canonicalPath

                    zipFile.fileHeaders.forEach { header ->
                        totalEntries++
                        if (totalEntries > MAX_ENTRIES) {
                            throw IllegalStateException("Too many entries in ZIP archive")
                        }

                        if (header.uncompressedSize > MAX_SINGLE_FILE_SIZE) {
                            throw IllegalStateException("Zip entry '${header.fileName}' exceeds maximum size")
                        }

                        totalBytes += header.uncompressedSize
                        if (totalBytes > MAX_TOTAL_SIZE) {
                            throw IllegalStateException("Total uncompressed size of ZIP archive exceeds maximum")
                        }

                        if (header.fileName.contains("..") || header.fileName.startsWith("/")) {
                            throw SecurityException("Illegal zip entry name detected: ${header.fileName}")
                        }

                        val destinationFile = File(tempDir, header.fileName)
                        val canonicalDestPath = destinationFile.canonicalPath
                        if (!canonicalDestPath.startsWith(canonicalTempDir + File.separator)) {
                            throw SecurityException("Illegal zip entry path detected: ${header.fileName}")
                        }

                        zipFile.extractFile(header, tempDir.absolutePath)
                    }
                } else {
                    extractPlainZip(tempZipFile, tempDir)
                }
            }

            val manifestJsonFile = tempDir.walkTopDown().find { it.name == "manifest.json" }
            val manifestText = manifestJsonFile?.readTextWithLimit(ImportLimits.MAX_TEXT_IMPORT_BYTES)
            val isSchema7Exchange =
                manifestText?.let { text ->
                    runCatching {
                        val root = json.parseToJsonElement(text) as? JsonObject
                        (
                            root?.get("format")?.jsonPrimitive?.content == "mks.exchange" &&
                                root["schemaVersion"]?.jsonPrimitive?.content?.toIntOrNull() == 7
                        )
                    }.getOrDefault(false)
                } ?: false

            if (isSchema7Exchange) {
                val bundle = MksExchangeV7Archive.readLegacyBundleFromDirectory(tempDir)
                val manifest =
                    manifestText.let {
                        json.decodeFromString(ManifestDto.serializer(), it)
                    }
                return ZipResult(bundle, manifest, tempDir, tempDir)
            }

            var libraryJsonFile =
                tempDir.walkTopDown()
                    .find { it.name == "library.json" || it.name == "bundle.json" || it.name == "data.json" }

            if (libraryJsonFile == null) {
                // Look for any non-manifest JSON file if the canonical names are absent.
                libraryJsonFile =
                    tempDir.walkTopDown()
                        .find { it.extension.lowercase() == "json" && it.name != "manifest.json" }
            }

            if (libraryJsonFile == null) throw IllegalStateException("No JSON library file found in ZIP")

            val bundle = libraryJsonFile.inputStream().use { jsonParser.parse(it) }
            val manifest =
                manifestText?.let {
                    json.decodeFromString(ManifestDto.serializer(), it)
                }

            return ZipResult(bundle, manifest, libraryJsonFile.parentFile ?: tempDir, tempDir)
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            throw e
        } finally {
            tempZipFile.delete()
        }
    }

    private fun extractPlainZip(
        zipFile: File,
        targetDir: File,
    ) {
        var totalBytes = 0L
        var totalEntries = 0
        val canonicalTempDir = targetDir.canonicalFile.toPath()
        ZipInputStream(zipFile.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                totalEntries++
                if (totalEntries > MAX_ENTRIES) throw IllegalStateException("Too many entries in ZIP archive")
                val entryName = entry.name.replace('\\', '/')
                if (entryName.contains("..") || entryName.startsWith("/")) {
                    throw SecurityException(
                        "Illegal zip entry name detected: $entryName",
                    )
                }
                if (entry.size > MAX_SINGLE_FILE_SIZE) throw IllegalStateException("Zip entry '$entryName' exceeds maximum size")
                val destinationFile = File(targetDir, entryName)
                val destinationPath = destinationFile.canonicalFile.toPath()
                if (!destinationPath.startsWith(canonicalTempDir)) throw SecurityException("Illegal zip entry path detected: $entryName")
                destinationFile.parentFile?.mkdirs()
                destinationFile.outputStream().use { output ->
                    val copied = zip.copyToWithLimit(output, MAX_SINGLE_FILE_SIZE)
                    totalBytes += copied
                    if (totalBytes > MAX_TOTAL_SIZE) throw IllegalStateException("Total uncompressed size of ZIP archive exceeds maximum")
                }
                zip.closeEntry()
            }
        }
    }
}
