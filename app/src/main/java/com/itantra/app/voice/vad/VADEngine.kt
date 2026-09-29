package com.itantra.app.voice.vad

import kotlinx.coroutines.flow.Flow

/**
 * Voice Activity Detection engine interface.
 *
 * Phase 2 implementation: Silero VAD (ONNX Runtime Mobile, ~2MB INT8 model).
 *
 * Pipeline: Microphone → VAD → gates STT activation
 * VAD must be separate from STT — do not make STT detect speech boundaries.
 *
 * Per project rules:
 *  - Audio capture gated through lightweight VAD before triggering STT
 *  - Must run off main thread (Dispatchers.Default)
 *  - Pause VAD during TTS playback (acoustic ducking)
 */
interface VADEngine {

    /** Start the VAD engine, begin processing audio frames. */
    suspend fun start()

    /** Stop the VAD engine, release resources. */
    suspend fun stop()

    /** Observable speech detection state. */
    fun isSpeechDetected(): Flow<Boolean>

    /** Check if the VAD model is available on-device. */
    fun isAvailable(): Boolean

    /** Set speech end timeout in ms (silence duration to finalize segment). */
    fun setSilenceThreshold(durationMs: Long)
}
