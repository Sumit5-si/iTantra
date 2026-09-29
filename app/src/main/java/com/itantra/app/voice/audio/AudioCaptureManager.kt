package com.itantra.app.voice.audio

import kotlinx.coroutines.flow.Flow

/**
 * Audio capture manager for microphone input.
 *
 * Phase 2 implementation: AudioRecord with PCM 16-bit, 16kHz mono.
 *
 * Per project rules:
 *  - Raw audio NEVER transmitted — only processed locally through VAD → STT
 *  - Run off main thread (Dispatchers.Default)
 *  - Release immediately when not in use
 *  - 30ms frame processing for VAD
 */
interface AudioCaptureManager {

    /** Start audio capture from microphone. */
    suspend fun start()

    /** Stop audio capture and release microphone. */
    suspend fun stop()

    /** Observable audio frames (PCM 16-bit, 16kHz mono). */
    fun audioFrames(): Flow<ShortArray>

    /** Check if microphone is available. */
    fun isAvailable(): Boolean

    /** Check if currently capturing. */
    fun isCapturing(): Boolean
}
