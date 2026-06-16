package com.ahmedyejam.mks.ui.navigation

import java.net.URLEncoder

object MksRouteBuilders {
    fun quiz(quizId: Long, sessionId: Long? = null): String {
        return if (sessionId != null) {
            "quiz/$quizId?sessionId=$sessionId"
        } else {
            "quiz/$quizId"
        }
    }

    fun category(category: String): String {
        val encoded = URLEncoder.encode(category, "UTF-8").replace("+", "%20")
        return "category/$encoded"
    }

    fun adaptive(type: String, id: String): String {
        val encodedId = URLEncoder.encode(id, "UTF-8").replace("+", "%20")
        return "adaptive/$type/$encodedId"
    }
}
