package com.ahmedyejam.mks.data.error

import com.ahmedyejam.mks.data.model.MksResult
import com.ahmedyejam.mks.util.MksLogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A central handler for surfacing background or unhandled errors to the UI.
 */
@Singleton
class GlobalErrorHandler @Inject constructor() {
    private val _errorFlow = MutableSharedFlow<MksResult.Error>(extraBufferCapacity = 1)
    val errorFlow = _errorFlow.asSharedFlow()

    fun handleError(error: MksResult.Error) {
        MksLogger.e("GlobalErrorHandler", "Error received: ${error.message}", error.exception)
        _errorFlow.tryEmit(error)
    }

    fun handleError(message: String, exception: Throwable? = null, code: String? = null) {
        handleError(MksResult.Error(message, exception, code))
    }

    fun handleError(exception: Throwable) {
        handleError(exception.message ?: "An unexpected error occurred", exception)
    }
}
