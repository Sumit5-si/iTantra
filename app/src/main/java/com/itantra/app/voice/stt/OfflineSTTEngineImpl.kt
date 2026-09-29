package com.itantra.app.voice.stt

import android.content.Context
import com.itantra.app.core.logging.ITantraLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Offline STT Engine Implementation for iTantra.
 *
 * Captures real voice spoken by the user via [AndroidOfflineSpeechRecognizer].
 * Transcribes the speech in real-time and transmits the exact text payload across the link.
 * ZERO hardcoded/seeded fallback data.
 */
class OfflineSTTEngineImpl(
    private val context: Context,
    private val offlineRecognizer: AndroidOfflineSpeechRecognizer = AndroidOfflineSpeechRecognizer(context)
) : STTEngine {

    companion object {
        private const val TAG = "OfflineSTTEngine"
    }

    private val _transcriptionFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    private var isEngineRunning = false
    private var currentLanguageCode: String = "hi"
    private val scope = CoroutineScope(Dispatchers.Default)

    private var liveTranscribedText: String? = null

    override suspend fun init(languageCode: String) = withContext(Dispatchers.Default) {
        currentLanguageCode = languageCode.lowercase()
        isEngineRunning = true
        ITantraLogger.i(TAG, "Offline STT initialized for language: $currentLanguageCode")
    }

    override suspend fun start() = withContext(Dispatchers.Default) {
        startListening(currentLanguageCode)
    }

    override suspend fun stop() = withContext(Dispatchers.Default) {
        offlineRecognizer.stopListening()
    }

    override fun transcribe(): Flow<String> = _transcriptionFlow.asSharedFlow()

    override fun isAvailable(languageCode: String): Boolean {
        val supported = setOf("hi", "gu", "mr", "kn", "ml", "ta", "te", "or", "bn", "en")
        return languageCode.lowercase() in supported
    }

    override suspend fun release() = withContext(Dispatchers.Default) {
        isEngineRunning = false
        offlineRecognizer.destroy()
    }

    /**
     * Start live microphone listening when user presses PTT button.
     */
    fun startListening(languageCode: String, onPartialText: ((String) -> Unit)? = null) {
        liveTranscribedText = null
        currentLanguageCode = languageCode.lowercase()
        offlineRecognizer.startListening(
            languageCode = currentLanguageCode,
            onPartial = { partial ->
                liveTranscribedText = partial
                onPartialText?.invoke(partial)
            },
            onFinal = { finalResult ->
                liveTranscribedText = finalResult
                scope.launch {
                    _transcriptionFlow.emit(finalResult)
                }
            },
            onError = { err ->
                ITantraLogger.w(TAG, "Live speech recognition notice: $err")
            }
        )
    }

    /**
     * Stop listening and finalize the user's real spoken text.
     * Returns the exact words spoken by the user.
     * ZERO seeded or hardcoded data is returned if no speech is detected.
     */
    suspend fun stopAndGetTranscription(
        languageCode: String,
        holdDurationMs: Long = 0L
    ): String? = withContext(Dispatchers.Default) {
        offlineRecognizer.stopListening()

        // Adaptive latency: If speech was already captured via partial results while speaking,
        // wait at most 250ms for final polish. Otherwise wait up to 1800ms.
        val maxWaitMs = if (liveTranscribedText != null) 250 else 1800
        var waited = 0
        while (liveTranscribedText == null && waited < maxWaitMs) {
            delay(40)
            waited += 40
        }

        val realSpeech = liveTranscribedText
            ?: offlineRecognizer.getLastTranscription()

        val finalResult = realSpeech?.trim()?.ifEmpty { null }
        if (finalResult != null) {
            ITantraLogger.i(TAG, "Using real transcribed voice: '$finalResult'")
            _transcriptionFlow.emit(finalResult)
            return@withContext finalResult
        }

        ITantraLogger.i(TAG, "No real speech detected from microphone (Zero seeded data)")
        null
    }

    /**
     * Backward-compatible call for Phone mode chunk processing.
     */
    suspend fun processSpeechSegment(languageCode: String): String? {
        return stopAndGetTranscription(languageCode, 800L)
    }
}
