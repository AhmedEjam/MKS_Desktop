package com.ahmedyejam.mks.ui.importer

import com.ahmedyejam.mks.data.importer.model.ImportFormat
import com.ahmedyejam.mks.data.importer.model.ImportPreviewDto
import com.ahmedyejam.mks.data.importer.model.ImportResult
import com.ahmedyejam.mks.data.importer.model.MergeStrategy
import com.ahmedyejam.mks.data.importer.repository.ImportLibraryManager
import com.ahmedyejam.mks.data.model.MksResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * KMP ImportViewModel — manages the import state machine:
 * Idle → Loading → Review → Success/Error
 *
 * Delegates all parsing and persistence to [ImportLibraryManager].
 * Uses filePath (String) instead of Android Uri.
 */
class ImportViewModel(
    private val importManager: ImportLibraryManager,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) {
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState

    fun detectFormat(filePath: String): ImportFormat {
        return importManager.detectFormat(filePath)
    }

    fun getImportPreview(filePath: String, targetBookId: Long? = null, targetQuizId: Long? = null) {
        coroutineScope.launch {
            _importState.value = ImportState.Loading(0.1f, "Preparing preview...")
            try {
                val preview = importManager.getImportPreview(filePath)
                _importState.value = ImportState.Review(preview, filePath, targetBookId, targetQuizId)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Failed to load preview")
            }
        }
    }

    fun importLibrary(
        filePath: String,
        strategy: MergeStrategy = MergeStrategy.SKIP_EXISTING,
        targetBookId: Long? = null,
        targetQuizId: Long? = null,
        allowInsecureRemoteImages: Boolean = false,
        activeWorkspaceId: Long? = null,
    ) {
        coroutineScope.launch {
            _importState.value = ImportState.Loading(0f, "Starting import...")
            try {
                val result = importManager.importLibrary(
                    filePath = filePath,
                    strategy = strategy,
                    targetBookId = targetBookId,
                    targetQuizId = targetQuizId,
                    allowInsecureRemoteImages = allowInsecureRemoteImages,
                    activeWorkspaceId = activeWorkspaceId,
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
            val filePath: String,
            val targetBookId: Long? = null,
            val targetQuizId: Long? = null,
        ) : ImportState()
        data class Success(val result: ImportResult) : ImportState()
        data class Error(val message: String) : ImportState()
    }
}
