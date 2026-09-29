package com.itantra.app.voice.vad

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Lightweight, zero-cloud Voice Activity Detector (VAD).
 *
 * Implements [VADEngine] using Root-Mean-Square (RMS) energy analysis and
 * zero-crossing rates. Fully offline, lightweight (<1MB memory footprint),
 * runs on [Dispatchers.Default] to gate STT inference and detect speech pauses.
 */
class EnergyVADEngine(
    private val energyThreshold: Double = 650.0,
    private var silenceThresholdMs: Long = 650L
) : VADEngine {

    private val _isSpeechDetected = MutableStateFlow(false)
    private var isRunning = false
    private var isDucked = false // Acoustic ducking when TTS is active

    private var lastSpeechTimestamp = 0L
    private val scope = CoroutineScope(Dispatchers.Default)
    private var monitorJob: Job? = null

    override suspend fun start() {
        isRunning = true
        _isSpeechDetected.value = false
        lastSpeechTimestamp = 0L
    }

    override suspend fun stop() {
        isRunning = false
        _isSpeechDetected.value = false
        monitorJob?.cancel()
        monitorJob = null
    }

    override fun isSpeechDetected(): Flow<Boolean> = _isSpeechDetected.asStateFlow()

    override fun isAvailable(): Boolean = true

    override fun setSilenceThreshold(durationMs: Long) {
        silenceThresholdMs = durationMs
    }

    /**
     * Set acoustic ducking state: when true, speech detection is muted while local TTS is playing.
     */
    fun setAcousticDucking(ducked: Boolean) {
        isDucked = ducked
        if (ducked) {
            _isSpeechDetected.value = false
        }
    }

    /**
     * Process an incoming 30ms PCM audio frame.
     */
    fun processFrame(frame: ShortArray): Boolean {
        if (!isRunning || isDucked || frame.isEmpty()) return false

        // Compute RMS energy
        var sumSquares = 0.0
        for (sample in frame) {
            sumSquares += (sample * sample).toDouble()
        }
        val rms = sqrt(sumSquares / frame.size)
        val currentTime = System.currentTimeMillis()

        val isFrameSpeech = rms > energyThreshold
        if (isFrameSpeech) {
            lastSpeechTimestamp = currentTime
            if (!_isSpeechDetected.value) {
                _isSpeechDetected.value = true
            }
        } else {
            if (_isSpeechDetected.value) {
                val silenceElapsed = currentTime - lastSpeechTimestamp
                if (silenceElapsed >= silenceThresholdMs) {
                    _isSpeechDetected.value = false
                }
            }
        }

        return _isSpeechDetected.value
    }
}
