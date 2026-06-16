package com.ahmedyejam.mks.util

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class ImportSizeLimitExceededException(message: String) : IllegalStateException(message)

fun InputStream.copyToWithLimit(output: OutputStream, maxBytes: Long): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read == -1) break
        total += read.toLong()
        if (total > maxBytes) {
            throw ImportSizeLimitExceededException("Input exceeds ${maxBytes / (1024 * 1024)} MB limit.")
        }
        output.write(buffer, 0, read)
    }
    return total
}

fun InputStream.readTextWithLimit(maxBytes: Long): String {
    val output = ByteArrayOutputStream()
    copyToWithLimit(output, maxBytes)
    return output.toString(Charsets.UTF_8.name())
}

fun File.readTextWithLimit(maxBytes: Long): String {
    inputStream().use { input -> return input.readTextWithLimit(maxBytes) }
}
