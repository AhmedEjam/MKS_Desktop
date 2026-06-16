package com.ahmedyejam.mks.data.local

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileManagerTest {
    private lateinit var fileManager: FileManager
    private lateinit var context: Context
    private lateinit var filesDir: File
    private lateinit var imagesDir: File

    @Before
    fun setup() {
        context = mockk()
        // Create a temporary directory for filesDir
        filesDir = Files.createTempDirectory("mks_files").toFile()
        imagesDir = File(filesDir, "images")
        imagesDir.mkdirs()

        every { context.filesDir } returns filesDir
    }

    @Test
    fun saveImage_RejectsPathOutsideImagesDir() {
        fileManager = FileManager(context)

        // Create a file in filesDir but NOT in imagesDir
        val sensitiveFile = File(filesDir, "sensitive.txt")
        sensitiveFile.writeText("secret data")

        val result = fileManager.saveImage(sensitiveFile.absolutePath)
        assertNull("Should reject path outside images directory", result)
    }

    @Test
    fun saveImage_AcceptsPathInsideImagesDir() {
        fileManager = FileManager(context)

        val imageFile = File(imagesDir, "test.png")
        imageFile.writeText("image data")

        val result = fileManager.saveImage(imageFile.absolutePath)
        assertEquals("Should accept path inside images directory", imageFile.absolutePath, result)
    }

    @Test
    fun getFile_RejectsPathOutsideImagesDir() {
        fileManager = FileManager(context)

        val sensitiveFile = File(filesDir, "sensitive.txt")
        sensitiveFile.writeText("secret data")

        val result = fileManager.getFile(sensitiveFile.absolutePath)
        assertNull("Should reject file access outside images directory", result)
    }

    @Test
    fun getFile_AcceptsPathInsideImagesDir() {
        fileManager = FileManager(context)

        val imageFile = File(imagesDir, "test.png")
        imageFile.writeText("image data")

        val result = fileManager.getFile(imageFile.absolutePath)
        assertNotNull("Should allow file access inside images directory", result)
        assertEquals(imageFile.absolutePath, result?.absolutePath)
    }

    @Test
    fun saveImage_RejectsSiblingDirectoryWithSamePrefix() {
        fileManager = FileManager(context)

        val siblingDir = File(filesDir, "images_evil")
        siblingDir.mkdirs()
        val siblingFile = File(siblingDir, "test.png")
        siblingFile.writeText("image data")

        val result = fileManager.saveImage(siblingFile.absolutePath)
        assertNull("Should reject sibling directories that only share the images prefix", result)
    }

    @Test
    fun getBase64Image_RejectsPathOutsideImagesDir() {
        fileManager = FileManager(context)

        val sensitiveFile = File(filesDir, "sensitive.png")
        sensitiveFile.writeText("not an app image")

        val result = fileManager.getBase64Image(sensitiveFile.absolutePath)
        assertEquals("Should not export files outside the images directory", "", result)
    }
}
