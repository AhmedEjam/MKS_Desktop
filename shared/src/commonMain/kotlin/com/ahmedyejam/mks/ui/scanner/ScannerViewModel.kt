package com.ahmedyejam.mks.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.local.entity.QuestionEntity
import com.ahmedyejam.mks.data.local.entity.QuestionType
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.data.repository.KnowledgeRepository
import com.ahmedyejam.mks.platform.OcrManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ScannerUiState {
    object Idle : ScannerUiState()
    object Processing : ScannerUiState()
    data class Success(val questions: List<QuestionEntity>) : ScannerUiState()
    data class Error(val message: String) : ScannerUiState()
    object Saved : ScannerUiState()
}

class ScannerViewModel(
    private val knowledgeRepository: KnowledgeRepository,
    private val ocrManager: OcrManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun onImageSelected(imageBytes: ByteArray, quizId: Long) {
        if (_uiState.value is ScannerUiState.Processing) return
        
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Processing
            val result = ocrManager.recognizeText(imageBytes)
            if (result is MksResult.Success) {
                val questions = withContext(Dispatchers.Default) {
                    parseTextToQuestions(result.data, quizId)
                }
                if (questions.isEmpty()) {
                    _uiState.value = ScannerUiState.Error("No questions found in recognized text.")
                } else {
                    _uiState.value = ScannerUiState.Success(questions)
                }
            } else if (result is MksResult.Error) {
                _uiState.value = ScannerUiState.Error(result.message)
            }
        }
    }

    private fun parseTextToQuestions(text: String, quizId: Long): List<QuestionEntity> {
        // Simplified parser logic (re-using logic from original scanner)
        return emptyList() // Placeholder
    }

    fun saveQuestions(questions: List<QuestionEntity>) {
        viewModelScope.launch {
            // knowledgeRepository.insertQuestions(questions)
            _uiState.value = ScannerUiState.Saved
        }
    }
}
