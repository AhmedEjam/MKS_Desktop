package com.ahmedyejam.mks.ui.data

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DataToolsUiState(
    val isWorking: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class DataToolsViewModel @Inject constructor(
    private val exportManager: com.ahmedyejam.mks.data.repository.ExportManager
) : ViewModel() {
    private val _state = MutableStateFlow(DataToolsUiState())
    val state: StateFlow<DataToolsUiState> = _state.asStateFlow()

    fun exportFullLibrary(outputStream: java.io.OutputStream) {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, error = null, message = null) }
            val result = exportManager.exportAllToZip(outputStream)
            _state.update {
                it.copy(
                    isWorking = false,
                    message = if (result.success) "Export completed successfully." else null,
                    error = if (!result.success) result.errorMessage else null
                )
            }
        }
    }
}
