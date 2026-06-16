package com.ahmedyejam.mks.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class MksRouteBuildersTest {
    @Test
    fun testQuizRoute() {
        assertEquals("quiz/123", MksRouteBuilders.quiz(123L))
        assertEquals("quiz/123?sessionId=456", MksRouteBuilders.quiz(123L, 456L))
    }

    @Test
    fun testCategoryRouteEncoding() {
        // Spaces and special characters should be encoded
        assertEquals("category/Medical%20Science", MksRouteBuilders.category("Medical Science"))
        assertEquals("category/%D8%A7%D9%84%D8%B7%D8%A8", MksRouteBuilders.category("الطب"))
    }

    @Test
    fun testAdaptiveRouteEncoding() {
        assertEquals("adaptive/CATEGORY/Medical%20Science", MksRouteBuilders.adaptive("CATEGORY", "Medical Science"))
    }
}
