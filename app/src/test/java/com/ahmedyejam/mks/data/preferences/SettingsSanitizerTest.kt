package com.ahmedyejam.mks.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSanitizerTest {
    @Test
    fun testThemeSanitization() {
        assertEquals("DAWN", SettingsSanitizer.theme(null))
        assertEquals("DAWN", SettingsSanitizer.theme("INVALID"))
        assertEquals("MIDNIGHT", SettingsSanitizer.theme("midnight"))
        assertEquals("PLAIN_LIGHT", SettingsSanitizer.theme("plain_light"))
        assertEquals("PLAIN_DARK", SettingsSanitizer.theme("plain_dark"))
        assertEquals("SYSTEM", SettingsSanitizer.theme("plain_system"))
        assertEquals("PLAIN_LIGHT", SettingsSanitizer.theme("light"))
        assertEquals("PLAIN_DARK", SettingsSanitizer.theme("dark"))
        assertEquals("MIDNIGHT", SettingsSanitizer.theme("nord"))
        assertEquals("LAVENDER", SettingsSanitizer.theme("ocean"))
    }

    @Test
    fun testLanguageSanitization() {
        assertEquals("en", SettingsSanitizer.language(null))
        assertEquals("en", SettingsSanitizer.language("fr"))
        assertEquals("ar", SettingsSanitizer.language("AR"))
    }

    @Test
    fun testClamping() {
        assertEquals(0.5f, SettingsSanitizer.fontScale(0.1f), 0.01f)
        assertEquals(2.0f, SettingsSanitizer.fontScale(5.0f), 0.01f)
        assertEquals(300, SettingsSanitizer.autoAdvanceDelay(100))
        assertEquals(10000, SettingsSanitizer.autoAdvanceDelay(50000))
    }

    @Test
    fun testFilters() {
        val input = setOf("unanswered", "corrupt", "missed")
        val expected = setOf("unanswered", "missed")
        assertEquals(expected, SettingsSanitizer.includeFilters(input))
    }
}
