package com.ahmedyejam.mks.data.network

import android.net.Uri
import com.ahmedyejam.mks.util.copyToWithLimit
import com.ahmedyejam.mks.util.MksLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class RemoteAssetFetcher(
    private val clientFactory: (RemoteAssetPolicy) -> OkHttpClient = { policy ->
        OkHttpClient.Builder()
            .connectTimeout(policy.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(policy.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(policy.writeTimeoutSeconds, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
) {
    suspend fun fetch(url: String, policy: RemoteAssetPolicy = RemoteAssetPolicy.Default): RemoteAssetResult = withContext(Dispatchers.IO) {
        val parsed = runCatching { Uri.parse(url) }.getOrNull()
            ?: return@withContext RemoteAssetResult(error = "Invalid remote image URL.")
        val scheme = parsed.scheme?.lowercase()
        if (scheme !in listOf("http", "https")) {
            return@withContext RemoteAssetResult(error = "Only HTTP(S) image URLs are supported.")
        }
        if (scheme == "http" && !policy.allowPlainHttp) {
            return@withContext RemoteAssetResult(
                warning = "Plain HTTP image URL was not downloaded. Confirm insecure image downloads to import it.",
                plainHttpConsentRequired = true
            )
        }

        try {
            val request = Request.Builder().url(url).build()
            clientFactory(policy).newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext RemoteAssetResult(error = "Download failed with HTTP ${response.code}.")
                }
                val body = response.body ?: return@withContext RemoteAssetResult(error = "Remote image response body is empty.")
                val contentType = body.contentType()?.toString()
                if (policy.requireImageContentType && contentType != null && !contentType.startsWith("image/", ignoreCase = true)) {
                    return@withContext RemoteAssetResult(error = "Remote asset is not an image ($contentType).")
                }
                val contentLength = body.contentLength()
                if (contentLength > policy.maxBytes) {
                    return@withContext RemoteAssetResult(error = "Remote image exceeds ${policy.maxBytes / (1024 * 1024)} MB limit.")
                }
                val output = ByteArrayOutputStream()
                body.byteStream().use { input -> input.copyToWithLimit(output, policy.maxBytes) }
                RemoteAssetResult(
                    bytes = output.toByteArray(),
                    warning = if (scheme == "http") "Downloaded image over plain HTTP after user confirmation." else null,
                    contentType = contentType
                )
            }
        } catch (e: Exception) {
            MksLogger.w("RemoteAssetFetcher", "Remote asset download failed", e)
            RemoteAssetResult(error = e.message ?: "Failed to download remote image.")
        }
    }
}
