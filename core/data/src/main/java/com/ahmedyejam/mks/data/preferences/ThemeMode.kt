package com.ahmedyejam.mks.data.preferences

object MksThemeModes {
    const val DAWN = "DAWN"
    const val FOREST = "FOREST"
    const val MIDNIGHT = "MIDNIGHT"
    const val LAVENDER = "LAVENDER"
    const val PLAIN_LIGHT = "PLAIN_LIGHT"
    const val PLAIN_DARK = "PLAIN_DARK"
    const val SYSTEM = "SYSTEM"

    fun normalize(value: String?): String =
        when (value?.uppercase()) {
            DAWN, "SUNSET" -> DAWN
            FOREST -> FOREST
            MIDNIGHT, "NORD", "CYBERPUNK" -> MIDNIGHT
            LAVENDER, "OCEAN", "RETRO" -> LAVENDER
            PLAIN_LIGHT, "LIGHT" -> PLAIN_LIGHT
            PLAIN_DARK, "DARK" -> PLAIN_DARK
            SYSTEM, "PLAIN_SYSTEM" -> SYSTEM
            else -> DAWN
        }
}

fun normalizeMksThemeMode(value: String?): String = MksThemeModes.normalize(value)
