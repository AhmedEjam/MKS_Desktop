package com.ahmedyejam.mks.data.importer.parser

import android.content.Context
import android.net.Uri
import com.ahmedyejam.mks.data.local.entity.CourseSlideEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.util.UUID

class PptxSlideParser {
    suspend fun parse(
        context: Context,
        uri: Uri,
        courseId: Long,
        startIndex: Int = 0,
    ): List<CourseSlideEntity> {
        return withContext(Dispatchers.IO) {
            val result = mutableListOf<CourseSlideEntity>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val ppt = XMLSlideShow(inputStream)
                var order = startIndex
                val now = System.currentTimeMillis()

                for (slide in ppt.slides) {
                    val title = slide.title ?: ""

                    // Extract text from text shapes, excluding the title shape
                    val bodyParts = mutableListOf<String>()
                    for (shape in slide.shapes) {
                        if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                            // POI has placeholder checking
                            if (shape.textType != org.apache.poi.sl.usermodel.Placeholder.TITLE &&
                                shape.textType != org.apache.poi.sl.usermodel.Placeholder.CENTERED_TITLE
                            ) {
                                val text = shape.text?.trim()
                                if (!text.isNullOrEmpty()) {
                                    bodyParts.add(text)
                                }
                            }
                        }
                    }
                    val body = bodyParts.joinToString("\n\n").trim()

                    // Extract notes
                    val notesSlide = slide.notes
                    val notesParts = mutableListOf<String>()
                    if (notesSlide != null) {
                        for (shape in notesSlide.shapes) {
                            if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                                // Exclude header/footer placeholders typically on notes
                                if (shape.textType == org.apache.poi.sl.usermodel.Placeholder.BODY) {
                                    val text = shape.text?.trim()
                                    if (!text.isNullOrEmpty()) {
                                        notesParts.add(text)
                                    }
                                }
                            }
                        }
                    }
                    val notes = notesParts.joinToString("\n\n").trim().takeIf { it.isNotEmpty() }

                    // We add a slide even if it's empty to preserve structure
                    result.add(
                        CourseSlideEntity(
                            externalId = UUID.randomUUID().toString(),
                            courseId = courseId,
                            title = title,
                            body = body,
                            speakerNotes = notes,
                            orderIndex = order++,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                }
            }
            result
        }
    }
}
