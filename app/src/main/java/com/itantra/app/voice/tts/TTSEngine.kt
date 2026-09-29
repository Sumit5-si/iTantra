package com.itantra.app.voice.tts

/**
 * Offline Text-to-Speech engine interface.
 *
 * Phase 2 implementation:
 *  - Primary: Android built-in TextToSpeech (offline language packs, zero model size)
 *  - Fallback: Sherpa-ONNX VITS TTS for unsupported languages
 *
 * Per project rules:
 *  - No cloud TTS APIs
 *  - Signal VAD to pause during playback (acoustic ducking)
 *  - Emergency alerts: route to AudioManager.STREAM_ALARM at max volume
 */
enum class VoiceGender {
    BOY, // Male voice
    GIRL // Female voice
}

interface TTSEngine {

    /** Initialize TTS for a specific language. */
    suspend fun init(languageCode: String)

    /** Set voice persona (Boy / Girl). */
    fun setVoiceGender(gender: VoiceGender)

    /** Get current voice persona. */
    fun getVoiceGender(): VoiceGender

    /** Speak the given text. */
    suspend fun speak(text: String)

    /** Stop current speech playback. */
    fun stop()

    /** Check if TTS is available for the given language. */
    fun isAvailable(languageCode: String): Boolean

    /** Release all TTS resources. */
    fun release()

    /** Check if currently speaking. */
    fun isSpeaking(): Boolean
}
