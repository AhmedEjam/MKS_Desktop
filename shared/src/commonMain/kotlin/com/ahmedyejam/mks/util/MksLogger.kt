package com.ahmedyejam.mks.util

/**
 * Multiplatform logging wrapper.
 *
 * Android: delegates to android.util.Log.
 * Desktop: delegates to println (stderr for errors).
 */
expect object MksLogger {
    fun d(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
