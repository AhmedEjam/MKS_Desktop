package com.ahmedyejam.mks.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val images: List<String>? = null,
    val options: Map<String, String>? = null
)

@Serializable
data class OllamaResponse(
    val model: String? = null,
    val createdAt: String? = null,
    val response: String? = null,
    val done: Boolean? = null
)

data class OllamaConnectionResult(
    val success: Boolean,
    val message: String? = null
)
