package com.ahmedyejam.mks.platform

import com.ahmedyejam.mks.data.model.MksResult

/**
 * Platform-agnostic OCR (Optical Character Recognition) manager.
 */
interface OcrManager {
    suspend fun recognizeText(imageBytes: ByteArray): MksResult<String>
    fun isAvailable(): Boolean
}
