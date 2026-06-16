package com.ahmedyejam.mks.data.validation

import com.ahmedyejam.mks.data.local.entity.SessionEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStateValidatorTest {
    @Test
    fun uninitializedQuestionSequenceIsRepairable() {
        val session =
            SessionEntity(
                quizId = 1L,
                label = "New session",
            )

        val result = SessionStateValidator.validate(session)

        assertFalse(result.isValid)
        assertTrue(result.canBeRepaired)
    }

    @Test
    fun currentIndexOutsideInitializedSequenceIsRepairable() {
        val session =
            SessionEntity(
                quizId = 1L,
                label = "Existing session",
                questionIds = listOf(10L, 11L),
                currentQuestionIndex = 3,
            )

        val result = SessionStateValidator.validate(session)

        assertFalse(result.isValid)
        assertTrue(result.canBeRepaired)
    }

    @Test
    fun initializedSequenceWithValidIndexIsValid() {
        val session =
            SessionEntity(
                quizId = 1L,
                label = "Existing session",
                questionIds = listOf(10L, 11L),
                currentQuestionIndex = 1,
            )

        val result = SessionStateValidator.validate(session)

        assertTrue(result.isValid)
    }
}
