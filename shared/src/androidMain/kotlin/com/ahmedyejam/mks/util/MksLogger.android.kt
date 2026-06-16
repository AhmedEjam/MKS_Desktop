package com.ahmedyejam.mks.util

import android.util.Log

actual object MksLogger {
    private const val MAX_MESSAGE_LENGTH = 600

    actual fun d(tag: String, message: String, throwable: Throwable?) {
        Log.d(tag, sanitize(message), throwable)
    }

    actual fun i(tag: String, message: String) {
        Log.i(tag, sanitize(message))
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag, sanitize(message), throwable)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, sanitize(message), throwable)
    }

    private fun sanitize(message: String): String {
        return message
            .replace(Regex("content://[^\\s]+"), "content://<redacted>")
            .replace(Regex("file://[^\\s]+"), "file://<redacted>")
            .replace(Regex("/[^\\s]+"), "/<redacted>")
            .take(MAX_MESSAGE_LENGTH)
    }
}
