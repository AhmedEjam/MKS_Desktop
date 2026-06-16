package com.ahmedyejam.mks.data.preview

import com.ahmedyejam.mks.data.local.dao.QuestionCategoryDao
import com.ahmedyejam.mks.data.simulation.ChangeSimulationResult
import com.ahmedyejam.mks.data.simulation.SimulatedItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryMergePreviewService @Inject constructor(
    private val questionCategoryDao: QuestionCategoryDao,
) {
    suspend fun previewMerge(
        source: String,
        target: String,
    ): ChangeSimulationResult {
        val questionsFromSource = questionCategoryDao.getQuestionsByCategory(source)
        val questionsInTarget = questionCategoryDao.getQuestionsByCategory(target).map { it.id }.toSet()

        val toUpdate = questionsFromSource.filter { it.id !in questionsInTarget }
        val toSkip = questionsFromSource.filter { it.id in questionsInTarget }

        return ChangeSimulationResult(
            title = "Merge Category: $source -> $target",
            summary = "This will update ${toUpdate.size} questions and remove the '$source' category metadata.",
            affectedQuestions = toUpdate.size,
            updatedItems = toUpdate.map { SimulatedItem(it.id.toString(), "Question", it.text.take(80)) },
            skippedItems =
                toSkip.map {
                    SimulatedItem(
                        it.id.toString(),
                        "Question",
                        it.text.take(80),
                        reason = "Already has target category",
                    )
                },
        )
    }
}
