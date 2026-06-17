package com.ahmedyejam.mks.platform

import com.ahmedyejam.mks.data.model.MksResult

class DesktopOcrManager : OcrManager {
    override suspend fun recognizeText(imageBytes: ByteArray): MksResult<String> =
        MksResult.Error("OCR is not supported on Desktop yet.")

    override fun isAvailable(): Boolean = false
}
