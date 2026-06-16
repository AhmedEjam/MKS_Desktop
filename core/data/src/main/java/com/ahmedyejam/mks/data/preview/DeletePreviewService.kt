package com.ahmedyejam.mks.data.preview

import com.ahmedyejam.mks.data.local.dao.BookDao
import com.ahmedyejam.mks.data.local.dao.QuestionDao
import com.ahmedyejam.mks.data.local.dao.QuizDao
import com.ahmedyejam.mks.data.simulation.ChangeSimulationResult
import com.ahmedyejam.mks.data.simulation.SimulatedItem
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeletePreviewService @Inject constructor(
    private val bookDao: BookDao,
    private val quizDao: QuizDao,
    private val questionDao: QuestionDao,
) {
    suspend fun previewBookDeletion(bookId: Long): ChangeSimulationResult {
        val book = bookDao.getBookById(bookId) ?: return emptyResult("Delete Book")
        val quizzes = quizDao.getQuizzesByBookId(bookId).first()
        val questionCount = quizDao.getBookQuestionCount(bookId).first()

        return ChangeSimulationResult(
            title = "Delete Book: ${book.title}",
            summary = "This will delete the book, ${quizzes.size} quizzes, and $questionCount questions.",
            affectedBooks = 1,
            affectedQuizzes = quizzes.size,
            affectedQuestions = questionCount,
            deletedItems =
                listOf(SimulatedItem(bookId.toString(), "Book", book.title)) +
                    quizzes.map { SimulatedItem(it.id.toString(), "Quiz", it.title) },
        )
    }

    suspend fun previewQuizDeletion(quizId: Long): ChangeSimulationResult {
        val quiz = quizDao.getQuizById(quizId) ?: return emptyResult("Delete Quiz")
        val questionCount = questionDao.getQuestionsByQuizId(quizId).first().size

        return ChangeSimulationResult(
            title = "Delete Quiz: ${quiz.title}",
            summary = "This will delete the quiz and $questionCount questions.",
            affectedQuizzes = 1,
            affectedQuestions = questionCount,
            deletedItems = listOf(SimulatedItem(quizId.toString(), "Quiz", quiz.title)),
        )
    }

    private fun emptyResult(title: String) = ChangeSimulationResult(title, "Item not found.")
}
