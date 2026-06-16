package com.ahmedyejam.mks.ui.scanner

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject



import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.repository.KnowledgeRepository
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val knowledgeRepository: KnowledgeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun onImageCaptured(bitmap: Bitmap, quizId: Long) {
        if (_uiState.value is ScannerUiState.Processing) return
        
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Processing
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val visionText = recognizer.process(image).await()
                
                val recognizedText = visionText.text
                if (recognizedText.isBlank()) {
                    _uiState.value = ScannerUiState.Error("No text detected. Please ensure the camera is focused on the text.")
                    return@launch
                }

                val questions = withContext(Dispatchers.Default) {
                    parseTextToQuestions(recognizedText, quizId)
                }
                if (questions.isEmpty()) {
                    _uiState.value = ScannerUiState.Error("Could not parse any questions from the text.")
                } else {
                    _uiState.value = ScannerUiState.Success(questions)
                }
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error(e.message ?: "Unknown error during text recognition")
            }
        }
    }

    private fun parseTextToQuestions(text: String, quizId: Long): List<QuestionEntity> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val questions = mutableListOf<QuestionEntity>()
        
        var currentQuestionText = ""
        var currentOptions = mutableListOf<String>()
        var currentCorrectAnswers = mutableListOf<Int>()
        
        // Regex to match question starts like "1.", "1)", "Q1:", "Question 1:"
        val questionStartRegex = Regex("""^(?:Q(?:uestion)?\s*)?(\d+)[\.\)\:]\s+(.*)""", RegexOption.IGNORE_CASE)
        // Regex to match option starts like "A.", "a)", "1.", "(1)"
        val optionStartRegex = Regex("""^([\*])?\s*([A-Ea-e]|[1-5])[\.\)\-\s]\s*(.*)""")
        val bulletOptionRegex = Regex("""^([\*])?\s*[\-\•]\s+(.*)""")

        for (line in lines) {
            val qMatch = questionStartRegex.find(line)
            if (qMatch != null) {
                if (currentQuestionText.isNotBlank()) {
                    questions.add(createQuestion(quizId, currentQuestionText, currentOptions, currentCorrectAnswers))
                }
                currentQuestionText = qMatch.groupValues[2]
                currentOptions = mutableListOf()
                currentCorrectAnswers = mutableListOf()
                continue
            }

            val oMatch = optionStartRegex.find(line) ?: bulletOptionRegex.find(line)
            if (oMatch != null) {
                val isCorrectMarked = oMatch.groupValues[1] == "*"
                val optionContent = if (oMatch.groupValues.size > 2) {
                    if (oMatch.groupValues.size > 3) oMatch.groupValues[3] else oMatch.groupValues[2]
                } else ""
                
                if (isCorrectMarked) {
                    currentCorrectAnswers.add(currentOptions.size)
                }
                currentOptions.add(optionContent)
                continue
            }

            if (currentOptions.isEmpty()) {
                currentQuestionText = if (currentQuestionText.isEmpty()) line else "$currentQuestionText $line"
            } else {
                val lastIdx = currentOptions.size - 1
                currentOptions[lastIdx] = "${currentOptions[lastIdx]} $line"
            }
        }

        if (currentQuestionText.isNotBlank()) {
            questions.add(createQuestion(quizId, currentQuestionText, currentOptions, currentCorrectAnswers))
        }

        if (questions.isEmpty() && text.isNotBlank()) {
            val blocks = text.split("\n\n").filter { it.isNotBlank() }
            return blocks.map { block ->
                val blockLines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
                createQuestion(quizId, blockLines.firstOrNull() ?: "Empty Question", blockLines.drop(1), emptyList())
            }
        }

        return questions
    }

    private fun createQuestion(quizId: Long, text: String, options: List<String>, correctAnswers: List<Int>): QuestionEntity {
        val finalOptions = if (options.isEmpty()) listOf("Option A", "Option B") else options.map { it.trim() }
        val finalCorrect = if (correctAnswers.isEmpty()) listOf(0) else correctAnswers.filter { it < finalOptions.size }
        return QuestionEntity(
            externalId = java.util.UUID.randomUUID().toString(),
            quizId = quizId,
            text = text.trim(),
            type = if (finalCorrect.size > 1) QuestionType.MULTIPLE_CHOICE else QuestionType.SINGLE_CHOICE,
            options = finalOptions,
            correctAnswers = finalCorrect,
            explanation = "Imported from camera"
        )
    }

    fun updateQuestion(index: Int, updatedQuestion: QuestionEntity) {
        val currentState = _uiState.value
        if (currentState is ScannerUiState.Success) {
            val updatedList = currentState.questions.toMutableList()
            updatedList[index] = updatedQuestion
            _uiState.value = ScannerUiState.Success(updatedList)
        }
    }

    fun deleteQuestion(index: Int) {
        val currentState = _uiState.value
        if (currentState is ScannerUiState.Success) {
            val updatedList = currentState.questions.toMutableList()
            updatedList.removeAt(index)
            if (updatedList.isEmpty()) _uiState.value = ScannerUiState.Idle
            else _uiState.value = ScannerUiState.Success(updatedList)
        }
    }

    fun saveQuestions(questions: List<QuestionEntity>) {
        viewModelScope.launch {
            knowledgeRepository.insertQuestions(questions)
            _uiState.value = ScannerUiState.Saved
        }
    }
    
    fun reset() {
        _uiState.value = ScannerUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        recognizer.close()
    }
}

sealed class ScannerUiState {
    object Idle : ScannerUiState()
    object Processing : ScannerUiState()
    data class Success(val questions: List<QuestionEntity>) : ScannerUiState()
    data class Error(val message: String) : ScannerUiState()
    object Saved : ScannerUiState()
}
