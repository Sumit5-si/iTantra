package com.itantra.app.voice.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.itantra.app.core.logging.ITantraLogger

/**
 * Real On-Device Speech Recognizer for iTantra.
 *
 * Uses Android's built-in [SpeechRecognizer] with [RecognizerIntent.EXTRA_PREFER_OFFLINE] set to true.
 * This ensures that spoken audio is transcribed 100% on the device with zero cloud API calls.
 *
 * Transcribes the actual voice spoken by the user in any of the 10 supported Indic languages:
 * hi-IN, gu-IN, mr-IN, ta-IN, te-IN, kn-IN, ml-IN, bn-IN, or-IN, en-IN.
 */
class AndroidOfflineSpeechRecognizer(
    private val context: Context
) {
    companion object {
        private const val TAG = "OfflineSpeechRec"

        val LANGUAGE_TAG_MAP = mapOf(
            "hi" to "hi-IN",
            "gu" to "gu-IN",
            "mr" to "mr-IN",
            "ta" to "ta-IN",
            "te" to "te-IN",
            "kn" to "kn-IN",
            "ml" to "ml-IN",
            "bn" to "bn-IN",
            "or" to "or-IN",
            "en" to "en-IN"
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastTranscribedText: String? = null

    private var onPartialCallback: ((String) -> Unit)? = null
    private var onFinalCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    init {
        mainHandler.post {
            initRecognizerOnMainThread()
        }
    }

    private fun initRecognizerOnMainThread() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null

            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
                ITantraLogger.i(TAG, "Native SpeechRecognizer successfully initialized")
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
                ITantraLogger.i(TAG, "Native On-Device SpeechRecognizer (API 33+) initialized")
            } else {
                ITantraLogger.w(TAG, "SpeechRecognizer is not available on this device")
            }
        } catch (e: Exception) {
            ITantraLogger.e(TAG, "Error initializing SpeechRecognizer: ${e.message}")
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                ITantraLogger.i(TAG, "SpeechRecognizer ready for speech")
            }

            override fun onBeginningOfSpeech() {
                ITantraLogger.i(TAG, "User started speaking...")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                ITantraLogger.i(TAG, "User stopped speaking")
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    else -> "Recognition error: $error"
                }
                ITantraLogger.w(TAG, "SpeechRecognizer error: $errorMsg (code=$error) | existingPartial='$lastTranscribedText'")
                isListening = false
                // If user already spoke words and partial results were captured, do not discard them!
                if (!lastTranscribedText.isNullOrBlank()) {
                    onFinalCallback?.invoke(lastTranscribedText!!)
                } else {
                    onErrorCallback?.invoke(errorMsg)
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim()
                ITantraLogger.i(TAG, "Final live transcription received: '$recognizedText'")
                if (!recognizedText.isNullOrEmpty()) {
                    lastTranscribedText = recognizedText
                    onFinalCallback?.invoke(recognizedText)
                } else {
                    onErrorCallback?.invoke("No words recognized")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = matches?.firstOrNull()?.trim()
                if (!partialText.isNullOrEmpty()) {
                    ITantraLogger.d(TAG, "Partial live transcription: '$partialText'")
                    lastTranscribedText = partialText
                    onPartialCallback?.invoke(partialText)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    /**
     * Start listening to user's real voice using offline recognition.
     */
    fun startListening(
        languageCode: String,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        lastTranscribedText = null
        onPartialCallback = onPartial
        onFinalCallback = onFinal
        onErrorCallback = onError

        mainHandler.post {
            try {
                // Cleanly destroy and recreate SpeechRecognizer before each session to avoid ERROR_RECOGNIZER_BUSY
                try {
                    speechRecognizer?.destroy()
                } catch (_: Exception) {}

                speechRecognizer = if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createListener())
                    }
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context).apply {
                        setRecognitionListener(createListener())
                    }
                } else {
                    null
                }

                if (speechRecognizer == null) {
                    ITantraLogger.w(TAG, "SpeechRecognizer is unavailable on this device")
                    onError.invoke("Speech recognition service not found on this device")
                    return@post
                }

                val targetTag = LANGUAGE_TAG_MAP[languageCode.lowercase()] ?: "hi-IN"

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetTag)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(targetTag, "hi-IN", "en-IN", "en-US"))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 450L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 450L)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
                ITantraLogger.i(TAG, "SpeechRecognizer started listening for language: $targetTag")
            } catch (e: Exception) {
                ITantraLogger.e(TAG, "Error starting SpeechRecognizer: ${e.message}")
                isListening = false
                onError.invoke(e.message ?: "Failed to start voice recognition")
            }
        }
    }

    /**
     * Stop listening when user releases PTT button.
     */
    fun stopListening() {
        mainHandler.post {
            try {
                if (isListening) {
                    speechRecognizer?.stopListening()
                    isListening = false
                    ITantraLogger.i(TAG, "SpeechRecognizer stopped listening")
                }
            } catch (e: Exception) {
                ITantraLogger.e(TAG, "Error stopping SpeechRecognizer: ${e.message}")
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                isListening = false
            } catch (_: Exception) {}
        }
    }

    fun getLastTranscription(): String? = lastTranscribedText

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                isListening = false
            } catch (_: Exception) {}
        }
    }
}
