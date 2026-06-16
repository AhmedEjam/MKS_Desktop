package com.ahmedyejam.mks.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

inline fun <reified T> T.toJson(): String = json.encodeToString(this)

inline fun <reified T> String?.fromJson(defaultValue: T): T =
    if (this.isNullOrBlank()) defaultValue
    else try {
        json.decodeFromString(this)
    } catch (e: Exception) {
        defaultValue
    }
