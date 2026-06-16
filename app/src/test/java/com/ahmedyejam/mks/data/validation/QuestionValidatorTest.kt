package com.ahmedyejam.mks.data.validation

import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuestionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionValidatorTest {
    @Test
    fun testValidQuestion() {
        val q =
            QuestionEntity(
                externalId = "test",
                quizId = 1L,
                text = "What is 2+2?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf("3", "4"),
                correctAnswers = listOf(1),
            )
        assertTrue(QuestionValidator.validate(q).isValid)
    }

    @Test
    fun testBlankStem() {
        val q =
            QuestionEntity(
                externalId = "test",
                quizId = 1L,
                text = " ",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf("3", "4"),
                correctAnswers = listOf(1),
            )
        assertFalse(QuestionValidator.validate(q).isValid)
    }

    @Test
    fun testInsufficientOptions() {
        val q =
            QuestionEntity(
                externalId = "test",
                quizId = 1L,
                text = "What is 2+2?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf("4"),
                correctAnswers = listOf(0),
            )
        assertFalse(QuestionValidator.validate(q).isValid)
    }

    @Test
    fun testSingleChoiceMultipleAnswers() {
        val q =
            QuestionEntity(
                externalId = "test",
                quizId = 1L,
                text = "What is 2+2?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf("3", "4"),
                correctAnswers = listOf(0, 1),
            )
        assertFalse(QuestionValidator.validate(q).isValid)
    }
}
