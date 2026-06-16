package com.ahmedyejam.mks.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmedyejam.mks.data.focus.FocusManager
import com.ahmedyejam.mks.data.preferences.DataStoreManager
import com.ahmedyejam.mks.data.repository.AssetRepository
import com.ahmedyejam.mks.data.repository.ExportManager
import com.ahmedyejam.mks.data.repository.OllamaRepository
import com.ahmedyejam.mks.data.repository.QuizRepository
import com.ahmedyejam.mks.data.repository.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportManager: ExportManager,
    private val workspaceRepository: WorkspaceRepository,
    private val quizRepository: QuizRepository,
    private val assetRepository: AssetRepository,
    val ollamaRepository: OllamaRepository,
    val dataStoreManager: DataStoreManager,
    val focusManager: FocusManager
) : ViewModel() {

    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage = _exportMessage.asStateFlow()
    
    private val _isResetting = MutableStateFlow(false)
    val isResetting = _isResetting.asStateFlow()

    fun clearExportMessage() {
        _exportMessage.value = null
    }

    fun exportAllData(stream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            _exportMessage.value = "Exporting data... please wait."
            try {
                val result = exportManager.exportAllToZip(stream)
                _exportMessage.value = if (result.success) {
                    "Export completed successfully."
                } else {
                    "Export failed: ${result.errorMessage}"
                }
            } catch (e: Exception) {
                _exportMessage.value = "Error during export: ${e.message}"
            } finally {
                _isExporting.value = false
                stream.close()
            }
        }
    }

    fun resetDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            _isResetting.value = true
            try {
                workspaceRepository.resetDatabase()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isResetting.value = false
            }
        }
    }

    fun clearCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            quizRepository.clearAllCategories()
        }
    }

    fun rebuildDerivedIndexes() {
        viewModelScope.launch(Dispatchers.IO) {
            assetRepository.rebuildDerivedIndexes()
        }
    }
}
