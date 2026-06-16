package com.ahmedyejam.mks.data.importer.security

import com.ahmedyejam.mks.util.ImportSizeLimitExceededException
import com.ahmedyejam.mks.util.copyToWithLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BoundedStreamsTest {
    @Test
    fun copyToWithLimitCopiesWhenInputIsUnderLimit() {
        val output = ByteArrayOutputStream()
        val copied = ByteArrayInputStream("hello".toByteArray()).copyToWithLimit(output, 5)

        assertEquals(5, copied)
        assertEquals("hello", output.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun copyToWithLimitThrowsWhenInputExceedsLimit() {
        val output = ByteArrayOutputStream()

        assertThrows(ImportSizeLimitExceededException::class.java) {
            ByteArrayInputStream("too large".toByteArray()).copyToWithLimit(output, 3)
        }
    }
}
