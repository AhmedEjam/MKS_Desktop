package com.ahmedyejam.mks.data.importer.validation

import com.ahmedyejam.mks.data.importer.dto.BookDto
import com.ahmedyejam.mks.data.importer.dto.LibraryBundleDto
import com.ahmedyejam.mks.data.importer.dto.OptionDto
import com.ahmedyejam.mks.data.importer.dto.QuestionDto
import com.ahmedyejam.mks.data.importer.dto.QuizDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportValidatorTest {
    @Test
    fun invalidQuestionsAreSkippedWithLineAndReasonReport() {
        val bundle =
            LibraryBundleDto(
                books = listOf(BookDto(id = "book-1", title = "Book")),
                quizzes =
                    listOf(
                        QuizDto(
                            id = "quiz-1",
                            bookId = "book-1",
                            title = "Quiz",
                            questions =
                                listOf(
                                    QuestionDto(
                                        id = "valid-1",
                                        stem = "Valid?",
                                        options = listOf(OptionDto("A", "Yes"), OptionDto("B", "No")),
                                        correct = listOf("A"),
                                        sourceLine = 10,
                                    ),
                                    QuestionDto(
                                        id = "invalid-1",
                                        stem = "",
                                        options = listOf(OptionDto("A", "Yes")),
                                        correct = listOf("Z"),
                                        sourceLine = 11,
                                    ),
                                ),
                        ),
                    ),
            )

        val result = ImportValidator().validate(bundle)

        assertTrue(result.isValid)
        val sanitized = result.sanitizedBundle!!
        assertEquals(1, result.skippedRecordsCount)
        assertEquals(1, sanitized.quizzes.single().questions.size)
        assertEquals("valid-1", sanitized.quizzes.single().questions.single().id)
        assertEquals(11, result.skippedRecords.single().line)
        assertTrue(result.skippedRecords.single().reason.contains("blank question stem"))
        assertTrue(result.skippedRecords.single().reason.contains("unknown correct option ID 'Z'"))
        assertTrue(
            result.warnings.any { warning ->
                warning.message.contains("line/row 11") && warning.details.orEmpty().contains("line=11")
            },
        )
    }

    @Test
    fun duplicateQuestionIdInSameQuizIsSkipped() {
        val question =
            QuestionDto(
                id = "duplicate",
                stem = "Q?",
                options = listOf(OptionDto("A", "Answer")),
                correct = listOf("A"),
            )
        val bundle =
            LibraryBundleDto(
                books = listOf(BookDto(id = "book-1", title = "Book")),
                quizzes =
                    listOf(
                        QuizDto(
                            id = "quiz-1",
                            bookId = "book-1",
                            title = "Quiz",
                            questions = listOf(question, question.copy(stem = "Q2?", sourceLine = 12)),
                        ),
                    ),
            )

        val result = ImportValidator().validate(bundle)

        assertEquals(1, result.skippedRecordsCount)
        assertEquals(1, result.sanitizedBundle!!.quizzes.single().questions.size)
        assertTrue(result.skippedRecords.single().reason.contains("duplicate question ID"))
    }
}
