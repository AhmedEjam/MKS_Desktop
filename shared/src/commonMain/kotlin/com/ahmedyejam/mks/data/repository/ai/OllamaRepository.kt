package com.ahmedyejam.mks.data.repository.ai

import com.ahmedyejam.mks.data.model.OllamaConnectionResult
import com.ahmedyejam.mks.data.model.OllamaRequest
import com.ahmedyejam.mks.data.model.OllamaResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class OllamaRepository(private val client: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    fun generateCompletionStream(
        baseUrl: String,
        model: String,
        prompt: String,
        images: List<String> = emptyList(),
        apiKey: String? = null
    ): Flow<String> = flow {
        client.preparePost("$baseUrl/api/generate") {
            contentType(ContentType.Application.Json)
            if (apiKey != null) header("Authorization", "Bearer $apiKey")
            setBody(OllamaRequest(model = model, prompt = prompt, stream = true, images = images.takeIf { it.isNotEmpty() }))
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                try {
                    val res = json.decodeFromString<OllamaResponse>(line)
                    res.response?.let { emit(it) }
                    if (res.done == true) break
                } catch (e: Exception) { }
            }
        }
    }

    suspend fun testConnection(
        baseUrl: String,
        apiKey: String? = null
    ): OllamaConnectionResult {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/tags") {
                if (apiKey != null) header("Authorization", "Bearer $apiKey")
            }
            if (response.status.value in 200..299) {
                OllamaConnectionResult(true, "Connected to Ollama")
            } else {
                OllamaConnectionResult(false, "Error: ${response.status.description}")
            }
        } catch (e: Exception) {
            OllamaConnectionResult(false, e.message)
        }
    }
}
