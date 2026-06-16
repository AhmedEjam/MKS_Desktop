package com.ahmedyejam.mks.data.preview

import com.ahmedyejam.mks.data.local.dao.QuestionDao
import com.ahmedyejam.mks.data.simulation.ChangeSimulationResult
import com.ahmedyejam.mks.data.simulation.SimulatedItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClearMarksPreviewService @Inject constructor(
    private val questionDao: QuestionDao,
) {
    suspend fun previewClearMarksForQuiz(quizId: Long): ChangeSimulationResult {
        val questions = questionDao.getMarkedQuestionsByQuiz(quizId)
        return ChangeSimulationResult(
            title = "Clear Marked Questions",
            summary = "This will clear marks from ${questions.size} questions in this quiz.",
            affectedQuestions = questions.size,
            updatedItems = questions.map { SimulatedItem(it.id.toString(), "Question", it.text.take(80), reason = it.markReason) },
        )
    }
}
