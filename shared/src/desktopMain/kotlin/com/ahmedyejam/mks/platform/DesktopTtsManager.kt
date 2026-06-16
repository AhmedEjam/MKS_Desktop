package com.ahmedyejam.mks.platform

import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

/**
 * Desktop (Linux) implementation of [TtsManager].
 *
 * Uses espeak-ng subprocess for speech synthesis.
 * Wrapped in withTimeoutOrNull to prevent JVM thread hanging
 * (espeak-ng can sometimes block on audio device contention).
 *
 * Outputs WAV to a temp file and plays via aplay.
 */
class DesktopTtsManager : TtsManager {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile override var isInitialized: Boolean = false
        private set
    @Volatile override var isPlaying: Boolean = false
        private set

    init {
        isInitialized = checkEspeakInstalled()
    }

    override fun play(text: String, pitch: Float, speechRate: Float) {
        stop()
        job = scope.launch {
            isPlaying = true
            try {
                withTimeoutOrNull(10_000L) {
                    val tempFile = File.createTempFile("mks_tts_", ".wav")
                    tempFile.deleteOnExit()

                    // Synthesize WAV with espeak-ng
                    val pitchVal = (pitch * 50).toInt().coerceIn(0, 99)
                    val speedVal = ((speechRate.coerceIn(0.5f, 2.0f) * 100) - 100).toInt()

                    val synthProcess = ProcessBuilder(
                        "espeak-ng",
                        "-w", tempFile.absolutePath,
                        "-p", pitchVal.toString(),
                        "-s", speedVal.toString(),
                        "--", text
                    )
                        .redirectErrorStream(true)
                        .start()

                    val synthResult = synthProcess.waitFor()
                    if (synthResult != 0) {
                        isPlaying = false
                        return@withTimeoutOrNull
                    }

                    // Play the WAV via aplay
                    val playProcess = ProcessBuilder(
                        "aplay", "-q", tempFile.absolutePath
                    )
                        .redirectErrorStream(true)
                        .start()

                    playProcess.waitFor()
                }
            } catch (_: Exception) {
                // Subprocess killed by timeout or interrupted
            } finally {
                isPlaying = false
            }
        }
    }

    override fun stop() {
        job?.cancel()
        isPlaying = false
    }

    override fun shutdown() {
        stop()
        scope.cancel()
        isInitialized = false
    }

    private fun checkEspeakInstalled(): Boolean {
        return try {
            val process = ProcessBuilder("which", "espeak-ng")
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        } catch (_: Exception) { false }
    }
}
