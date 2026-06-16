package com.ahmedyejam.mks.ui.importer


import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.importer.model.ImportFormat
import com.ahmedyejam.mks.data.importer.model.ImportPreviewDto
import com.ahmedyejam.mks.data.importer.model.ImportResult
import com.ahmedyejam.mks.data.importer.model.MergeStrategy
import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.data.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState

    fun detectFormat(uri: Uri): ImportFormat {
        return quizRepository.detectFormat(uri)
    }

    fun getImportPreview(uri: Uri, targetBookId: Long? = null, targetQuizId: Long? = null) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading(0.1f, "Preparing preview...")
            try {
                val preview = quizRepository.getImportPreview(uri)
                    ?: throw Exception("Could not load preview")
                _importState.value = ImportState.Review(preview, uri, targetBookId, targetQuizId)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Failed to load preview")
            }
        }
    }

    fun importLibrary(
        uri: Uri,
        strategy: MergeStrategy = MergeStrategy.SKIP_EXISTING,
        targetBookId: Long? = null,
        targetQuizId: Long? = null,
        allowInsecureRemoteImages: Boolean = false,
        activeWorkspaceId: Long? = null
    ) {
        viewModelScope.launch {
            _importState.value = ImportState.Loading(0f, "Starting import...")
            try {
                val result = quizRepository.importFromUri(
                    uri = uri,
                    strategy = strategy,
                    targetBookId = targetBookId,
                    targetQuizId = targetQuizId,
                    allowInsecureRemoteImages = allowInsecureRemoteImages,
                    activeWorkspaceId = activeWorkspaceId
                ) { progress, message ->
                    _importState.value = ImportState.Loading(progress, message)
                }

                when (result) {
                    is MksResult.Success -> {
                        _importState.value = ImportState.Success(result.data)
                    }

                    is MksResult.Error -> {
                        _importState.value = ImportState.Error(result.message)
                    }

                    null -> {
                        _importState.value = ImportState.Error("Import manager not available")
                    }
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Critical import failure")
            }
        }
    }

    fun resetState() {
        _importState.value = ImportState.Idle
    }

    sealed class ImportState {
        object Idle : ImportState()
        data class Loading(val progress: Float, val message: String) : ImportState()
        data class Review(
            val preview: ImportPreviewDto,
            val uri: Uri,
            val targetBookId: Long? = null,
            val targetQuizId: Long? = null
        ) : ImportState()
        data class Success(val result: ImportResult) : ImportState()
        data class Error(val message: String) : ImportState()
    }
}
