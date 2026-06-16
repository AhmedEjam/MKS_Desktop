package com.ahmedyejam.mks.platform

/**
 * Platform text-to-speech abstraction.
 *
 * Android: uses android.speech.tts.TextToSpeech.
 * Desktop: uses espeak-ng subprocess with 10-second timeout kill-switch.
 */
interface TtsManager {
    /** Whether the TTS engine is initialized and ready. */
    val isInitialized: Boolean

    /** Whether audio is currently playing. */
    val isPlaying: Boolean

    /**
     * Start speaking the given text.
     * @param text The text to speak.
     * @param pitch Voice pitch (0.5–2.0, default 1.0).
     * @param speechRate Speaking rate (0.5–2.0, default 1.0).
     */
    fun play(text: String, pitch: Float = 1.0f, speechRate: Float = 1.0f)

    /** Stop any ongoing speech. */
    fun stop()

    /** Release TTS resources. */
    fun shutdown()
}
