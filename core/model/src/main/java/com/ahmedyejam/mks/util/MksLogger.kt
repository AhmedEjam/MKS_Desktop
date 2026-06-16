package com.ahmedyejam.mks.util

import android.util.Log
import com.ahmedyejam.mks.core.model.BuildConfig

/**
 * Small production-safe logging wrapper.
 *
 * Debug builds keep stack traces. Release builds log redacted messages only.
 */
object MksLogger {
    private const val MAX_MESSAGE_LENGTH = 600

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.d(tag, sanitize(message), throwable)
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(tag, sanitize(message))
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, sanitize(message), throwable)
        } else {
            Log.w(tag, sanitize(message))
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, sanitize(message), throwable)
        } else {
            Log.e(tag, sanitize(message))
        }
    }

    private fun sanitize(message: String): String {
        return message
            .replace(Regex("content://[^\\s]+"), "content://<redacted>")
            .replace(Regex("file://[^\\s]+"), "file://<redacted>")
            .replace(Regex("/[^\\s]+"), "/<redacted>")
            .take(MAX_MESSAGE_LENGTH)
    }
}
