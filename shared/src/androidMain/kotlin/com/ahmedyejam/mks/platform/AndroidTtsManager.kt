package com.ahmedyejam.mks.platform

import android.content.Context
import com.ahmedyejam.mks.ui.utils.TtsManager as LegacyTtsManager

/**
 * Android implementation of [TtsManager] — delegates to the existing
 * Android TtsManager which uses android.speech.tts.TextToSpeech.
 */
class AndroidTtsManager(context: Context, onError: () -> Unit = {}) : TtsManager {
    private val delegate = LegacyTtsManager(context, onError)

    override val isInitialized: Boolean get() = delegate.isInitialized
    override val isPlaying: Boolean get() = delegate.isPlaying

    override fun play(text: String, pitch: Float, speechRate: Float) {
        delegate.play(text, pitch, speechRate)
    }

    override fun stop() = delegate.stop()
    override fun shutdown() = delegate.shutdown()
}
