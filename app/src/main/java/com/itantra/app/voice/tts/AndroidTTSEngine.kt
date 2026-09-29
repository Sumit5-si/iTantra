package com.itantra.app.voice.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Native Android offline Text-To-Speech engine.
 *
 * Implements [TTSEngine] using Android's built-in offline voice synthesis engines.
 * Zero-cloud, runs fully on-device, compliant with ISRO PS 26173 rules.
 */
class AndroidTTSEngine(
    private val context: Context
) : TTSEngine, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeakingActive = false
    private var currentLanguageCode: String = "hi"

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var currentVoiceGender: VoiceGender = VoiceGender.GIRL

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setLanguage(currentLanguageCode)
            applyVoiceParameters()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeakingActive = true
                }

                override fun onDone(utteranceId: String?) {
                    isSpeakingActive = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeakingActive = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    isSpeakingActive = false
                }
            })
        } else {
            isInitialized = false
        }
    }

    override fun setVoiceGender(gender: VoiceGender) {
        currentVoiceGender = gender
        applyVoiceParameters()
    }

    override fun getVoiceGender(): VoiceGender = currentVoiceGender

    private fun applyVoiceParameters() {
        if (!isInitialized || tts == null) return
        when (currentVoiceGender) {
            VoiceGender.BOY -> {
                tts?.setPitch(0.85f)      // Deeper, masculine acoustic pitch
                tts?.setSpeechRate(1.02f)  // Steady, natural cadence
                selectSystemVoice(isMale = true)
            }
            VoiceGender.GIRL -> {
                tts?.setPitch(1.22f)      // Clear, feminine acoustic pitch
                tts?.setSpeechRate(1.06f)  // Melodious, natural cadence
                selectSystemVoice(isMale = false)
            }
        }
    }

    private fun selectSystemVoice(isMale: Boolean) {
        try {
            val targetLocale = getLocaleForCode(currentLanguageCode)
            val voices = tts?.voices ?: return
            val matchingVoice = voices.firstOrNull { voice ->
                voice.locale.language.equals(targetLocale.language, ignoreCase = true) &&
                    if (isMale) {
                        voice.name.contains("male", ignoreCase = true) && !voice.name.contains("female", ignoreCase = true)
                    } else {
                        voice.name.contains("female", ignoreCase = true)
                    }
            }
            if (matchingVoice != null) {
                tts?.voice = matchingVoice
            }
        } catch (_: Exception) {}
    }

    override suspend fun init(languageCode: String) {
        currentLanguageCode = languageCode
        if (isInitialized) {
            setLanguage(languageCode)
            applyVoiceParameters()
        }
    }

    private fun setLanguage(languageCode: String) {
        val locale = getLocaleForCode(languageCode)
        tts?.language = locale
    }

    override suspend fun speak(text: String) {
        withContext(Dispatchers.Default) {
            if (!isInitialized || tts == null || text.isBlank()) return@withContext

            applyVoiceParameters()

            val params = Bundle()
            val utteranceId = "iTantra_${System.currentTimeMillis()}"

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)

            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    /**
     * Emergency alert speech synthesis.
     * Overrides silent mode, sets volume to maximum, and routes to STREAM_ALARM per project rules.
     */
    suspend fun speakEmergencyAlert(alertText: String) {
        withContext(Dispatchers.Default) {
            if (!isInitialized || tts == null) return@withContext

            // Maximize alarm stream volume
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
            }

            val alarmAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            tts?.setAudioAttributes(alarmAttributes)

            val params = Bundle()
            val utteranceId = "iTantra_EMERGENCY_${System.currentTimeMillis()}"
            tts?.speak(alertText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    override fun stop() {
        tts?.stop()
        isSpeakingActive = false
    }

    override fun isAvailable(languageCode: String): Boolean {
        if (!isInitialized || tts == null) return false
        val locale = getLocaleForCode(languageCode)
        val availability = tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        return availability >= TextToSpeech.LANG_AVAILABLE
    }

    override fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        isSpeakingActive = false
    }

    override fun isSpeaking(): Boolean = isSpeakingActive || (tts?.isSpeaking == true)

    private fun getLocaleForCode(code: String): Locale {
        return when (code.lowercase()) {
            "hi" -> Locale("hi", "IN")
            "gu" -> Locale("gu", "IN")
            "mr" -> Locale("mr", "IN")
            "ta" -> Locale("ta", "IN")
            "te" -> Locale("te", "IN")
            "kn" -> Locale("kn", "IN")
            "ml" -> Locale("ml", "IN")
            "bn" -> Locale("bn", "IN")
            "or" -> Locale("or", "IN")
            "en" -> Locale("en", "IN")
            else -> Locale("hi", "IN")
        }
    }
}
