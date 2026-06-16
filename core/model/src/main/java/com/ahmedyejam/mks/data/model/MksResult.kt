package com.ahmedyejam.mks.data.model

/**
 * A generic sealed class for operations that can fail.
 */
sealed class MksResult<out T> {
    data class Success<out T>(val data: T) : MksResult<T>()

    data class Error(
        val message: String,
        val exception: Throwable? = null,
        val code: String? = null
    ) : MksResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun exceptionOrNull(): Throwable? = when (this) {
        is Success -> null
        is Error -> exception
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
            exception = exceptionOrNull()
        )
    }
}
