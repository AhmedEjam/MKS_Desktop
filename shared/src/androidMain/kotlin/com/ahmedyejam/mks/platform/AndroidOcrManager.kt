package com.ahmedyejam.mks.platform

import android.graphics.BitmapFactory
import com.ahmedyejam.mks.data.model.MksResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class AndroidOcrManager : OcrManager {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeText(imageBytes: ByteArray): MksResult<String> {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()
            MksResult.Success(visionText.text)
        } catch (e: Exception) {
            MksResult.Error(e.message ?: "OCR Failed")
        }
    }

    override fun isAvailable(): Boolean = true
}
