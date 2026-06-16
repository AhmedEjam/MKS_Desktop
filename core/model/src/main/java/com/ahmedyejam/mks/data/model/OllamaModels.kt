package com.ahmedyejam.mks.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val system: String? = null,
    val stream: Boolean = false,
    val options: Map<String, Any>? = null,
    val images: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class OllamaResponse(
    val model: String,
    @param:Json(name = "created_at") val createdAt: String,
    val response: String,
    val done: Boolean
)
