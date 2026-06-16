package com.ahmedyejam.mks.ui.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context, private val onInitError: () -> Unit = {}) {
    private var tts: TextToSpeech? = null
    var isInitialized = false
        private set
    var isPlaying = false
        private set

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isInitialized = true
            } else {
                Log.e("TtsManager", "TTS Initialization failed")
                onInitError()
            }
        }
    }

    fun play(text: String, pitch: Float = 1.0f, speechRate: Float = 1.0f) {
        if (!isInitialized) return
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speechRate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_article")
        isPlaying = true
    }

    fun stop() {
        tts?.stop()
        isPlaying = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        isPlaying = false
    }
}
