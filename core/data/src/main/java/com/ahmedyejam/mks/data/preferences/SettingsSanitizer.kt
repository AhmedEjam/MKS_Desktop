package com.ahmedyejam.mks.data.preferences

/**
 * Sanitizes and clamps user settings to prevent extreme values or corruption.
 */
object SettingsSanitizer {
    fun theme(value: String?): String = normalizeMksThemeMode(value)

    fun language(value: String?): String =
        when (value?.lowercase()) {
            "en", "ar" -> value.lowercase()
            else -> "en"
        }

    fun sortOption(value: String?): String =
        when (value?.uppercase()) {
            "TITLE", "LAST_EDIT", "LAST_STUDIED", "COMPLETION", "QUESTION_COUNT", "ACCURACY", "PROGRESS" -> value.uppercase()
            else -> "TITLE"
        }

    fun viewMode(value: String?): String =
        when (value?.uppercase()) {
            "GRID", "LIST" -> value.uppercase()
            else -> "LIST"
        }

    fun fontScale(value: Float?): Float = (value ?: 1.0f).coerceIn(0.5f, 2.0f)

    fun uiDensity(value: Float?): Float = (value ?: 1.0f).coerceIn(0.5f, 1.5f)

    fun autoAdvanceDelay(value: Int?): Int = (value ?: 500).coerceIn(300, 10000)

    fun quizTimerSeconds(value: Int?): Int = (value ?: 0).coerceIn(0, 7200)

    fun questionTimerSeconds(value: Int?): Int = (value ?: 0).coerceIn(0, 600)

    fun includeFilters(value: Set<String>?): Set<String> {
        val valid = setOf("unanswered", "missed", "marked", "categorized", "uncategorized")
        return (value ?: emptySet()).filter { it in valid }.toSet()
    }
}
