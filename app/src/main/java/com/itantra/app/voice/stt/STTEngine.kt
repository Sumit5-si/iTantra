package com.itantra.app.voice.stt

import kotlinx.coroutines.flow.Flow

/**
 * Offline Speech-to-Text engine interface.
 *
 * Phase 2 implementation: Sherpa-ONNX with INT8 quantized models per language.
 *
 * Per project rules:
 *  - FULLY OFFLINE: No Google Cloud Speech, Azure, AWS, or any cloud API
 *  - OPEN-SOURCE ONLY: Sherpa-ONNX or TFLite runtimes
 *  - INT8 quantized models exclusively (no FP32/FP16)
 *  - Models stored in app/src/main/assets/models/stt/
 *  - Only active language model loaded in memory
 *  - Load via MappedByteBuffer, not byte arrays
 *  - Run on Dispatchers.Default, never main thread
 */
interface STTEngine {

    /** Initialize the engine for a specific language. */
    suspend fun init(languageCode: String)

    /** Start speech recognition. */
    suspend fun start()

    /** Stop recognition and finalize. */
    suspend fun stop()

    /** Observable transcription results. */
    fun transcribe(): Flow<String>

    /** Check if a model is available for the given language. */
    fun isAvailable(languageCode: String): Boolean

    /** Release all resources and unload model. */
    suspend fun release()
}
