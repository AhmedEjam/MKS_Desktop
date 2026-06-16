package com.ahmedyejam.mks.util

actual object MksLogger {
    private const val MAX_MESSAGE_LENGTH = 600

    actual fun d(tag: String, message: String, throwable: Throwable?) {
        println("[DEBUG] $tag: ${message.take(MAX_MESSAGE_LENGTH)}")
        throwable?.printStackTrace()
    }

    actual fun i(tag: String, message: String) {
        println("[INFO] $tag: ${message.take(MAX_MESSAGE_LENGTH)}")
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        System.err.println("[WARN] $tag: ${message.take(MAX_MESSAGE_LENGTH)}")
        throwable?.printStackTrace(System.err)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        System.err.println("[ERROR] $tag: ${message.take(MAX_MESSAGE_LENGTH)}")
        throwable?.printStackTrace(System.err)
    }
}
