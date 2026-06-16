package com.ahmedyejam.mks.data.model

import kotlinx.serialization.Serializable

/**
 * A generic sealed class for operations that can fail.
 */
@Serializable
sealed class MksResult<out T> {
    @Serializable
    data class Success<out T>(val data: T) : MksResult<T>()

    @Serializable
    data class Error(
        val message: String,
        val errorMessage: String? = null,
        val code: String? = null
    ) : MksResult<Nothing>() {
        constructor(message: String, exception: Throwable?, code: String? = null) : this(
            message = message,
            errorMessage = exception?.message,
            code = code
        )
    }

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun exceptionOrNull(): Throwable? = when (this) {
        is Success -> null
        is Error -> errorMessage?.let { RuntimeException(it) }
    }
}

/**
 * Extension to convert a standard Result to MksResult
 */
fun <T> Result<T>.toMksResult(): MksResult<T> {
    return when {
        isSuccess -> MksResult.Success(getOrThrow())
        else -> MksResult.Error(
            message = exceptionOrNull()?.message ?: "Unknown error",
            errorMessage = exceptionOrNull()?.message
        )
    }
}
