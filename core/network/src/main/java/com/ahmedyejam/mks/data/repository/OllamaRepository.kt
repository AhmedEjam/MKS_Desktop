package com.ahmedyejam.mks.data.repository

import android.util.Log
import com.ahmedyejam.mks.data.model.OllamaRequest
import com.ahmedyejam.mks.data.model.OllamaResponse
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class OllamaApiException(message: String) : Exception(message)

/**
 * Result of a connectivity test against an Ollama server.
 *
 * @param success whether the server responded successfully.
 * @param message a human-readable description of the outcome.
 * @param models the list of model names available on the server (empty on failure).
 * @param modelAvailable whether the configured model was found among [models] (null if not checked).
 */
data class OllamaConnectionResult(
    val success: Boolean,
    val message: String,
    val models: List<String> = emptyList(),
    val modelAvailable: Boolean? = null
)

@Singleton
class OllamaRepository @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // LLMs can take time to respond
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // A short-timeout client for quick metadata calls (connection test, model listing).
    private val quickClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .build()

    private val requestAdapter = moshi.adapter(OllamaRequest::class.java)
    private val responseAdapter = moshi.adapter(OllamaResponse::class.java)

    private fun normalizeBaseUrl(baseUrl: String): String {
        val formatted =
            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                "http://$baseUrl"
            } else {
                baseUrl
            }
        return formatted.trimEnd('/')
    }

    private fun Request.Builder.applyAuth(apiKey: String?): Request.Builder {
        if (!apiKey.isNullOrBlank()) {
            // Support both bare tokens and pre-formatted schemes (e.g. "Bearer xyz").
            val headerValue = if (apiKey.contains(' ')) apiKey else "Bearer $apiKey"
            header("Authorization", headerValue)
        }
        return this
    }

    fun generateCompletionStream(
        baseUrl: String,
        modelName: String,
        prompt: String,
        systemPrompt: String? = null,
        options: Map<String, Any>? = null,
        images: List<String>? = null,
        apiKey: String? = null
    ): Flow<String> = flow {
        val url = normalizeBaseUrl(baseUrl) + "/api/generate"
        val requestObj = OllamaRequest(
            model = modelName, 
            prompt = prompt, 
            system = systemPrompt, 
            stream = true,
            options = options,
            images = images
        )
        val jsonPayload = requestAdapter.toJson(requestObj)
        
        val body = jsonPayload.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .applyAuth(apiKey)
            .post(body)
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    if (response.code == 401 || response.code == 403) {
                        throw OllamaApiException("Authentication failed (HTTP ${response.code}). Check your API key.")
                    }
                    if (errorBody != null && errorBody.contains("\"error\"")) {
                        try {
                            val json = JSONObject(errorBody)
                            val errMsg = json.optString("error")
                            if (errMsg.isNotBlank()) {
                                throw OllamaApiException(errMsg)
                            }
                        } catch (_: Exception) {}
                    }
                    throw IOException("HTTP Error: ${response.code} - ${response.message}\n$errorBody")
                }
                
                val bodyStream = response.body?.byteStream() ?: throw IOException("Empty response body")
                val reader = BufferedReader(InputStreamReader(bodyStream))
                
                reader.useLines { lines ->
                    for (line in lines) {
                        currentCoroutineContext().ensureActive()
                        
                        if (line.isNotBlank()) {
                            if (line.contains("\"error\"")) {
                                try {
                                    val json = JSONObject(line)
                                    val errMsg = json.optString("error")
                                    if (errMsg.isNotBlank()) {
                                        throw OllamaApiException(errMsg)
                                    }
                                } catch (_: Exception) {}
                            }
                            
                            try {
                                val ollamaResponse = responseAdapter.fromJson(line)
                                if (ollamaResponse != null) {
                                    emit(ollamaResponse.response)
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                Log.e("OllamaRepository", "Failed to parse stream chunk: $line", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (e is SocketTimeoutException) {
                throw OllamaApiException("Connection timed out. The AI model is taking too long to respond.")
            }
            throw e
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Retrieves the list of model names installed on the Ollama server via `/api/tags`.
     * Throws [OllamaApiException] on authentication or server errors.
     */
    suspend fun listModels(baseUrl: String, apiKey: String? = null): List<String> =
        withContext(Dispatchers.IO) {
            val url = normalizeBaseUrl(baseUrl) + "/api/tags"
            val request = Request.Builder()
                .url(url)
                .applyAuth(apiKey)
                .get()
                .build()
            try {
                quickClient.newCall(request).execute().use { response ->
                    if (response.code == 401 || response.code == 403) {
                        throw OllamaApiException("Authentication failed (HTTP ${response.code}). Check your API key.")
                    }
                    if (!response.isSuccessful) {
                        throw IOException("HTTP Error: ${response.code} - ${response.message}")
                    }
                    val bodyString =
                        response.body?.string() ?: throw IOException("Empty response body")
                    val json = JSONObject(bodyString)
                    val modelsArray =
                        json.optJSONArray("models") ?: return@withContext emptyList<String>()
                    val result = mutableListOf<String>()
                    for (i in 0 until modelsArray.length()) {
                        val name = modelsArray.optJSONObject(i)?.optString("name")
                        if (!name.isNullOrBlank()) result.add(name)
                    }
                    result
                }
            } catch (e: OllamaApiException) {
                throw e
            } catch (e: SocketTimeoutException) {
                throw OllamaApiException("Connection timed out. Ensure the server is reachable.")
            }
        }

    /**
     * Performs a lightweight connectivity check against the Ollama server and verifies
     * whether [modelName] is available. Never throws; returns a structured result instead.
     */
    suspend fun testConnection(
        baseUrl: String,
        modelName: String? = null,
        apiKey: String? = null
    ): OllamaConnectionResult = withContext(Dispatchers.IO) {
        try {
            val models = listModels(baseUrl, apiKey)
            if (modelName.isNullOrBlank()) {
                OllamaConnectionResult(
                    success = true,
                    message = "Connected. ${models.size} model(s) available.",
                    models = models
                )
            } else {
                // Ollama tags are usually "name:tag"; match exact or base name.
                val available = models.any {
                    it == modelName || it.substringBefore(':') == modelName.substringBefore(':')
                }
                val message = if (available) {
                    "Connected. Model \"$modelName\" is available."
                } else {
                    "Connected, but model \"$modelName\" was not found. Available: ${
                        models.joinToString(
                            ", "
                        ).ifBlank { "none" }
                    }"
                }
                OllamaConnectionResult(
                    success = true,
                    message = message,
                    models = models,
                    modelAvailable = available
                )
            }
        } catch (e: OllamaApiException) {
            OllamaConnectionResult(success = false, message = e.message ?: "Connection failed.")
        } catch (e: Exception) {
            OllamaConnectionResult(
                success = false,
                message = "Connection failed: ${e.message}. Ensure Ollama is running and the URL is correct."
            )
        }
    }
}
