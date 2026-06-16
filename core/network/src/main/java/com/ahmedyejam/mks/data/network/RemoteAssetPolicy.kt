package com.ahmedyejam.mks.data.network

data class RemoteAssetPolicy(
    val allowPlainHttp: Boolean = false,
    val maxBytes: Long = 12L * 1024L * 1024L,
    val requireImageContentType: Boolean = true,
    val connectTimeoutSeconds: Long = 10,
    val readTimeoutSeconds: Long = 20,
    val writeTimeoutSeconds: Long = 10
) {
    companion object {
        val Default = RemoteAssetPolicy()
        val UserAllowedPlainHttp = RemoteAssetPolicy(allowPlainHttp = true)
    }
}

data class RemoteAssetResult(
    val bytes: ByteArray? = null,
    val warning: String? = null,
    val error: String? = null,
    val plainHttpConsentRequired: Boolean = false,
    val contentType: String? = null
)
